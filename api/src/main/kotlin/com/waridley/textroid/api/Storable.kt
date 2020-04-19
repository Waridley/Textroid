package com.waridley.textroid.api

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonValue
import kotlin.reflect.KCallable
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.jvmErasure

abstract class Storable<
		T: Storable<T, I, S>,
		I: StorageId<T, I, S, *>,
		S: StorageInterface<T, I>>(@JsonValue open val id: I, open val storage: S, val type: Class<T>) {
	
	@PublishedApi internal inline operator fun <reified T> get(path: String): T = readAttribute<T>(path).unwrap()
	inline operator fun <reified T> get(property: KProperty<T>): T =
			this[property.path.relativeTo(type)]
	
	@PublishedApi internal inline fun <reified T> readAttribute(path: String): MaybeAttribute<T> =
			storage.readAttribute(id, path, T::class.java)
			?: throw AttributeException("Failed to get value of attribute \"$path\" for $this")
	inline fun <reified T> readAttribute(property: KProperty<T>): MaybeAttribute<T> = readAttribute(property.path.relativeTo(type))
	
	@PublishedApi internal inline fun <reified T> readUnique(path: String): MaybeAttribute<T> =
			storage.readUnique(id, path, T::class.java)
			?: throw AttributeException("Failed to get value of unique attribute \"$path\" for $this")
	inline fun <reified T> readUnique(property: KProperty<T>): MaybeAttribute<T> = readUnique(property.path.relativeTo(type))
	
	
	
	@PublishedApi internal operator fun <T> set(path: String, value: T) = writeAttribute(path stores value)
	operator fun <T> set(property: KProperty<T>, value: T) =
			set(property.path.relativeTo(type), value)
	
	@PublishedApi internal fun clear(path: String) = writeAttribute(path.undefined)
	fun clear(property: KProperty<*>) = clear(property.path.relativeTo(type))
	
	@PublishedApi internal fun <T> writeAttribute(attribute: MaybeAttribute<T>) =
			storage.writeAttribute(id, attribute) ?: throw AttributeAssignmentException(id, attribute)
	
	@PublishedApi internal fun <T> writeUnique(attribute: MaybeAttribute<T>) =
			storage.writeUnique(id, attribute) ?: throw AttributeAssignmentException(id, attribute)
	
	override fun toString(): String {
		return id.toString()
	}
	
}

@JsonIgnoreProperties(ignoreUnknown = true)
interface StorageId<
		T: Storable<T, I, S>,
		I: StorageId<T, I, S, Q>,
		S: StorageInterface<T, I>,
		Q> {
	val _id: Q
	
	infix fun storedIn(storage: S): T
}

open class Storage<T> @PublishedApi internal constructor(open val path: String) {
	var init: (Storable<*, *, *>.() -> T?)? = null
	fun init(initializer: (Storable<*, *, *>.() -> T?)?): Storage<T> { this.init = initializer; return this }
	
	inline operator fun <reified G: T> getValue(thisRef: Storable<*, *, *>, property: KProperty<*>): G  {
		return thisRef.readAttribute<G>(path.relativeTo(thisRef.javaClass)).orElse {
			init?.let {
				setValue(thisRef, property, thisRef.it() as G)
			} ?: throw AttributeException("Couldn't read value of $property for $thisRef")
		}
	}
	inline operator fun <reified S: T> setValue(thisRef: Storable<*, *, *>, property: KProperty<*>, value: S): S? {
		return thisRef.set(path.relativeTo(thisRef.javaClass), value).orNull()
	}
}

open class UniqueStorage<UT> @PublishedApi internal constructor(val path: String) {
	var init: (Storable<*, *, *>.() -> UT?)? = null
	fun init(initializer: (Storable<*, *, *>.() -> UT?)?): UniqueStorage<UT> { this.init = initializer; return this }
	
	inline operator fun <reified G: UT> getValue(thisRef: Storable<*, *, *>, property: KProperty<*>): G {
		return thisRef.readUnique<G>(path.relativeTo(thisRef.javaClass)).orElse {
			init?.let {
				setValue(thisRef, property, thisRef.it() as G)
			} ?: throw AttributeException("Couldn't read value of $property for $thisRef")
		}
	}
	
	inline operator fun <reified S: UT> setValue(thisRef: Storable<*, *, *>, property: KProperty<*>, value: S): S? {
		return thisRef.writeUnique(path.relativeTo(thisRef.javaClass) stores value).orNull()
	}
	
}

open class ForeignStorage<
		FT: Storable<FT, FI, FS>,
		FI: StorageId<FT, FI, FS, *>,
		FS: StorageInterface<FT, FI>> @PublishedApi internal constructor(val path: String, val foreignStorage: FS) {
	var init: (Storable<*, *, *>.() -> FT?)? = null
	fun init(initializer: (Storable<*, *, *>.() -> FT?)?): ForeignStorage<FT, FI, FS> {
		this.init = initializer; return this
	}
	
	inline operator fun <reified G: FT> getValue(thisRef: Storable<*, *, *>, property: KProperty<*>): FT? {
		val key = thisRef.readUnique<G>(path.relativeTo(thisRef.javaClass)).orElse {
			init?.let {
				setValue(thisRef, property, thisRef.it() as G)
			} ?: throw AttributeException("Couldn't read value for $property")
		}.id
		return foreignStorage[key] ?: throw AttributeException("Couldn't get $property for $thisRef from foreign storage $foreignStorage")
	}
	
	inline operator fun <reified S: FT> setValue(thisRef: Storable<*, *, *>, property: KProperty<*>, value: S): S? {
		return thisRef.writeUnique(path.relativeTo(thisRef.javaClass) stores value).orNull()
	}
	
}



inline fun <reified T> storage(key: KCallable<T>) = Storage<T>(key.path)
inline fun <reified T> storage(key: Class<T>) = Storage<T>(key.path)
inline fun <reified T> storage() = storage(T::class.java)
inline fun <reified T> storage(key: KCallable<T>, noinline initializer: Storable<*, *, *>.() -> T) =
		Storage<T>(key.path).init(initializer)



inline fun <reified T> uniqueStorage(key: KCallable<T>) = UniqueStorage<T>(key.path)
inline fun <reified T> uniqueStorage(key: Class<T>) = UniqueStorage<T>(key.path)
inline fun <reified T> uniqueStorage() = uniqueStorage(T::class.java)
inline fun <reified T> uniqueStorage(key: KCallable<T>, noinline initializer: Storable<*, *, *>.() -> T) =
		UniqueStorage<T>(key.path).init(initializer)

inline fun <reified T: Storable<T, I, S>,
		I: StorageId<T, I, S, *>,
		S: StorageInterface<T, I>>
		foreignKey(key: KCallable<T>, foreignStorage: S)= ForeignStorage(key.path, foreignStorage)

inline fun <reified T: Storable<T, I, S>,
		I: StorageId<T, I, S, *>,
		S: StorageInterface<T, I>>
		foreignKey(key: Class<T>, foreignStorage: S) = ForeignStorage(key.path, foreignStorage)
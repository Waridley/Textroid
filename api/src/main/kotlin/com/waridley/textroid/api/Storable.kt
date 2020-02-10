package com.waridley.textroid.api

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import kotlin.reflect.KCallable
import kotlin.reflect.KProperty

abstract class Storable<T, I: StorageId<T>>(open val id: I, open val storage: StorageInterface<T, I>) {
	
	@PublishedApi internal inline operator fun <reified T> get(path: String): T = readAttribute<T>(path).unwrap()
	
	@PublishedApi internal inline fun <reified T> readAttribute(path: String): MaybeAttribute<T> =
			storage.readAttribute(id, path, T::class.java)
			?: throw AttributeException("Failed to get value for attribute \"$path\"")
	
	@PublishedApi internal inline fun <reified T> readUnique(path: String): MaybeAttribute<T> =
			storage.readUnique(id, path, T::class.java)
			?: throw AttributeException("Failed to get value for unique attribute \"$path\"")
	
	
	
	@PublishedApi internal operator fun <T> set(path: String, value: T) = writeAttribute(path stores value)
	
	@PublishedApi internal fun clear(path: String) = writeAttribute(path.undefined)
	
	@PublishedApi internal fun <T> writeAttribute(attribute: MaybeAttribute<T>) =
			storage.writeAttribute(id, attribute) ?: throw AttributeAssignmentException(id, attribute)
	
	@PublishedApi internal fun <T> writeUnique(attribute: MaybeAttribute<T>) =
			storage.writeUnique(id, attribute) ?: throw AttributeAssignmentException(id, attribute)
	
}

@JsonIgnoreProperties(ignoreUnknown = true)
interface StorageId<T> {
	val _id: Any
}

open class Storage<T> @PublishedApi internal constructor(open val path: String) {
	var init: (Storable<*, *>.() -> T?)? = null
	fun init(initializer: (Storable<*, *>.() -> T?)?): Storage<T> { this.init = initializer; return this }
	
	inline operator fun <reified G: T> getValue(thisRef: Storable<*, *>, property: KProperty<*>): G  {
		return thisRef.readAttribute<G>(path).orElse {
			init?.let {
				setValue(thisRef, property, thisRef.it() as G)
			} ?: throw AttributeException("Couldn't read value for $property")
		}
	}
	inline operator fun <reified S: T> setValue(thisref: Storable<*, *>, property: KProperty<*>, value: S): S? {
		return thisref.set(path, value).orNull()
	}
}

open class UniqueStorage<UT> @PublishedApi internal constructor(val path: String) {
	var init: (Storable<*, *>.() -> UT?)? = null
	fun init(initializer: (Storable<*, *>.() -> UT?)?): UniqueStorage<UT> { this.init = initializer; return this }
	
	inline operator fun <reified G: UT> getValue(thisRef: Storable<*, *>, property: KProperty<*>): G {
		return thisRef.readUnique<G>(path).orElse {
			init?.let {
				setValue(thisRef, property, thisRef.it() as G)
			} ?: throw AttributeException("Couldn't read value for $property")
		}
	}
	
	inline operator fun <reified S: UT> setValue(thisRef: Storable<*, *>, property: KProperty<*>, value: S): S? {
		return thisRef.writeUnique(path stores value).orNull()
	}
	
}

open class ForeignStorage<FT: Storable<FT, I>, I: StorageId<FT>> @PublishedApi internal constructor(val path: String, val foreignStorage: StorageInterface<FT, I>) {
	var init: (Storable<*, *>.() -> FT?)? = null
	fun init(initializer: (Storable<*, *>.() -> FT?)?): ForeignStorage<FT, I> {
		this.init = initializer; return this
	}
	
	inline operator fun <reified G: FT> getValue(thisRef: Storable<*, *>, property: KProperty<*>): FT? {
		val key = thisRef.readUnique<G>(path).orElse {
			init?.let {
				setValue(thisRef, property, thisRef.it() as G)
			} ?: throw AttributeException("Couldn't read value for $property")
		}.id
		return foreignStorage[key] ?: throw AttributeException("Couldn't get $property from foreign storage $foreignStorage")
	}
	
	inline operator fun <reified S: FT> setValue(thisRef: Storable<*, *>, property: KProperty<*>, value: S): S? {
		return thisRef.writeUnique(path stores value).orNull()
	}
	
}



inline fun <reified T> storage(receiver: KCallable<T>) = Storage<T>(receiver.path)
inline fun <reified T> storage(receiver: Class<T>) = Storage<T>(receiver.path)
inline fun <reified T> storage() = storage(T::class.java)
inline fun <reified T> storage(receiver: KCallable<T>, noinline initializer: Storable<*, *>.() -> T) =
		Storage<T>(receiver.path).init(initializer)



inline fun <reified T> uniqueStorage(receiver: KCallable<T>) = UniqueStorage<T>(receiver.path)
inline fun <reified T> uniqueStorage(receiver: Class<T>) = UniqueStorage<T>(receiver.path)
inline fun <reified T> uniqueStorage() = uniqueStorage(T::class.java)
inline fun <reified T> uniqueStorage(receiver: KCallable<T>, noinline initializer: Storable<*, *>.() -> T) =
		UniqueStorage<T>(receiver.path).init(initializer)

inline fun <reified T: Storable<T, I>, I: StorageId<T>> foreignKey(receiver: KCallable<T>, foreignStorage: StorageInterface<T, I>) =
		ForeignStorage(receiver.path, foreignStorage)
inline fun <reified T: Storable<T, I>, I: StorageId<T>> foreignKey(receiver: Class<T>, foreignStorage: StorageInterface<T, I>) =
		ForeignStorage(receiver.path, foreignStorage)
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
	
	inline operator fun <reified S: UT> setValue(player: Storable<*, *>, property: KProperty<*>, value: S): S? {
		return player.writeUnique(path stores value).orNull()
	}
	
}


inline fun <reified A> storage(receiver: KCallable<A>) = Storage<A>(receiver.path)
inline fun <reified B> storage(receiver: Class<B>) = Storage<B>(receiver.path)
inline fun <reified C> storage() = storage(C::class.java)
inline fun <reified D> storage(receiver: KCallable<D>, noinline initializer: Storable<*, *>.() -> D) =
		Storage<D>(receiver.path).init(initializer)



inline fun <reified UA> uniqueStorage(receiver: KCallable<UA>) = UniqueStorage<UA>(receiver.path)
inline fun <reified UB> uniqueStorage(receiver: Class<UB>) = UniqueStorage<UB>(receiver.path)
inline fun <reified UC> uniqueStorage() = uniqueStorage(UC::class.java)
inline fun <reified UD> uniqueStorage(receiver: KCallable<UD>, noinline initializer: Storable<*, *>.() -> UD) =
		UniqueStorage<UD>(receiver.path).init(initializer)
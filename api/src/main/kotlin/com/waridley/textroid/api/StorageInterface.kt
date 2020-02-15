package com.waridley.textroid.api

import kotlin.reflect.KProperty

interface StorageInterface<T: Storable<T, I, *>, I: StorageId<T, I, *, *>> {
	fun new(key: Attribute<*>): T
	operator fun get(id: I): T?
	operator fun get(attribute: MaybeAttribute<*>): Iterable<T>
	fun <T> readAttribute(id: I, path: String, type: Class<T>): MaybeAttribute<T>?
	fun <T> readUnique(id: I, path: String, type: Class<T>): MaybeAttribute<T>?
	fun <T> writeAttribute(id: I, attribute: MaybeAttribute<T>): MaybeAttribute<T?>?
	fun <T> writeUnique(id: I, attribute: MaybeAttribute<T>): MaybeAttribute<T?>?
	
	fun findOne(uniqueAttribute: Attribute<*>): T? =
			get(uniqueAttribute).apply { if(count() > 1) throw StorableNotFoundException("Found ${count()} players, expected 1") }
					.firstOrNull()
	
	fun findOrCreateOne(key: Attribute<*>, setOnInsert: List<Attribute<*>>): T
	
	
}

inline fun <reified R: Storable<R, I, *>, reified T, I: StorageId<R, I, *, *>> StorageInterface<R, I>.readAttribute(id: I, property: KProperty<T>): MaybeAttribute<T>? {
	return readAttribute(id, property.path.relativeTo<R>(), T::class.java)
}

inline fun <reified R: Storable<R, I, *>, reified T, I: StorageId<R, I, *, *>> StorageInterface<R, I>.readUnique(id: I, property: KProperty<T>): MaybeAttribute<T>? {
	return readUnique(id, property.path.relativeTo<R>(), T::class.java)
}

data class StorableCreationException(val reason: Any? = null, override val cause: Throwable? = null) : Exception(cause)
data class StorableNotFoundException(val reason: Any? = null, override val cause: Throwable? = null) : Exception(cause)
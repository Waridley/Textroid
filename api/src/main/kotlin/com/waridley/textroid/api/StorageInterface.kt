package com.waridley.textroid.api

import kotlin.reflect.KProperty

interface StorageInterface<C> {
	operator fun get(id: StorageId<C>): C?
	operator fun get(attribute: Attribute<*>): Iterable<C>
	fun <T> readAttribute(id: StorageId<C>, path: String, type: Class<T>): MaybeAttribute<T>?
	fun <T> readUnique(id: StorageId<C>, path: String, type: Class<T>): MaybeAttribute<T>?
	fun <T> writeAttribute(id: StorageId<C>, attribute: MaybeAttribute<T>): MaybeAttribute<T?>?
	fun <T> writeUnique(id: StorageId<C>, attribute: MaybeAttribute<T>): MaybeAttribute<T?>?
}

/** calls `readAttribute(id, property.name, T::class.java)` */
inline fun <reified T, C> StorageInterface<C>.readAttribute(id: StorageId<C>, property: KProperty<T>): MaybeAttribute<T>? {
	return readAttribute(id, property.name, T::class.java)
}

/** calls `readUnique(id, property.name, T::class.java)` */
inline fun <reified T, C> StorageInterface<C>.readUnique(id: StorageId<C>, property: KProperty<T>): MaybeAttribute<T>? {
	return readUnique(id, property.name, T::class.java)
}
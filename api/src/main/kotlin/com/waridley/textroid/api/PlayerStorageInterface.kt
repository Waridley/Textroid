@file:Suppress("UNUSED")

package com.waridley.textroid.api

import kotlin.reflect.KProperty

interface PlayerStorageInterface {
	
	/** calls `new(Player.Name(username))` */
	fun new(username: String) = new(Player.Name(username))
	
	/**
	 * Returns a new player with the provided username.
	 * @throws PlayerCreationException if it is impossible to generate a new player.
	 */
	fun new(username: Player.Name): Player
	
	/** @return The player with the given ID if it is stored in the backend, or null otherwise. */
	operator fun get(id: PlayerId): Player?
	
	/** calls `get(Player.Name(username))` */
	operator fun get(username: String) = get(Player.Name(username))
	
	/** @return The player with the given username if it is stored in this storage interface, or null otherwise. */
	operator fun get(username: Player.Name) = this["username" stores username].firstOrNull()
	
	/** @return A list of players that match the given attribute */
	operator fun <T> get(attribute: Attribute<T>): Iterable<Player>
	
	/** @return The value stored in the given path if it exited for the given player, Undefined if it was missing for the player, or null if the player was not found. */
	fun <T> readAttribute(id: PlayerId, path: String, type: Class<T>): MaybeAttribute<T>?
	
	/**
	 * Unique paths can be missing for multiple players, but values cannot be duplicated.
	 * @return The value stored in the given path if it exits for the given player, Undefined if it is missing for the player, or null if the player was not found.
	 */
	fun <T> readUnique(id: PlayerId, path: String, type: Class<T>): MaybeAttribute<T>?
	
	/**
	 * Stores the given attribute for the player, clearing it if `attribute` is Undefined.
	 * @return The previous value stored in the given path if it exited for the given player, Undefined if it was missing for the player, or null if the player was not found.
	 */
	fun <T> writeAttribute(id: PlayerId, attribute: MaybeAttribute<T>): MaybeAttribute<Any?>?
	
	/**
	 * Unique paths can be missing for multiple players, but values cannot be duplicated.
	 * @return The previous value stored in the given path if it exited for the given player, Undefined if it was missing for the player, or null if the player was not found.
	 */
	fun <T> writeUnique(id: PlayerId, attribute: MaybeAttribute<T>): MaybeAttribute<Any?>?
}

/** calls `readAttribute(id, property.name, T::class.java)` */
inline fun <reified T> PlayerStorageInterface.readAttribute(id: PlayerId, property: KProperty<T>): MaybeAttribute<T>? {
	return readAttribute(id, property.name, T::class.java)
}

/** calls `readUnique(id, property.name, T::class.java)` */
inline fun <reified T> PlayerStorageInterface.readUnique(id: PlayerId, property: KProperty<T>): MaybeAttribute<T>? {
	return readUnique(id, property.name, T::class.java)
}

data class PlayerCreationException(val reason: Any? = null, override val cause: Throwable? = null) : Exception(cause)
@file:Suppress("UNUSED")

package com.waridley.textroid.api

import kotlin.reflect.KProperty

interface PlayerStorageInterface: StorageInterface<Player> {
	
	/** calls `new(Player.Name(username))` */
	fun new(username: String, nickname: String = username) = new(Player.Name(username))
	
	/**
	 * Returns a new player with the provided username.
	 * @throws PlayerCreationException if it is impossible to generate a new player.
	 */
	fun new(username: Player.Name, nickname: Player.Name? = null): Player
	
	/** @return The player with the given ID if it is stored in the backend, or null otherwise. */
//	operator fun get(id: PlayerId): Player?
	
	/** calls `get(Player.Name(username))` */
	operator fun get(username: String) = get(Player.Name(username))
	
	/** @return The player with the given username if it is stored in this storage interface, or null otherwise. */
	operator fun get(username: Player.Name) = findOne("username" stores username)
	
	/** @return A list of players that match the given attribute */
//	operator fun get(attribute: Attribute<*>): Iterable<Player>
	
	fun findOne(uniqueAttribute: Attribute<*>): Player? =
			get(uniqueAttribute).apply { if(count() > 1) throw PlayerNotFoundException("Found ${count()} players, expected 1") }.firstOrNull()
	
	fun findOrCreateOne(username: Player.Name, nickName: Player.Name? = null): Player =
			get(username) ?: new(username, nickName)
	
	fun findOrCreateOne(uniqueAttribute: Attribute<*>, username: Player.Name, nickname: Player.Name? = null): Player =
			findOne(uniqueAttribute)?: findOrCreateOne(username, nickname).apply { writeUnique(uniqueAttribute) }
	
	fun findOrCreateOne(uniqueAttribute: Attribute<*>, username: String, nickName: String? = null): Player =
			findOrCreateOne(uniqueAttribute, Player.Name(username), nickName?.let { Player.Name(it) })
	
	/** @return The value stored in the given path if it exited for the given player, Undefined if it was missing for the player, or null if the player was not found. */
//	fun <T> readAttribute(id: PlayerId, path: String, type: Class<T>): MaybeAttribute<T>?
	
	/**
	 * Unique paths can be missing for multiple players, but values cannot be duplicated.
	 * @return The value stored in the given path if it exits for the given player, Undefined if it is missing for the player, or null if the player was not found.
	 */
//	fun <T> readUnique(id: PlayerId, path: String, type: Class<T>): MaybeAttribute<T>?
	
	/**
	 * Stores the given attribute for the player, clearing it if `attribute` is Undefined.
	 * @return The previous value stored in the given path if it exited for the given player, Undefined if it was missing for the player, or null if the player was not found.
	 */
//	fun <T> writeAttribute(id: PlayerId, attribute: MaybeAttribute<T>): MaybeAttribute<T?>?
	
	/**
	 * Unique paths can be missing for multiple players, but values cannot be duplicated.
	 * @return The previous value stored in the given path if it exited for the given player, Undefined if it was missing for the player, or null if the player was not found.
	 */
//	fun <T> writeUnique(id: PlayerId, attribute: MaybeAttribute<T>): MaybeAttribute<T?>?
}

data class PlayerCreationException(val reason: Any? = null, override val cause: Throwable? = null) : Exception(cause)
data class PlayerNotFoundException(val reason: Any? = null, override val cause: Throwable? = null) : Exception(cause)
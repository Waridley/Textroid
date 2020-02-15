package com.waridley.textroid.api

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonValue
import org.litote.kmongo.Id

@Suppress("UNUSED")
class Player(@JsonValue override val id: PlayerId, override val storage: PlayerStorageInterface): Storable<Player, PlayerId, PlayerStorageInterface>(id, storage, Player::class.java) {
	
	var username: Name by uniqueStorage(::username)
	
	var nickname: Name
		get() = readAttribute<Name>(::nickname.path).orNull() ?: username
		set(value) { this[::nickname.path] = value }
	
	
	data class Name(@JsonValue val value: String) {
		init {
			value.validate()
		}
		
		companion object {
			private fun String.validate() {
				fun err(reason: String): Nothing = throw InvalidUsernameException(
						reason,
						this
				)
				when {
					isBlank()                    -> err("Blank")
					matches("^\\s+.+".toRegex()) -> err("Starts with whitespace")
					matches(".+\\s+$".toRegex()) -> err("Ends with whitespace")
					length > 50                  -> err("Too long")
					else                         -> LOG.trace("Accepting player name: $this | Don't forget to add more rules!")
				}
			}
		}
		
		override fun toString(): String {
			return value
		}
	}
	
}

@JsonIgnoreProperties(ignoreUnknown = true)
inline class PlayerId(override val _id: Id<Player>): StorageId<Player, PlayerId, PlayerStorageInterface, Id<Player>> {
	override fun storedIn(storage: PlayerStorageInterface): Player {
		return Player(this, storage)
	}
}

fun Id<Player>.toPlayerId() = PlayerId(this)

val String.asPlayerName get() = Player.Name(this)

data class InvalidUsernameException(val reason: String? = null, val input: Any? = null): Exception("$reason: $input")

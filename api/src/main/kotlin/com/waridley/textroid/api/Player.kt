package com.waridley.textroid.api

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonValue
import org.litote.kmongo.Id

@Suppress("UNUSED")
class Player(@JsonValue override val id: PlayerId, override val storage: PlayerStorageInterface): Storable<Player, PlayerId>(id, storage) {
	
	var username: Name by uniqueStorage(::username)
	
	var nickname: Name
		get() = readAttribute<Name>("nickname").orNull() ?: username
		set(value) { this["nickname"] = value }
	
	
	override fun toString() = id.toString()
	
	
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
					else                         -> println("Accepting player name: $this | Don't forget to add more rules!")
				}
			}
		}
		
		override fun toString(): String {
			return value
		}
	}
}

@JsonIgnoreProperties(ignoreUnknown = true)
inline class PlayerId(override val _id: Id<Player>): StorageId<Player>

infix fun PlayerId?.storedIn(storage: PlayerStorageInterface) = this?.let { storage[it] }
fun Id<Player>.toPlayerId() = PlayerId(this)

data class InvalidUsernameException(val reason: String? = null, val input: Any? = null): Exception("$reason: $input")

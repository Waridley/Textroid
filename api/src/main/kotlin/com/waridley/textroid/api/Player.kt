package com.waridley.textroid.api

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonValue
import org.litote.kmongo.Id
import kotlin.reflect.*

@Suppress("UNUSED")
class Player(@JsonValue val id: PlayerId, val storage: PlayerStorageInterface) {
	
	var username: Name by Attrs.Unique(null)
	
	var nickname: Name
		get() = readAttribute<Name>("nickname").orNull() ?: username
		set(value) {
			this["nickname"] = value
		}
	
	inline operator fun <reified T> get(path: String): T = readAttribute<T>(path).unwrap()
	inline fun <reified T> readAttribute(path: String): MaybeAttribute<T> =
			storage.readAttribute(id, path, T::class.java)
			?: throw AttributeException("Failed to get value for attribute \"$path\"")
	
	inline fun <reified T> readUnique(path: String): MaybeAttribute<T> =
			storage.readUnique(id, path, T::class.java)?: throw AttributeException("Failed to get value for unique attribute \"$path\"")
	
	operator fun <T> set(path: String, value: T) = writeAttribute(path stores value)
	fun clear(path: String) = writeAttribute(path.undefined)
	
	fun <T> writeAttribute(attribute: MaybeAttribute<T>) =
			storage.writeAttribute(id, attribute)?: throw AttributeAssignmentException(id, attribute)
	
	fun <T> writeUnique(attribute: MaybeAttribute<T>) =
			storage.writeUnique(id, attribute)?: throw AttributeAssignmentException(id, attribute)
	
	override fun toString() = id.toString()
	
	data class Attrs internal constructor(val receiver: KAnnotatedElement?) {
		
		val receiverPath = receiver.path.run { if(!isEmpty()) "$this." else this }
		
		companion object {
			operator fun <T> invoke(receiver: KCallable<T>) = Attrs(receiver)
			operator fun <T: Any> invoke(receiver: KClass<T>) = Attrs(receiver)
			inline operator fun <reified T: Any> invoke() = invoke(T::class)
		}
		
		inline operator fun <reified T> getValue(player: Player, property: KProperty<*>): T  {
			return player["$receiverPath${property.name}"]
		}
		operator fun <T> setValue(player: Player, property: KProperty<*>, value: T) {
			player["$receiverPath${property.name}"] = value
		}
		
		data class Unique internal constructor(val receiver: KAnnotatedElement?) {
			
			val receiverPath = receiver.path.run { if(!isEmpty()) "$this." else this }
			
			companion object{
				operator fun <T> invoke(receiver: KCallable<T>) = Unique(receiver)
				operator fun <T: Any> invoke(receiver: KClass<T>) = Unique(receiver)
				inline operator fun <reified T: Any> invoke() = invoke(T::class)
			}
			
			inline operator fun <reified T> getValue(player: Player, property: KProperty<*>): T {
				return player.readUnique<T>("$receiverPath${property.name}").unwrap()
			}
			
			operator fun <T> setValue(player: Player, property: KProperty<*>, value: T) {
				player.writeUnique("$receiverPath${property.name}" stores value)
			}
			
		}
	}
	
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
		
	}
	
}

@JsonIgnoreProperties(ignoreUnknown = true)
inline class PlayerId(val _id: Id<Player>)

infix fun PlayerId?.storedIn(storage: PlayerStorageInterface) = this?.let { storage[it] }
fun Id<Player>.toPlayerId() = PlayerId(this)

data class InvalidUsernameException(val reason: String? = null, val input: Any? = null) : Exception("$reason: $input")

package com.waridley.textroid.engine

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonValue
import com.natpryce.Failure
import org.litote.kmongo.Id
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

@Suppress("UNUSED")
class Player(@JsonValue val id: PlayerId, val storage: PlayerStorageInterface) {
	
	var username: Name by uniqueAttrs
	
	var nickname: Name
		get() = readAttribute<Name>("nickname").orNull()?: username
		set(value) { this["nickname"] = value }
	
	inline operator fun <reified T> get(path: String): T = readAttribute<T>(path).unwrap()
//	inline operator fun <reified T> get(kProperty: KProperty<T>): T = readAttribute(kProperty).unwrap()
	inline fun <reified T> readAttribute(path: String): MaybeAttribute<T> = storage.readAttribute(id, path, T::class.java)
		?: throw AttributeException("Failed to get value for attribute \"$path\"")
//	inline fun <reified T> readAttribute(property: KProperty<T>): MaybeAttribute<T> = storage.readAttribute(id, property)
//		?: throw AttributeException("Failed to get value for attribute \"${property.name}\"")
	inline fun <reified T> readUnique(path: String): MaybeAttribute<T> = storage.readUnique(id, path, T::class.java)
		?: throw AttributeException("Failed to get value for unique attribute \"$path\"")
	
	operator fun <T> set(path: String, value: T) = writeAttribute(path stores value)
//	operator fun <T> set(kProperty: KProperty<T>, value: T) = set(kProperty.name, value)
	fun clear(path: String) = writeAttribute(path.clear)
	fun <T> writeAttribute(attribute: MaybeAttribute<T>)  = storage.writeAttribute(id, attribute)
		?: throw AttributeAssignmentException(id, attribute)
	fun <T> writeUnique(attribute: MaybeAttribute<T>) = storage.writeUnique(id, attribute)
		?: throw AttributeAssignmentException(id, attribute)
	
	override fun toString() = id.toString()
	
	class Attrs internal constructor(val namespace: String) {
		
		companion object {
			private val cache = mutableMapOf<KClass<*>, Attrs>()
			operator fun invoke(context: KClass<*>) = cache.getOrPut(context) {
				Attrs("${context.qualifiedName}.")
			}
			inline operator fun <reified Context> invoke() = invoke(Context::class)
		}
		
		fun <T> filter(property: KProperty<*>, value: T): Attribute<T> = "${namespace}${property.name}" stores value
		fun clear(property: KProperty<*>): Undefined = "${namespace}${property.name}".clear
		
		inline operator fun <reified T> getValue(player: Player, property: KProperty<*>)
				= player.get<T>("${namespace}${property.name}")
		
		operator fun <T> setValue(player: Player, property: KProperty<*>, value: T)
				= player.set("${namespace}${property.name}", value)
		
		class Unique internal constructor(val namespace: String) {
			
			companion object {
				private val cache = mutableMapOf<KClass<*>, Unique>()
				operator fun invoke(context: KClass<*>) = cache.getOrPut(context) {
					Unique("${context.qualifiedName}.")
				}
				inline operator fun <reified Context> invoke() = invoke(Context::class)
			}
			
			inline operator fun <reified T> getValue(player: Player, property: KProperty<*>)
					= player.readUnique<T>("${namespace}${property.name}").unwrap()
			
			operator fun <T> setValue(player: Player, property: KProperty<*>, value: T)
					= player.writeUnique("${namespace}${property.name}" stores value)
		}
	}
	
	data class Name(@JsonValue val value: String) {
		init {
			value.validate()
		}
		
		companion object {
			private fun String.validate() {
				fun err(reason: String): Nothing = throw InvalidUsernameException(reason, this)
				when {
					contains("\\s".toRegex()) -> err("Contains whitespace")
					isBlank() -> err("Blank")
					length > 50 -> err("Too long")
					else -> println("Accepting player name: $this | Don't forget to add more rules!")
				}
			}
		}
		
	}
	
	companion object {
		private val attrs = Attrs("")
		private val uniqueAttrs = Attrs.Unique("")
	}
}

@JsonIgnoreProperties(ignoreUnknown = true)
inline class PlayerId(val _id: Id<Player>)
infix fun PlayerId?.storedIn(storage: PlayerStorageInterface) = this?.let { storage[it] }
fun Id<Player>.toPlayerId() = PlayerId(this)

fun String.asUsername() = Player.Name(this)

sealed class MaybeAttribute<out T>(val path: String)
fun <T> MaybeAttribute<T>?.unwrap(): T = when(this) {
		is Attribute -> value
		else -> throw AttributeException(message = "Tried to unwrap non-existent attribute \"${this?.path}\"")
	}

fun <T> MaybeAttribute<T>?.orNull(): T? = when(this) {
		is Attribute -> value
		else -> null
	}
data class Attribute<out T>(private val _path: String, val value: T): MaybeAttribute<T>(_path)
data class Undefined(private val _path: String): MaybeAttribute<Nothing>(_path)
infix fun <T> String.stores(value: T) = Attribute(this, value)
infix fun <T> KProperty<T>.stores(value: T) = name stores value
val String.clear get() = Undefined(this)

data class InvalidUsernameException(val reason: String? = null, val input: Any? = null): Exception("$reason: $input")

open class AttributeException(message: String? = null, cause: Throwable? = null): Exception(message, cause)
open class AttributeAssignmentException(id: PlayerId?, attribute: MaybeAttribute<*>?, failure: Failure<Throwable>? = null)
	: AttributeException(
		when(attribute) {
			is Attribute -> "Failed to set attribute \"${attribute.path}\" to ${attribute.value} for player ${id?._id}"
			is Undefined -> "Failed to clear attribute \"${attribute.path}\" for player ${id?._id}"
			else -> null
		},
		failure?.reason
	)
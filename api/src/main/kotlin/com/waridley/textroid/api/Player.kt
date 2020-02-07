package com.waridley.textroid.api

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonValue
import com.natpryce.Failure
import org.litote.kmongo.Id
import kotlin.reflect.*
import kotlin.reflect.full.extensionReceiverParameter
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.jvm.jvmErasure

@Suppress("UNUSED")
class Player(@JsonValue val id: PlayerId, val storage: PlayerStorageInterface) {
	
	var username: Name by Attrs.Unique(null)
	
	var nickname: Name
		get() = readAttribute<Name>("nickname").orNull() ?: username
		set(value) {
			this["nickname"] = value
		}
	
	inline operator fun <reified T> get(path: String): T = readAttribute<T>(path).unwrap()
	//	inline operator fun <reified T> get(kProperty: KProperty<T>): T = readAttribute(kProperty).unwrap()
	inline fun <reified T> readAttribute(path: String): MaybeAttribute<T> =
			storage.readAttribute(id, path, T::class.java)
			?: throw AttributeException("Failed to get value for attribute \"$path\"")
	
	//	inline fun <reified T> readAttribute(property: KProperty<T>): MaybeAttribute<T> = storage.readAttribute(id, property)
//		?: throw AttributeException("Failed to get value for attribute \"${property.name}\"")
	inline fun <reified T> readUnique(path: String): MaybeAttribute<T> =
			storage.readUnique(id, path, T::class.java)?: throw AttributeException("Failed to get value for unique attribute \"$path\"")
	
	operator fun <T> set(path: String, value: T) = writeAttribute(path stores value)
	//	operator fun <T> set(kProperty: KProperty<T>, value: T) = set(kProperty.name, value)
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
		
//		class AttrsFactory(val context: KClass<*>) {
//			operator fun invoke(prop: KProperty<*>): Attrs {
//				return Attrs[context, prop]
//			}
//		}
		
//		companion object {
//			internal val root = Attrs("")
//			private val contextMap = mutableMapOf<KClass<*>, Attrs<*>>()
//			private val propertyPaths = mutableMapOf<KProperty<*>, String>()
			
//			operator fun get(context: KClass<*>) = AttrsFactory(context)
			
//			operator fun get(context: KClass<*>, prop: KProperty<*>): Attrs {
//				return contextMap.getOrPut(context) { Attrs("${context.qualifiedName}.") }.apply { prop.registerPath() }
//			}
			
//			inline operator fun <reified Context> invoke(prop: KProperty<*>) = this[Context::class, prop]
			
//			operator fun get(key: KProperty<*>) = propertyPaths[key]
			
//		}
		
//		fun <T> filter(property: KProperty<*>, value: T) = path stores value
//		fun clear(property: KProperty<*>) = path.undefined
		
		inline operator fun <reified T> getValue(player: Player, property: KProperty<*>): T  {
			return player["$receiverPath${property.name}"]
		}
		operator fun <T> setValue(player: Player, property: KProperty<*>, value: T) {
			player["$receiverPath${property.name}"] = value
		}
		
//		fun KProperty<*>.registerPath() = propertyPaths[this]?.also {
//					if(!it.startsWith(namespace)) {
//						throw AttributeException("Property namespace conflict: existing = $it | conflicting = $namespace${this.name}")
//					} else if(extensionReceiverParameter != Player::username.extensionReceiverParameter) {
//						throw AttributeException("Property must be an extension of Player")
//					}
//				} ?: "$namespace${this.name}".also { propertyPaths[this] = it }
		
//		val KProperty<*>.path
//			get() = propertyPaths[this]?: throw AttributeException("No path found in ${this@Attrs} for property: $this")
		
		data class Unique internal constructor(val receiver: KAnnotatedElement?) {
			
			val receiverPath = receiver.path.run { if(!isEmpty()) "$this." else this }
			
			companion object{
				operator fun <T> invoke(receiver: KCallable<T>) = Unique(receiver)
				operator fun <T: Any> invoke(receiver: KClass<T>) = Unique(receiver)
				inline operator fun <reified T: Any> invoke() = invoke(T::class)
			}
			
//			class UniqueAttrsFactory(val context: KClass<*>) {
//				operator fun invoke(prop: KProperty<*>): Unique {
//					return Unique[context, prop]
//				}
//			}
			
//			companion object {
//				internal val root = Unique("")
//				internal val contextMap = mutableMapOf<KClass<*>, Unique<*>>()
				
//				operator fun get(context: KClass<*>) = UniqueAttrsFactory(context)
				
//				operator fun get(context: KClass<*>, prop: KProperty<*>): Unique {
//					return contextMap.getOrPut(context) { Unique("${context.qualifiedName}.") }.apply { prop.registerPath() }
//				}
				
//				inline operator fun <reified Context> invoke(prop: KProperty<*>) = this[Context::class, prop]
//			}
			
			inline operator fun <reified T> getValue(player: Player, property: KProperty<*>): T {
				return player.readUnique<T>("$receiverPath${property.name}").unwrap()
			}
			
			operator fun <T> setValue(player: Player, property: KProperty<*>, value: T) {
				player.writeUnique("$receiverPath${property.name}" stores value)
			}
			
//			fun KProperty<*>.registerPath() = propertyPaths[this]?.also {
//						if(!it.startsWith(namespace)) {
//							throw AttributeException("Property namespace conflict: existing = $it | conflicting = $namespace${this.name}")
//						}
//					} ?: "$namespace${this.name}".also { propertyPaths[this] = it }
//
//			val KProperty<*>.path
//				get() = propertyPaths[this]?: throw AttributeException("No path found in ${this@Unique} for property: $this")
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
					contains("\\s".toRegex()) -> err("Contains whitespace")
					isBlank()                 -> err("Blank")
					length > 50               -> err("Too long")
					else                      -> println("Accepting player name: $this | Don't forget to add more rules!")
				}
			}
		}
		
	}
	
}

@JsonIgnoreProperties(ignoreUnknown = true)
inline class PlayerId(val _id: Id<Player>)

infix fun PlayerId?.storedIn(storage: PlayerStorageInterface) = this?.let { storage[it] }
fun Id<Player>.toPlayerId() = PlayerId(this)



sealed class MaybeAttribute<out T>(val path: String)
data class Attribute<out T>(private val _path: String, val value: T) : MaybeAttribute<T>(_path)
data class Undefined(private val _path: String) : MaybeAttribute<Nothing>(_path)

inline fun <reified T> MaybeAttribute<T>?.unwrap(): T = when(this) {
	is Attribute -> value
	else         -> throw AttributeException("Tried to unwrap non-existent attribute \"${this?.path}\"")
}

inline fun <reified T> MaybeAttribute<T>?.orNull(): T? = when(this) {
	is Attribute -> value
	else         -> null
}

infix fun <T> String.stores(value: T) = Attribute(this, value)
infix fun <T> KProperty<T>.stores(value: T) = path stores value
val String.undefined get() = Undefined(this)

val KAnnotatedElement?.path: String
	get() = when(this) {
        is KCallable<*> -> ((instanceParameter?: extensionReceiverParameter)?.type?.jvmErasure?.run {"$qualifiedName."}?: "") + name
        is KClass<*>    -> qualifiedName?: simpleName?: ""
		null            -> ""
		else            -> this.toString().replace(" ", "_")
	}



data class InvalidUsernameException(val reason: String? = null, val input: Any? = null) : Exception("$reason: $input")

open class AttributeException(message: String? = null, cause: Throwable? = null) : Exception(message, cause)
open class AttributeAssignmentException(id: PlayerId?,
                                        attribute: MaybeAttribute<*>?,
                                        failure: Failure<Throwable>? = null): AttributeException(
		when (attribute) {
			is Attribute -> "Failed to set attribute \"${attribute.path}\" to ${attribute.value} for player ${id?._id}"
			is Undefined -> "Failed to clear attribute \"${attribute.path}\" for player ${id?._id}"
			else         -> null
		},
		failure?.reason
)
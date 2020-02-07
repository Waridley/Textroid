package com.waridley.textroid.api

import com.natpryce.Failure
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.extensionReceiverParameter
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.jvm.jvmErasure


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


open class AttributeException(message: String? = null, cause: Throwable? = null) : Exception(message, cause)
open class AttributeAssignmentException(id: Any?,
                                        attribute: MaybeAttribute<*>?,
                                        failure: Failure<Throwable>? = null): AttributeException(
		when (attribute) {
			is Attribute -> "Failed to set attribute \"${attribute.path}\" to ${attribute.value} for id: $id"
			is Undefined -> "Failed to clear attribute \"${attribute.path}\" for id: $id"
			else         -> null
		},
		failure?.reason
)


val KAnnotatedElement?.path: String
	get() = when(this) {
		is KCallable<*> -> ((instanceParameter ?: extensionReceiverParameter)?.type?.jvmErasure
				                    ?.run {"${qualifiedName?: simpleName?: toString()}."} ?: "") + name
		is KClass<*>    -> qualifiedName ?: simpleName ?: toString()
		null            -> ""
		else            -> this.toString().replace(" ", "_")
	}
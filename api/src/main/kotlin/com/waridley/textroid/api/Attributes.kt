package com.waridley.textroid.api

import com.natpryce.Failure
import kotlin.reflect.*
import kotlin.reflect.full.extensionReceiverParameter
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.jvm.jvmErasure


sealed class MaybeAttribute<out T>(val path: String)
data class Attribute<out T>(private val _path: String, val value: T): MaybeAttribute<T>(_path)
data class Undefined(private val _path: String): MaybeAttribute<Nothing>(_path)

inline fun <reified T> MaybeAttribute<T>?.unwrap(): T = when(this) {
	is Attribute -> value
	else         -> throw AttributeException("Tried to unwrap non-existent attribute \"${this?.path}\"")
}

inline fun <reified T> MaybeAttribute<T>?.orNull(): T? = when(this) {
	is Attribute -> value
	else         -> null
}

inline fun <reified T> MaybeAttribute<T>.orElse(fallback: MaybeAttribute<T>.() -> T) = when(this) {
	is Attribute -> value
	else         -> this.fallback()
}

infix fun <T> String.stores(value: T) = Attribute(this, value)
infix fun <T> KProperty<T>.stores(value: T) = path stores value

val String.undefined get() = Undefined(this)
val KProperty<*>.undefined get() = path.undefined


open class AttributeException(message: String? = null, cause: Throwable? = null) : Exception(message, cause)
open class AttributeAssignmentException(
		id: Any?,
		attribute: MaybeAttribute<*>?,
		failure: Failure<Throwable>? = null
): AttributeException(
		when (attribute) {
			is Attribute -> "Failed to set attribute \"${attribute.path}\" to ${attribute.value} for id: $id"
			is Undefined -> "Failed to clear attribute \"${attribute.path}\" for id: $id"
			else         -> null
		},
		failure?.reason
)

val KParameter.path
		get() = this.type.jvmErasure.path

val KCallable<*>.path
		get() = when(this) {
			is PathNode<*,*> -> "$this"
			else             -> "$containingPath$name"
		}

val KCallable<*>.containingPath
		get() = (instanceParameter ?: extensionReceiverParameter)?.path?.plus(".") ?: ""

val KClass<*>.path
		get() = (qualifiedName ?: simpleName ?: toString().replace(" ", "_")).replace("com.waridley.textroid.", "")

val <T> Class<T>.path
		get() = (canonicalName ?: name ?: simpleName?: toString().replace(" ", "_")).replace("com.waridley.textroid.", "")

fun String.relativeTo(other: KClass<*>) = replaceFirst("${other.path}.", "")
fun String.relativeTo(other: KParameter) = replaceFirst("${other.path}.", "")
fun <T> String.relativeTo(other: Class<T>) = replaceFirst("${other.path}.", "")
fun String.relativeTo(other: KCallable<*>) = replaceFirst("${other.path}.", "")
inline fun <reified T> String.relativeTo() = relativeTo(T::class.java)


inline operator fun <reified P, reified C> KProperty<P?>.div(other: KFunction1<P, C>) = PathNode(PathNode<Nothing?, P?>(this), other.syn)
inline operator fun <reified P, reified C> KProperty<P?>.div(other: KProperty1<P, C>) = PathNode(PathNode<Nothing?, P?>(this), other)
inline operator fun <reified P, reified C> KProperty<P?>.div(other: KProperty2<*, P, C>) = PathNode(PathNode<Nothing?, P?>(this), other)
inline operator fun <reified P, reified C> PathNode<*, P>.div(other: KProperty1<P, C>) = PathNode(this, other)
inline operator fun <reified P, reified C> PathNode<*, P>.div(other: KProperty2<*, P, C>) = PathNode(this, other)

class PathNode<out P, out C> @PublishedApi internal constructor(val parent: PathNode<*, P>?, val value: KProperty<C>): KProperty<C> {
	
	constructor(root: KProperty<C>): this(null, root)
	constructor(parent: PathNode<*, P>, child: KProperty1<P, C>): this(parent, child as KProperty<C>)
	constructor(parent: PathNode<*, P>, child: KProperty2<*, P, C>): this(parent, child as KProperty<C>)
	
	override fun toString() = when(parent) {
		null -> value.path
		else -> "${parent}.${value.name}"
	}
	
	override val annotations: List<Annotation> get() = value.annotations
	override val isAbstract: Boolean get() = value.isAbstract
	override val isFinal: Boolean get() = value.isFinal
	override val isOpen: Boolean get() = value.isOpen
	override val isSuspend: Boolean get() = value.isSuspend
	override val name: String get() = value.name
	override val parameters: List<KParameter> get() = value.parameters
	override val returnType: KType get() = value.returnType
	override val typeParameters: List<KTypeParameter> get() = value.typeParameters
	override val visibility: KVisibility? get() = value.visibility
	override fun call(vararg args: Any?): C = value.call(args)
	override fun callBy(args: Map<KParameter, Any?>): C = value.callBy(args)
	override val getter: KProperty.Getter<C> get() = value.getter
	override val isConst: Boolean get() = value.isConst
	override val isLateinit: Boolean get() = value.isLateinit
	
}


inline val <reified R, reified T> KFunction1<R, T>.syn: KProperty1<R, T>
	get() = SyntheticJavaProperty(R::class.java, this, T::class.java)

@Suppress("UNCHECKED_CAST")
@PublishedApi internal class SyntheticJavaProperty<R, T>(val receiver: Class<R>, javaGetter: Function1<R, T>, type: Class<T>) : KProperty1<R, T> {
	val it = javaGetter as KCallable<*>
	
	override val name = it.name.replace("get", "").run { "${this[0].toLowerCase()}${substring(1, length)}" }
	
	override val annotations: List<Annotation> get() = it.annotations
	override val getter: KProperty1.Getter<R, T> get() = throw NotImplementedError()
	override val isAbstract: Boolean get() = it.isAbstract
	override val isConst: Boolean get() = throw NotImplementedError()
	override val isFinal: Boolean get() = it.isFinal
	override val isLateinit: Boolean get() = throw NotImplementedError()
	override val isOpen: Boolean get() = it.isOpen
	override val isSuspend: Boolean get() = it.isSuspend
	override val parameters: List<KParameter> get() = it.parameters
	override val returnType: KType get() = it.returnType
	override val typeParameters: List<KTypeParameter> get() = it.typeParameters
	override val visibility: KVisibility? get() = it.visibility
	override fun call(vararg args: Any?): T = it.call(args) as T
	override fun callBy(args: Map<KParameter, Any?>): T = it.callBy(args) as T
	override fun get(receiver: R): T = throw NotImplementedError()
	override fun getDelegate(receiver: R): Any? = throw NotImplementedError()
	override fun invoke(p1: R): T = throw NotImplementedError()
}
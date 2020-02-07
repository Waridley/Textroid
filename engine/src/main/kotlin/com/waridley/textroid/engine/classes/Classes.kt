@file:Suppress("UNUSED")

package com.waridley.textroid.engine.classes

import kotlin.reflect.KProperty
import kotlin.reflect.full.memberProperties

abstract class Entity(description: String? = null) {
	var name = this::class.simpleName?.replace("_", " ") ?: "Entity"
	var description = description ?: name
	var fullName = this::class.qualifiedName
	val contents
		get() = this::class.memberProperties
				        .map { it.name }
				        .filter {
					        !arrayOf("contents", "storage", "equals", "hashCode", "invoke", "toString").contains(it)
				        } + this::class.nestedClasses.filter { it.objectInstance != null }.map { it.simpleName }
	
	override fun toString() = name
	val storage = object {
		inline operator fun <reified T> getValue(thisRef: Entity, property: KProperty<*>): T {
			TODO()
		}

		inline operator fun <reified T> setValue(thisRef: Entity, property: KProperty<*>, value: T) {
			TODO()
		}
	}
}

inline operator fun <reified T : Entity> T.invoke(block: T.() -> Unit) = this.block()

abstract class GameWorld(description: String = "Game World") : Entity(description)

abstract class Planet(var size: Int = 0, description: String = "Size $size Planet") : Entity(description) {
	
	abstract class Region(var location: Int = 0, description: String = "Region at $location") : Entity(description) {

	}
	
}
@file:Suppress("UNUSED")

package com.waridley.textroid.api.game

import kotlin.reflect.KProperty
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.memberProperties

abstract class Entity(description: String? = null) {
	var name = "${this::class.simpleName?.replace(" _ ", " ")}"
	var description = description ?: name
	var fullName = this::class.qualifiedName
	val contents
		get() = this::class.memberProperties
				        .filter { it.visibility == KVisibility.PUBLIC }
						.map { it.name }
						.filter {
							!arrayOf("name", "description", "fullName", "contents", "storage", "equals", "hashCode", "invoke", "toString").contains(it)
						} + this::class.nestedClasses.filter { it.objectInstance != null }.map { it.simpleName }
	val actions get() = this::class.declaredFunctions.filter { it.visibility == KVisibility.PUBLIC }.map { it.name }
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

abstract class GameWorld(description: String = "Game World") : Entity(description) {
//	private val conns = mutableListOf<Connection>()
//
//	inner class ConnectionAppender {
//		internal val newConns = mutableListOf<Connection>()
//		fun connect(room1: Planet.Region.Area.Room, exit1: Exit,
//		            room2: Planet.Region.Area.Room, exit2: Exit) {
//			newConns.add(Connection(Pair(room1, exit1), Pair(room2, exit2)))
//		}
//	}
//
//	fun connections(block: ConnectionAppender.() -> Unit): List<Connection> {
//		val app = ConnectionAppender()
//		app.block()
//		conns.addAll(app.newConns)
//		return app.newConns
//	}
}

abstract class Planet(description: String, val size: Int = 0) : Entity(description) {
	abstract class Region(description: String) : Entity(description) {
		abstract class Area(description: String): Entity(description) {
			abstract class Room(description: String, vararg val exits: Exit): Entity(description) {
				abstract class Exit(val description: String, val direction: String, val destination: Room)
				abstract class Door(description: String, direction: String, destination: Room): Exit(description, direction, destination)
				class BlueDoor(direction: String, destination: Room): Door(
						"A normal door that can be opened with any weapon", direction, destination)
				class DeadEnd(direction: String): Exit(
						"This doesn't lead anywhere yet", direction, object: Room("This room has not been constructed yet") {})
			}
		}
	}
}

const val N = "North"
const val S = "South"
const val E = "East"
const val W = "West"
const val NE = "Northeast"
const val NW = "Northwest"
const val SE = "Southeast"
const val SW = "Southwest"


//class Connection(val end1: Pair<Planet.Region.Area.Room, Exit>, val end2: Pair<Planet.Region.Area.Room, Exit>)

abstract class Ability

interface Expansion
class MissileExpansion: Expansion

interface MorphBallTunnel

class BankAccount private constructor(val currentAmount: Long)
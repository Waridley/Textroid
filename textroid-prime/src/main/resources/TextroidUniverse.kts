@file:Suppress("UNUSED")

import com.waridley.textroid.api.game.GameWorld
import com.waridley.textroid.api.game.Planet
import com.waridley.textroid.engine.*

class MissingPlanet(name: String): Planet("Failed to load $name")

object Textroid_Universe: GameWorld("The Textroid Universe") {
	private val pl = ScriptLoader<Planet>()
	val planets = arrayOf(
			pl.load("textroid-prime/src/main/resources/TallonIV.kts") ?: MissingPlanet("Tallon IV")
	)
}

Textroid_Universe //return to script invoker
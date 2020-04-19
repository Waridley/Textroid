package com.waridley.textroid.engine

import com.waridley.textroid.api.LOG
import com.waridley.textroid.api.game.GameWorld

class Game(val world: GameWorld) {
	init {
		LOG.info("Game World: {}", world)
	}
}

package com.waridley.textroid.server

import com.waridley.textroid.Game

class Server(commandsScriptFilePath: String): AutoCloseable {
//	val game: Game = Game()
	val actionValidator = ActionValidator()
	val actionResponder = ActionResponder(commandsScriptFilePath)
	
	override fun close() {
		actionValidator.close()
		actionResponder.close()
	}
}

package com.waridley.textroid.server

import com.waridley.textroid.engine.Game

class Server(commandsScriptFilePath: String, val game: Game): AutoCloseable {
	val actionValidator = ActionValidator()
	val actionResponder = ActionResponder(commandsScriptFilePath)
	
	override fun close() {
		actionValidator.close()
		actionResponder.close()
	}
}

package com.waridley.textroid.server

import com.github.philippheuer.events4j.core.EventManager
import com.waridley.textroid.*
import com.waridley.textroid.api.EVENT_MANAGER
import com.waridley.textroid.api.LOG
import com.waridley.textroid.api.TextroidEventHandler
import com.waridley.textroid.api.asPlayerName
import java.io.File
import javax.script.Invocable
import javax.script.ScriptContext
import javax.script.ScriptEngineManager
import javax.script.SimpleScriptContext

class ActionResponder(commandsScriptFilePath: String = "server/src/main/resources/Commands.kts", eventManager: EventManager = EVENT_MANAGER): TextroidEventHandler(eventManager, {
	
	val scriptExecutor = ScriptExecutor(commandsScriptFilePath, eventManager)
	
	on<MintApprovedEvent> {
		ActionExecutor.adjustCurrency(player, amount)
	}
	on<InfoRequestEvent.CurrencyInBank> {
		respond("${ActionExecutor.readCurrency(player)}")
	}
	on<CommandApprovedEvent> {
		scriptExecutor.handle(commandEvent)
	}
	
})

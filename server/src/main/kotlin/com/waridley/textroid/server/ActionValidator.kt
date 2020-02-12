package com.waridley.textroid.server

import com.waridley.textroid.*
import com.waridley.textroid.api.*
import java.io.File
import java.lang.reflect.Method
import javax.script.Invocable
import javax.script.ScriptContext
import javax.script.ScriptEngineManager
import javax.script.SimpleScriptContext

class ActionValidator: TextroidEventHandler({
	
	on<MintRequestEvent> {
		LOG.info("Pretending to validate $this")
		publish(MintApprovedEvent(player, amount, this))
	}
	on<ChatCommandEvent> {
		//TODO validate command permissions
		publish(CommandApprovedEvent(this))
	}
	
})
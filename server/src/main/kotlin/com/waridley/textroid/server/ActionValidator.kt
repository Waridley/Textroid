package com.waridley.textroid.server

import com.waridley.textroid.*
import com.waridley.textroid.api.LOG
import com.waridley.textroid.api.TextroidEventHandler
import java.io.File
import javax.script.Invocable
import javax.script.ScriptContext
import javax.script.ScriptEngineManager
import javax.script.SimpleScriptContext

class ActionValidator: TextroidEventHandler() {
	private val classLoader: ClassLoader = Thread.currentThread().contextClassLoader
	private val engine = ScriptEngineManager(classLoader).getEngineByExtension("kts")
	
	private val commandsScript get() = File("server/src/main/resources/Commands.kts").readText() + "\nthis"
	
	private val commandRunner = engine as Invocable
	
	var chatCommands: Any? = null
	fun reloadCommands() {
		engine.context =
				SimpleScriptContext().also { it.setBindings(engine.createBindings(), ScriptContext.ENGINE_SCOPE) }
		chatCommands = engine.eval(commandsScript)
	}
	
	init {
		chatCommands = engine.eval(commandsScript)
		on<MintRequestEvent> {
			LOG.info("Pretending to validate $this")
			publish(MintApprovedEvent(player, amount, this))
		}
		on<ChatCommandEvent> {
			when {
				command == "commands" -> {
					responseHandler("Available commands: ${
						chatCommands?.javaClass?.declaredMethods?.map { it.name }?.filter {
							!listOf("equals",
									"hashCode",
									"toString",
							        "main"
							).contains(it) && !it.contains("$")
						}?.joinToString()
					}")
				}
				command == "reloadcommands" -> reloadCommands()
				else -> {
					when(val consumer = chatCommands?.javaClass?.getDeclaredMethod(command)?.invoke(chatCommands)) {
						is Response -> responseHandler("${consumer(this)}")
						is Trigger  -> publish(consumer(this))
					}
				}
			}
		}
	}
	
}
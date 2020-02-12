package com.waridley.textroid.server

import com.waridley.textroid.*
import com.waridley.textroid.api.*
import java.io.File
import javax.script.ScriptContext
import javax.script.ScriptEngineManager
import javax.script.SimpleScriptContext

class ActionValidator: TextroidEventHandler() {
	private val classLoader: ClassLoader = Thread.currentThread().contextClassLoader
	private val engine = ScriptEngineManager(classLoader).getEngineByExtension("kts")
	
	private val commandsScript get() = File("server/src/main/resources/Commands.kts").readText() + "\nthis"
	
	var chatCommands: Any? = null
	fun reloadCommands(): Boolean {
		return try {
			engine.context = SimpleScriptContext().also { it.setBindings(engine.createBindings(), ScriptContext.ENGINE_SCOPE) }
			chatCommands = engine.eval(commandsScript)
			true
		} catch (e: Exception) {
			e.printStackTrace()
			false
		}
	}
	
	val commandStates = mutableMapOf<String, StringBuilder>()
	
	init {
		chatCommands = engine.eval(commandsScript)
		on<MintRequestEvent> {
			LOG.info("Pretending to validate $this")
			publish(MintApprovedEvent(player, amount, this))
		}
		on<ChatCommandEvent> {
			when(command.toLowerCase()) {
				"commands"             -> {
					responseHandler("Available commands: ${
					chatCommands?.javaClass?.declaredMethods?.map { it.name }?.filter {
						!listOf("equals", "hashCode", "toString", "main").contains(it) && !it.contains("$")
					}?.joinToString()
					}")
				}
				"reload"               -> responseHandler(
						if(reloadCommands()) "Reloaded command script."
						else "Failed to load commands. You may need to fix the script file."
				)
				"addcom", "addtrigger" -> if(player.username == "waridley".asPlayerName) command.let {
					val separator = args.indexOf(" ")
					val commandName = args.substring(0, separator)
					val body = args.substring(separator + 1)
					val builder = commandStates[command] ?: StringBuilder()
					
					if(body.endsWith("\\")) {
						builder.append("${body.substring(0, body.length - 1)}\n\t")
						commandStates[command] = builder
					} else {
						builder.append(body)
						File("server/src/main/resources/Commands.kts").run {
							appendText(
									"""
									|
									|
									|fun $commandName() = ${if(it == "addtrigger") "Trigger" else "Response"} {
									|	$builder
									|}
									""".trimMargin()
							)
						}
						responseHandler(
								if(reloadCommands()) "Added command %$commandName"
								else "Failed to add command. You may need to fix the script file."
						)
						commandStates.remove(command)
					}
					
				}
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
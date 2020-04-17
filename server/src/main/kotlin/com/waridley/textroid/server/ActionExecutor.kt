package com.waridley.textroid.server

import com.github.philippheuer.events4j.core.EventManager
import com.waridley.textroid.BankAccount
import com.waridley.textroid.ChatCommandEvent
import com.waridley.textroid.Response
import com.waridley.textroid.Trigger
import com.waridley.textroid.api.*
import java.io.File
import javax.script.Invocable
import javax.script.ScriptContext
import javax.script.ScriptEngineManager
import javax.script.SimpleScriptContext

object ActionExecutor {
	private var Player.currencyInBankAccount: Long by storage(BankAccount::currentAmount) { 0L }
	
	@Synchronized fun adjustCurrency(player: Player, amount: Long) {
		player.currencyInBankAccount += amount
	}
	
	fun readCurrency(player: Player): Long {
		return player.currencyInBankAccount
	}
	
}

class ScriptExecutor(commandsScriptFilePath: String = "server/src/main/resources/Commands.kts", val eventManager: EventManager) {
	
	private val classLoader: ClassLoader = Thread.currentThread().contextClassLoader
	private val engine = ScriptEngineManager(classLoader).getEngineByExtension("kts")
	
	private val commandsScriptFile = File(commandsScriptFilePath)
	private val commandsScript get() = commandsScriptFile.readText() + "\nthis" // make the script return itself
	
	var chatCommands = emptySet<String>()
	fun reloadCommands(): Set<String>? {
		return try {
			engine.context = SimpleScriptContext().also { it.setBindings(engine.createBindings(), ScriptContext.ENGINE_SCOPE) }
			chatCommands = engine.eval(commandsScript)?.javaClass?.declaredMethods?.map { it.name }?.filter {
				!listOf("equals", "hashCode", "toString", "main").contains(it) && !it.contains("$")
			}?.toHashSet() ?: emptySet()
			chatCommands.also { LOG.trace("Commands: $it")}
		} catch (e: Exception) {
			e.printStackTrace()
			null
		}
	}
	
	init {
		reloadCommands()
	}
	
	val commandStates: MutableMap<String, StringBuilder> = mutableMapOf()
	fun ChatCommandEvent.execute() {
		when(val it = command.toLowerCase()) {
			"commands"       -> {
				respond("Available commands: ${chatCommands.joinToString()}")
			}
			"reload",
			"reloadcommands" -> respond(
					reloadCommands()?.let { "Reloaded command script." }
					?: "Failed to load commands. You may need to fix the script file."
			)
			"addcommand",
			"addcom",
			"addtrigger"     -> if(player.username == "waridley".asPlayerName) {
				val separator = args.indexOf(" ")
				val commandName = args.substring(0, separator)
				val body = args.substring(separator + 1)
				val builder = commandStates[command] ?: StringBuilder()
				
				if(body.endsWith("\\")) {
					builder.append("${body.substring(0, body.length - 1)}\n\t")
					commandStates[command] = builder
				} else {
					builder.append(body)
					val type = if(it == "addtrigger") "Trigger" else "Response"
					commandsScriptFile.appendText(
							"""
							|
							|
							|fun $commandName() = $type {
							|	$builder
							|}
							""".trimMargin()
					)
					respond(
							reloadCommands()?.let { "Added command %$commandName" }
							?: "Failed to add command. You may need to fix the script file."
					)
					commandStates.remove(command)
				}
				
			} else -> {
				LOG.trace("Not a built-in command. Checking script file for $it")
				if(chatCommands.contains(command)) {
					val consumer = (engine as Invocable).invokeFunction(command)
					LOG.trace(consumer.toString())
					when(consumer) {
						is Response -> respond("${consumer(this)}")
						is Trigger  -> eventManager.publish(consumer(this))
						else        -> {
							LOG.error("Return value of $command is: $consumer")
							respond("Ope, something went wrong! @Waridley, check the script file.")
						}
					}
				}
			}
		}
	}
	
	fun handle(event: ChatCommandEvent) = event.also { LOG.trace(it.toString()) }.execute()
	
}
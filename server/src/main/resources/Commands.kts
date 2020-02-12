@file:Suppress("UNUSED")
import com.waridley.textroid.Response
import com.waridley.textroid.InfoRequestEvent
import com.waridley.textroid.Trigger
import com.waridley.textroid.api.*
import java.io.File

fun phazon() = Trigger {
	InfoRequestEvent.CurrencyInBank(
			player,
			{ responseHandler("You currently have $CURRENCY_SYMBOL$it phazon units in your bank account.") },
			this
	)
}

fun sayHi() = Response {
	"Hi there!"
}

fun nickname() = Response {
	"Your nickname is \"${player.nickname}\""
}

fun setNickname() = Response {
	try {
		player.nickname = Player.Name(args)
		"Successfully set your nickname to $args"
	} catch(e: Exception) {
		LOG.error("Failed to set ${player.username}'s nickname: {}", e)
		"Failed to set your nickname: ${e.message}"
	}
}

fun addCommand() = Response {
	val separator = args.indexOf(" ")
	val commandName = args.substring(0, separator)
	val body = args.substring(separator + 1)
	File("server/src/main/resources/Commands.kts").run {
		appendText("""
			
			
			fun $commandName() = Command {
				$body
			}
		""".trimIndent())
	}
	"Added command %$commandName"
}
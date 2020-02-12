@file:Suppress("UNUSED")
import com.waridley.textroid.Response
import com.waridley.textroid.InfoRequestEvent
import com.waridley.textroid.Trigger
import com.waridley.textroid.api.*

fun phazon() = Trigger {
	InfoRequestEvent.CurrencyInBank(
			player,
			{ respond("You currently have $CURRENCY_SYMBOL$it phazon units in your bank account.") },
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
		player.nickname = args.asPlayerName
		"Successfully set your nickname to $args"
	} catch(e: Exception) {
		LOG.error("Failed to set ${player.username}'s nickname: {}", e)
		"Failed to set your nickname: ${e.message}"
	}
}
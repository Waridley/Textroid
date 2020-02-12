package com.waridley.textroid.ttv

import com.github.twitch4j.chat.events.CommandEvent
import com.github.twitch4j.common.events.domain.EventUser
import com.github.twitch4j.pubsub.domain.ChannelPointsUser
import com.github.twitch4j.pubsub.events.RewardRedeemedEvent
import com.waridley.textroid.ChatCommandEvent
import com.waridley.textroid.MintRequestEvent
import com.waridley.textroid.api.*

class TtvEventConverter(val playerStorage: PlayerStorageInterface): TextroidEventHandler() {
	fun findPlayer(user: ChannelPointsUser) =
			playerStorage.findOrCreateOne(TtvUser::id stores user.id, user.login, user.displayName?: user.login)
	fun findPlayer(user: EventUser) =
			playerStorage.findOrCreateOne(TtvUser::id stores user.id, user.name)
	
	init {
		on<RewardRedeemedEvent> {
			val title = redemption.reward.title
			if(title.startsWith(CURRENCY_SYMBOL)) {
				val amount =  title.substring(1).split(" ")[0].replace(",", "").toLong()
				publish(MintRequestEvent(findPlayer(redemption.user), amount, this))
			}
		}
		on<CommandEvent> {
			val argsIndex = command.indexOf(" ")
			publish(
					ChatCommandEvent(findPlayer(user),
					                 command.substring(0, if(argsIndex > 0) argsIndex else command.length),
					                 if(argsIndex > 0) command.substring(argsIndex + 1) else "",
					                 { respondToUser("@${user.name}: $it") },
					                 this)
			)
		}
	}

}
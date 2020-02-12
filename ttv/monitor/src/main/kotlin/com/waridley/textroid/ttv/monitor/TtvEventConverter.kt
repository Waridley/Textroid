package com.waridley.textroid.ttv.monitor

import com.github.twitch4j.chat.events.CommandEvent
import com.github.twitch4j.common.events.domain.EventUser
import com.github.twitch4j.pubsub.domain.ChannelPointsUser
import com.github.twitch4j.pubsub.events.RewardRedeemedEvent
import com.waridley.textroid.ChatCommandEvent
import com.waridley.textroid.MintRequestEvent
import com.waridley.textroid.api.*
import com.waridley.textroid.ttv.TtvUser

typealias Twitch4jCommandEvent = CommandEvent

class TtvEventConverter(val playerStorage: PlayerStorageInterface): TextroidEventHandler() {
	
	init {
		Thread {
			on<RewardRedeemedEvent> {
				val title = redemption.reward.title
				if(title.startsWith(CURRENCY_SYMBOL)) {
					val amount =  title.substring(1).split(" ")[0].replace(",", "").toLong()
					publish(MintRequestEvent(findPlayer(redemption.user), amount, this))
				}
			}
			on<Twitch4jCommandEvent> {
				val argsIndex = command.indexOf(" ")
				publish(ChatCommandEvent(
						findPlayer(user),
						command.substring(0, if(argsIndex > 0) argsIndex else command.length),
						if(argsIndex > 0) command.substring(argsIndex + 1) else "",
						{ respondToUser("@${user.name}: $it") },
						this)
				)
			}
		}.start()
	}
	
	val playerCache = mutableMapOf<String, Player>()
	
	fun findPlayer(user: ChannelPointsUser) =
			playerCache[user.id]
			?: playerStorage
					.findOrCreateOne(TtvUser::id stores user.id, user.login, user.displayName ?: user.login)
					.also { playerCache[user.id] = it }
	fun findPlayer(user: EventUser) =
			playerCache[user.id]
			?: playerStorage
					.findOrCreateOne(TtvUser::id stores user.id, user.name)
					.also { playerCache[user.id] = it}
	
}
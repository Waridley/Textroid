package com.waridley.textroid.ttv

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent
import com.github.twitch4j.common.events.domain.EventUser
import com.github.twitch4j.pubsub.domain.ChannelPointsUser
import com.github.twitch4j.pubsub.events.RewardRedeemedEvent
import com.waridley.textroid.ChatCommandEvent
import com.waridley.textroid.MintRequestEvent
import com.waridley.textroid.api.LOG
import com.waridley.textroid.api.PlayerStorageInterface
import com.waridley.textroid.api.TextroidEventHandler
import com.waridley.textroid.api.stores

class TtvEventConverter(val playerStorage: PlayerStorageInterface, val commandPrefix: String = "}"): TextroidEventHandler() {
	fun findPlayer(user: ChannelPointsUser) =
			playerStorage.findOrCreateOne(TtvUser::id stores user.id, user.login, user.displayName?: user.login)
	fun findPlayer(user: EventUser) =
			playerStorage.findOrCreateOne(TtvUser::id stores user.id, user.name)
	
	init {
		on<RewardRedeemedEvent> {
			publish(MintRequestEvent(findPlayer(redemption.user), redemption.reward.cost / 10L, this))
		}
		on<ChannelMessageEvent> {
			if(message.startsWith(commandPrefix)) publish(ChatCommandEvent(findPlayer(user), message, this))
		}
	}

}
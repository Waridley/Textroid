package com.waridley.textroid.ttv

import com.github.philippheuer.events4j.core.EventManager
import com.github.philippheuer.events4j.simple.SimpleEventHandler
import com.github.twitch4j.pubsub.events.ChannelPointsRedemptionEvent
import com.waridley.textroid.RequestMintEvent
import com.waridley.textroid.api.Player
import com.waridley.textroid.api.PlayerStorageInterface
import com.waridley.textroid.api.path
import com.waridley.textroid.api.stores

class TtvEventConverter(val playerStorage: PlayerStorageInterface, val eventManager: EventManager = EventManager().apply { autoDiscovery() }) {
	val handler: SimpleEventHandler = eventManager.getEventHandler(SimpleEventHandler::class.java)
	
	init {
		handler.onEvent(ChannelPointsRedemptionEvent::class.java) {
			val user = it.redemption.user
			eventManager.publish(
					RequestMintEvent(
							playerStorage.findOrCreateOne(
									TtvUser::id stores user.id,
									user.login,
									user.displayName
							),
							it
					)
			)
		}
	}
}
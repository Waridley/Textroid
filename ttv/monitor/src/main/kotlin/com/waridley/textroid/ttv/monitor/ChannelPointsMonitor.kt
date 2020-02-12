package com.waridley.textroid.ttv.monitor

import com.github.philippheuer.events4j.core.EventManager
import com.github.twitch4j.pubsub.PubSubSubscription
import com.github.twitch4j.pubsub.TwitchPubSub
import com.waridley.textroid.credentials.AuthenticationHelper

class ChannelPointsMonitor(
		authHelper: AuthenticationHelper,
		val pubsub: TwitchPubSub): AutoCloseable {
	
	var sub: PubSubSubscription? = null
	
	init {
		Thread {
			authHelper.retrieveCredential("TtvChannelMonitorCredential", listOf("channel:read:redemptions")) {
				println("Requesting Channel Points PubSub subscription")
				sub = pubsub.listenForChannelPointsRedemptionEvents(it, it.userId)
			}
		}.start()
	}
	
	override fun close() {
		sub?.let { pubsub.unsubscribeFromTopic(it) }
	}
	
}
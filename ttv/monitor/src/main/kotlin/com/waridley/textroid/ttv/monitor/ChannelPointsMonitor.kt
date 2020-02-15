package com.waridley.textroid.ttv.monitor

import com.github.philippheuer.events4j.core.EventManager
import com.github.twitch4j.pubsub.PubSubSubscription
import com.github.twitch4j.pubsub.TwitchPubSub
import com.waridley.textroid.api.logger
import com.waridley.textroid.credentials.AuthenticationHelper
import org.slf4j.LoggerFactory

class ChannelPointsMonitor(
		authHelper: AuthenticationHelper,
		val pubsub: TwitchPubSub
): AutoCloseable {
	
	val log = logger<ChannelPointsMonitor>()
	
	var sub: PubSubSubscription? = null
	
	init {
		Thread {
			authHelper.retrieveCredential("TtvChannelMonitorCredential", listOf("channel:read:redemptions")) { cred ->
				log.info("Requesting Channel Points PubSub subscription")
				sub = pubsub.listenForChannelPointsRedemptionEvents(cred, cred.userId)
			}
		}.start()
	}
	
	override fun close() {
		sub?.let { pubsub.unsubscribeFromTopic(it) }
	}
	
}
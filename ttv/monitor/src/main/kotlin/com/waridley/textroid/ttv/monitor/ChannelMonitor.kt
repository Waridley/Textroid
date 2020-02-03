package com.waridley.textroid.ttv.monitor

import com.github.philippheuer.events4j.core.EventManager
import com.github.twitch4j.pubsub.TwitchPubSub
import com.waridley.textroid.credentials.AuthenticationHelper

class ChannelMonitor(
	authHelper: AuthenticationHelper,
	eventManager: EventManager = EventManager().apply { autoDiscovery() }
) {

	init {
		authHelper.retrieveCredential("TtvChannelMonitorCredential", listOf("channel:read:redemptions")) {
			println("Requesting Channel Points PubSub subscription")
			TwitchPubSub(eventManager).listenForChannelPointsRedemptionEvents(it, it.userId)
		}
	}
}
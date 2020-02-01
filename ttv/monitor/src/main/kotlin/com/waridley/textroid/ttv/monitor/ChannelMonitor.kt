package com.waridley.textroid.ttv.monitor

import com.github.philippheuer.events4j.api.domain.IEvent
import com.github.philippheuer.events4j.core.EventManager
import com.github.philippheuer.events4j.reactor.ReactorEventHandler
import com.github.twitch4j.pubsub.TwitchPubSub
import com.github.twitch4j.pubsub.events.ChannelPointsRedemptionEvent
import com.waridley.textroid.credentials.AuthenticationHelper
import reactor.core.Disposable
import java.io.Closeable
import kotlin.reflect.KClass

class ChannelMonitor private constructor(
	val authHelper: AuthenticationHelper,
	val channelName: String,
	val eventManager: EventManager = EventManager()
): Closeable {
	
	val subscriptions = mutableListOf<Disposable>()
	
	fun start() {
		authHelper.retrieveCredential("TtvChannelMonitorCredential", listOf("channel:read:redemptions")) {
			println("Got credential!")
			subscriptions.add(eventManager.getEventHandler(ReactorEventHandler::class.java).onEvent(ChannelPointsRedemptionEvent::class.java) { event ->
				println(event)
			})
			val pubsub = TwitchPubSub(eventManager)
			val sub = pubsub.listenForChannelPointsRedemptionEvents(it, it.userId)
		}
	}
	
	fun <T: IEvent> waitFor(eventClass: KClass<T>, tick: Long = 1000L) {
		var shouldClose = false
		subscriptions.add(
			eventManager.getEventHandler(ReactorEventHandler::class.java).onEvent(eventClass.java) { shouldClose = true }
		)
		while(!shouldClose) { Thread.sleep(tick) }
	}
	
	override fun close() {
		subscriptions.forEach {
			it.dispose()
		}
	}
	
	companion object {
		operator fun invoke(
			authHelper: AuthenticationHelper,
			channelName: String,
			eventManager: EventManager = EventManager().apply { autoDiscovery() },
			block: ChannelMonitor.() -> Unit) {
			
			ChannelMonitor(authHelper, channelName, eventManager).also(block).close()
		}
	}
}
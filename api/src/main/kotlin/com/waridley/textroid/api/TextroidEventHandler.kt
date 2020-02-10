package com.waridley.textroid.api

import com.github.philippheuer.events4j.api.domain.IEvent
import com.github.philippheuer.events4j.core.EventManager
import com.github.philippheuer.events4j.reactor.ReactorEventHandler
import reactor.core.Disposable

abstract class TextroidEventHandler(val eventManager: EventManager = EVENT_MANAGER): AutoCloseable {
	@PublishedApi internal val handler: ReactorEventHandler = eventManager.getEventHandler(ReactorEventHandler::class.java)
	
	inline fun <reified T: IEvent> on(crossinline consumer: T.() -> Unit) {
		subs.add(handler.onEvent(T::class.java) {
			it.run {
				try {
					consumer()
				} catch(e: Exception) {
					e.printStackTrace()
				}
			}
		})
	}
	
	fun publish(event: IEvent) = eventManager.publish(event)
	
	fun IEvent.publish(event: IEvent) = this@TextroidEventHandler.publish(event)
	
	val subs = mutableListOf<Disposable>()
	
	override fun close() {
		subs.forEach { it.dispose() }
	}
}
package com.waridley.textroid.api

import com.github.philippheuer.events4j.api.domain.IEvent
import com.github.philippheuer.events4j.reactor.ReactorEventHandler
import reactor.core.Disposable

abstract class TextroidEventHandler: AutoCloseable {
	protected val handler: ReactorEventHandler = EVENT_MANAGER.getEventHandler(ReactorEventHandler::class.java)
	
	protected inline fun <reified T: IEvent> on(crossinline consumer: T.() -> Unit) {
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
	
	protected fun publish(event: IEvent) = EVENT_MANAGER.publish(event)
	
	val subs = mutableListOf<Disposable>()
	
	override fun close() {
		subs.forEach { it.dispose() }
	}
}
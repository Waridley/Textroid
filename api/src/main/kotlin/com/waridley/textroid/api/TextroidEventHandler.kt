package com.waridley.textroid.api

import com.github.philippheuer.events4j.api.domain.IDisposable
import com.github.philippheuer.events4j.api.domain.IEvent
import com.github.philippheuer.events4j.core.EventManager
import com.github.philippheuer.events4j.core.domain.Event
import com.github.philippheuer.events4j.reactor.ReactorEventHandler
import com.github.philippheuer.events4j.simple.SimpleEventHandler
import reactor.core.Disposable

abstract class TextroidEventHandler(val eventManager: EventManager = EVENT_MANAGER, initializer: (TextroidEventHandler.() -> Unit)? = null): AutoCloseable {
	
	open val log = logger<TextroidEventHandler>()
	
	@PublishedApi internal val handler = eventManager.getEventHandler(SimpleEventHandler::class.java)
	
	constructor(config: TextroidEventHandler.() -> Unit): this(EVENT_MANAGER, config)
	
	val subs: MutableList<IDisposable>
	
	init  {
		subs = mutableListOf()
		initializer?.let {
//			config(it) //Possibly causing loss of events and other weird bugs
			it()
		}
	}
	
	fun config(block: TextroidEventHandler.() -> Unit) {
		Thread { block() }.start()
	}
	
	inline fun <reified T: IEvent> on(crossinline consumer: T.() -> Unit): IDisposable? {
		return handler.onEvent(T::class.java) {
			it.run {
				try {
					consumer()
				} catch(e: Exception) {
					log.error("{}", e)
				}
			}
		}?.also { subs.add(it) }
	}
	
	fun publish(event: IEvent) = eventManager.publish(event)
	
	
	override fun close() {
		log.warn("Closing event handler {}", this.javaClass)
		subs.forEach { it.dispose() }
	}
	
}

abstract class TextroidEvent: Event()

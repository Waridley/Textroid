@file:Suppress("UNUSED")
package com.waridley.textroid.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.philippheuer.events4j.api.domain.IEvent
import com.github.philippheuer.events4j.core.EventManager
import com.github.philippheuer.events4j.core.domain.Event
import com.github.philippheuer.events4j.reactor.ReactorEventHandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import reactor.core.Disposable

object Global

val EVENT_MANAGER = EventManager().apply { autoDiscovery() }

inline fun <reified T> logger(): Logger = LoggerFactory.getLogger(T::class.java)
val LOG: Logger = logger<Global>()

val JACKSON = ObjectMapper()

const val CURRENCY_SYMBOL = '⌬'

object GlobalEventHandler: TextroidEventHandler()

inline fun <reified T: Event> on(crossinline consumer: T.() -> Unit) = GlobalEventHandler.on(consumer)

fun publish(event: IEvent) = EVENT_MANAGER.publish(event)

object Services: AutoCloseable {
	private val serviceMap = mutableMapOf<String, AutoCloseable>()
	
	init {
		add(GlobalEventHandler, "TextroidEvents")
	}
	
	fun add(service: AutoCloseable, name: String = service.javaClass.simpleName) {
		if(serviceMap.putIfAbsent(name, service) != null) throw Exception("There is already a service named $name!")
	}
	
	operator fun get(name: String) = serviceMap[name]
	
	operator fun set(name: String, service: AutoCloseable) {
		serviceMap.putIfAbsent(name, service)?.close()
		EVENT_MANAGER.serviceMediator.addService(name, service)
	}
	
	operator fun invoke(block: Services.() -> Unit) {
		this.block()
	}
	
	operator fun invoke(vararg services: AutoCloseable) {
		services.forEach { add(it) }
	}
	
	fun remove(name: String) {
		serviceMap[name]?.close()
		serviceMap.remove(name)
	}
	
	override fun close() {
		serviceMap.values.forEach { it.close() }
	}
}

inline fun <reified T: IEvent> ReactorEventHandler.on(crossinline consumer: T.() -> Unit): Disposable =
		onEvent(T::class.java) { it.consumer() }

inline fun <reified T: IEvent> ReactorEventHandler.take(n: Long = 1L, crossinline consumer: T.() -> Unit): Disposable =
		processor.publishOn(scheduler).ofType(T::class.java).take(n).limitRequest(15).subscribe { it.consumer() }
@file:Suppress("UNUSED")
package com.waridley.textroid.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.philippheuer.events4j.api.domain.IEvent
import com.github.philippheuer.events4j.core.EventManager
import com.github.philippheuer.events4j.core.domain.Event
import com.github.philippheuer.events4j.reactor.ReactorEventHandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.function.Consumer

object Global

val EVENT_MANAGER = EventManager().apply { autoDiscovery() }

val LOG: Logger = LoggerFactory.getLogger(Global::class.java)

val JACKSON = ObjectMapper()

val CURRENCY_SYMBOL = '⌬'

object GlobalEventHandler: TextroidEventHandler()

inline fun <reified T: Event> on(crossinline consumer: T.() -> Unit) = GlobalEventHandler.on(consumer)
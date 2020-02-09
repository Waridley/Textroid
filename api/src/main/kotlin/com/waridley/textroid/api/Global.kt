@file:Suppress("UNUSED")
package com.waridley.textroid.api

import com.github.philippheuer.events4j.api.domain.IEvent
import com.github.philippheuer.events4j.core.EventManager
import com.github.philippheuer.events4j.reactor.ReactorEventHandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Global

val EVENT_MANAGER = EventManager().apply { autoDiscovery() }

val LOG: Logger = LoggerFactory.getLogger(Global::class.java)

private val eventTracer = if(LOG.isTraceEnabled) {
	EVENT_MANAGER.getEventHandler(ReactorEventHandler::class.java).onEvent(IEvent::class.java) {
		LOG.trace("$it")
	}
} else { null }
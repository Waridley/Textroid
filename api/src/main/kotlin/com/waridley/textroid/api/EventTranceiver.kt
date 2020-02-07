package com.waridley.textroid.api

import com.github.philippheuer.events4j.api.domain.IEvent
import com.github.philippheuer.events4j.core.EventManager

interface EventTranceiver {
	val eventManager: EventManager
	
	fun sendEvent(event: IEvent)
	
	fun receiveEvent(event: IEvent) {
		eventManager.publish(event)
	}
}
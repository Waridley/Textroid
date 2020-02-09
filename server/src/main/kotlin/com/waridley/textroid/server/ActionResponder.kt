package com.waridley.textroid.server

import com.github.philippheuer.events4j.api.domain.IEvent
import com.github.philippheuer.events4j.core.EventManager
import com.github.philippheuer.events4j.reactor.ReactorEventHandler
import com.waridley.textroid.MintApprovedEvent
import com.waridley.textroid.api.EVENT_MANAGER
import com.waridley.textroid.api.LOG
import com.waridley.textroid.api.TextroidEventHandler
import reactor.core.Disposable
import java.lang.RuntimeException

typealias executor = ActionExecutor

class ActionResponder: TextroidEventHandler() {
	
	init {
		on<MintApprovedEvent> {
			executor.adjustCurrency(player, amount)
		}
	}
	
}
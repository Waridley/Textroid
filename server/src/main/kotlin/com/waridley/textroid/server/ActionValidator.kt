package com.waridley.textroid.server

import com.waridley.textroid.MintApprovedEvent
import com.waridley.textroid.MintRequestEvent
import com.waridley.textroid.api.LOG
import com.waridley.textroid.api.TextroidEventHandler

class ActionValidator: TextroidEventHandler() {
	
	init {
		on<MintRequestEvent> {
			LOG.info("Pretending to validate $this")
			publish (MintApprovedEvent(player, amount, this))
		}
	}
	
}
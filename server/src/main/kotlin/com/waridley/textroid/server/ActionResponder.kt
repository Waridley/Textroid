package com.waridley.textroid.server

import com.waridley.textroid.InfoRequestEvent
import com.waridley.textroid.MintApprovedEvent
import com.waridley.textroid.api.TextroidEventHandler

typealias executor = ActionExecutor

class ActionResponder: TextroidEventHandler() {
	
	init {
		on<MintApprovedEvent> {
			executor.adjustCurrency(player, amount)
		}
		on<InfoRequestEvent.CurrencyInBank> {
			responseHandler("${executor.readCurrency(player)}")
		}
	}
	
}
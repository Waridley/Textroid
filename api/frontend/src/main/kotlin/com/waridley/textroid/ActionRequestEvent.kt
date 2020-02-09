package com.waridley.textroid

import com.github.philippheuer.events4j.api.domain.IEvent
import com.github.philippheuer.events4j.core.domain.Event
import com.waridley.textroid.api.Player

sealed class ActionRequestEvent: Event()

data class MintRequestEvent(val player: Player, val amount: Long, val source: IEvent): ActionRequestEvent()

sealed class InfoRequestEvent: ActionRequestEvent() {
	data class CurrencyInBank(val player: Player, val source: IEvent): InfoRequestEvent()
}

data class ChatCommandEvent(val player: Player, val message: String, val source: IEvent): ActionRequestEvent()
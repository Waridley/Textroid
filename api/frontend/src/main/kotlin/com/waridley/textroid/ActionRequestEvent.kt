package com.waridley.textroid

import com.fasterxml.jackson.annotation.JsonIgnore
import com.github.philippheuer.events4j.api.domain.IEvent
import com.waridley.textroid.api.Player
import com.waridley.textroid.api.TextroidEvent
import kotlin.reflect.KProperty

sealed class ActionRequestEvent: TextroidEvent()

data class MintRequestEvent(val player: Player, val amount: Long, val source: IEvent): ActionRequestEvent()

sealed class InfoRequestEvent: ActionRequestEvent() {
	data class CurrencyInBank(
			val player: Player,
			@JsonIgnore val respond: (response: String) -> Unit,
			val source: IEvent
	): InfoRequestEvent()
}

data class ChatCommandEvent(
		val player: Player,
		val command: String,
		val args: String,
		@JsonIgnore val respond: (response: String) -> Unit,
		val source: IEvent
): ActionRequestEvent()

sealed class Command

class Response(val action: ChatCommandEvent.() -> Any?): Command() {
	operator fun invoke(event: ChatCommandEvent) = event.action()
}

class Trigger(val action: ChatCommandEvent.() -> IEvent): Command() {
	operator fun invoke(event: ChatCommandEvent) = event.action()
}
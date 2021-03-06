package com.waridley.textroid

import com.github.philippheuer.events4j.api.domain.IEvent
import com.waridley.textroid.api.Player
import com.waridley.textroid.api.TextroidEvent

sealed class ActionApprovedEvent: TextroidEvent()

data class MintApprovedEvent(val player: Player, val amount: Long, val request: IEvent): ActionApprovedEvent()

data class CommandApprovedEvent(val commandEvent: ChatCommandEvent): ActionApprovedEvent()
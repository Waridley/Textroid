package com.waridley.textroid

import com.github.philippheuer.events4j.api.domain.IEvent
import com.github.philippheuer.events4j.core.domain.Event
import com.waridley.textroid.api.Player

sealed class ActionApprovedEvent: Event()

data class MintApprovedEvent(val player: Player, val amount: Long, val request: IEvent): ActionApprovedEvent()
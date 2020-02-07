package com.waridley.textroid

import com.github.philippheuer.events4j.api.domain.IEvent
import com.github.philippheuer.events4j.core.domain.Event
import com.waridley.textroid.api.Player

sealed class ActionRequestEvent(val trigger: IEvent): Event()

data class RequestMintEvent(val player: Player, val reason: IEvent): ActionRequestEvent(reason)

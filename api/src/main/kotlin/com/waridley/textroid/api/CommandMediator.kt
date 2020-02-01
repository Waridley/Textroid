package com.waridley.textroid.api

import com.waridley.textroid.engine.Player

interface CommandMediator {
	fun loadPlayerByTtvLogin(login: String): Player?
	fun loadPlayerByTtvUserId(userId: String): Player?
}
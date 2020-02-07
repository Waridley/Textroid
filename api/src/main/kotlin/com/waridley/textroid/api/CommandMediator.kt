package com.waridley.textroid.api

interface CommandMediator {
	fun loadPlayerByTtvLogin(login: String): Player?
	fun loadPlayerByTtvUserId(userId: String): Player?
}
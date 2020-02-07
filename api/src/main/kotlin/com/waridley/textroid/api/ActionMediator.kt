package com.waridley.textroid.api

interface ActionMediator {
	fun loadPlayerByTtvLogin(login: String): Player?
	fun loadPlayerByTtvUserId(userId: String): Player?
}
package com.waridley.textroid.server

import com.waridley.textroid.api.Player
import com.waridley.textroid.api.storage
import com.waridley.textroid.BankAccount

object ActionExecutor {
	private var Player.currencyInBankAccount: Long by storage(BankAccount::currentAmount) { 0L }
	
	@Synchronized fun adjustCurrency(player: Player, amount: Long) {
		player.currencyInBankAccount += amount
	}
	
	fun readCurrency(player: Player): Long {
		return player.currencyInBankAccount
	}
	
	
}
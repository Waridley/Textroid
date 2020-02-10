package com.waridley.textroid

import com.github.twitch4j.helix.domain.User
import com.mongodb.ConnectionString
import com.waridley.textroid.api.*
import com.waridley.textroid.mongo.game.MongoPlayerStorage
import com.waridley.textroid.mongo.game.MongoStorage
import com.waridley.textroid.ttv.TtvUser
import org.litote.kmongo.KMongo
import kotlin.random.Random
import kotlin.random.nextULong

fun main(args: Array<String>) {
	println((TtvUser::helixUser / User::getId).path)
}

@ExperimentalUnsignedTypes
@Suppress("UNUSED")
fun testPlayer(connStr: String) {
	val db = KMongo.createClient(
			connectionString = ConnectionString(connStr)
	).getDatabase("chatgame")
	
	val players = MongoPlayerStorage(db, "test")
	
	val randomPlayer = players.new(Random.nextULong().toString(32))
	val player4 = players["testuser4"] ?: players.new("testuser4")
	player4.answer = 42
	println("${player4.answer}")
	player4.brain = "Nope! :)"
	println(player4.brain)
	
	player4.nickname = Player.Name(Random.nextULong().toString(32))
	
	println("Random player username: ${randomPlayer.username}")
	println("Random player nickname: ${randomPlayer.nickname}")
	println(randomPlayer)
	println("Player 4 username: ${player4.username}")
	println("Player 4 nickname: ${player4.nickname}")
	println(player4)
	
	val x = player4 offer 200 to randomPlayer
	println(x)
	
	fun testUsernames(vararg values: String) {
		for (v in values) {
			try {
				players.new(v)
			} catch (e: Exception) {
				println("Caught $e")
			}
		}
	}
	
	testUsernames(
			"",
			"               ",
			"\t\t\t\t\t",
			Long.MAX_VALUE.toString(2),
			"testuser4"
	)
	
}

infix fun Player?.offer(amount: Int) = Offer(this, amount)
data class Offer(val from: Player?, val amount: Int)

class Launcher(val answer: Int, val brain: String?)

var Player.answer:Int by storage(Launcher::answer)
var Player.brain: String? by storage(Launcher::brain)
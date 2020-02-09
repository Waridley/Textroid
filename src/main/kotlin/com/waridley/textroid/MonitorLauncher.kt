package com.waridley.textroid

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import com.github.philippheuer.credentialmanager.CredentialManagerBuilder
import com.github.twitch4j.auth.providers.TwitchIdentityProvider
import com.github.twitch4j.pubsub.TwitchPubSub
import com.mongodb.ConnectionString
import com.mongodb.client.MongoDatabase
import com.waridley.textroid.api.EVENT_MANAGER
import com.waridley.textroid.api.PlayerStorageInterface
import com.waridley.textroid.credentials.AuthenticationHelper
import com.waridley.textroid.credentials.DesktopAuthController
import com.waridley.textroid.mongo.credentials.MongoCredentialMap
import com.waridley.textroid.mongo.game.MongoPlayerStorage
import com.waridley.textroid.server.ActionResponder
import com.waridley.textroid.server.ActionValidator
import com.waridley.textroid.ttv.TtvEventConverter
import com.waridley.textroid.ttv.monitor.ChannelPointsMonitor
import org.litote.kmongo.KMongo
import java.net.URI

class MonitorLauncher : CliktCommand("ttvchannelmonitor") {
	private val channelName by option(
			"-c",
			"--channel",
			help = "The name of the channel to join."
	).prompt("Channel name")
	private val clientId by option("-i", "--client-id", help = "Your application's client ID").prompt("Client ID")
	private val clientSecret by option(
			"-s",
			"--client-secret",
			help = "Your application's client secret."
	).prompt("Client secret", hideInput = true)
	private val redirectUrl by option(
			"-r",
			"--redirect-url",
			help = "The redirect URL for OAuth2 code flow. Must match the URL registered with your client ID."
	).default("http://localhost")
	private val dbConnStr by option(
			"-d",
			"--connection-string",
			help = "Your MongoDB connection string"
	).prompt("MongoDB connection string")
	
	var redirectPort: Int = 80
	
	var playerStorage: PlayerStorageInterface? = null
	
	override fun run() {
		redirectPort = URI(redirectUrl).port
		
		val db = KMongo.createClient(
				connectionString = ConnectionString(dbConnStr)
		).getDatabase("chatgame")
		
		startMonitors(db)
	}
	
	fun startMonitors(db: MongoDatabase) {
		val idProvider = TwitchIdentityProvider(clientId, clientSecret, redirectUrl)
		val credentialManager = CredentialManagerBuilder.builder()
				.withAuthenticationController(DesktopAuthController("$redirectUrl/info.html"))
				.withStorageBackend(MongoCredentialMap<String>(db))
				.build()
		credentialManager.registerIdentityProvider(idProvider)
		playerStorage = MongoPlayerStorage(db, "test")
		val authHelper = AuthenticationHelper(idProvider, redirectUrl, redirectPort)
		
		ChannelPointsMonitor(authHelper, TwitchPubSub(EVENT_MANAGER))
		TtvEventConverter(playerStorage!!)
		ActionValidator()
		ActionResponder()
		
	}
}

fun main(args: Array<String>) = MonitorLauncher().main(args)

package com.waridley.textroid

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import com.github.philippheuer.credentialmanager.CredentialManagerBuilder
import com.github.philippheuer.events4j.api.domain.IEvent
import com.github.twitch4j.auth.providers.TwitchIdentityProvider
import com.github.twitch4j.pubsub.TwitchPubSub
import com.mongodb.ConnectionString
import com.mongodb.client.MongoDatabase
import com.waridley.textroid.api.*
import com.waridley.textroid.credentials.AuthenticationHelper
import com.waridley.textroid.credentials.DesktopAuthController
import com.waridley.textroid.mongo.MongoEventLogger
import com.waridley.textroid.mongo.credentials.MongoCredentialMap
import com.waridley.textroid.mongo.game.MongoPlayerStorage
import com.waridley.textroid.server.Server
import com.waridley.textroid.ttv.TtvEventConverter
import com.waridley.textroid.ttv.chat_client.TtvChatGameClient
import com.waridley.textroid.ttv.monitor.ChannelPointsMonitor
import org.litote.kmongo.KMongo
import java.net.URI

class MongoLauncher : CliktCommand(name = "textroid") {
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
		val playerStorage = MongoPlayerStorage(db)
		val cpAuthHelper = AuthenticationHelper(idProvider, redirectUrl, redirectPort)
		
		MongoEventLogger(db)
		ChannelPointsMonitor(cpAuthHelper, TwitchPubSub(EVENT_MANAGER))
//		TtvChatGameClient(cpAuthHelper, channelName)
		TtvEventConverter(playerStorage)
		Server()
		
		on<IEvent> { LOG.trace("$this") }
	}
}

fun main(args: Array<String>) = MongoLauncher().main(args)
package com.waridley.textroid

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import com.github.philippheuer.credentialmanager.CredentialManagerBuilder
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential
import com.github.philippheuer.events4j.core.domain.Event
import com.github.twitch4j.auth.providers.TwitchIdentityProvider
import com.github.twitch4j.pubsub.TwitchPubSub
import com.mongodb.ConnectionString
import com.mongodb.client.MongoDatabase
import com.waridley.textroid.api.*
import com.waridley.textroid.credentials.AuthenticationHelper
import com.waridley.textroid.credentials.DesktopAuthController
import com.waridley.textroid.credentials.orNull
import com.waridley.textroid.mongo.MongoEventLogger
import com.waridley.textroid.mongo.credentials.MongoCredentialMap
import com.waridley.textroid.mongo.game.MongoPlayerStorage
import com.waridley.textroid.mongo.ttv.MongoTtvUserStorage
import com.waridley.textroid.server.Server
import com.waridley.textroid.ttv.monitor.TtvEventConverter
import com.waridley.textroid.ttv.chat_client.TtvChatGameClient
import com.waridley.textroid.ttv.monitor.ChannelPointsMonitor
import com.waridley.textroid.ttv.monitor.WatchtimeMonitor
import org.litote.kmongo.KMongo

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
	private val commandsScriptFilePath by option(
			"-f",
			"--commands-script-path",
			help = "The path to the script file for commands"
	).default("server/src/main/resources/Commands.kts")
	
	
	override fun run() {
		on<Event> { LOG.trace("$this") }
		
		val db = KMongo.createClient(
				connectionString = ConnectionString(dbConnStr)
		).getDatabase("chatgame")
		
		startServices(db)
	}
	
	fun startServices(db: MongoDatabase) {
		val idProvider = TwitchIdentityProvider(clientId, clientSecret, redirectUrl)
		val credStorage = MongoCredentialMap<String>(db)
		val credentialManager = CredentialManagerBuilder.builder()
				.withAuthenticationController(DesktopAuthController("$redirectUrl/info.html"))
				.withStorageBackend(credStorage)
				.build()
		credentialManager.registerIdentityProvider(idProvider)
		val playerStorage = MongoPlayerStorage(db)
		val authHelper = AuthenticationHelper(idProvider, redirectUrl)
		val appCredential = (credStorage["TextroidAppAccessToken"] as OAuth2Credential?)
				            ?: idProvider.appAccessToken.let { cred ->
			                    (idProvider.getAdditionalCredentialInformation(cred).orNull() ?: cred)
					                    .also { credStorage["TextroidAppAccessToken"] = it }
		                    }
		
		val ttvUserStorage = MongoTtvUserStorage(db, credential = appCredential)
		
		Services (
				MongoEventLogger(db),
				ChannelPointsMonitor(authHelper, TwitchPubSub(EVENT_MANAGER)),
				WatchtimeMonitor(channelName, 1L, credential = appCredential),
				TtvChatGameClient(authHelper, channelName),
				TtvEventConverter(playerStorage, ttvUserStorage),
				Server(commandsScriptFilePath)
		)
	}
}

fun main(args: Array<String>) = MongoLauncher().main(args)
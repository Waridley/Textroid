package com.waridley.textroid

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import com.github.philippheuer.credentialmanager.CredentialManagerBuilder
import com.github.twitch4j.auth.providers.TwitchIdentityProvider
import com.mongodb.ConnectionString
import com.waridley.textroid.credentials.AuthenticationHelper
import com.waridley.textroid.credentials.DesktopAuthController
import com.waridley.textroid.mongo.credentials.MongoCredentialMap
import com.waridley.textroid.ttv.monitor.ChannelMonitor
import org.litote.kmongo.KMongo

class MonitorLauncher: CliktCommand() {
	private val channelName by option("-c", "--channel", help = "The name of the channel to join.").prompt("Channel name:")
	private val clientId by option("-i", "--client-id", help = "Your application's client ID").prompt("Client ID:")
	private val clientSecret by option("-s", "--client-secret", help = "Your application's client secret.").prompt("Client secret:", hideInput = true)
	private val redirectUrl by option("-r", "--redirect-url", help = "The redirect URL for OAuth2 code flow. Must match the URL registered with your client ID.").default("http://localhost")
	private val dbConnStr by option("-d", "--connection-string", help = "Your MongoDB connection string").prompt("MongoDB connection string:")
	private val redirectPort = 4242
	
	override fun run() {
		val idProvider = TwitchIdentityProvider(clientId, clientSecret, redirectUrl)
		val db = KMongo.createClient(
			connectionString = ConnectionString(dbConnStr)
		).getDatabase("chatgame")
		
		val credentialManager = CredentialManagerBuilder.builder()
			.withAuthenticationController(DesktopAuthController("$redirectUrl/info.html"))
			.withStorageBackend(MongoCredentialMap<String>(db))
			.build()
		credentialManager.registerIdentityProvider(idProvider)
		
		val authHelper = AuthenticationHelper(idProvider, redirectUrl, redirectPort)
		ChannelMonitor(authHelper)

	}
}
fun main(args: Array<String>) = MonitorLauncher().main(args)
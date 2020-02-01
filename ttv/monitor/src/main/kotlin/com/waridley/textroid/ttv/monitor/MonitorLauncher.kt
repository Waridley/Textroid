package com.waridley.textroid.ttv.monitor

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import com.github.philippheuer.credentialmanager.CredentialManagerBuilder
import com.github.philippheuer.events4j.api.domain.IEvent
import com.github.philippheuer.events4j.api.service.IServiceMediator
import com.github.twitch4j.auth.providers.TwitchIdentityProvider
import com.mongodb.ConnectionString
import com.waridley.textroid.credentials.AuthenticationHelper
import com.waridley.textroid.credentials.DesktopAuthController
import com.waridley.textroid.mongo.credentials.MongoCredentialMap
import org.litote.kmongo.KMongo
import java.io.Closeable
import java.util.*

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
		ChannelMonitor(authHelper, channelName) {
			waitFor(CloseEvent::class)
		}
	}
}

class CloseEvent: IEvent {
	override fun getFiredAt(): Calendar {
		TODO("auto-generated function body")
	}
	
	override fun setServiceMediator(serviceMediator: IServiceMediator?) {
		TODO("auto-generated function body")
	}
	
	override fun setEventId(id: String?) {
		TODO("auto-generated function body")
	}
	
	override fun getServiceMediator(): IServiceMediator {
		TODO("auto-generated function body")
	}
	
	override fun getEventId(): String {
		TODO("auto-generated function body")
	}
	
	override fun setFiredAt(calendar: Calendar?) {
		TODO("auto-generated function body")
	}
	
}

fun main(args: Array<String>) = MonitorLauncher().main(args)
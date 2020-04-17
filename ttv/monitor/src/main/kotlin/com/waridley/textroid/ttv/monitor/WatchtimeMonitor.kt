package com.waridley.textroid.ttv.monitor

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential
import com.github.philippheuer.events4j.core.EventManager
import com.github.philippheuer.events4j.core.domain.Event
import com.github.twitch4j.helix.TwitchHelix
import com.github.twitch4j.helix.TwitchHelixBuilder
import com.github.twitch4j.helix.domain.StreamList
import com.github.twitch4j.helix.domain.User
import com.github.twitch4j.helix.domain.UserList
import com.github.twitch4j.tmi.TwitchMessagingInterface
import com.github.twitch4j.tmi.TwitchMessagingInterfaceBuilder
import com.github.twitch4j.tmi.domain.Chatters
import com.github.twitch4j.tmi.domain.HostList
import com.waridley.textroid.api.*
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit.*

class WatchtimeMonitor(
		val channelName: String,
		val intervalMinutes: Long = 10L,
		val credential: OAuth2Credential? = null,
		val tmi: TwitchMessagingInterface = TwitchMessagingInterfaceBuilder.builder().build(),
		val helix: TwitchHelix = TwitchHelixBuilder.builder().build(),
		eventManager: EventManager = EVENT_MANAGER
): TextroidEventHandler(eventManager) {
	
	override val log = logger<WatchtimeMonitor>()
	
	private val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
	
	private val channelId =
			helix.getUsers(credential?.accessToken, null, listOf(channelName)).execute().users.first().id
	
	init {
		config {
			executor.scheduleAtFixedRate(::checkOnline, 0L, intervalMinutes, MINUTES)
			on<TtvWatchtimeEvent.Online> { checkHosts() }
			on<TtvWatchtimeEvent.Offline> { checkGuestChat() }
			on<StreamsFutureEvent> { waitForResponse() }
			on<GuestsFutureEvent> { waitForResponse() }
			on<HostsFutureEvent> { waitForResponse() }
			on<ChattersFutureEvent> { waitForResponse() }
		}
	}
	
	fun checkOnline() {
		try {
			println("\n\n")
			log.trace("Checking if $channelName is online.")
			val future = helix.getStreams(
					credential?.accessToken,
					"", null, 1, null, null, null, null, listOf(channelName)
			).queue()
			
			publish(StreamsFutureEvent(future))
//			StreamsFutureEvent(future).waitForResponse()
		} catch (e: Exception) {
			log.error("Error while checking if online: {}", e)
		}
	}
	
	fun checkGuestChat() {
		try {
			log.info("Checking if $channelName is hosting anyone.")
			val future = tmi.getHosts(listOf(channelId)).queue()
			publish(GuestsFutureEvent(future))
//			GuestsFutureEvent(future).waitForResponse()
		} catch (e: Exception) {
			log.error("Error getting guest chatters: {}", e)
		}
	}
	
	fun checkHosts() {
		try {
			log.info("Checking for hosts of $channelName.")
			val future = tmi.getHostsOf(channelId).queue()
			publish(HostsFutureEvent(future))
//			HostsFutureEvent(future).waitForResponse()
		} catch (e: Exception) {
			log.error("Error getting hosts: {}", e)
		}
	}
	
	fun getChatters(channelName: String, callback: Chatters.() -> Unit) {
		try {
			log.trace("Requesting chatters list.")
			val chattersFuture = tmi.getChatters(channelName).queue()
			publish(ChattersFutureEvent(chattersFuture, callback))
//			chattersFuture.callback()
		} catch (e: Exception) {
			log.error("Error getting chatters: {}", e)
		}
	}
	
	private fun StreamsFutureEvent.waitForResponse() {
		log.trace("$this")
		streamListFuture.get(30L, SECONDS)?.also { log.trace("StreamList: $it") }?.streams?.firstOrNull()?.run {
			log.trace("Stream record: $this")
			if(type.equals("live", true)) {
				log.info("$channelName is live!")
				getChatters(channelName) {
					getHelixUsers(allViewers + channelName) {
						publish(TtvWatchtimeEvent.Online(this, intervalMinutes))
					}
				}
			} else {
				log.error("Stream found, but type is not \"live\": {}", this)
			}
		} ?: run {
			log.info("No stream record found. Getting offline chatters.")
			getChatters(channelName) {
				log.trace("Getting Helix users for $this")
				getHelixUsers(allViewers + channelName) {
					log.trace("Publishing offline watchtime event for: $this")
					publish(TtvWatchtimeEvent.Offline(this, intervalMinutes))
				}
			}
		}
	}
	
	private fun GuestsFutureEvent.waitForResponse() {
		guestsFuture.get(30L, SECONDS)?.also { log.trace("Guest: $it") }?.hosts?.firstOrNull()?.let { hostRecord ->
			hostRecord.targetLogin?.let {
				getChatters(it) {
					getHelixUsers(allViewers + hostRecord.targetLogin) {
						publish(TtvWatchtimeEvent.Guest(this, intervalMinutes, hostRecord))
					}
				}
			} ?: log.info("Not currently hosting anyone.")
		}
	}
	
	private fun HostsFutureEvent.waitForResponse() {
		hostsFuture.get(30L, SECONDS)?.also { log.trace("Hosts: $it") }?.hosts?.forEach { hostRecord ->
			getChatters(hostRecord.hostLogin) {
				getHelixUsers(listOf(hostRecord.hostLogin)) {
					val hostChannel = this[0]
					getHelixUsers(allViewers) {
						publish(TtvWatchtimeEvent.Host(this, hostChannel, intervalMinutes, hostRecord))
					}
				}
			}
		}
	}
	
	private fun ChattersFutureEvent.waitForResponse() {
		chattersFuture.get(30L, SECONDS)?.also { log.trace("Chatters: $it") }?.callback()
	}
	
	override fun close() {
		try {
			executor.shutdown()
			executor.awaitTermination(30L, SECONDS)
		} catch (e: Exception) {
			log.error("Couldn't shut down executor: {}", e)
		} finally {
			super.close()
		}
	}
	
	private data class StreamsFutureEvent(val streamListFuture: Future<StreamList>): Event()
	
	private data class ChattersFutureEvent(val chattersFuture: Future<Chatters>, val callback: Chatters.() -> Unit): Event()
	private data class GuestsFutureEvent(val guestsFuture: Future<HostList>): Event()
	private data class HostsFutureEvent(val hostsFuture: Future<HostList>): Event()
	
	private val helixUserCache = mutableMapOf<String, User>()
	fun getHelixUsers(logins: List<String>, consumer: List<User>.() -> Unit) {
		val foundUsers = mutableListOf<User>()
		val unknownLogins = mutableListOf<String>()
		for(login in logins) {
			helixUserCache[login]?.let {
				foundUsers += it
			} ?: run { unknownLogins += login }
		}
		val loginLists = unknownLogins.chunked(100)
		for(list in loginLists) {
			val userList: UserList = helix.getUsers(
					credential?.accessToken,
					null,
					list
			).execute()
			foundUsers.addAll(userList.users)
		}
		consumer(foundUsers)
	}
}

sealed class TtvWatchtimeEvent: Event() {
	data class Online(val users: List<User>, val time: Long): TtvWatchtimeEvent()
	data class Guest(val users: List<User>, val time: Long, val hostRecord: com.github.twitch4j.tmi.domain.Host): TtvWatchtimeEvent()
	data class Host(val users: List<User>, val hostChannel: User, val time: Long, val hostRecord: com.github.twitch4j.tmi.domain.Host): TtvWatchtimeEvent()
	data class Offline(val users: List<User>, val time: Long): TtvWatchtimeEvent()
}
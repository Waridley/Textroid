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
import kotlin.math.min

class WatchtimeMonitor(
		val channelName: String,
		val intervalMinutes: Long = 10L,
		val credential: OAuth2Credential? = null,
		val tmi: TwitchMessagingInterface = TwitchMessagingInterfaceBuilder.builder().build(),
		val helix: TwitchHelix = TwitchHelixBuilder.builder().build(),
		eventManager: EventManager = EVENT_MANAGER
): TextroidEventHandler(eventManager) {
	
	val log = logger<WatchtimeMonitor>()
	
	private val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
	
	private val channelId =
			helix.getUsers(credential?.accessToken, null, listOf(channelName)).execute().users.first().id
	
	init {
		config {
			executor.scheduleAtFixedRate(::checkOnline, 0L, intervalMinutes, MINUTES)
			on<TtvWatchtimeEvent.Online> { checkHosts() }
			on<TtvWatchtimeEvent.Offline> { checkGuestChat() }
			
			on<StreamsResponse> {
				streamListFuture.get().streams.firstOrNull()?.run {
					if(type.equals("live", true)) {
						getChatters(channelName) {
							getHelixUsersFromLogins(allViewers + channelName) {
								eventManager.publish(TtvWatchtimeEvent.Online(users, intervalMinutes))
							}
						}
					} else {
						log.error("Stream found, but type is not \"live\": {}", this)
					}
				} ?: getChatters(channelName) {
					getHelixUsersFromLogins(allViewers + channelName) {
						eventManager.publish(TtvWatchtimeEvent.Offline(users, intervalMinutes))
					}
				}
			}
			on<GuestsResponse> {
				guestsFuture.get().hosts.firstOrNull()?.let { hostRecord ->
					hostRecord.targetLogin?.let {
						getChatters(it) {
							getHelixUsersFromLogins(allViewers + hostRecord.targetLogin) {
								eventManager.publish(TtvWatchtimeEvent.Guest(users, intervalMinutes, hostRecord))
							}
						}
					} ?: log.info("Not currently hosting anyone.")
				}
			}
			
			on<HostsResponse> {
				hostsFuture.get().hosts.forEach { hostRecord ->
					getChatters(hostRecord.hostLogin) {
						getHelixUsersFromLogins(allViewers + hostRecord.hostLogin) {
							eventManager.publish(TtvWatchtimeEvent.Host(users, intervalMinutes, hostRecord))
						}
					}
				}
			}
			
			on<ChattersResponse> { this.chattersFuture.get(30L, SECONDS).callback() }
		}
	}
	
	fun checkOnline() {
		val future = helix.getStreams(
				credential?.accessToken,
				"", null, 1, null, null, null, null, listOf(channelName)
		).queue()
		
		eventManager.publish(StreamsResponse(future))
	}
	
	fun checkGuestChat() {
		val future = tmi.getHosts(listOf(channelId)).queue()
		eventManager.publish(GuestsResponse(future))
	}
	
	fun checkHosts() {
		val future = tmi.getHostsOf(channelId).queue()
		eventManager.publish(HostsResponse(future))
	}
	
	fun getChatters(channelName: String, callback: Chatters.() -> Unit) {
		val chattersFuture = tmi.getChatters(channelName).queue()
		eventManager.publish(ChattersResponse(chattersFuture, callback))
	}
	
	override fun close() {
		executor.shutdown()
		executor.awaitTermination(30L, MINUTES)
		super.close()
	}
	
	private class StreamsResponse(val streamListFuture: Future<StreamList>): Event()
	
	private class ChattersResponse(val chattersFuture: Future<Chatters>, val callback: Chatters.() -> Unit): Event()
	private class GuestsResponse(val guestsFuture: Future<HostList>): Event()
	private class HostsResponse(val hostsFuture: Future<HostList>): Event()
	
	
	fun getHelixUsersFromLogins(logins: List<String>, consumer: UserList.() -> Unit) {
		val loginLists = logins / 100
		for(list in loginLists) {
			val userList: UserList = helix.getUsers(
					credential?.accessToken,
					null,
					list
			).execute()
			consumer(userList)
		}
	}
}

sealed class TtvWatchtimeEvent: Event() {
	data class Online(val users: List<User>, val time: Long): TtvWatchtimeEvent()
	data class Guest(val users: List<User>, val time: Long, val hostRecord: com.github.twitch4j.tmi.domain.Host): TtvWatchtimeEvent()
	data class Host(val users: List<User>, val time: Long, val hostRecord: com.github.twitch4j.tmi.domain.Host): TtvWatchtimeEvent()
	data class Offline(val users: List<User>, val time: Long): TtvWatchtimeEvent()
}

operator fun <T> List<T>.div(chunkSize: Int): Sequence<List<T>> {
	return sequence {
		for(i in 0..size step chunkSize) {
			yield(subList(i, min(i + chunkSize, size)))
		}
	}
}
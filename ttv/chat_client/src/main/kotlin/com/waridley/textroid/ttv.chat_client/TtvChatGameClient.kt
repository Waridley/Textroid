package com.waridley.textroid.ttv.chat_client

import com.github.twitch4j.auth.domain.TwitchScopes.*
import com.github.twitch4j.chat.TwitchChat
import com.github.twitch4j.chat.TwitchChatBuilder
import com.waridley.textroid.api.EVENT_MANAGER
import com.waridley.textroid.credentials.AuthenticationHelper

class TtvChatGameClient(authHelper: AuthenticationHelper, channelName: String, commandPrefix: String = "%"): AutoCloseable {
	
	var twitchChat: TwitchChat? = null
	
	init {
		Thread {
			authHelper.retrieveCredential("TtvChatClientCredential",
			                              listOf(CHAT_READ,
			                                     CHAT_EDIT,
			                                     CHAT_WHISPERS_READ,
			                                     CHAT_WHISPERS_EDIT,
			                                     CHAT_CHANNEL_MODERATE)) {
				twitchChat = TwitchChatBuilder.builder()
						.withEventManager(EVENT_MANAGER)
						.withCredentialManager(authHelper.identityProvider.credentialManager)
						.withChatAccount(it)
						.withCommandTrigger(commandPrefix)
						.build()
				twitchChat?.joinChannel(channelName)
				
			}
		}.start()
	}
	
	override fun close() {
		twitchChat?.close()
		twitchChat = null
	}
	
}

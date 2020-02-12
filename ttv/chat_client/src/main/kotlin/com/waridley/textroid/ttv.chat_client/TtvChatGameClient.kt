package com.waridley.textroid.ttv.chat_client

import com.github.twitch4j.auth.domain.TwitchScopes.*
import com.github.twitch4j.chat.TwitchChatBuilder
import com.waridley.textroid.api.EVENT_MANAGER
import com.waridley.textroid.credentials.AuthenticationHelper

class TtvChatGameClient(authHelper: AuthenticationHelper, channelName: String) {
	init {
		authHelper.retrieveCredential("TtvChatClientCredential",
		                              listOf(CHAT_READ,
		                                     CHAT_EDIT,
		                                     CHAT_WHISPERS_READ,
		                                     CHAT_WHISPERS_EDIT,
		                                     CHAT_CHANNEL_MODERATE)) {
			val twitchChat = TwitchChatBuilder.builder()
					.withEventManager(EVENT_MANAGER)
					.withCredentialManager(authHelper.identityProvider.credentialManager)
					.withChatAccount(it)
					.build()
			twitchChat.joinChannel(channelName)
			
		}
	}
}

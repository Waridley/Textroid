package com.waridley.textroid.ttv.chat_client

import com.github.twitch4j.chat.TwitchChatBuilder
import com.waridley.textroid.credentials.AuthenticationHelper

class TtvChatGameClient(authHelper: AuthenticationHelper, channelName: String) {
	init {
		authHelper.retrieveCredential("TtvTextroidClientCredential") {
			val twitchChat = TwitchChatBuilder.builder()
				.withCredentialManager(authHelper.identityProvider.credentialManager)
				.withChatAccount(it)
				.build()
			twitchChat.joinChannel(channelName)
			
		}
	}
}

package com.waridley.textroid.ttv

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential
import com.github.twitch4j.helix.TwitchHelix
import com.github.twitch4j.helix.domain.User

interface TtvUserStorageInterface {
	
	val helix: TwitchHelix
	val credential: OAuth2Credential
	
	fun getHelixUser(id: TtvUserId): User? =
			helix.getUsers(credential.accessToken, listOf(id._id), null).execute().users.firstOrNull()
	
	
}
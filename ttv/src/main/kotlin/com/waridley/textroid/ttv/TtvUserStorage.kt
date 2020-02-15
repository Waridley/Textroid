package com.waridley.textroid.ttv

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential
import com.github.twitch4j.helix.TwitchHelix
import com.github.twitch4j.helix.domain.User
import com.waridley.textroid.api.StorageInterface
import com.waridley.textroid.api.stores

interface TtvUserStorage: StorageInterface<TtvUser, TtvUserId> {
	
	val helix: TwitchHelix
	val credential: OAuth2Credential?
	
	fun getHelixUser(id: TtvUserId): User? =
			helix.getUsers(credential?.accessToken, listOf(id._id), null).execute().users.firstOrNull()
	
	fun findOrCreateOne(helixUser: User) =
			findOrCreateOne("_id" stores helixUser.id,
			                listOf(TtvUser::helixUser stores helixUser,
			                       TtvUser::onlineMinutes stores 0L,
			                       TtvUser::offlineMinutes stores 0L,
			                       TtvUser::guestMinutes stores 0L,
			                       TtvUser::hostMinutes stores 0L))
}
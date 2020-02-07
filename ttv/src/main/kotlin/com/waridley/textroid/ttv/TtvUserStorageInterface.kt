package com.waridley.textroid.ttv

import com.github.twitch4j.helix.domain.User

interface TtvUserStorageInterface {
	
	fun getHelixUser(id: TtvUserId): User?
	
}
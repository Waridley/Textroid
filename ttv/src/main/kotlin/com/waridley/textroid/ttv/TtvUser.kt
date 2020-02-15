package com.waridley.textroid.ttv

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonValue
import com.github.twitch4j.helix.domain.User
import com.waridley.textroid.api.*

class TtvUser(@JsonValue override val id: TtvUserId, override val storage: TtvUserStorage): Storable<TtvUser, TtvUserId, TtvUserStorage>(id, storage, TtvUser::class.java) {
	
	val helixUser: User by storage(::helixUser)
	
	var onlineMinutes: Long by storage(::onlineMinutes)
	var offlineMinutes: Long by storage(::offlineMinutes)
	var guestMinutes: Long by storage(::guestMinutes)
	var hostMinutes: Long by storage(::hostMinutes)
}

@JsonIgnoreProperties(ignoreUnknown = true)
inline class TtvUserId(override val _id: String): StorageId<TtvUser, TtvUserId, TtvUserStorage, String> {
	override infix fun storedIn(storage: TtvUserStorage): TtvUser {
		return TtvUser(this, storage)
	}
}
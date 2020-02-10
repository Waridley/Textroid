package com.waridley.textroid.ttv

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonValue
import com.github.twitch4j.helix.domain.User
import com.waridley.textroid.api.*

class TtvUser(@JsonValue override val id: TtvUserId, override val storage: TtvUserStorage): Storable<TtvUser, TtvUserId>(id, storage) {
	
	val helixUser: User? get() = storage.getHelixUser(id)
	
	var onlineMinutes: Long by storage(::onlineMinutes)
	var offlineMinutes: Long by storage(::offlineMinutes)
	var guestMinutes: Long by storage(::guestMinutes)
}

@JsonIgnoreProperties(ignoreUnknown = true)
inline class TtvUserId(override val _id: String): StorageId<TtvUser>
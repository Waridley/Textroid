package com.waridley.textroid.ttv

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonValue
import com.github.twitch4j.helix.domain.User
import com.waridley.textroid.api.*

class TtvUser(@JsonValue val id: TtvUserId, val storage: TtvUserStorageInterface) {
	
	val helixUser: User? get() = storage.getHelixUser(id)
	
}

@JsonIgnoreProperties(ignoreUnknown = true)
inline class TtvUserId(val _id: String)

var Player.ttvUser: TtvUser by storage<TtvUser>()
var Player.ttvUserId: TtvUserId by uniqueStorage(TtvUser::id)
var Player.helixUser: User by storage(TtvUser::helixUser)
var Player.helixUserId: String by storage(TtvUser::helixUser / User::getId)

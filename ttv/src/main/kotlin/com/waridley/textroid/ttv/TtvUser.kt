package com.waridley.textroid.ttv

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonValue
import com.github.twitch4j.helix.TwitchHelix
import com.github.twitch4j.helix.domain.User
import com.waridley.textroid.api.Player

class TtvUser(@JsonValue val id: TtvUserId, val storage: TtvUserStorageInterface) {
	
	val helixUser: User? get() = storage.getHelixUser(id)
	
}

@JsonIgnoreProperties(ignoreUnknown = true)
inline class TtvUserId(val _id: String)

val twitch = Unit
var Player.userId: String by Player.Attrs.Unique(::twitch)

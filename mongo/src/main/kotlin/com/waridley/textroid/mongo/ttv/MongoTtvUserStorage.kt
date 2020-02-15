package com.waridley.textroid.mongo.ttv

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential
import com.github.twitch4j.helix.TwitchHelix
import com.github.twitch4j.helix.TwitchHelixBuilder
import com.mongodb.client.MongoDatabase
import com.waridley.textroid.api.JACKSON
import com.waridley.textroid.mongo.game.MongoStorage
import com.waridley.textroid.ttv.TtvUser
import com.waridley.textroid.ttv.TtvUserId
import com.waridley.textroid.ttv.TtvUserStorage
import org.bson.Document

class MongoTtvUserStorage(db: MongoDatabase,
                          collectionName: String = "ttv_users",
                          override val helix: TwitchHelix = TwitchHelixBuilder.builder().build(),
                          override val credential: OAuth2Credential? = null
): TtvUserStorage, MongoStorage<TtvUser, TtvUserId, TtvUserStorage, String>(db, collectionName, TtvUser::class.java) {
	
	override fun Document.intoT(): TtvUser {
		return TtvUserId((this["_id"] as String)) storedIn this@MongoTtvUserStorage
	}
	
}
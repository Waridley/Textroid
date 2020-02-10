package com.waridley.textroid.mongo.ttv

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential
import com.github.twitch4j.helix.TwitchHelix
import com.mongodb.client.MongoDatabase
import com.waridley.textroid.mongo.game.MongoStorage
import com.waridley.textroid.ttv.TtvUser
import com.waridley.textroid.ttv.TtvUserId
import com.waridley.textroid.ttv.TtvUserStorage

class MongoTtvStorage(db: MongoDatabase,
                      collectionName: String = "ttv_users",
                      override val helix: TwitchHelix,
                      override val credential: OAuth2Credential): TtvUserStorage,
                                                                  MongoStorage<TtvUser, TtvUserId>(db, collectionName) {


}
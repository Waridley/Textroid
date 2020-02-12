package com.waridley.textroid.mongo.credentials

import com.mongodb.client.MongoDatabase
import com.waridley.textroid.credentials.TextroidCredential
import com.waridley.textroid.mongo.getOrCreateCollection
import org.litote.kmongo.eq
import org.litote.kmongo.findOne

class MongoTextroidCredentialStorageBackend(db: MongoDatabase, collectionName: String = "credentials") {
	
	val col = db.getOrCreateCollection<TextroidCredential>(collectionName)
	
	fun getCredentialByName(name: String) = col.findOne(TextroidCredential::credentialName eq name)
	
}

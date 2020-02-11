package com.waridley.textroid.mongo

import com.github.philippheuer.events4j.core.domain.Event
import com.mongodb.client.MongoDatabase
import com.waridley.textroid.api.TextroidEventHandler
import org.litote.kmongo.withKMongo

class MongoEventLogger(db: MongoDatabase, collectionName: String = "events"): TextroidEventHandler() {
	
	val col = db.getOrCreateCollection<Event>(collectionName).withKMongo()
	
	init {
		on<Event> {
			col.insertOne(this)
		}
	}
}
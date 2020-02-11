package com.waridley.textroid.mongo

import com.github.philippheuer.events4j.core.domain.Event
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.InsertOneOptions
import com.waridley.textroid.api.TextroidEventHandler
import org.litote.kmongo.withKMongo

class MongoEventLogger(db: MongoDatabase, collectionName: String = "events"): TextroidEventHandler() {
	
	val col = db.getOrCreateCollection<EventLogEntry>(collectionName).withKMongo()
	
	init {
		on<Event> {
			col.insertOne(EventLogEntry(this.javaClass.simpleName, this), InsertOneOptions())
		}
	}
	
	class EventLogEntry(val type: String, val event: Event)
}
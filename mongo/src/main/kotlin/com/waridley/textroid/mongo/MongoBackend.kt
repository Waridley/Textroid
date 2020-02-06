package com.waridley.textroid.mongo

import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.ReturnDocument
import org.bson.Document
import org.bson.conversions.Bson

@Suppress("UNUSED")
interface MongoBackend {
	
	val db: MongoDatabase
	
	fun <T> getOrCreateCollection(collectionName: String, documentClass: Class<T>): MongoCollection<T> {
		return getOrCreateCollection(db, collectionName, documentClass)
	}
	
	fun getOrCreateCollection(collectionName: String) = getOrCreateCollection(collectionName, Document::class.java)
	
}

fun <T> getOrCreateCollection(db: MongoDatabase, collectionName: String, documentClass: Class<T>): MongoCollection<T> {
	createCollectionIfNotExists(db, collectionName)
	return db.getCollection(collectionName, documentClass)
}

inline fun <reified T> MongoDatabase.getOrCreateCollection(collectionName: String): MongoCollection<T> {
	createCollectionIfNotExists(this, collectionName)
	return getCollection(collectionName, T::class.java)
}

fun createCollectionIfNotExists(db: MongoDatabase, collectionName: String) {
	var collectionExists = false
	for (name in db.listCollectionNames()) {
		if (name == collectionName) {
			collectionExists = true
			break
		}
	}
	if (!collectionExists) {
		db.createCollection(collectionName)
	}
}

tailrec fun Document.at(path: List<String>): Any? {
	return when (path.size) {
		1    -> {
			get(path[0]).also { println(it) }
		}
		else -> (get(path[0]) as Document).at(path.slice(1 until path.size))
	}
}

infix fun String.eq(other: Any?): Bson = Filters.eq(this, other)

fun before(): FindOneAndUpdateOptions = FindOneAndUpdateOptions().returnDocument(ReturnDocument.BEFORE)

infix fun String.containsAll(list: MutableList<*>): Bson {
	return Filters.all(this, list)
}

infix fun Bson?.andOr(other: Bson?): Bson? {
	return when {
		this == null && other == null -> null
		this != null && other == null -> this
		this == null && other != null -> other
		else                          -> Filters.and(this, other)
	}
}
package com.waridley.textroid.mongo.game

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.exists
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Updates.setOnInsert
import com.mongodb.client.result.UpdateResult
import com.waridley.textroid.api.*
import com.waridley.textroid.mongo.*
import org.bson.Document
import org.litote.kmongo.*

open class MongoStorage<T, I: StorageId<T>>(db: MongoDatabase, collectionName: String = "players"): StorageInterface<T, I> {
	
	private val col = db.getOrCreateCollection<Document>(collectionName).withKMongo()
	
	override fun new(key: Attribute<*>): T {
		return col.findOneAndUpdate(
				key.filter,
				setOnInsert(key.path, key.value),
				findOneAndUpdateUpsert()
		).let { it?.intoT() ?: throw StorableCreationException(this) }
	}
	
	override operator fun get(id: I): T? {
		return col.find(id.filter).projection(StorageId<T>::_id).first()?.intoT()
	}
	
	override operator fun get(attribute: MaybeAttribute<*>): Iterable<T> {
		return col.find(attribute.filter).map { it.intoT() }
	}
	
	override fun findOrCreateOne(key: Attribute<*>, setOnInsert: List<Attribute<*>>): T {
		return col.findOneAndUpdate(
				key.filter,
				combine(setOnInsert.map { setOnInsert(it.path, it.value) } ),
				findOneAndUpdateUpsert()
		).let { it?.intoT() ?: throw StorableCreationException(it) }
	}
	
	
	override fun <T> readAttribute(id: I, path: String, type: Class<T>): MaybeAttribute<T>? {
		return col.find(id.filter)
				.projection("{\"$path\":1}")
				.first()?.run {
					at(path.split("."))?.let { value ->
						path stores JACKSON.convertValue(value, type)
					} ?: path.undefined
				}
	}
	
	override fun <T> readUnique(id: I, path: String, type: Class<T>): MaybeAttribute<T>? {
		ensureUnique(path)
		return readAttribute(id, path, type)
	}
	
	
	override fun <T> writeAttribute(id: I, attribute: MaybeAttribute<T>): MaybeAttribute<T?>? {
		return col.findOneAndUpdate(id.filter, attribute.update, after())?.let { doc ->
			doc.at(attribute.path.split(".")).let { value ->
				return when(value) {
					null -> attribute.path.undefined
					else -> attribute.path stores JACKSON.convertValue(value, object: TypeReference<T>() { })
				}
			}
		}
	}
	
	override fun <T> writeUnique(id: I, attribute: MaybeAttribute<T>): MaybeAttribute<T?>? {
		ensureUnique(attribute.path)
		return writeAttribute(id, attribute)
	}
	
	private fun Any.intoT(): T {
		return JACKSON.convertValue(this, object: TypeReference<T>() {})
	}
	
	private fun ensureUnique(path: String, options: IndexOptions = IndexOptions()): String {
		return col.ensureIndex("{\"$path\":1}", options.unique(true).partialFilterExpression(exists(path)))
	}
	
	internal val StorageId<T>.filter get() = StorageId<T>::_id eq _id
	
	
}


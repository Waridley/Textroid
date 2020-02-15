package com.waridley.textroid.mongo.game

import com.fasterxml.jackson.core.type.TypeReference
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.exists
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Updates.setOnInsert
import com.waridley.textroid.api.*
import com.waridley.textroid.mongo.*
import org.bson.Document
import org.litote.kmongo.*

abstract class MongoStorage<T: Storable<T, I, S>, I: StorageId<T, I, S, Q>, S: StorageInterface<T, I>, Q>(db: MongoDatabase, collectionName: String, val docClass: Class<T>): StorageInterface<T, I> {
	
	private val col = db.getOrCreateCollection<Document>(collectionName).withKMongo()
	
	override fun new(key: Attribute<*>): T {
		return col.findOneAndUpdate(
				key.filter(docClass),
				setOnInsert(key.path.relativeTo(docClass), key.value),
				after().upsert(true)
		).let { it?.intoT() ?: throw StorableCreationException(this) }
	}
	
	override operator fun get(id: I): T? {
		return col.find(id.filter).projection().first()?.intoT()
	}
	
	override operator fun get(attribute: MaybeAttribute<*>): Iterable<T> {
		return col.find(attribute.filter(docClass)).map { it.intoT() }
	}
	
	override fun findOrCreateOne(key: Attribute<*>, setOnInsert: List<Attribute<*>>): T {
		return col.findOneAndUpdate(
				key.filter(docClass),
				combine(
						setOnInsert.map {
							setOnInsert(it.path.relativeTo(docClass), it.value)
						} + setOnInsert(key.path.relativeTo(docClass), key.value)
				),
				after().upsert(true)
		).let { it?.intoT() ?: throw StorableCreationException(it) }
	}
	
	override fun <T> readAttribute(id: I, path: String, type: Class<T>): MaybeAttribute<T>? {
		val relPath = path.relativeTo(docClass)
		return col.find(id.filter)
				.projection("{\"$relPath\":1}")
				.first()?.run {
					at(relPath.split("."))?.let { value ->
						relPath stores JACKSON.convertValue(value, type)
					}
				} ?: relPath.undefined
	}
	
	override fun <T> readUnique(id: I, path: String, type: Class<T>): MaybeAttribute<T>? {
		val relPath = path.relativeTo(docClass)
		ensureUnique(relPath)
		return readAttribute(id, relPath, type)
	}
	
	
	override fun <T> writeAttribute(id: I, attribute: MaybeAttribute<T>): MaybeAttribute<T?>? {
		return col.findOneAndUpdate(id.filter, attribute.update(docClass), after())?.let { doc ->
			val path = attribute.path.relativeTo(docClass)
			doc.at(path.split(".")).let { value ->
				return when(value) {
					null -> path.undefined
					else -> path stores JACKSON.convertValue(value, object: TypeReference<T>() { })
				}
			}
		}
	}
	
	override fun <T> writeUnique(id: I, attribute: MaybeAttribute<T>): MaybeAttribute<T?>? {
		ensureUnique(attribute.path.relativeTo(docClass))
		return writeAttribute(id, attribute)
	}
	
	abstract fun Document.intoT(): T
	
	private fun ensureUnique(relativePath: String, options: IndexOptions = IndexOptions()): String {
		return col.ensureIndex("{\"$relativePath\":1}", options.unique(true).partialFilterExpression(exists(relativePath)))
	}
	
	
	internal val <I: StorageId<*, I, *, Q>, Q> I.filter get() = "_id" eq _id
}


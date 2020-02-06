package com.waridley.textroid.mongo.game

import com.fasterxml.jackson.databind.ObjectMapper
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Updates.set
import com.mongodb.client.model.Updates.unset
import com.mongodb.client.model.Filters.exists
import com.mongodb.client.model.IndexOptions
import com.waridley.textroid.engine.*
import com.waridley.textroid.mongo.at
import com.waridley.textroid.mongo.before
import com.waridley.textroid.mongo.eq
import com.waridley.textroid.mongo.getOrCreateCollection
import org.bson.Document
import org.bson.types.ObjectId
import org.litote.kmongo.*
import org.litote.kmongo.id.toId

class MongoPlayerStorage(db: MongoDatabase, collectionName: String = "players") : PlayerStorageInterface {
	
	private val col = db.getOrCreateCollection<Document>(collectionName).withKMongo()
	val mapper = ObjectMapper()
	
	override fun new(username: Player.Name): Player {
		val result = col.updateOne(
				Player::username eq username,
				setOnInsert(Player::username, username),
				upsert()
		)
		return result.upsertedId?.asObjectId()?.value?.playerId storedIn this ?: throw PlayerCreationException(result)
	}
	
	override operator fun get(id: PlayerId): Player? {
		return col.find(id.filter).projection(PlayerId::_id).first()?.intoPlayer()
	}
	
	override operator fun <T> get(attribute: Attribute<T>): Iterable<Player> {
		return col.find(attribute.filter).onEach { println(it) }.map { it.intoPlayer() }
	}
	
	
	override fun <T> readAttribute(id: PlayerId, path: String, type: Class<T>): MaybeAttribute<T>? {
		return col.find(id.filter)
				.projection("{\"$path\":1}")
				.first()?.let {
					it.at(path.split("."))?.let { value ->
						path stores mapper.convertValue(value, type)
					} ?: path.clear
				}
	}
	
	override fun <T> readUnique(id: PlayerId, path: String, type: Class<T>): MaybeAttribute<T>? {
		ensureUnique(path)
		return readAttribute(id, path, type)
	}
	
	
	override fun <T> writeAttribute(id: PlayerId, attribute: MaybeAttribute<T>): MaybeAttribute<Any?>? {
		return col.findOneAndUpdate(
				id.filter,
				when (attribute) {
					is Attribute -> set(attribute.path, attribute.value)
					is Undefined -> unset(attribute.path)
				},
				before()
		)?.let {
			it[attribute.path]?.let { value -> attribute.path stores value } ?: attribute.path.clear
		}
	}
	
	override fun <T> writeUnique(id: PlayerId, attribute: MaybeAttribute<T>): MaybeAttribute<Any?>? {
		ensureUnique(attribute.path)
		return writeAttribute(id, attribute)
	}
	
	
	private fun Document.intoPlayer(): Player {
		return Player(
				(this["_id"] as ObjectId).playerId,
				this@MongoPlayerStorage
		)
	}
	
	private fun ensureUnique(path: String, options: IndexOptions = IndexOptions()): String {
		return col.ensureIndex("{\"$path\":1}", options.unique(true).partialFilterExpression(exists(path)))
	}
	
}

internal val PlayerId.filter get() = PlayerId::_id eq _id
private val ObjectId.playerId get() = toId<Player>().toPlayerId()

val Attribute<*>.filter get() = path eq value
package com.waridley.textroid.mongo.game

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Updates.set
import com.mongodb.client.model.Updates.unset
import com.mongodb.client.model.Filters.exists
import com.mongodb.client.model.IndexOptions
import com.waridley.textroid.api.*
import com.waridley.textroid.mongo.*
import org.bson.Document
import org.bson.types.ObjectId
import org.litote.kmongo.*
import org.litote.kmongo.id.toId

class MongoPlayerStorage(db: MongoDatabase, collectionName: String = "players"): PlayerStorageInterface {
	
	private val col = db.getOrCreateCollection<Document>(collectionName).withKMongo()
	val mapper = ObjectMapper()
	
	override fun new(username: Player.Name, nickname: Player.Name?): Player {
		val result = col.updateOne(
				Player::username eq username,
				combine(setOnInsert(Player::username, username), setOnInsert(Player::nickname, nickname)),
				upsert()
		)
		return result.upsertedId?.asObjectId()?.value?.playerId storedIn this ?: throw PlayerCreationException(result)
	}
	
	override operator fun get(id: StorageId<Player>): Player? {
		return col.find(id.filter).projection(StorageId<Player>::_id).first()?.intoPlayer()
	}
	
	override operator fun get(attribute: Attribute<*>): Iterable<Player> {
		return col.find(attribute.filter).map { it.intoPlayer() }
	}
	
	
	override fun <T> readAttribute(id: StorageId<Player>, path: String, type: Class<T>): MaybeAttribute<T>? {
		return col.find(id.filter)
				.projection("{\"$path\":1}")
				.first()?.run {
					at(path.split("."))?.let { value ->
						path stores mapper.convertValue(value, type)
					} ?: path.undefined
				}
	}
	
	override fun <T> readUnique(id: StorageId<Player>, path: String, type: Class<T>): MaybeAttribute<T>? {
		ensureUnique(path)
		return readAttribute(id, path, type)
	}
	
	
	override fun <T> writeAttribute(id: StorageId<Player>, attribute: MaybeAttribute<T>): MaybeAttribute<T?>? {
		return col.findOneAndUpdate(
				id.filter,
				attribute.run {
					when (this) {
						is Attribute -> set(path, value)
						is Undefined -> unset(path)
					}
				},
				after()
		)?.let {
			it.at(attribute.path.split(".")).let { value ->
				return when(value) {
					null -> attribute.path.undefined
					else -> attribute.path stores mapper.convertValue(value, object: TypeReference<T>() { })
				}
			}
		}
	}
	
	override fun <T> writeUnique(id: StorageId<Player>, attribute: MaybeAttribute<T>): MaybeAttribute<T?>? {
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

internal val StorageId<Player>.filter get() = StorageId<Player>::_id eq _id
private val ObjectId.playerId get() = toId<Player>().toPlayerId()

val Attribute<*>.filter get() = path eq value
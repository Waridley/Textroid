package com.waridley.textroid.mongo.game

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Updates.set
import com.mongodb.client.model.Updates.unset
import com.mongodb.client.model.Filters.exists
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Updates.setOnInsert
import com.mongodb.client.result.UpdateResult
import com.waridley.textroid.api.*
import com.waridley.textroid.mongo.*
import org.bson.Document
import org.bson.types.ObjectId
import org.litote.kmongo.*
import org.litote.kmongo.id.toId

class MongoPlayerStorage(db: MongoDatabase,
                         collectionName: String = "players"): PlayerStorageInterface,
                                                              MongoStorage<Player, PlayerId>(db, collectionName) {
	
	private val col = db.getOrCreateCollection<Document>(collectionName).withKMongo()
	
	override fun new(username: Player.Name, nickname: Player.Name?): Player {
		val result = col.updateOne(
				Player::username eq username,
				combine(setOnInsert(Player::username, username), setOnInsert(Player::nickname, nickname)),
				upsert()
		)
		return result.toPlayerId() ?: throw StorableCreationException(result)
	}
	
	override fun new(key: Attribute<*>): Player {
		val result = col.updateOne(
				key.filter,
				setOnInsert(key.path, key.value),
				upsert()
		)
		return result.toPlayerId() ?: throw StorableCreationException(result)
	}
	
	override operator fun get(id: PlayerId): Player? {
		return col.find(id.filter).projection(PlayerId::_id).first()?.intoPlayer()
	}
	
	override operator fun get(attribute: MaybeAttribute<*>): Iterable<Player> {
		return col.find(attribute.filter).map { it.intoPlayer() }
	}
	
	override fun findOrCreateOne(key: Attribute<*>, setOnInsert: List<Attribute<*>>): Player {
		return col.findOneAndUpdate(
				key.filter,
				combine(setOnInsert.map { setOnInsert(it.path, it.value) } ),
				findOneAndUpdateUpsert()
		).let { it?.intoPlayer() ?: throw StorableCreationException(it) }
	}
	
	
	override fun <T> readAttribute(id: PlayerId, path: String, type: Class<T>): MaybeAttribute<T>? {
		return col.find(id.filter)
				.projection("{\"$path\":1}")
				.first()?.run {
					at(path.split("."))?.let { value ->
						path stores JACKSON.convertValue(value, type)
					} ?: path.undefined
				}
	}
	
	override fun <T> readUnique(id: PlayerId, path: String, type: Class<T>): MaybeAttribute<T>? {
		ensureUnique(path)
		return readAttribute(id, path, type)
	}
	
	
	override fun <T> writeAttribute(id: PlayerId, attribute: MaybeAttribute<T>): MaybeAttribute<T?>? {
		return col.findOneAndUpdate(id.filter, attribute.update, after())?.let { doc ->
			doc.at(attribute.path.split(".")).let { value ->
				return when(value) {
					null -> attribute.path.undefined
					else -> attribute.path stores JACKSON.convertValue(value, object: TypeReference<T>() { })
				}
			}
		}
	}
	
	override fun <T> writeUnique(id: PlayerId, attribute: MaybeAttribute<T>): MaybeAttribute<T?>? {
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
	
	private fun UpdateResult.toPlayerId() = upsertedId?.asObjectId()?.value?.playerId storedIn this@MongoPlayerStorage
}

private val ObjectId.playerId get() = toId<Player>().toPlayerId()

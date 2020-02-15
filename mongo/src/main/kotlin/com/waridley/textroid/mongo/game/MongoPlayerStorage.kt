package com.waridley.textroid.mongo.game

import com.fasterxml.jackson.core.type.TypeReference
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.exists
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.ReturnDocument.AFTER
import com.mongodb.client.model.Updates.setOnInsert
import com.mongodb.client.result.UpdateResult
import com.waridley.textroid.api.*
import com.waridley.textroid.mongo.*
import org.bson.Document
import org.bson.types.ObjectId
import org.litote.kmongo.*
import org.litote.kmongo.id.toId

class MongoPlayerStorage(
		db: MongoDatabase,
		collectionName: String = "players"
): PlayerStorageInterface, MongoStorage<Player, PlayerId, PlayerStorageInterface, Id<Player>>(db, collectionName, Player::class.java) {
	
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
				key.filter(Player::class.java),
				setOnInsert(key.path.relativeTo<Player>(), key.value),
				upsert()
		)
		return result.toPlayerId() ?: throw StorableCreationException(result)
	}
	
	override operator fun get(id: PlayerId): Player? {
		return col.find(id.filter).projection(PlayerId::_id).first()?.intoPlayer()
	}
	
	override operator fun get(attribute: MaybeAttribute<*>): Iterable<Player> {
		return col.find(attribute.filter(Player::class.java)).map { it.intoPlayer() }
	}
	
	override fun findOrCreateOne(key: Attribute<*>, setOnInsert: List<Attribute<*>>): Player {
		return col.findOneAndUpdate(
				key.filter(Player::class.java),
				combine(setOnInsert.map { setOnInsert(it.path.relativeTo<Player>(), it.value) } + setOnInsert(key.path.relativeTo<Player>(), key.value)),
				findOneAndUpdateUpsert().returnDocument(AFTER)
		).let { it?.intoPlayer() ?: throw StorableCreationException(it) }
	}
	
	
	override fun <T> readAttribute(id: PlayerId, path: String, type: Class<T>): MaybeAttribute<T>? {
		val relPath = path.relativeTo<Player>()
		return col.find(id.filter)
				.projection("{\"$relPath\":1}")
				.first()?.run {
					at(relPath.split("."))?.let { value ->
						relPath stores JACKSON.convertValue(value, type)
					} ?: relPath.undefined
				}
	}
	
	override fun <T> readUnique(id: PlayerId, path: String, type: Class<T>): MaybeAttribute<T>? {
		val relPath = path.relativeTo<Player>()
		ensureUnique(relPath)
		return readAttribute(id, relPath, type)
	}
	
	
	override fun <T> writeAttribute(id: PlayerId, attribute: MaybeAttribute<T>): MaybeAttribute<T?>? {
		return col.findOneAndUpdate(id.filter, attribute.update(Player::class.java), after())?.let { doc ->
			val relPath = attribute.path.relativeTo<Player>()
			doc.at(relPath.split(".")).let { value ->
				return when(value) {
					null -> relPath.undefined
					else -> relPath stores JACKSON.convertValue(value, object: TypeReference<T>() { })
				}
			}
		}
	}
	
	override fun <T> writeUnique(id: PlayerId, attribute: MaybeAttribute<T>): MaybeAttribute<T?>? {
		ensureUnique(attribute.path.relativeTo<Player>())
		return writeAttribute(id, attribute)
	}
	
	
	private fun Document.intoPlayer(): Player {
		return Player(
				(this["_id"] as ObjectId).playerId,
				this@MongoPlayerStorage
		)
	}
	override fun Document.intoT(): Player = intoPlayer()
	
	private fun ensureUnique(path: String, options: IndexOptions = IndexOptions()): String {
		return col.ensureIndex("{\"$path\":1}", options.unique(true).partialFilterExpression(exists(path)))
	}
	
	private fun UpdateResult.toPlayerId() = upsertedId?.asObjectId()?.value?.playerId?.storedIn(this@MongoPlayerStorage)
	
}

private val ObjectId.playerId get() = toId<Player>().toPlayerId()

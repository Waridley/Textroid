package com.waridley.textroid.mongo.credentials

import com.github.philippheuer.credentialmanager.domain.Credential
import com.mongodb.MongoClient
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.ReturnDocument
import com.mongodb.client.model.Updates
import com.waridley.textroid.credentials.CredentialMap
import com.waridley.textroid.mongo.credentials.codecs.CredentialCodecProvider
import com.waridley.textroid.mongo.eq
import com.waridley.textroid.mongo.getOrCreateCollection
import org.bson.BsonDocumentReader
import org.bson.Document
import org.bson.codecs.DecoderContext
import org.bson.codecs.configuration.CodecRegistries
import org.bson.codecs.configuration.CodecRegistries.fromRegistries
import org.litote.kmongo.*
import java.util.*

class MongoCredentialMap<T>(db: MongoDatabase, collectionName: String = "credentials") : CredentialMap<T>() {
	
	private val collection = db
		.getOrCreateCollection<Document>(collectionName)
		.withCodecRegistry(fromRegistries(
			MongoClient.getDefaultCodecRegistry(),
			CodecRegistries.fromProviders(CredentialCodecProvider())
		)).withKMongo()
	
	override val entries: MutableSet<MutableMap.MutableEntry<T, Credential?>>
		get() =  collection.find(Entry<T>::value.exists()).toMutableSet().toMutableSet().map {
			@Suppress("UNCHECKED_CAST")
			Entry<T>(it["key"] as T, it["value"].docToCredential())
		}.toMutableSet()
	
	override fun put(key: T, value: Credential?): Credential? {
		return collection.findOneAndUpdate(
			"key" eq key,
			combine(Updates.set("key", key), Updates.set("value", value)),
			findOneAndUpdateUpsert().returnDocument(ReturnDocument.BEFORE)
		)?.get("value")?.docToCredential()
	}
	
	override operator fun get(key: T): Credential? {
		return collection.findOne(Entry<T>::key eq key)?.get("value").docToCredential()
	}
	
	override fun saveCredentials(credentials: MutableList<Credential>) {
		throw UnsupportedOperationException("This backend needs keys to associate with credentials")
	}
	
	override fun getCredentialByUserId(userId: String): Optional<Credential> {
		return Optional.ofNullable(collection.findOne(Document("value", Document("userId", userId)))?.get("value").docToCredential())
	}
	
	data class Entry<T>(override val key: T, override var value: Credential?): MutableMap.MutableEntry<T, Credential?> {
		
		override fun setValue(newValue: Credential?): Credential {
			throw UnsupportedOperationException("I can't figure out how to implement this.")
		}
		
	}
	
	private fun Any?.docToCredential(): Credential? {
		return this?.let {
			val doc = this as Document
			collection.codecRegistry.get(Credential::class.java).decode(
				BsonDocumentReader(
					doc.toBsonDocument(
						Credential::class.java,
						collection.codecRegistry
					)
				),
				DecoderContext.builder().build()
			)
		}
	}
}
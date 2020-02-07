package com.waridley.textroid.mongo.credentials

import com.github.philippheuer.credentialmanager.api.IOAuth2StorageBackend
import com.github.philippheuer.credentialmanager.domain.Credential
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential
import com.mongodb.MongoClient
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.exists
import com.mongodb.client.model.FindOneAndReplaceOptions
import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.ReturnDocument
import com.waridley.textroid.mongo.andOr
import com.waridley.textroid.mongo.containsAll
import com.waridley.textroid.mongo.credentials.codecs.CredentialCodecProvider
import com.waridley.textroid.mongo.eq
import com.waridley.textroid.mongo.getOrCreateCollection
import org.bson.codecs.configuration.CodecRegistries
import org.bson.conversions.Bson
import org.litote.kmongo.ensureIndex
import org.litote.kmongo.updateOne
import org.litote.kmongo.upsert
import org.litote.kmongo.util.KMongoUtil
import org.litote.kmongo.withKMongo

class MongoOAuth2StorageBackend(db: MongoDatabase, collectionName: String = "credentials") : IOAuth2StorageBackend {
	
	private val collection = db.getOrCreateCollection<Credential>(collectionName)
			.withCodecRegistry(
					CodecRegistries.fromRegistries(
							MongoClient.getDefaultCodecRegistry(),
							CodecRegistries.fromProviders(CredentialCodecProvider())
					)
			).withKMongo()
	
	init {
		collection.ensureIndex(
				"accessToken",
				IndexOptions().unique(true).partialFilterExpression(exists("accessToken"))
		)
	}
	
	override fun loadCredentials(): MutableList<Credential> {
		return collection.find().toMutableList()
	}
	
	override fun loadOAuth2Credentials(): MutableList<OAuth2Credential> {
		return collection.find(exists("accessToken")).map { it as OAuth2Credential }.toMutableList()
	}
	
	override fun saveCredentials(credentials: MutableList<Credential>?) {
		credentials?.forEach {
			when (it) {
				is OAuth2Credential -> collection.updateOne("accessToken" eq it.accessToken, it, upsert(), true)
				else                -> collection.insertOne(it)
			}
		}
	}
	
	override fun saveOAuth2Credentials(credentials: MutableList<OAuth2Credential>?) {
		credentials?.forEach { collection.updateOne("accessToken" eq it.accessToken, it, upsert(), true) }
	}
	
	fun updateOne(oldCred: OAuth2Credential,
	              newCred: OAuth2Credential,
	              upsert: Boolean = true,
	              updateOnlyNonNull: Boolean = true): Credential? {
		
		return collection.findOneAndUpdate(
				createFilter(oldCred),
				KMongoUtil.toBsonModifier(newCred, updateOnlyNonNull),
				FindOneAndUpdateOptions().upsert(upsert).returnDocument(ReturnDocument.BEFORE)
		)
	}
	
	fun replaceOne(oldCred: OAuth2Credential, newCred: OAuth2Credential, upsert: Boolean = true): Credential? {
		return collection.findOneAndReplace(
				createFilter(oldCred),
				newCred,
				FindOneAndReplaceOptions().upsert(upsert).returnDocument(ReturnDocument.BEFORE)
		)
	}
	
	override fun filter(identityProvider: String?,
	                    userId: String?,
	                    accessToken: String?,
	                    refreshToken: String?,
	                    userName: String?,
	                    scopes: MutableList<String>?): MutableList<OAuth2Credential> {
		
		return collection.withDocumentClass(OAuth2Credential::class.java).find(
				createFilter(identityProvider, userId, accessToken, refreshToken, userName, scopes)
		).toMutableList()
	}
	
	private fun createFilter(credential: OAuth2Credential): Bson {
		
		return credential.let {
			createFilter(it.identityProvider, it.userId, it.accessToken, it.refreshToken, it.userName, it.scopes)
		}
		
	}
	
	private fun createFilter(identityProvider: String?,
	                         userId: String?,
	                         accessToken: String?,
	                         refreshToken: String?,
	                         userName: String?,
	                         scopes: MutableList<String>?): Bson {
		
		return identityProvider?.let { "identityProvider" eq identityProvider } andOr
		       userId?.let { "userId" eq userId } andOr
		       accessToken?.let { "accessToken" eq accessToken } andOr
		       refreshToken?.let { "refreshToken" eq refreshToken } andOr
		       userName?.let { "userName" eq userName } andOr
		       scopes?.let { "scopes" containsAll scopes }
		       ?: exists("_id")
	}
	
}

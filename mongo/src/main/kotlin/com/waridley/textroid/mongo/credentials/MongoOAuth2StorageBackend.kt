package com.waridley.textroid.mongo.credentials

import com.github.philippheuer.credentialmanager.api.IOAuth2StorageBackend
import com.github.philippheuer.credentialmanager.domain.Credential
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential
import com.mongodb.MongoClient
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Filters.exists
import com.mongodb.client.model.IndexOptions
import com.waridley.textroid.mongo.credentials.codecs.CredentialCodecProvider
import com.waridley.textroid.mongo.eq
import com.waridley.textroid.mongo.getOrCreateCollection
import org.bson.codecs.configuration.CodecRegistries
import org.bson.conversions.Bson
import org.litote.kmongo.*

class MongoOAuth2StorageBackend(db: MongoDatabase, collectionName: String = "credentials"): IOAuth2StorageBackend {

    private val collection = db.getOrCreateCollection<Credential>(collectionName)
        .withCodecRegistry(
            CodecRegistries.fromRegistries(
                MongoClient.getDefaultCodecRegistry(),
                CodecRegistries.fromProviders(CredentialCodecProvider())
            )
        ).withKMongo()

    init {
        collection.ensureIndex("accessToken", IndexOptions().unique(true).partialFilterExpression(exists("accessToken")) )
    }

    override fun loadCredentials(): MutableList<Credential> {
        return collection.find().toMutableList()
    }

    override fun loadOAuth2Credentials(): MutableList<OAuth2Credential> {
        return collection.find(exists("accessToken")).map { it as OAuth2Credential }.toMutableList()
    }

    override fun saveCredentials(credentials: MutableList<Credential>?) {
        credentials?.forEach {
            when(it) {
                is OAuth2Credential -> collection.updateOne("accessToken" eq it.accessToken, it, upsert(), true)
                else -> collection.insertOne(it)
            }
        }
    }

    override fun saveOAuth2Credentials(credentials: MutableList<OAuth2Credential>?) {
        credentials?.forEach { collection.updateOne("accessToken" eq it.accessToken, it, upsert(), true) }
    }

    override fun filter(
        identityProvider: String?,
        userId: String?,
        accessToken: String?,
        refreshToken: String?,
        userName: String?,
        scopes: MutableList<String>?
    ): MutableList<OAuth2Credential> {
        return collection.withDocumentClass(OAuth2Credential::class.java).find(
            identityProvider?.let { "identityProvider" eq identityProvider } andOr
                userId?.let { "userId" eq userId } andOr
                accessToken?.let { "accessToken" eq accessToken } andOr
                refreshToken?.let { "refreshToken" eq refreshToken } andOr
                userName?.let { "userName" eq userName } andOr
                scopes?.let { "scopes" all scopes}
            ?: exists("_id")
        ).toMutableList()
    }

}

infix fun String.all(list: MutableList<*>): Bson {
    return Filters.all(this, list)
}

infix fun Bson?.andOr(other: Bson?): Bson? {
    return when {
        this == null && other == null -> null
        this != null && other == null -> this
        this == null && other != null -> other
        else -> Filters.and(this, other)
    }
}
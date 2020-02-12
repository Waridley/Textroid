package com.waridley.textroid.credentials

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential

data class TextroidCredential(val credentialName: String, val oAuth2Credential: OAuth2Credential) {
	constructor(credentialName: String, identityProvider: String, accessToken: String): this(credentialName, OAuth2Credential(identityProvider, accessToken))
	
	constructor(credentialName: String,
	            identityProvider: String,
	            accessToken: String,
	            refreshToken: String,
	            userId: String,
	            userName: String,
	            expiresIn: Int?,
	            scopes: List<String>
	): this(credentialName, OAuth2Credential(identityProvider, accessToken, refreshToken, userId, userName, expiresIn, scopes))
	
}
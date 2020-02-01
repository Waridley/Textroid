package com.waridley.textroid.credentials

import com.github.philippheuer.credentialmanager.api.IStorageBackend
import com.github.philippheuer.credentialmanager.domain.Credential
import java.util.*

abstract class CredentialMap<T>: AbstractMutableMap<T, Credential?>(), IStorageBackend {
	
	override fun loadCredentials(): MutableList<Credential?> {
		return values.toMutableList()
	}
	
	override fun getCredentialByUserId(userId: String): Optional<Credential> {
		return Optional.ofNullable(values.first { it?.userId == userId })
	}
	
}
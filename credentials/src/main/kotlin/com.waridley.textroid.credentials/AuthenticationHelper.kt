package com.waridley.textroid.credentials

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential
import com.github.philippheuer.credentialmanager.identityprovider.OAuth2IdentityProvider
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.InetSocketAddress
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.*

class AuthenticationHelper(val identityProvider: OAuth2IdentityProvider,
                           private val redirectUrl: String? = null,
                           redirectPort: Int? = null) {

	private val credentialManager = identityProvider.credentialManager
	@Suppress("UNCHECKED_CAST")
	private val credentialStorage = credentialManager.storageBackend as CredentialMap<String>
	private val authController = credentialManager.authenticationController as DesktopAuthController

	private val redirectUri by lazy { redirectUrl?.let { URI(redirectUrl) } ?: URI("http://localhost") }
	private val port by lazy { redirectPort ?: redirectUri.port }

	private val server: HttpServer by lazy {
		val s = HttpServer.create(InetSocketAddress(this.port), 0)
		s.start()
		s
	}

	fun retrieveCredential(credName: String,
	                       scopes: List<Any> = listOf(),
	                       infoPageHandler: (e: HttpExchange) -> Unit = ::handleInfoPage,
	                       onRetrieved: (c: OAuth2Credential) -> Unit) {

		credentialStorage[credName]
				?.let { it as OAuth2Credential }
				?.let { oauth2cred ->
					log.info("Found credential named $credName.")
					identityProvider
							.refreshCredential(oauth2cred).orNull()?.let { refreshedCred ->
								log.info("Successfully refreshed credential")
								val cred = identityProvider.getAdditionalCredentialInformation(refreshedCred).orNull()
								           ?: refreshedCred
								credentialStorage[credName] = cred
								cred
							} ?: run {
						log.error("Failed to refresh credential. If this one doesn't work, delete it from storage to generate a new one.")
						oauth2cred
					}
				}?.also { onRetrieved(it) }
		?: run {
			log.info("No saved credential named $credName found. Starting OAuth2 Authorization Code Flow.")
			try {
				retrieveNewCredential(credName, onRetrieved, infoPageHandler, scopes)
			} catch (e: IOException) {
				e.printStackTrace()
			}
		}
	}

	private fun retrieveNewCredential(credName: String,
	                                  onRetrieved: (c: OAuth2Credential) -> Any?,
	                                  infoPageHandler: (e: HttpExchange) -> Any? = this::handleInfoPage,
	                                  scopes: List<Any> = listOf()) {
		
		server.createContext(redirectUri.path.let {
			when {
				it.isBlank() -> "/"
				else         -> it
			}
		}) { onReceivedCode(credName, it, onRetrieved) }
		authController.infoUrl?.let { infoUrl ->
			val path = URI(infoUrl).path
			server.createContext(path) { infoPageHandler(it) }
		}
		authController.startOAuth2AuthorizationCodeGrantType(
				identityProvider,
				redirectUrl,
				scopes
		)
	}

	private fun onReceivedCode(credName: String, exchange: HttpExchange, onRetrieved: (c: OAuth2Credential) -> Any?) {
		val uri = exchange.requestURI

		val query = uri.query
		val splitQuery = query.split("&").toTypedArray()
		var cred: OAuth2Credential? = null
		var code: String? = null
		for (s in splitQuery) {
			if (s.startsWith("code=")) {
				try {
					code = s.substring(5)
					cred = identityProvider.getCredentialByCode(code)
					cred = identityProvider.getAdditionalCredentialInformation(cred).orElse(null) ?: cred
//					credentialManager.addCredential("twitch", cred)
					credentialStorage[credName] = cred
					server.stop(0)
					onRetrieved(cred)
				} catch (e: Exception) {
					e.printStackTrace()
				}
				break
			}
		}

		try {
			val codeBodyPair: Pair<Int, String> =
					cred?.let { 200 to "<h1>Success!</h1>Received authorization code and retrieved OAuth2 credential." }
					?: code?.let { 500 to "<h1>Oops!</h1>Received code, but couldn't retrieve credential." }
					?: 500 to "<h1>Oops!</h1>Did not receive a code in the response."

			val payload = "<html><head></head><body>${codeBodyPair.second}</body></html>"
			exchange.sendResponseHeaders(codeBodyPair.first, payload.length.toLong())
			exchange.responseBody.write(payload.toByteArray())
			exchange.responseBody.close()
		} catch (e: Exception) {
			e.printStackTrace()
		}

	}

	private fun handleInfoPage(exchange: HttpExchange) {
		val reqURI = exchange.requestURI
		val splitQuery = reqURI.query.split("&").toTypedArray()
		var authUrl: String? = null
		for (s in splitQuery) {
			if (s.startsWith("authurl=")) {
				authUrl = URLDecoder.decode(s.substring(8), StandardCharsets.UTF_8.toString())
				break
			}
		}
		val response = """
			<html>
				<head>
				</head>
				<body>
					<h1>Log in to your desired chat bot account</h1>
					The following link will take you to the Twitch authentication page to log in.<br>
					If you do not want to use your main account for the chat bot, you can either:<br>
					<p style="margin-left: 40px">1) Click "Not you?" on that page, however, this will permanently change the account you are logged into on Twitch until you manually switch back.</p>
					<p style="margin-left: 40px">2) Right-click this link, and open it in a private/incognito window. This will allow you to stay logged in to Twitch on your main account in normal browser windows.</p>
					<a href=$authUrl>$authUrl</a>
				</body>
			</html>
		"""


		val responseCode = authUrl?.let { 200 } ?: 500
		exchange.sendResponseHeaders(responseCode, response.length.toLong())
		exchange.responseBody.write(response.toByteArray())
		exchange.responseBody.close()

	}

	companion object {
		private val log = LoggerFactory.getLogger(AuthenticationHelper::class.java)
	}
	
}

fun <T> Optional<T>.orNull(): T? = this.orElse(null)
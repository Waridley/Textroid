/**
 * MIT License
 *
 * Copyright (c) 2019 Kevin day
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.waridley.textroid.credentials

import com.github.philippheuer.credentialmanager.domain.AuthenticationController
import com.github.philippheuer.credentialmanager.identityprovider.OAuth2IdentityProvider
import java.awt.Desktop
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * An AuthenticationController which uses java.awt.Desktop.browse() to send the user to the identity provider's authentication URL.
 * If Desktop is not supported, it will print an error telling the user they may be able to paste the URL in a browser to continue authentication.
 */
open class DesktopAuthController(val infoUrl: String? = null) : AuthenticationController() {
	override fun startOAuth2AuthorizationCodeGrantType(provider: OAuth2IdentityProvider,
	                                                   redirectUrl: String?,
	                                                   scopes: List<Any>?) {

		val authURL = redirectUrl?.let {
			provider.getAuthenticationUrl(it, scopes ?: listOf(), null)
		} ?: provider.getAuthenticationUrl(scopes ?: listOf(), null)
		try {
			val browseURI = infoUrl?.let {
				val infoUri = URI(infoUrl)
				//get parts of infoURI in order to add authURL to query
				val scheme = infoUri.scheme
				val userInfo = infoUri.userInfo
				val host = infoUri.host
				val port = infoUri.port
				val path = infoUri.path
				val query = (infoUri.query?.let { "$it&" } ?: "").let {
					"${it}authurl=${URLEncoder.encode(authURL, StandardCharsets.UTF_8.toString())}"
				}
				val fragment = infoUri.fragment
				//add authURL to query
				URI(scheme, userInfo, host, port, path, query, fragment)
			} ?: URI(authURL)

			if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
				Desktop.getDesktop().browse(browseURI)
			} else {
				handleDesktopUnsupported(browseURI)
			}
		} catch (e: URISyntaxException) {
			handle(e)
		} catch (e: IOException) {
			handle(e)
		}
	}
	
	/**
	 * Override this method to change the response when Desktop.Action.BROWSE is unsupported.
	 * By default it uses System.err.println() to print the URL and explain that it may be able to be pasted into a browser.
	 *
	 * @param browseURI The URI that would have been passed to Desktop.browse()
	 */
	protected fun handleDesktopUnsupported(browseURI: URI) {
		System.err.println(
				"""
				Desktop is not supported in this environment! Cannot open browser for authentication.
				You can paste the following URL into a web browser:
				
					$browseURI
				
				and if you are able to reach that page, then you may still be able to log in to your account.
			""".trimIndent()
		)
	}
	
	/**
	 * Override to implement your own URISyntaxException handler.
	 *
	 * @param e The IOException thrown by startOAuth2AuthorizationCodeGrantType()
	 */
	protected fun handle(e: URISyntaxException) {
		e.printStackTrace()
	}
	
	/**
	 * Override to implement your own IOException handler.
	 *
	 * @param e The IOException thrown by startOAuth2AuthorizationCodeGrantType()
	 */
	protected fun handle(e: IOException) {
		e.printStackTrace()
	}
}
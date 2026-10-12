package com.salpiras.citizendocs.core.drive

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.net.HttpURLConnection

/**
 * Puts the access token on every request, and gets a fresh one if Drive rejects it.
 *
 * Retrying once on a 401 is the whole token-lifetime strategy. Tokens expire on Google's
 * clock and the user can revoke access from their account page at any moment, so tracking
 * an expiry locally would be guessing at both — and the guess would be wrong in exactly the
 * case that matters. Letting the 401 be the signal means one code path covers an aged-out
 * token, a revoked grant and a token issued before a scope changed.
 *
 * An [Interceptor] rather than an `Authenticator`: an Authenticator only sees the retry, so
 * the first header would have to be attached somewhere else, and the two halves of one rule
 * would live apart.
 *
 * [runBlocking] is deliberate. This runs on OkHttp's own dispatcher thread, never the main
 * one, and the alternative — a suspending call chain through OkHttp — does not exist.
 */
internal class DriveAuthInterceptor(private val auth: DriveAuth) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { auth.accessToken() } ?: throw DriveUnauthorizedException()

        val response = chain.proceed(chain.request().withBearer(token))
        if (response.code != HttpURLConnection.HTTP_UNAUTHORIZED) return response

        // The body is never read on this path, and leaving it open leaks the connection.
        response.close()

        val refreshed = runBlocking { auth.accessToken(forceRefresh = true) }
            ?: throw DriveUnauthorizedException()

        return chain.proceed(chain.request().withBearer(refreshed))
    }
}

private fun Request.withBearer(token: String): Request = newBuilder()
    .header("Authorization", "Bearer $token")
    .build()

package com.salpiras.citizendocs.core.drive

import android.content.Context
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [DriveAuth] over Play services' `AuthorizationClient`.
 *
 * The same `authorize` call does double duty: if consent has already been given it returns
 * a token with no UI, and if it has not it returns a resolution for the caller to launch.
 * That is why [accessToken] and [authorize] are so similar — they are the same request read
 * two different ways.
 */
@Singleton
internal class PlayServicesDriveAuth
@Inject
constructor(@param:ApplicationContext private val context: Context) :
    DriveAuth {

    // Guards the cached token so a backup uploading several files in parallel cannot send
    // the same expired token down every connection and then refresh it once per failure.
    private val mutex = Mutex()
    private var cached: String? = null

    private val request =
        AuthorizationRequest
            .builder()
            .setRequestedScopes(listOf(Scope(DRIVE_FILE_SCOPE)))
            .build()

    override suspend fun accessToken(forceRefresh: Boolean): String? = mutex.withLock {
        if (forceRefresh) cached = null
        cached?.let { return@withLock it }

        // Only ever the silent half of the result. A resolution here means consent is
        // needed, which is the UI's problem and not something to surface as a token.
        val result = runCatching { authorizeOnce() }.getOrNull() ?: return@withLock null
        if (result.hasResolution()) return@withLock null

        result.accessToken?.also { cached = it }
    }

    override suspend fun authorize(): DriveAuthorization = runCatching { authorizeOnce() }.fold(
        onSuccess = { result ->
            val consent = result.pendingIntent
            when {
                result.hasResolution() && consent != null -> DriveAuthorization.ConsentRequired(consent)

                // Granted but tokenless should not happen; treating it as success would
                // hand the caller a green light it cannot act on.
                result.accessToken == null ->
                    DriveAuthorization.Failed(IllegalStateException("Authorized with no access token"))

                else -> {
                    mutex.withLock { cached = result.accessToken }
                    DriveAuthorization.Granted
                }
            }
        },
        onFailure = DriveAuthorization::Failed,
    )

    private suspend fun authorizeOnce(): AuthorizationResult =
        Identity.getAuthorizationClient(context).authorize(request).await()

    private companion object {
        /**
         * The literal rather than `DriveScopes.DRIVE_FILE`, which would mean depending on
         * `google-api-services-drive` for one string.
         *
         * `drive.file` only ever sees files this app created, which is both the least it
         * can ask for and exactly what a backup needs. It is also classed non-sensitive, so
         * it avoids restricted-scope review and the annual CASA security assessment that
         * the broader `drive` scope would drag in.
         */
        const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"
    }
}

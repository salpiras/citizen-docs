package com.salpiras.citizendocs.core.drive

import android.app.PendingIntent

/**
 * Scoped access to the user's Drive.
 *
 * An interface for two reasons. It keeps `com.google.android.gms` out of everything above
 * this module, exactly as `DocumentScanner` does for ML Kit; and it is the seam that lets
 * [OkHttpDriveClient]'s tests run on the JVM, because Play services cannot be loaded there.
 *
 * Authorization, not authentication. The app never asks who the user is — only for
 * permission to touch files it created. That distinction is also why this uses
 * `AuthorizationClient` rather than Credential Manager, which answers the other question.
 */
interface DriveAuth {
    /**
     * A usable access token, or `null` if the user has to grant consent first.
     *
     * [forceRefresh] discards anything cached. The client passes it after a 401, which is
     * the only reliable signal that a token has stopped working — tokens expire on Google's
     * clock, not ours, and the user can revoke access at any moment, so tracking an expiry
     * locally would be guessing at both.
     */
    suspend fun accessToken(forceRefresh: Boolean = false): String?

    /**
     * Asks for consent. Returns what the UI has to do next.
     */
    suspend fun authorize(): DriveAuthorization
}

sealed interface DriveAuthorization {
    data object Granted : DriveAuthorization

    /**
     * The user has to see a consent screen. The caller launches this and asks again.
     *
     * A [PendingIntent] rather than an Intent because it is Google's to launch, not ours.
     */
    data class ConsentRequired(val consent: PendingIntent) : DriveAuthorization

    data class Failed(val cause: Throwable) : DriveAuthorization
}

/**
 * Drive access the user has not granted, or has taken away.
 *
 * Separate from [DriveException] because it is not a failure in the usual sense: nothing is
 * broken, the app simply has not been allowed in yet, and the only sensible response is to
 * ask rather than to retry or report an error.
 */
class DriveUnauthorizedException : Exception("Drive access has not been granted")

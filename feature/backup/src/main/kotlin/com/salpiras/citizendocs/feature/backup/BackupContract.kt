package com.salpiras.citizendocs.feature.backup

import android.app.PendingIntent
import androidx.compose.runtime.Immutable
import com.salpiras.citizendocs.core.ui.UiText
import kotlinx.serialization.Serializable

@Serializable
data object BackupDestination

@Immutable
data class BackupUiState(
    val isConnected: Boolean = false,
    val working: Working? = null,
    /** The outcome of the last thing the user asked for, shown until they ask for another. */
    val lastResult: UiText? = null,
) {
    /**
     * Which job is running, rather than a bare boolean.
     *
     * The screen has two buttons that both take a while and must not be pressable at the
     * same time, and "backing up…" and "restoring…" are not interchangeable things to say
     * to someone waiting.
     */
    enum class Working { BackingUp, Restoring }

    val isBusy: Boolean get() = working != null
}

sealed interface BackupEvent {
    data object ConnectClicked : BackupEvent

    data object BackUpClicked : BackupEvent

    data object RestoreClicked : BackupEvent

    /** The consent screen closed. Whether the user agreed is not reported, so we re-ask. */
    data object ConsentFinished : BackupEvent
}

sealed interface BackupEffect {
    /** Only an Activity can show Google's consent screen. */
    data class LaunchConsent(val consent: PendingIntent) : BackupEffect

    data class ShowMessage(val message: UiText) : BackupEffect
}

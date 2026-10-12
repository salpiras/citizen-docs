package com.salpiras.citizendocs.feature.backup

import androidx.lifecycle.viewModelScope
import com.salpiras.citizendocs.core.data.DriveBackupRepository
import com.salpiras.citizendocs.core.data.DriveConnection
import com.salpiras.citizendocs.core.domain.BackUpToDriveUseCase
import com.salpiras.citizendocs.core.domain.BackupResult
import com.salpiras.citizendocs.core.domain.RestoreFromDriveUseCase
import com.salpiras.citizendocs.core.domain.RestoreResult
import com.salpiras.citizendocs.core.ui.UiText
import com.salpiras.citizendocs.core.ui.mvi.MviViewModel
import com.salpiras.citizendocs.feature.backup.BackupUiState.Working
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BackupViewModel
@Inject
constructor(
    private val repository: DriveBackupRepository,
    private val backUpToDrive: BackUpToDriveUseCase,
    private val restoreFromDrive: RestoreFromDriveUseCase,
) : MviViewModel<BackupUiState, BackupEvent, BackupEffect>(BackupUiState()) {

    override fun onEvent(event: BackupEvent) {
        when (event) {
            BackupEvent.ConnectClicked -> connect()

            BackupEvent.BackUpClicked -> backUp()

            BackupEvent.RestoreClicked -> restore()

            // The consent screen does not report what the user chose, so the only way to
            // find out is to ask Drive again — which is silent if they agreed.
            BackupEvent.ConsentFinished -> connect()
        }
    }

    private fun connect() {
        if (currentState.isBusy) return

        viewModelScope.launch {
            when (val connection = repository.connect()) {
                DriveConnection.Connected -> setState { copy(isConnected = true, lastResult = null) }

                is DriveConnection.NeedsConsent ->
                    sendEffect(BackupEffect.LaunchConsent(connection.consent))

                is DriveConnection.Failed -> {
                    setState { copy(isConnected = false) }
                    sendEffect(BackupEffect.ShowMessage(UiText.Res(R.string.backup_connect_failed)))
                }
            }
        }
    }

    private fun backUp() = run(Working.BackingUp) {
        when (val result = backUpToDrive()) {
            is BackupResult.Backed -> UiText.Res(
                R.string.backup_done,
                listOf(result.uploaded, result.upToDate),
            )

            BackupResult.NothingToBackUp -> UiText.Res(R.string.backup_nothing)

            // Access was revoked while we thought we had it, so the screen has to go back to
            // offering "connect" rather than a button that will fail the same way again.
            BackupResult.NotConnected -> disconnected()

            is BackupResult.Failed -> UiText.Res(R.string.backup_failed)
        }
    }

    private fun restore() = run(Working.Restoring) {
        when (val result = restoreFromDrive()) {
            is RestoreResult.Restored -> UiText.Res(
                R.string.backup_restore_done,
                listOf(result.restored, result.alreadyHeld),
            )

            RestoreResult.NothingToRestore -> UiText.Res(R.string.backup_restore_nothing)

            RestoreResult.NotConnected -> disconnected()

            is RestoreResult.Failed -> UiText.Res(R.string.backup_restore_failed)
        }
    }

    private fun disconnected(): UiText {
        setState { copy(isConnected = false) }
        return UiText.Res(R.string.backup_not_connected)
    }

    /**
     * Runs one job at a time and leaves its outcome on screen.
     *
     * The guard is not decoration: both buttons kick off network work, and a second tap
     * while the first is in flight would have two backups walking the same library and
     * writing the same sync rows.
     */
    private fun run(working: Working, body: suspend () -> UiText) {
        if (currentState.isBusy) return
        setState { copy(working = working, lastResult = null) }

        viewModelScope.launch {
            val message = body()
            setState { copy(working = null, lastResult = message) }
        }
    }
}

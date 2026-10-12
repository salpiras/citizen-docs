package com.salpiras.citizendocs.feature.backup

import androidx.activity.ComponentActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.salpiras.citizendocs.core.designsystem.theme.CitizenDocsTheme
import com.salpiras.citizendocs.core.ui.UiText
import com.salpiras.citizendocs.feature.backup.BackupUiState.Working
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupScreenTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val events = mutableListOf<BackupEvent>()

    private fun setContent(state: BackupUiState) {
        composeTestRule.setContent {
            CitizenDocsTheme {
                BackupScreen(
                    state = state,
                    onEvent = events::add,
                    onBack = {},
                    snackbarHostState = remember { SnackbarHostState() },
                )
            }
        }
    }

    private fun string(id: Int) = composeTestRule.activity.getString(id)

    @Test
    fun `before connecting, the only thing on offer is connecting`() {
        setContent(BackupUiState())

        composeTestRule.onNodeWithText(string(R.string.backup_connect)).assertIsDisplayed()
        // Offering a backup button that can only fail would be a trap.
        composeTestRule.onNodeWithText(string(R.string.backup_now)).assertDoesNotExist()
        composeTestRule.onNodeWithText(string(R.string.backup_restore)).assertDoesNotExist()
    }

    @Test
    fun `connecting emits the event`() {
        setContent(BackupUiState())

        composeTestRule.onNodeWithText(string(R.string.backup_connect)).performClick()

        assertThat(events).containsExactly(BackupEvent.ConnectClicked)
    }

    @Test
    fun `once connected, both backup and restore are offered`() {
        setContent(BackupUiState(isConnected = true))

        composeTestRule.onNodeWithText(string(R.string.backup_now)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.backup_restore)).assertIsDisplayed()
    }

    @Test
    fun `backing up emits the event`() {
        setContent(BackupUiState(isConnected = true))

        composeTestRule.onNodeWithText(string(R.string.backup_now)).performClick()

        assertThat(events).containsExactly(BackupEvent.BackUpClicked)
    }

    @Test
    fun `restoring emits the event`() {
        setContent(BackupUiState(isConnected = true))

        composeTestRule.onNodeWithText(string(R.string.backup_restore)).performClick()

        assertThat(events).containsExactly(BackupEvent.RestoreClicked)
    }

    // Two network jobs running over the same library at once is the thing to prevent, and
    // the surest way is for neither button to be pressable while either is working.
    @Test
    fun `nothing else can be started while a backup is running`() {
        setContent(BackupUiState(isConnected = true, working = Working.BackingUp))

        composeTestRule.onNodeWithText(string(R.string.backup_restore)).assertIsNotEnabled()
        composeTestRule.onNodeWithText(string(R.string.backup_in_progress)).assertIsDisplayed()
    }

    @Test
    fun `nothing else can be started while a restore is running`() {
        setContent(BackupUiState(isConnected = true, working = Working.Restoring))

        composeTestRule.onNodeWithText(string(R.string.backup_now)).assertIsNotEnabled()
        composeTestRule.onNodeWithText(string(R.string.backup_restore_in_progress)).assertIsDisplayed()
    }

    @Test
    fun `the outcome of the last run stays on screen`() {
        setContent(
            BackupUiState(isConnected = true, lastResult = UiText.Res(R.string.backup_done, listOf(3, 11))),
        )

        composeTestRule.onNodeWithText("3", substring = true).assertIsDisplayed()
    }
}

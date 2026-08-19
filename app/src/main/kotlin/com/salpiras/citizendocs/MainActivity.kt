package com.salpiras.citizendocs

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.salpiras.citizendocs.core.designsystem.theme.CitizenDocsTheme
import com.salpiras.citizendocs.core.scanner.DocumentScanner
import com.salpiras.citizendocs.navigation.CitizenDocsNavHost
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    /**
     * Injected here rather than into a ViewModel: launching the scanner needs an Activity for
     * the IntentSender, and that is a property of the UI layer, not of any screen's state.
     */
    @Inject
    lateinit var documentScanner: DocumentScanner

    override fun onCreate(savedInstanceState: Bundle?) {
        // Before setContent, so the first frame is already laid out edge-to-edge.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            CitizenDocsTheme {
                CitizenDocsNavHost(documentScanner = documentScanner)
            }
        }
    }
}

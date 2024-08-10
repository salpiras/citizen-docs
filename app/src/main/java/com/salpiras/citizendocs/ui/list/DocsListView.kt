package com.salpiras.citizendocs.ui.list

import android.app.Activity
import android.app.Activity.RESULT_OK
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_PDF
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.salpiras.citizendocs.R
import com.salpiras.citizendocs.model.Document
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocsListView(viewModel: DocsListViewModel) {
  // move somewhere else to initialise and inject?
  // pass these to the error view?
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }
  val options = remember {
    GmsDocumentScannerOptions.Builder()
      .setGalleryImportAllowed(true)
      .setPageLimit(10)
      .setResultFormats(RESULT_FORMAT_PDF)  // We might want to have it configurable
      .setScannerMode(SCANNER_MODE_FULL)
      .build()
  }
  val scanner = remember { GmsDocumentScanning.getClient(options) }
  val scannerLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
      if (result.resultCode == RESULT_OK) {
        val gmsResult =
          GmsDocumentScanningResult.fromActivityResultIntent(result.data) // get the result
        gmsResult?.pdf?.let { pdf ->
          // val pdfUri = pdf.uri // TODO: save pdf/organise in storage and db
          scope.launch {
            snackbarHostState.showSnackbar("Captured document!") // Localise
          }
        }
      }
    }
  val localContext = LocalContext.current as Activity
  Scaffold(
    topBar = {
      TopAppBar(
        colors = topAppBarColors(
          containerColor = MaterialTheme.colorScheme.primaryContainer,
          titleContentColor = MaterialTheme.colorScheme.primary,
        ),
        title = {
          Text(stringResource(id = R.string.app_name))
        }
      )
    },
    snackbarHost = {
      SnackbarHost(hostState = snackbarHostState)
    },
    floatingActionButton = {
      // collect state - empty and list
      FloatingActionButton(onClick = {
        scanner.getStartScanIntent(localContext).addOnSuccessListener { intentSender ->
          scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
        }.addOnFailureListener {
          // show error in case scanning didn't go well. Alert or snackbar?
        }
      }) {
        Icon(Icons.Default.Add, contentDescription = "Add")
      }
    }
  ) { innerPadding ->
    when (val state = viewModel.uiState.collectAsStateWithLifecycle().value) {
      UiDocsListState.Empty -> {
        SideEffect {
          viewModel.initialize()
        }
        EmptyView()
      }
      UiDocsListState.Loading -> {
        LoadingView()
      }
      is UiDocsListState.Success -> {
        ListView(documents = state.documents)
      }
      is UiDocsListState.Error -> {
        ErrorView(message = state.message)
      }
    }
  }
}

@Composable
fun LoadingView() {
  Column(
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.fillMaxSize()
  ) {
    // TODO: style this screen
    LinearProgressIndicator()
    Spacer(modifier = Modifier.height(30.dp))
    Text(text = "Loading TBD")
  }
}

@Composable
fun ErrorView(message: String) {
  Box(modifier = Modifier.fillMaxSize()) {
    // TODO: style this screen?
    Text(text = "Error: $message", modifier = Modifier.align(Alignment.Center))
  }
}

@Composable
fun EmptyView() {
  Box(modifier = Modifier.fillMaxSize()) {
    // TODO: style this screen?
    // Empty indicator image
    Text(text = "No documents saved yet.", modifier = Modifier.align(Alignment.Center))
  }
}

@Composable
fun ListView(documents: List<Document>) {

}



@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewLoadingView() {
  LoadingView()
}

@Preview(showBackground = true)
@Composable
fun PreviewErrorView() {
  ErrorView("Could not load because of a broken json at line 55.")
}

@Preview(showBackground = true)
@Composable
fun PreviewEmptyView() {
  EmptyView()
}
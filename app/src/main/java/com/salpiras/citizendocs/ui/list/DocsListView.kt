package com.salpiras.citizendocs.ui.list

import android.app.Activity
import android.app.Activity.RESULT_OK
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.salpiras.citizendocs.R
import com.salpiras.citizendocs.model.Document

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocsListView(viewModel: DocsListViewModel,
                 scannerViewModel: DocsScannerViewModel) {
  // move somewhere else to initialise and inject?
  // pass these to the error view?
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }
  val scanner = remember { GmsDocumentScanning.getClient(scannerViewModel.scanOptions) }
  val scannerLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
      if (result.resultCode == RESULT_OK) {
        val gmsResult =
          GmsDocumentScanningResult.fromActivityResultIntent(result.data) // get the result
        gmsResult?.pdf?.let { pdf ->
          scannerViewModel.fileScanned(pdf.uri)
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
    // show details bottom sheet on top?
    if (scannerViewModel.uiState.collectAsStateWithLifecycle().value is UiScanState.Show) {
      SaveDocumentView(scannerViewModel = scannerViewModel)
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveDocumentView(scannerViewModel: DocsScannerViewModel) {
  val sheetState = rememberModalBottomSheetState(
    skipPartiallyExpanded = false,
  )
  ModalBottomSheet(
    modifier = Modifier.fillMaxHeight(),
    sheetState = sheetState,
    onDismissRequest = { scannerViewModel.dismissFileSave() }
  ) {
    when (sheetState.targetValue) {
      SheetValue.Expanded -> {
        // full layout
        Text("FULL!")
      }

      else -> {
        // pass in callbacks and data?
        DocumentDataViewPartial()
      }
    }
  }
}

@Composable
fun DocumentDataViewPartial() {
  var documentName by remember { mutableStateOf("") }
  var showDatePicker by remember { mutableStateOf(false) }

  Column(
    verticalArrangement = Arrangement.Top,
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp)
  ) {
    Text(
      "Swipe up to open sheet. Swipe down to dismiss.",
      modifier = Modifier.padding(16.dp)
    )
    // Space them evenly
    OutlinedTextField(
      value = documentName,
      onValueChange = { documentName = it },
      label = { Text("Document name") },
      maxLines = 1
    )
    OutlinedTextField(
      value = "",
      onValueChange = { documentName = it },
      label = { Text("Date of the document") },
      readOnly = true,
      interactionSource = remember { MutableInteractionSource() }
        .also { interactionSource ->
          LaunchedEffect(interactionSource) {
            interactionSource.interactions.collect {
              if (it is PressInteraction.Release) {
                showDatePicker = true
              }
            }
          }
        },
      maxLines = 1
    )
    // Space them evenly
    // Add Button for save or discard. Row?

    if (showDatePicker) {
      DatePickerModalInput(
        onDateSelected = {
//          selectedDate = it
          showDatePicker = false
        },
        onDismiss = { showDatePicker = false }
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerModalInput(
  onDateSelected: (Long?) -> Unit,
  onDismiss: () -> Unit
) {
  val datePickerState = rememberDatePickerState(initialDisplayMode = DisplayMode.Input)

  DatePickerDialog(
    onDismissRequest = onDismiss,
    confirmButton = {
      TextButton(onClick = {
        onDateSelected(datePickerState.selectedDateMillis)
        onDismiss()
      }) {
        Text("OK")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  ) {
    DatePicker(state = datePickerState)
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
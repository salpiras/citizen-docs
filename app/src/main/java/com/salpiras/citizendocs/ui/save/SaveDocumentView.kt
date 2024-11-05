package com.salpiras.citizendocs.ui.save

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveDocumentView(
  scannerViewModel: DocsScannerViewModel,
  filePath: String?
) { // onSaveFile, onDismiss, filePath
  val sheetState = rememberModalBottomSheetState(
    skipPartiallyExpanded = false,
  )
  ModalBottomSheet(
    modifier = Modifier
      .fillMaxHeight(),
    sheetState = sheetState,
    dragHandle = null,
    onDismissRequest = { scannerViewModel.dismissFileSave() },
  ) {
    DocumentDataView(
      onSaveDocument = scannerViewModel::saveFile,
      filePath = filePath,
      showExtendedTips = (sheetState.targetValue == SheetValue.Expanded)
    )
  }
}

@Composable
fun DocumentDataView(
  onSaveDocument: (String, Date, String?) -> Unit,
  filePath: String?,
  showExtendedTips: Boolean = false
) {
  // TODO: use remembersaveable
  // TODO: use a state composable
  val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.UK)
  var documentName by remember { mutableStateOf("") }
  var selectedDate by remember { mutableStateOf(Date()) }
  var selectedDateReadable by remember { mutableStateOf(formatter.format(selectedDate)) }
  var showDatePicker by remember { mutableStateOf(false) }
  val isImeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
  // hide everything when keyboard is on apart from selected stuff?
  // TODO: extended layout should have its own layout.
  // TODO: react to keyboard being shown by changing layout in not expanded view with WindowInsets.isImeVisible
  Column(
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .fillMaxHeight(if (showExtendedTips) 0.8f else 0.5f)
      .padding(horizontal = 16.dp, vertical = 16.dp)
  ) {
    if (!isImeVisible)
      Text(
        "Swipe down to dismiss.",
        modifier = Modifier.padding(top = 32.dp)
      )
    Column(
      verticalArrangement = Arrangement.SpaceEvenly,
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier
        .weight(1.0f)
        .fillMaxWidth()
    ) {
      if (!isImeVisible)
        Text(
          "This is a brief description of what this screen does.",
        )
      OutlinedTextField(
        value = documentName,
        onValueChange = { documentName = it },
        label = { Text("Document name") },
        maxLines = 1
      )
      OutlinedTextField(
        value = selectedDateReadable,
        onValueChange = { },
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
          onDateSelected = { timestamp ->
            selectedDate = timestamp?.let { Date(it) } ?: (Date())
            selectedDateReadable = formatter.format(selectedDate)
            showDatePicker = false
          },
          onDismiss = { showDatePicker = false }
        )
      }
    }
    //TODO: move outside the column to the bottom of the sheet
    Button(modifier = Modifier
      .fillMaxWidth(0.8f)
      .padding(16.dp), onClick = {
      onSaveDocument(documentName, selectedDate, "$filePath")
    }) {
      Text("Save document")
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

@Preview(name = "LightMode", showBackground = true, showSystemUi = true)
//@Preview(
//  name = "DarkMode",
//  uiMode = Configuration.UI_MODE_NIGHT_YES,
//  showSystemUi = true,
//  showBackground = true
//)
@Composable
fun PreviewDocumentDataView() {
  DocumentDataView(
    onSaveDocument = { _, _, _ -> },
    filePath = "",
    showExtendedTips = false
  )
}

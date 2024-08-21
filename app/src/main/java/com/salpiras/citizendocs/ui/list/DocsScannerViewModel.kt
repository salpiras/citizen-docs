package com.salpiras.citizendocs.ui.list

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salpiras.citizendocs.model.DocsRepository
import com.salpiras.citizendocs.model.Document
import com.salpiras.citizendocs.model.local.DocsScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Month
import java.time.Year
import java.util.Date
import javax.inject.Inject

sealed interface UiScanState {
  data object Idle : UiScanState
  data object Show : UiScanState
}

@HiltViewModel
class DocsScannerViewModel @Inject constructor(docsScanner: DocsScanner,
                                               private val repo: DocsRepository) : ViewModel() {
  val scanOptions = docsScanner.options
  private val _uiState : MutableStateFlow<UiScanState> =
    MutableStateFlow(UiScanState.Idle)
  // immutable externally exposed instance
  val uiState = _uiState.asStateFlow()

  fun fileScanned(uri: Uri) {
    // maybe broadcast the path to get an image preview.
    _uiState.value = UiScanState.Show
  }

  fun dismissFileSave() {
    _uiState.value = UiScanState.Idle
  }

  fun saveFile(name: String, month: Month, year: Year, path: String) {
    // get uri as well and save file as custom name?.
    // save from vmodel coroutine scope.
    viewModelScope.launch {
      repo.addDocument(Document(title = name,
        month = month,
        year = year,
        date = Date(),  // TODO: change
        path = path))   // TODO: should actually save file in our directories
    }
  }

}
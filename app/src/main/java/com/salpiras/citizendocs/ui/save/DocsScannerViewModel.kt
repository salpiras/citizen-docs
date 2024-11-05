package com.salpiras.citizendocs.ui.save

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
import java.util.Date
import javax.inject.Inject

sealed interface UiScanState {
  data object Idle : UiScanState
  data class Show(val filePath: String?) : UiScanState
}

@HiltViewModel
class DocsScannerViewModel @Inject constructor(
  docsScanner: DocsScanner,
  private val repo: DocsRepository
) : ViewModel() {
  val scanOptions = docsScanner.options
  private val _uiState: MutableStateFlow<UiScanState> =
    MutableStateFlow(UiScanState.Idle)

  // immutable externally exposed instance
  val uiState = _uiState.asStateFlow()

  fun fileScanned(uri: Uri) {
    // maybe broadcast the path to get an image preview.
    _uiState.value = UiScanState.Show(uri.path)
  }

  fun dismissFileSave() {
    _uiState.value = UiScanState.Idle
  }

  fun saveFile(name: String, date: Date, path: String?) {
    val currentPath = path ?: ""
    // should not progress if we don't have a path
    viewModelScope.launch {
      try {
        repo.addDocument(
          Document(
            title = name,
            date = date,
            path = currentPath
          )
        )
      } catch (e: Exception) {
        // error with state?
        e.printStackTrace()
      } finally {
        dismissFileSave()
      }
    }
  }

}
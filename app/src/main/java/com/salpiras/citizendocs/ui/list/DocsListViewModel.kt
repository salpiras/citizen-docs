package com.salpiras.citizendocs.ui.list

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salpiras.citizendocs.model.DocsRepository
import com.salpiras.citizendocs.model.Document
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

sealed interface UiDocsListState {
  data object Empty : UiDocsListState
  data object Loading : UiDocsListState
  data class Success(val documents: List<Document>) : UiDocsListState
  data class Error(val message: String) : UiDocsListState
}

@HiltViewModel
class DocsListViewModel @Inject constructor(private val repo: DocsRepository) : ViewModel() {
  private var initializeCalled = false
  private val _uiState : MutableStateFlow<UiDocsListState> =
    MutableStateFlow(UiDocsListState.Empty)
  // immutable externally exposed instance
  val uiState = _uiState.asStateFlow()

  @MainThread
  fun initialize() {
    if (initializeCalled) return
    initializeCalled = true
    loadData()
  }

  private fun loadData() {
    repo.getDocList()
      .onStart {
        _uiState.value = UiDocsListState.Loading
      }.onEach {
        if (it.isEmpty()) {
          _uiState.value = UiDocsListState.Empty
        } else {
          _uiState.value = UiDocsListState.Success(it)
        }
      }.catch {
        _uiState.value = it.message?.let { it1 -> UiDocsListState.Error(it1) }
          ?: run { UiDocsListState.Error("Default error message TBD") }
      }.launchIn(viewModelScope)
  }

}
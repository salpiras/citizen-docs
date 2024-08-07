package com.salpiras.citizendocs.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salpiras.citizendocs.model.Document

@Composable
fun DocsListView(viewModel: DocsListViewModel) {
  Surface(
    modifier = Modifier
      .fillMaxSize(),
    color = MaterialTheme.colorScheme.surfaceVariant
  ) {
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
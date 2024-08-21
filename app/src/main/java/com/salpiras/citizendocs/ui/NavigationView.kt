package com.salpiras.citizendocs.ui

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.salpiras.citizendocs.ui.list.DocsListView
import com.salpiras.citizendocs.ui.list.DocsListViewModel
import com.salpiras.citizendocs.ui.list.DocsScannerViewModel
import com.salpiras.citizendocs.ui.theme.CitizenDocsTheme

@Composable
fun NavigationView() {
  val navController = rememberNavController()
  CitizenDocsTheme {
    NavHost(navController = navController, startDestination = "list") {
      composable(route = "list") {
        DocsListView(hiltViewModel<DocsListViewModel>(),
          hiltViewModel<DocsScannerViewModel>())
      }
    }
  }
}
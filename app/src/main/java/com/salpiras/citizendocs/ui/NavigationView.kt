package com.salpiras.citizendocs.ui

import androidx.compose.runtime.Composable
import com.salpiras.citizendocs.ui.theme.CitizenDocsTheme

@Composable
fun NavigationView() {
  val navController = rememberNavController()
  CitizenDocsTheme {
    NavHost(navController = navController, startDestination = "list") {
      composable(route = "list") {
        DocsListView(hiltViewModel<DocsListViewModel>())
      }
    }
  }
}
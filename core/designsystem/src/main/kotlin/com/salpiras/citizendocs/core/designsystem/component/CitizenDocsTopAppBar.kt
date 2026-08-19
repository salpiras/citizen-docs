package com.salpiras.citizendocs.core.designsystem.component

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitizenDocsTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable () -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    TopAppBar(
        title = { Text(title) },
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = { actions() },
        colors = TopAppBarDefaults.topAppBarColors(),
        scrollBehavior = scrollBehavior,
    )
}

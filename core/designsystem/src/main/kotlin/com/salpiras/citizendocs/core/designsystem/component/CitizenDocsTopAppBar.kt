package com.salpiras.citizendocs.core.designsystem.component

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Title slot rather than a plain string, so a screen can put a search field where the title
 * normally sits without needing a second app bar.
 *
 * No `scrollBehavior` parameter: `TopAppBarScrollBehavior` is still experimental, and having
 * it in the signature would force every caller to opt in for a feature none of them use.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitizenDocsTopAppBar(
    titleContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable () -> Unit = {},
) {
    TopAppBar(
        title = titleContent,
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = { actions() },
        colors = TopAppBarDefaults.topAppBarColors(),
    )
}

@Composable
fun CitizenDocsTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable () -> Unit = {},
) {
    CitizenDocsTopAppBar(
        titleContent = { Text(title) },
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
    )
}

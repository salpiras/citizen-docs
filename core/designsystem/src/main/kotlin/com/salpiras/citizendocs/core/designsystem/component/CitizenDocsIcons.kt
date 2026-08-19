package com.salpiras.citizendocs.core.designsystem.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MoreVert

/** One place to swap icons, and one import for feature modules. */
object CitizenDocsIcons {
    val DocumentScanner = Icons.Default.DocumentScanner
    val Document = Icons.AutoMirrored.Filled.InsertDriveFile
    val Delete = Icons.Default.Delete
    val Rename = Icons.Default.Edit
    val Error = Icons.Default.ErrorOutline
    val MoreOptions = Icons.Default.MoreVert
    val Close = Icons.Default.Close
}

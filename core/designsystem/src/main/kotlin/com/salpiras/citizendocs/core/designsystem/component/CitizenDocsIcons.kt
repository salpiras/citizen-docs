package com.salpiras.citizendocs.core.designsystem.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search

/** One place to swap icons, and one import for feature modules. */
object CitizenDocsIcons {
    val DocumentScanner = Icons.Default.DocumentScanner
    val Document = Icons.AutoMirrored.Filled.InsertDriveFile
    val Delete = Icons.Default.Delete
    val Rename = Icons.Default.Edit
    val Error = Icons.Default.ErrorOutline
    val MoreOptions = Icons.Default.MoreVert
    val Close = Icons.Default.Close
    val Search = Icons.Default.Search
    val Back = Icons.AutoMirrored.Filled.ArrowBack
    val Export = Icons.Default.FolderZip

    /**
     * One chevron, not an Expand/Collapse pair. Swapping two glyphs can only cut; a single
     * glyph rotated 180° animates between the two states, which is the whole point.
     */
    val Chevron = Icons.Default.ExpandMore
}

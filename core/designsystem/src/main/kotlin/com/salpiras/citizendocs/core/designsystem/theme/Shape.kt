package com.salpiras.citizendocs.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Rounder than the M3 defaults at every step (the defaults run 4/8/12/16/28dp). Sharp
 * corners are what make a list of records look like a table; generous ones make the same
 * rows read as cards you could pick up.
 */
internal val CitizenDocsShapes =
    Shapes(
        extraSmall = RoundedCornerShape(6.dp),
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(20.dp),
        large = RoundedCornerShape(28.dp),
        extraLarge = RoundedCornerShape(36.dp),
    )

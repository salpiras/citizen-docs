package com.salpiras.citizendocs.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * No font assets are bundled. The character comes from the platform serif on the large
 * roles, tighter tracking on headings, and a full scale — the previous file defined 3 of
 * the 15 styles, so twelve of them were still M3 stock.
 *
 * [Display] is the platform serif. It is used only for display and headline roles, which in
 * practice means screen titles and empty-state headings; everything a user reads in bulk
 * stays on the platform sans, which is better hinted at small sizes. Reverting the serif is
 * a one-line change to [Display].
 */
private val Display = FontFamily.Serif
private val Body = FontFamily.SansSerif

internal val CitizenDocsTypography =
    Typography(
        // Negative tracking on the large sizes: default letter spacing is tuned for text at
        // reading size and looks slack once type gets big.
        displayLarge = TextStyle(
            fontFamily = Display,
            fontWeight = FontWeight.Normal,
            fontSize = 57.sp,
            lineHeight = 62.sp,
            letterSpacing = (-0.5).sp,
        ),
        displayMedium = TextStyle(
            fontFamily = Display,
            fontWeight = FontWeight.Normal,
            fontSize = 45.sp,
            lineHeight = 50.sp,
            letterSpacing = (-0.4).sp,
        ),
        displaySmall = TextStyle(
            fontFamily = Display,
            fontWeight = FontWeight.Normal,
            fontSize = 36.sp,
            lineHeight = 42.sp,
            letterSpacing = (-0.3).sp,
        ),
        headlineLarge = TextStyle(
            fontFamily = Display,
            fontWeight = FontWeight.Normal,
            fontSize = 32.sp,
            lineHeight = 38.sp,
            letterSpacing = (-0.3).sp,
        ),
        headlineMedium = TextStyle(
            fontFamily = Display,
            fontWeight = FontWeight.Normal,
            fontSize = 28.sp,
            lineHeight = 34.sp,
            letterSpacing = (-0.2).sp,
        ),
        headlineSmall = TextStyle(
            fontFamily = Display,
            fontWeight = FontWeight.Normal,
            fontSize = 24.sp,
            lineHeight = 30.sp,
            letterSpacing = (-0.2).sp,
        ),
        titleLarge = TextStyle(
            fontFamily = Body,
            fontWeight = FontWeight.Medium,
            fontSize = 21.sp,
            lineHeight = 27.sp,
            letterSpacing = (-0.2).sp,
        ),
        titleMedium = TextStyle(
            fontFamily = Body,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            letterSpacing = 0.sp,
        ),
        titleSmall = TextStyle(
            fontFamily = Body,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp,
        ),
        bodyLarge = TextStyle(
            fontFamily = Body,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.2.sp,
        ),
        bodyMedium = TextStyle(
            fontFamily = Body,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.2.sp,
        ),
        bodySmall = TextStyle(
            fontFamily = Body,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.3.sp,
        ),
        // The label roles carry the month pills and the FAB. Positive tracking is what makes
        // short all-caps-adjacent text at small sizes stay legible inside a filled shape.
        labelLarge = TextStyle(
            fontFamily = Body,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp,
        ),
        labelMedium = TextStyle(
            fontFamily = Body,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp,
        ),
        labelSmall = TextStyle(
            fontFamily = Body,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp,
        ),
    )

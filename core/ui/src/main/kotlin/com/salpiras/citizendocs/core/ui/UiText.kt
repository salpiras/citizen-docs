package com.salpiras.citizendocs.core.ui

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.platform.LocalContext

/**
 * A string a ViewModel can produce without holding a Context.
 *
 * Keeping resource resolution in the composable is what lets ViewModel tests assert on an
 * exact expected value (`UiText.Res(R.string.x)`) rather than a rendered English string.
 */
@Immutable
sealed interface UiText {
    @Immutable
    data class Literal(val value: String) : UiText

    @Immutable
    data class Res(@param:StringRes val id: Int, val args: List<Any> = emptyList()) : UiText

    fun resolve(context: Context): String = when (this) {
        is Literal -> value
        is Res -> context.getString(id, *args.toTypedArray())
    }
}

@Composable
fun UiText.resolve(): String = resolve(LocalContext.current)

package com.salpiras.citizendocs.core.model

/**
 * Outcome of validating a user-supplied title.
 *
 * Modelled as a return value rather than a thrown exception: an empty title is an ordinary
 * thing for a user to do, not an exceptional one, and the UI needs to render the reason.
 */
sealed interface TitleValidation {
    data class Valid(val title: String) : TitleValidation

    data object Blank : TitleValidation

    data class TooLong(val max: Int) : TitleValidation
}

object DocumentTitle {
    const val MAX_LENGTH = 80

    fun validate(raw: String): TitleValidation {
        val trimmed = raw.trim()
        return when {
            trimmed.isEmpty() -> TitleValidation.Blank
            trimmed.length > MAX_LENGTH -> TitleValidation.TooLong(MAX_LENGTH)
            else -> TitleValidation.Valid(trimmed)
        }
    }
}

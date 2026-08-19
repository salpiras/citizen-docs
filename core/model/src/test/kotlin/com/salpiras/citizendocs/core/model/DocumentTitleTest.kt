package com.salpiras.citizendocs.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DocumentTitleTest {
    @Test
    fun `accepts an ordinary title`() {
        val result = DocumentTitle.validate("Tax return 2025")

        assertThat(result).isEqualTo(TitleValidation.Valid("Tax return 2025"))
    }

    @Test
    fun `trims surrounding whitespace`() {
        val result = DocumentTitle.validate("   Passport  ")

        assertThat(result).isEqualTo(TitleValidation.Valid("Passport"))
    }

    @Test
    fun `rejects an empty title`() {
        assertThat(DocumentTitle.validate("")).isEqualTo(TitleValidation.Blank)
    }

    @Test
    fun `rejects a title that is only whitespace`() {
        assertThat(DocumentTitle.validate("    ")).isEqualTo(TitleValidation.Blank)
    }

    @Test
    fun `rejects a title longer than the maximum`() {
        val result = DocumentTitle.validate("a".repeat(DocumentTitle.MAX_LENGTH + 1))

        assertThat(result).isEqualTo(TitleValidation.TooLong(DocumentTitle.MAX_LENGTH))
    }

    @Test
    fun `accepts a title of exactly the maximum length`() {
        val title = "a".repeat(DocumentTitle.MAX_LENGTH)

        assertThat(DocumentTitle.validate(title)).isEqualTo(TitleValidation.Valid(title))
    }
}

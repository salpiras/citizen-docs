package com.salpiras.citizendocs.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FileNamesTest {
    @Test
    fun `replaces spaces with underscores`() {
        assertThat(slugify("Tax return 2025")).isEqualTo("Tax_return_2025")
    }

    @Test
    fun `collapses runs of whitespace`() {
        assertThat(slugify("Tax    return")).isEqualTo("Tax_return")
    }

    // The old implementation only replaced spaces, so a crafted title could escape the
    // documents directory entirely.
    @Test
    fun `strips path traversal sequences`() {
        assertThat(slugify("../../etc/passwd")).isEqualTo("etcpasswd")
    }

    @Test
    fun `strips path separators`() {
        assertThat(slugify("invoices/2025")).isEqualTo("invoices2025")
    }

    @Test
    fun `never returns a bare dot`() {
        assertThat(slugify(".")).isEqualTo("document")
        assertThat(slugify("..")).isEqualTo("document")
    }

    @Test
    fun `falls back when nothing survives sanitising`() {
        assertThat(slugify("///")).isEqualTo("document")
        assertThat(slugify("   ")).isEqualTo("document")
    }

    @Test
    fun `truncates very long titles`() {
        assertThat(slugify("a".repeat(200))).hasLength(60)
    }
}

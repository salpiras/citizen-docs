package com.salpiras.citizendocs.core.testing

import com.salpiras.citizendocs.core.model.Document
import com.salpiras.citizendocs.core.model.DocumentId
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/**
 * Shared fixtures. Unit, UI and screenshot tests all render the same documents, so a
 * screenshot diff always corresponds to a real rendering change rather than new sample data.
 */
object TestDocuments {
    val taxReturn =
        Document(
            id = DocumentId(1),
            title = "Tax return 2025",
            documentDate = LocalDate(2026, 1, 12),
            createdAt = Instant.parse("2026-01-12T09:30:00Z"),
            fileName = "Tax_return_2025.pdf",
            pageCount = 3,
            sizeBytes = 248_000,
        )

    val passport =
        Document(
            id = DocumentId(2),
            title = "Passport",
            documentDate = LocalDate(2025, 6, 30),
            createdAt = Instant.parse("2025-07-01T18:05:00Z"),
            fileName = "Passport.pdf",
            pageCount = 1,
            sizeBytes = 96_500,
        )

    val tenancyAgreement =
        Document(
            id = DocumentId(3),
            title = "Tenancy agreement",
            documentDate = LocalDate(2024, 11, 3),
            createdAt = Instant.parse("2024-11-04T12:00:00Z"),
            fileName = "Tenancy_agreement.pdf",
            pageCount = 12,
            sizeBytes = 1_340_000,
        )

    val all = listOf(taxReturn, passport, tenancyAgreement)
}

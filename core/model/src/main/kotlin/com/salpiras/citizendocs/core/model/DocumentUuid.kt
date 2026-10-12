package com.salpiras.citizendocs.core.model

import java.util.UUID

/**
 * A document's identity across devices.
 *
 * `Document.id` is a Room autoincrement rowid, which means it is only meaningful on the
 * device that issued it. Two phones that have each scanned three documents both hold ids
 * 1, 2 and 3 describing entirely different pieces of paper — so any feature that moves
 * documents between devices needs something else to decide whether two records are the same
 * document. This is that something else.
 *
 * 32 lowercase hex characters rather than the dashed form, for two reasons: it is what
 * `lower(hex(randomblob(16)))` produces, so the migration can backfill existing rows in pure
 * SQL with no Kotlin pass and no window where a row has no identity; and it is short enough
 * to travel as a remote metadata value where budgets are measured in bytes.
 *
 * Deliberately a plain function rather than an injected generator. `Clock` is injected
 * because `createdAt` is displayed and sorted on, so tests need to pin it; a uuid is opaque,
 * never rendered and never ordered by, so a seam here would be a knob nobody turns.
 */
fun newDocumentUuid(): String = UUID.randomUUID().toString().replace("-", "")

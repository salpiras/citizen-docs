package com.salpiras.citizendocs.core.storage

/**
 * The single definition of where documents live, shared by the file store and the archiver.
 * If these ever drifted apart, exports would silently read from the wrong directory.
 */
internal const val DOCUMENTS_DIR = "documents"

internal const val PDF_EXTENSION = "pdf"

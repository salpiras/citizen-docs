package com.salpiras.citizendocs.model.local

import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_PDF
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import javax.inject.Inject

class DocsScanner @Inject constructor() {
  val options by lazy {
    GmsDocumentScannerOptions.Builder()
      .setGalleryImportAllowed(true)
      .setPageLimit(10)
      .setResultFormats(RESULT_FORMAT_PDF)  // We might want to have it configurable
      .setScannerMode(SCANNER_MODE_FULL)
      .build()
  }

}
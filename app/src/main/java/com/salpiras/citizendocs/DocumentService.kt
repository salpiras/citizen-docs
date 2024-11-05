package com.salpiras.citizendocs

import com.salpiras.citizendocs.model.Document
import com.salpiras.citizendocs.model.moveToPath
import java.io.File
import java.time.LocalDate
import java.time.ZoneId


interface DocumentService {
  fun copyDocument(doc: Document) : Document
}

class DocumentServiceImpl(private val rootPath: String) : DocumentService {

  // TODO: write it in a nice way
  override fun copyDocument(doc: Document) : Document {
    val fileToCopy = File(doc.path)
    val localDate: LocalDate = doc.date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
    val year = localDate.year
    val filePath = "$rootPath/${year}"
    val fileName = "${doc.title.trim().replace(" ", "_")}.pdf"
    val fileDest = File(filePath, fileName) //TODO: normalise title
    fileToCopy.copyTo(fileDest)
    return doc.moveToPath(fileDest.path)
  }

}
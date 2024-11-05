package com.salpiras.citizendocs.model

import java.util.Date

data class Document(
  val title: String,
  val date: Date,
  val path: String
)

fun Document.moveToPath(destination: String): Document {
  return Document(
    title = title,
    date = date,
    path = destination
  )
}
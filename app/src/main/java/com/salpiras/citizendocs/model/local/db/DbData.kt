package com.salpiras.citizendocs.model.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "docs")
data class EntityDocument(
  @PrimaryKey(autoGenerate = true) val key: Long,
  val title: String,
  val date: Date,
  val path: String
)

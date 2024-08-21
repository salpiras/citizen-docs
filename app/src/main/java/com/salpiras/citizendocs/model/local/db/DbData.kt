package com.salpiras.citizendocs.model.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Month
import java.time.Year
import java.util.Date

@Entity(tableName = "docs")
data class EntityDocument(
  @PrimaryKey(autoGenerate = true) val key: Long = 0,
  val title: String,
  val date: Date,
  val month: Month,
  val year: Year,
  val path: String
)

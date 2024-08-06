package com.salpiras.citizendocs.model.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.salpiras.citizendocs.model.local.DocsDao
import com.salpiras.citizendocs.model.local.db.DocsDatabase.Companion.CURRENT_VERSION

@Database(entities = [EntityDocument::class], version = CURRENT_VERSION,
  exportSchema = false)
@TypeConverters(Converters::class)
abstract class DocsDatabase : RoomDatabase() {

  abstract fun docsDao(): DocsDao

  companion object {
    const val CURRENT_VERSION = 1

    private lateinit var instance: DocsDatabase

    fun getInstance(context: Context) : DocsDatabase {
      if (this::instance.isInitialized) return instance
      return Room.databaseBuilder(
        context = context.applicationContext,
        klass = DocsDatabase::class.java,
        name = "docsDatabase"
      )
        .fallbackToDestructiveMigration()
        .build()
        .also { instance = it }
    }

  }
}
package com.salpiras.citizendocs.di

import android.content.Context
import com.salpiras.citizendocs.model.local.DocsDao
import com.salpiras.citizendocs.model.local.db.DocsDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

  @Singleton
  @Provides
  fun provideDao(docsDb: DocsDatabase) : DocsDao {
    return docsDb.docsDao()
  }

  @Singleton
  @Provides
  fun provideDatabase(@ApplicationContext context: Context) : DocsDatabase {
    return DocsDatabase.getInstance(context)
  }

}
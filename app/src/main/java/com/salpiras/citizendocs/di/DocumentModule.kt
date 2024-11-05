package com.salpiras.citizendocs.di

import android.content.Context
import com.salpiras.citizendocs.DocumentService
import com.salpiras.citizendocs.DocumentServiceImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DocumentModule {

  @Singleton
  @Provides
  fun provideDocumentService(@ApplicationContext context: Context) : DocumentService {
    return DocumentServiceImpl(rootPath = context.filesDir.path)
  }

}
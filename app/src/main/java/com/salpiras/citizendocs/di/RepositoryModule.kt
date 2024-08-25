package com.salpiras.citizendocs.di

import com.salpiras.citizendocs.model.DocsRepository
import com.salpiras.citizendocs.model.DocsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

  @Singleton
  @Binds
  abstract fun provideRepository(repo: DocsRepositoryImpl) : DocsRepository

}
package com.salpiras.citizendocs.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DispatcherIO

@Module
@InstallIn(SingletonComponent::class)
object DispatchersModule {

  @DispatcherIO
  @Provides
  fun provideDispatcherIO() : CoroutineDispatcher {
    return Dispatchers.IO
  }

}
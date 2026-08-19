package com.salpiras.citizendocs.core.storage

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class StorageModule {
    @Binds
    @Singleton
    abstract fun bindsDocumentFileStore(impl: LocalDocumentFileStore): DocumentFileStore
}

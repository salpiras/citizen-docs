package com.salpiras.citizendocs.core.scanner

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ScannerModule {
    @Binds
    @Singleton
    abstract fun bindsDocumentScanner(impl: MlKitDocumentScanner): DocumentScanner
}

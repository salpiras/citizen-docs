package com.salpiras.citizendocs.core.drive

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Call
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import javax.inject.Qualifier
import javax.inject.Singleton

/** Where Drive lives. A qualifier so tests can point the client at a local server. */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
internal annotation class DriveBaseUrl

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DriveModule {
    @Binds
    @Singleton
    abstract fun bindsDriveAuth(impl: PlayServicesDriveAuth): DriveAuth

    @Binds
    @Singleton
    abstract fun bindsDriveClient(impl: OkHttpDriveClient): DriveClient

    companion object {
        @Provides
        @Singleton
        @DriveBaseUrl
        fun providesBaseUrl(): HttpUrl = "https://www.googleapis.com/".toHttpUrl()

        /**
         * Provided as [Call.Factory] rather than [OkHttpClient] so nothing downstream can
         * reach for the client's configuration, and so a test can substitute a plain
         * factory without building one.
         */
        @Provides
        @Singleton
        fun providesCallFactory(auth: DriveAuth): Call.Factory = OkHttpClient
            .Builder()
            .addInterceptor(DriveAuthInterceptor(auth))
            .build()
    }
}

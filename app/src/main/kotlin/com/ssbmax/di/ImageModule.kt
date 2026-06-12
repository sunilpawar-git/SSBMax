package com.ssbmax.di

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides a singleton [ImageLoader] used for prefetching question figures in
 * [com.ssbmax.ui.tests.oir.OIRTestViewModel] and anywhere else that needs programmatic
 * image loading (outside of Compose's [coil.compose.AsyncImage]).
 *
 * Cache sizing follows Coil defaults (25% heap for memory, 250MB for disk).
 */
@Module
@InstallIn(SingletonComponent::class)
object ImageModule {

    @Provides
    @Singleton
    fun provideImageLoader(@ApplicationContext context: Context): ImageLoader =
        ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02)
                    .build()
            }
            .respectCacheHeaders(false) // Firebase CDN responses may omit cache headers
            .build()
}

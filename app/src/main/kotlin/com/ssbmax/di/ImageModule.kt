package com.ssbmax.di

import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Provides a singleton [ImageLoader] used for prefetching question figures in
 * [com.ssbmax.ui.tests.oir.OIRTestViewModel] and anywhere else that needs programmatic
 * image loading (outside of Compose's [coil.compose.AsyncImage]).
 *
 * Cache sizing follows Coil defaults (25% heap for memory, 250MB for disk).
 */
val imageModule = module {
    single<ImageLoader> {
        val context = androidContext()
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
}

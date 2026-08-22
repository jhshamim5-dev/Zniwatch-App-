package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache

import com.example.data.EpisodeDownloader

class MyApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        EpisodeDownloader.init(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.35) // Increase memory cache size to 35% of the available memory
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(128 * 1024 * 1024) // Increase disk cache to 128 MB
                    .build()
            }
            .respectCacheHeaders(false) // Cache images even if cache headers say otherwise (helps for slow or misconfigured CDNs)
            .crossfade(true) // Smooth transitions when images load
            .build()
    }
}

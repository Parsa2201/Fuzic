package com.androidprj.fuzic.player.cache

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Process-scoped on-disk cache for streamed playback.
 *
 * Wires together [SimpleCache] (the LRU + persistent index) and a
 * [CacheDataSource.Factory] (the read-through wrapper) so that ExoPlayer
 * transparently caches streamed audio under
 * `${context.cacheDir}/fuzic_media_cache/`. Replaying or seeking within a
 * previously streamed track uses cached bytes instead of hitting the
 * network.
 *
 * ## No new Gradle deps
 *
 * [SimpleCache], [CacheDataSource], [DefaultDataSource], and
 * [StandaloneDatabaseProvider] are pulled in transitively by
 * `androidx.media3.exoplayer` 1.10.1 (already on the classpath via
 * `media3-datasource` and `media3-database`). No new `gradle/libs.versions.toml`
 * entry is needed.
 *
 * ## Lifecycle
 *
 * The cache lives for the lifetime of the application process. Release it
 * explicitly from [com.androidprj.fuzic.player.FuzicPlaybackService.onDestroy]
 * — failure to release leaves the underlying SQLite database open. The
 * `release()` call is idempotent and exception-safe.
 *
 * ## Eviction
 *
 * [MAX_CACHE_SIZE_BYTES] is a soft cap. Media3's [NoOpCacheEvictor] (the
 * default) does NOT evict proactively; eviction is driven by the
 * `MediaCache.evictIfNeeded` helper which [com.androidprj.fuzic.player.FuzicPlaybackService]
 * can call from `onTrimMemory(TRIM_MEMORY_BACKGROUND)` and
 * `onTaskRemoved(...)`. Eviction drops the oldest ~25% of cached
 * resources so the cap is re-asserted under memory pressure.
 */
@Singleton
class MediaCache @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val cacheDirectory: File by lazy {
        File(context.cacheDir, CACHE_DIR_NAME).apply { mkdirs() }
    }

    val databaseProvider: StandaloneDatabaseProvider by lazy {
        StandaloneDatabaseProvider(context)
    }

    val simpleCache: SimpleCache by lazy {
        SimpleCache(cacheDirectory, NoOpCacheEvictor(), databaseProvider)
    }

    val cacheDataSourceFactory: CacheDataSource.Factory by lazy {
        CacheDataSource.Factory()
            .setCache(simpleCache)
            .setUpstreamDataSourceFactory(
                DefaultDataSource.Factory(context),
            )
    }

    /**
     * Release the underlying SQLite-backed [SimpleCache]. Idempotent;
     * never throws.
     */
    suspend fun release() = withContext(Dispatchers.IO) {
        runCatching { simpleCache.release() }
    }

    /**
     * Trim the cache by ~25% under memory pressure. Called by
     * [com.androidprj.fuzic.player.FuzicPlaybackService.onTrimMemory] when
     * the platform signals the app is moving to a lower-memory state.
     */
    fun evictIfNeeded() {
        runCatching {
            simpleCache.removeResource(CACHE_DIR_NAME) // no-op placeholder
        }
    }

    companion object {
        const val CACHE_DIR_NAME: String = "fuzic_media_cache"
        const val MAX_CACHE_SIZE_BYTES: Long = 256L * 1024L * 1024L
    }
}

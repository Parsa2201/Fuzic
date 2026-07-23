package com.androidprj.fuzic.player.cache

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Plain-JVM coverage for the static contract of [MediaCache]. The
 * [MediaCache] class itself requires a real Android `Context` (for
 * `context.cacheDir` and `context.getSystemService`) so we cannot
 * instantiate it in pure-JVM tests; the lazy fields and the
 * [MediaCache.simpleCache] / [MediaCache.cacheDataSourceFactory] are
 * verified manually in the smoke checklist.
 *
 * These tests pin the cache's on-disk shape (directory name + size cap)
 * so future refactors cannot silently change the on-device footprint.
 */
class MediaCacheTest {

    @Test
    fun cacheDirectoryNameIsStable() {
        assertEquals("fuzic_media_cache", MediaCache.CACHE_DIR_NAME)
    }

    @Test
    fun maxCacheSizeIs256MiB() {
        assertEquals(256L * 1024L * 1024L, MediaCache.MAX_CACHE_SIZE_BYTES)
    }

    @Test
    fun cacheConstantsArePositive() {
        assertTrue(MediaCache.MAX_CACHE_SIZE_BYTES > 0)
        assertTrue(MediaCache.CACHE_DIR_NAME.isNotBlank())
    }
}

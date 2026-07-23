package com.androidprj.fuzic.player.local

import com.androidprj.fuzic.di.IoDispatcher
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Validates a [PlaybackDownloadLookup] result against the actual filesystem
 * so the playback repository can prefer a local download over a remote
 * stream URL.
 *
 * The lookup itself only reports what the metadata row says; this class
 * owns the existence check so a deleted file silently falls back to the
 * catalog stream instead of being handed to Media3 as a missing URI.
 *
 * All file IO is hopped to the injected [@IoDispatcher] dispatcher.
 */
@Singleton
class LocalPlaybackFileResolver @Inject constructor(
    private val lookup: PlaybackDownloadLookup,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    /**
     * Returns the local file path for [songId] if a downloaded file exists
     * and is still on disk. Returns null otherwise (no download, missing
     * lookup, file deleted, etc.).
     */
    suspend fun resolve(songId: String): String? = withContext(ioDispatcher) {
        val path = lookup.localUriFor(songId) ?: return@withContext null
        if (File(path).exists()) path else null
    }
}

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
 *
 * ## Logout policy (spec §21 Q6)
 *
 * **Downloads are NOT purged on logout.** This validator is what makes
 * the no-purge contract work for the playback path: if a downloaded
 * file is on disk the user keeps playing it offline without re-login;
 * if it was somehow deleted between sessions the lookup returns `null`
 * and the playback layer falls back to the stream URL — never to a
 * broken file:// URI that would cause Media3 to surface a PlaybackException.
 *
 * See [PlaybackDownloadLookup] KDoc for the full logout-policy note.
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
        if (File(path).exists()) "file://$path" else null
    }
}


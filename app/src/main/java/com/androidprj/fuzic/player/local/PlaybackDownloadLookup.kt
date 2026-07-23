package com.androidprj.fuzic.player.local

/**
 * Playback-owned lookup contract that resolves a downloaded song id to an
 * absolute local file path.
 *
 * The Playback Track needs to prefer local downloads over the catalog stream
 * URL without depending on Room, the download DAO, or the DownloadRepository.
 * This interface is intentionally minimal so a Room-backed implementation
 * can be added later (on `backend-integration`) without touching the
 * playback layer beyond the Hilt binding.
 *
 * Contract:
 * - Returns `null` if the song is not downloaded, the metadata row is
 *   missing, or the recorded path is empty.
 * - Implementations MUST NOT perform file existence checks here; that is
 *   [LocalPlaybackFileResolver]'s job.
 * - Implementations MAY do light IO (single query) but must be safe to call
 *   on `Dispatchers.IO`.
 */
interface PlaybackDownloadLookup {
    /**
     * Resolve the absolute local file path for a downloaded song, or null
     * if not downloaded / file path unknown.
     */
    suspend fun localUriFor(songId: String): String?
}

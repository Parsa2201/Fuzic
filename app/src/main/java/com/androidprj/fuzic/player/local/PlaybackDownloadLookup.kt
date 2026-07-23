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
 *
 * ## Logout policy (spec §21 Q6, product call 2026-07-23)
 *
 * **Downloads are NOT purged on logout.** Returning to the app and
 * logging back in must show the same `downloads` rows and the same on-disk
 * audio files the user accumulated. The auth/logout flow MUST NOT call
 * [DownloadRepository.deleteDownload] / `purgeAll` / `DownloadWorker.cancel`
 * on user logout. The Playback Track tolerates a brief stale download
 * (file gone, row still present) by falling back to the catalog stream
 * via [LocalPlaybackFileResolver].
 *
 * Responsibility split:
 * - `PlaybackDownloadLookup` is the read side; it never deletes.
 * - Logout-flow contracts (in the Auth / Downloads Track) must call no
 *   method that purges downloads.
 * - User-initiated "Free up space" / "Delete download" actions on the
 *   Downloads screen are unrelated to logout and continue to work.
 */
interface PlaybackDownloadLookup {
    /**
     * Resolve the absolute local file path for a downloaded song, or null
     * if not downloaded / file path unknown.
     */
    suspend fun localUriFor(songId: String): String?
}


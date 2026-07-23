package com.androidprj.fuzic.player.local

import com.androidprj.fuzic.data.local.dao.DownloadDao
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room-backed [PlaybackDownloadLookup] that resolves a downloaded song id
 * to the absolute local file path recorded by the downloads worker.
 *
 * Wires the playback track to the downloads DAO on `backend-integration`.
 * The interface contract is preserved end-to-end:
 *  - Returns `null` if the row is missing or the recorded path is blank.
 *  - Performs no filesystem existence check; that is
 *    [LocalPlaybackFileResolver]'s responsibility (see spec §21 Q6,
 *    logout-no-purge decision).
 *
 * Replace the `@Binds` in [com.androidprj.fuzic.di.PlayerModule] from
 * `NoopPlaybackDownloadLookup` to this class when `backend-integration`
 * lands on master, then delete [NoopPlaybackDownloadLookup].
 *
 * The lookup runs a single suspending DAO query — safe to call on
 * `Dispatchers.IO`.
 */
@Singleton
class RoomPlaybackDownloadLookup @Inject constructor(
    private val downloadDao: DownloadDao,
) : PlaybackDownloadLookup {
    override suspend fun localUriFor(songId: String): String? =
        downloadDao.getDownload(songId)
            ?.localFilePath
            ?.takeIf { it.isNotBlank() }
}
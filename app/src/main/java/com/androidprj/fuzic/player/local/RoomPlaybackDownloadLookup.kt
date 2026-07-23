package com.androidprj.fuzic.player.local

import com.androidprj.fuzic.data.local.dao.DownloadDao
import com.androidprj.fuzic.data.local.entity.DownloadStatus
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room-backed [PlaybackDownloadLookup] that queries the downloads DAO for
 * a completed local file path.  The existence check is delegated to
 * [LocalPlaybackFileResolver] as per the contract.
 */
@Singleton
class RoomPlaybackDownloadLookup @Inject constructor(
    private val downloadDao: DownloadDao,
) : PlaybackDownloadLookup {

    override suspend fun localUriFor(songId: String): String? {
        val entity = downloadDao.getDownloadById(songId) ?: return null
        if (entity.status != DownloadStatus.COMPLETED) return null
        return entity.filePath?.ifBlank { null }
    }
}

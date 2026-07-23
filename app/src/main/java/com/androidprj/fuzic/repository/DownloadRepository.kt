package com.androidprj.fuzic.repository

import com.androidprj.fuzic.model.ui.DownloadRequest
import com.androidprj.fuzic.model.ui.DownloadSortOption
import com.androidprj.fuzic.model.ui.DownloadedSongItem
import kotlinx.coroutines.flow.Flow

/** Download mutations and summaries; long list UI must use [PagedDownloadRepository]. */
interface DownloadRepository {
    fun observeDownloads(sortOption: DownloadSortOption): Flow<List<DownloadedSongItem>>
    suspend fun enqueueDownload(request: DownloadRequest): Result<Unit>
    suspend fun deleteDownload(downloadId: String): Result<Unit>
    suspend fun restoreDownload(downloadId: String): Result<Unit>
    suspend fun removeDownloadFile(downloadId: String): Result<Unit>
    /** Returns the local file path if [songId] is fully downloaded, null otherwise. */
    suspend fun getLocalFilePath(songId: String): String?
}

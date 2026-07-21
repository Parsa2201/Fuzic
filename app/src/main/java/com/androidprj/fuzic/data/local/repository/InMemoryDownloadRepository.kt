package com.androidprj.fuzic.data.local.repository

import com.androidprj.fuzic.model.ui.DownloadRequest
import com.androidprj.fuzic.model.ui.DownloadSortOption
import com.androidprj.fuzic.model.ui.DownloadedSongItem
import com.androidprj.fuzic.repository.DownloadRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

@Singleton
class InMemoryDownloadRepository @Inject constructor() : DownloadRepository {
    private val downloads = MutableStateFlow(emptyList<DownloadedSongItem>())

    override fun observeDownloads(sortOption: DownloadSortOption): Flow<List<DownloadedSongItem>> {
        return downloads.map { items ->
            when (sortOption) {
                DownloadSortOption.DateDownloaded -> items
                DownloadSortOption.Title -> items.sortedBy { it.title }
                DownloadSortOption.Artist -> items.sortedBy { it.artist }
            }
        }
    }

    override suspend fun enqueueDownload(request: DownloadRequest): Result<Unit> = Result.success(Unit)

    override suspend fun deleteDownload(downloadId: String): Result<Unit> {
        downloads.value = downloads.value.filterNot { it.id == downloadId }
        return Result.success(Unit)
    }

    override suspend fun restoreDownload(downloadId: String): Result<Unit> = Result.success(Unit)

    override suspend fun removeDownloadFile(downloadId: String): Result<Unit> = Result.success(Unit)
}

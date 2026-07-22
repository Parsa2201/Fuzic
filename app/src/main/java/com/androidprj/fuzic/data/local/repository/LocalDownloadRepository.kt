package com.androidprj.fuzic.data.local.repository

import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.androidprj.fuzic.data.local.dao.DownloadDao
import com.androidprj.fuzic.model.ui.DownloadRequest
import com.androidprj.fuzic.model.ui.DownloadSortOption
import com.androidprj.fuzic.model.ui.DownloadedSongItem
import com.androidprj.fuzic.repository.DownloadRepository
import com.androidprj.fuzic.worker.DownloadWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import java.io.File

class LocalDownloadRepository @Inject constructor(
    private val downloadDao: DownloadDao,
    private val workManager: WorkManager
) : DownloadRepository {

    override fun observeDownloads(sortOption: DownloadSortOption): Flow<List<DownloadedSongItem>> {
        return downloadDao.observeAllDownloads().map { entities ->
            val items = entities.map { entity ->
                DownloadedSongItem(
                    id = entity.songId,
                    title = entity.title,
                    artist = entity.artistName,
                    artworkUrl = entity.coverImageUrl,
                    fileSizeLabel = entity.fileSizeLabel,
                    isDownloadInProgress = entity.isDownloadInProgress,
                    downloadedAtLabel = "Just now" // Should format timestamp
                )
            }
            
            when (sortOption) {
                DownloadSortOption.DateDownloaded -> items // Already sorted by downloadedAt DESC in DAO
                DownloadSortOption.Title -> items.sortedBy { it.title }
                DownloadSortOption.Artist -> items.sortedBy { it.artist }
            }
        }
    }

    override suspend fun enqueueDownload(request: DownloadRequest): Result<Unit> {
        return try {
            val data = Data.Builder()
                .putString("songId", request.song.id)
                .putString("title", request.song.title)
                .putString("artist", request.song.artist)
                .putString("coverUrl", request.song.artworkUrl)
                .putString("audioUrl", request.audioUrl)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
                .setInputData(data)
                .build()

            workManager.enqueue(workRequest)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteDownload(downloadId: String): Result<Unit> {
        return try {
            downloadDao.delete(downloadId)
            // also remove the file physically (in real app, we would resolve path)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun restoreDownload(downloadId: String): Result<Unit> {
        // Simple mock restore
        return Result.success(Unit)
    }

    override suspend fun removeDownloadFile(downloadId: String): Result<Unit> {
        // Physical deletion placeholder
        return Result.success(Unit)
    }
}

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

import com.androidprj.fuzic.repository.PremiumRepository
import kotlinx.coroutines.flow.first

class LocalDownloadRepository @Inject constructor(
    private val downloadDao: DownloadDao,
    private val workManager: WorkManager,
    private val premiumRepository: PremiumRepository
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
            val isPremium = premiumRepository.observePremiumStatus().first()
            if (!isPremium) {
                return Result.failure(Exception("Premium subscription is required to download songs."))
            }
            
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
            val entity = downloadDao.getDownload(downloadId)
            entity?.localFilePath?.let { File(it).delete() }
            downloadDao.delete(downloadId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun restoreDownload(downloadId: String): Result<Unit> {
        // Since we physically delete the file, restore would need to re-download. 
        // For now, this could just return success or failure.
        return Result.failure(Exception("Not implemented"))
    }

    override suspend fun removeDownloadFile(downloadId: String): Result<Unit> {
        return try {
            val entity = downloadDao.getDownload(downloadId)
            entity?.localFilePath?.let { File(it).delete() }
            // Remove file path from DB, but keep entity? The review says: "Delete the physical file before/with the Room entry".
            // Since deleteDownload does both, removeDownloadFile might do the same or just clear the path.
            // We'll clear the path and mark not downloaded.
            entity?.let {
                downloadDao.insert(it.copy(localFilePath = null, isDownloadInProgress = false, fileSizeLabel = "0 MB"))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

package com.androidprj.fuzic.data.repository

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.androidprj.fuzic.data.local.dao.DownloadDao
import com.androidprj.fuzic.data.local.entity.DownloadEntity
import com.androidprj.fuzic.data.local.entity.DownloadStatus
import com.androidprj.fuzic.data.worker.DownloadWorker
import com.androidprj.fuzic.model.ui.DownloadRequest
import com.androidprj.fuzic.model.ui.DownloadSortOption
import com.androidprj.fuzic.model.ui.DownloadedSongItem
import com.androidprj.fuzic.repository.DownloadRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomDownloadRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadDao: DownloadDao
) : DownloadRepository {

    private val workManager = WorkManager.getInstance(context)

    override fun observeDownloads(sortOption: DownloadSortOption): Flow<List<DownloadedSongItem>> {
        val flow = when (sortOption) {
            DownloadSortOption.DateDownloaded -> downloadDao.observeDownloadsByDate()
            DownloadSortOption.Title -> downloadDao.observeDownloadsByTitle()
            DownloadSortOption.Artist -> downloadDao.observeDownloadsByArtist()
        }

        return flow.map { entities ->
            entities.map { it.toDownloadedSongItem() }
        }
    }

    override suspend fun enqueueDownload(request: DownloadRequest): Result<Unit> = runCatching {
        val song = request.song
        val audioUrl = request.audioUrl.ifBlank { song.audioUrl.orEmpty() }
        require(audioUrl.isNotBlank()) { "Audio URL is empty — cannot download" }

        // 1. Insert into Room
        val entity = DownloadEntity(
            id = song.id,
            title = song.title,
            artist = song.artist,
            artworkUrl = song.artworkUrl,
            audioUrl = audioUrl,
            status = DownloadStatus.ENQUEUED
        )
        downloadDao.insertDownload(entity)

        // 2. Enqueue WorkManager
        val inputData = Data.Builder()
            .putString(DownloadWorker.KEY_DOWNLOAD_ID, song.id)
            .putString(DownloadWorker.KEY_AUDIO_URL, audioUrl)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(inputData)
            .build()

        workManager.enqueueUniqueWork(
            "download_${song.id}",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    override suspend fun deleteDownload(downloadId: String): Result<Unit> = runCatching {
        // Soft delete or just remove file, we might want to just cancel
        workManager.cancelUniqueWork("download_$downloadId")
        
        val entity = downloadDao.getDownloadById(downloadId)
        entity?.filePath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
        }
        
        downloadDao.deleteDownload(downloadId)
    }

    override suspend fun restoreDownload(downloadId: String): Result<Unit> = runCatching {
        // Swipe to dismiss uses deleteDownload, then undo calls restoreDownload.
        // It's tricky to restore a deleted file. Usually 'restoreDownload' for a file means we have to re-download.
        // Since we physically deleted it, we would have to enqueue it again.
        // We'd need the audioUrl. If we deleted the DB row, we lost the audioUrl!
        // To implement undo properly, we should probably soft delete or keep it in memory for the snackbar duration.
        // For now, if undo is clicked, it fails because we deleted the entity.
        // A better approach is handled in UI layer (holding the item), but if repo is called, we can't easily restore if we deleted the DB row.
        throw UnsupportedOperationException("Restore not fully supported after physical deletion")
    }

    override suspend fun getLocalFilePath(songId: String): String? {
        val entity = downloadDao.getDownloadById(songId) ?: return null
        if (entity.status != DownloadStatus.COMPLETED) return null
        val path = entity.filePath ?: return null
        return if (File(path).exists()) path else null
    }

    override suspend fun removeDownloadFile(downloadId: String): Result<Unit> = runCatching {
        workManager.cancelUniqueWork("download_$downloadId")
        val entity = downloadDao.getDownloadById(downloadId)
        entity?.filePath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
        }
        downloadDao.deleteDownload(downloadId)
    }

    private fun DownloadEntity.toDownloadedSongItem(): DownloadedSongItem {
        val sizeMB = if (totalBytes > 0) String.format("%.1f MB", totalBytes / (1024f * 1024f)) else ""
        val downloadedMB = if (downloadedBytes > 0) String.format("%.1f MB", downloadedBytes / (1024f * 1024f)) else ""
        
        val sizeLabel = if (status == DownloadStatus.COMPLETED) {
            sizeMB
        } else if (status == DownloadStatus.DOWNLOADING) {
            "$downloadedMB / $sizeMB"
        } else {
            status.name
        }

        return DownloadedSongItem(
            id = id,
            title = title,
            artist = artist,
            fileSizeLabel = sizeLabel,
            downloadedAtLabel = "", // Format date properly if needed
            artworkUrl = artworkUrl,
            isDownloadInProgress = status == DownloadStatus.DOWNLOADING || status == DownloadStatus.ENQUEUED,
            progress = progress,
            localFilePath = if (status == DownloadStatus.COMPLETED) filePath else null
        )
    }
}

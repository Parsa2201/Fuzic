package com.androidprj.fuzic.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.androidprj.fuzic.data.local.dao.DownloadDao
import com.androidprj.fuzic.data.local.entity.DownloadedSongEntity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val downloadDao: DownloadDao,
    private val supabaseClient: SupabaseClient
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val songId = inputData.getString("songId") ?: return@withContext Result.failure()
        val title = inputData.getString("title") ?: "Unknown"
        val artist = inputData.getString("artist") ?: "Unknown"
        val coverUrl = inputData.getString("coverUrl")
        val audioUrl = inputData.getString("audioUrl") ?: return@withContext Result.failure()

        try {
            // Update state to in-progress
            val entity = DownloadedSongEntity(
                songId = songId,
                title = title,
                artistName = artist,
                coverImageUrl = coverUrl,
                localFilePath = null,
                isDownloadInProgress = true,
                downloadedAtEpochMillis = System.currentTimeMillis(),
                fileSizeLabel = "0 MB"
            )
            downloadDao.insert(entity)

            // Perform real download
            val localFile = File(context.filesDir, "downloads/$songId.mp3")
            localFile.parentFile?.mkdirs()
            
            var totalBytes = 0L
            if (!localFile.exists()) {
                localFile.createNewFile()
                java.net.URL(audioUrl).openStream().use { input ->
                    FileOutputStream(localFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalBytes += bytesRead
                        }
                    }
                }
            } else {
                totalBytes = localFile.length()
            }
            
            // Format file size
            val fileSizeLabel = if (totalBytes > 0) {
                String.format("%.1f MB", totalBytes / (1024.0 * 1024.0))
            } else {
                "0 MB"
            }
            
            // Update state to completed
            val updatedEntity = entity.copy(
                isDownloadInProgress = false,
                localFilePath = localFile.absolutePath,
                fileSizeLabel = fileSizeLabel
            )
            downloadDao.insert(updatedEntity)
            
            Result.success()
        } catch (e: Exception) {
            downloadDao.delete(songId)
            Result.failure()
        }
    }
}

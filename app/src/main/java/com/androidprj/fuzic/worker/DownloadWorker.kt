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

            // Simulate actual downloading since we don't have real audio URLs yet
            // Normally: download from audioUrl, save to Context.filesDir
            // Since this is integration, we can write a dummy file or fetch from supabase storage
            val localFile = File(context.filesDir, "downloads/$songId.mp3")
            localFile.parentFile?.mkdirs()
            
            // if audioUrl is a supabase storage path, we can download it. For now just create a stub.
            if (!localFile.exists()) {
                localFile.createNewFile()
                FileOutputStream(localFile).use { it.write("stub_audio_data".toByteArray()) }
            }
            
            // Update state to completed
            val updatedEntity = entity.copy(
                isDownloadInProgress = false,
                localFilePath = localFile.absolutePath,
                fileSizeLabel = "3.2 MB" // Mock size
            )
            downloadDao.insert(updatedEntity)
            
            Result.success()
        } catch (e: Exception) {
            downloadDao.delete(songId)
            Result.failure()
        }
    }
}

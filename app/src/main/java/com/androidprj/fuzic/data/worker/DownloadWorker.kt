package com.androidprj.fuzic.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.androidprj.fuzic.data.local.dao.DownloadDao
import com.androidprj.fuzic.data.local.entity.DownloadStatus
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val downloadDao: DownloadDao
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val downloadId = inputData.getString(KEY_DOWNLOAD_ID) ?: return@withContext Result.failure()
        val audioUrl = inputData.getString(KEY_AUDIO_URL) ?: return@withContext Result.failure()

        downloadDao.updateStatus(downloadId, DownloadStatus.DOWNLOADING.name)

        val outputFile = File(context.filesDir, "downloads/$downloadId.mp3")
        if (!outputFile.parentFile!!.exists()) {
            outputFile.parentFile!!.mkdirs()
        }

        try {
            val url = URL(audioUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                downloadDao.updateStatus(downloadId, DownloadStatus.FAILED.name)
                return@withContext Result.failure()
            }

            val fileLength = connection.contentLength.toLong()
            var downloadedBytes = 0L

            connection.inputStream.use { input ->
                FileOutputStream(outputFile).use { output ->
                    val data = ByteArray(8192)
                    var count: Int
                    var lastProgressUpdate = System.currentTimeMillis()

                    while (input.read(data).also { count = it } != -1) {
                        if (isStopped) {
                            outputFile.delete()
                            downloadDao.updateStatus(downloadId, DownloadStatus.CANCELED.name)
                            return@withContext Result.failure()
                        }
                        
                        downloadedBytes += count
                        output.write(data, 0, count)

                        val now = System.currentTimeMillis()
                        if (now - lastProgressUpdate > 500 || downloadedBytes == fileLength) {
                            val progress = if (fileLength > 0) downloadedBytes.toFloat() / fileLength else 0f
                            downloadDao.updateProgress(downloadId, progress, downloadedBytes, fileLength, DownloadStatus.DOWNLOADING.name)
                            lastProgressUpdate = now
                        }
                        
                        if (fileLength > 0 && downloadedBytes >= fileLength) {
                            break
                        }
                    }
                }
            }

            downloadDao.markCompleted(downloadId, outputFile.absolutePath, DownloadStatus.COMPLETED.name)
            Result.success()

        } catch (e: Exception) {
            e.printStackTrace()
            outputFile.delete()
            try {
                downloadDao.updateStatus(downloadId, DownloadStatus.FAILED.name)
            } catch (e2: Exception) {}
            Result.failure()
        }
    }

    companion object {
        const val KEY_DOWNLOAD_ID = "DOWNLOAD_ID"
        const val KEY_AUDIO_URL = "AUDIO_URL"
    }
}

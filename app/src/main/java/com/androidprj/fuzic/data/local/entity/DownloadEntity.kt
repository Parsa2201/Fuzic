package com.androidprj.fuzic.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val artworkUrl: String?,
    val audioUrl: String,
    val downloadedAt: Long = System.currentTimeMillis(),
    val filePath: String? = null,
    val status: DownloadStatus = DownloadStatus.ENQUEUED,
    val progress: Float = 0f,
    val totalBytes: Long = 0L,
    val downloadedBytes: Long = 0L
)

enum class DownloadStatus {
    ENQUEUED,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    CANCELED
}

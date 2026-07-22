package com.androidprj.fuzic.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadedSongEntity(
    @PrimaryKey val songId: String,
    val title: String,
    val artistName: String,
    val coverImageUrl: String?,
    val localFilePath: String?,
    val isDownloadInProgress: Boolean,
    val downloadedAtEpochMillis: Long,
    val fileSizeLabel: String
)

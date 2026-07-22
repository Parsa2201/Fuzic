package com.androidprj.fuzic.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_playlists")
data class LocalPlaylistEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String?,
    val coverImageUrl: String?,
    val isSynced: Boolean = true
)

package com.androidprj.fuzic.data.local.entity

import androidx.room.Entity

@Entity(
    tableName = "local_playlist_songs",
    primaryKeys = ["playlistId", "songId"]
)
data class LocalPlaylistSongEntity(
    val playlistId: String,
    val songId: String,
    val addedAtEpochMillis: Long
)

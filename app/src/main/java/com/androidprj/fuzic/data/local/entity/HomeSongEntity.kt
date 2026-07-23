package com.androidprj.fuzic.data.local.entity

import androidx.room.Entity

@Entity(tableName = "home_songs", primaryKeys = ["id", "category"])
data class HomeSongEntity(
    val id: String,
    val category: String, // e.g., DAILY_PICKS, TRENDING, NEW_RELEASES
    val title: String,
    val artist: String,
    val artworkUrl: String?,
    val audioUrl: String?,
    val album: String?,
    val durationLabel: String?,
    val isExplicit: Boolean,
    val orderIndex: Int
)

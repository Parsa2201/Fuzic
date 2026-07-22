package com.androidprj.fuzic.repository

import com.androidprj.fuzic.model.ui.SongItem

interface InteractionRepository {
    suspend fun getRecentlyPlayed(userId: String, offset: Long = 0, limit: Long = 20): Result<List<SongItem>>
    suspend fun getLikedSongs(userId: String, offset: Long = 0, limit: Long = 20): Result<List<SongItem>>
    
    suspend fun recordPlay(songId: String): Result<Unit>
    suspend fun likeSong(songId: String): Result<Unit>
    suspend fun unlikeSong(songId: String): Result<Unit>
    suspend fun removeRecentlyPlayed(songId: String): Result<Unit>
}

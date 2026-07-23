package com.androidprj.fuzic.repository

import com.androidprj.fuzic.model.ui.SongItem

import kotlinx.coroutines.flow.Flow
import androidx.paging.PagingData

interface InteractionRepository {
    fun observeRecentlyPlayed(userId: String): Flow<PagingData<SongItem>>
    fun observeLikedSongs(userId: String): Flow<PagingData<SongItem>>
    
    suspend fun recordPlay(songId: String): Result<Unit>
    suspend fun likeSong(songId: String): Result<Unit>
    suspend fun unlikeSong(songId: String): Result<Unit>
    suspend fun removeRecentlyPlayed(songId: String): Result<Unit>
}

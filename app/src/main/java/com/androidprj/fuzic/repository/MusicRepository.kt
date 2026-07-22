package com.androidprj.fuzic.repository

import com.androidprj.fuzic.model.ui.SongItem

import kotlinx.coroutines.flow.Flow
import androidx.paging.PagingData

interface MusicRepository {
    fun observeDailyPicks(): Flow<PagingData<SongItem>>
    fun observeTrendingSongs(): Flow<PagingData<SongItem>>
    fun observeNewReleases(): Flow<PagingData<SongItem>>
    fun observeMostPopular(): Flow<PagingData<SongItem>>
    suspend fun getSongById(songId: String): Result<SongItem>
    fun searchSongs(query: String): Flow<PagingData<SongItem>>
}

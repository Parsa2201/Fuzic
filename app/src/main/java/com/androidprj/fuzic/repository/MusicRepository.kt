package com.androidprj.fuzic.repository

import com.androidprj.fuzic.model.ui.SongItem

interface MusicRepository {
    suspend fun getDailyPicks(offset: Long = 0, limit: Long = 20): Result<List<SongItem>>
    suspend fun getTrendingSongs(offset: Long = 0, limit: Long = 20): Result<List<SongItem>>
    suspend fun getNewReleases(offset: Long = 0, limit: Long = 20): Result<List<SongItem>>
    suspend fun getMostPopular(offset: Long = 0, limit: Long = 20): Result<List<SongItem>>
    suspend fun getSongById(songId: String): Result<SongItem>
    suspend fun searchSongs(query: String, offset: Long = 0, limit: Long = 20): Result<List<SongItem>>
}

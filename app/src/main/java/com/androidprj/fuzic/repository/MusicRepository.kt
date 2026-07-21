package com.androidprj.fuzic.repository

import com.androidprj.fuzic.model.Song

interface MusicRepository {
    suspend fun getDailyPicks(offset: Long = 0, limit: Long = 20): Result<List<Song>>
    suspend fun getTrendingSongs(offset: Long = 0, limit: Long = 20): Result<List<Song>>
    suspend fun getNewReleases(offset: Long = 0, limit: Long = 20): Result<List<Song>>
    suspend fun getMostPopular(offset: Long = 0, limit: Long = 20): Result<List<Song>>
    suspend fun getSongById(songId: String): Result<Song>
    suspend fun searchSongs(query: String, offset: Long = 0, limit: Long = 20): Result<List<Song>>
}

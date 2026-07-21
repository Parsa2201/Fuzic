package com.androidprj.fuzic.repository

import com.androidprj.fuzic.model.Song

interface MusicRepository {
    suspend fun getDailyPicks(): Result<List<Song>>
    suspend fun getTrendingSongs(): Result<List<Song>>
    suspend fun getNewReleases(): Result<List<Song>>
    suspend fun getMostPopular(): Result<List<Song>>
    suspend fun getSongById(songId: String): Result<Song>
    suspend fun searchSongs(query: String): Result<List<Song>>
}

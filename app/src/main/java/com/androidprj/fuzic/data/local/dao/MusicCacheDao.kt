package com.androidprj.fuzic.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.androidprj.fuzic.data.local.entity.LikedSongEntity
import com.androidprj.fuzic.data.local.entity.RecentlyPlayedEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MusicCacheDao {
    @Query("SELECT * FROM liked_songs ORDER BY likedAtEpochMillis DESC")
    fun observeLikedSongs(): Flow<List<LikedSongEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLikedSong(entity: LikedSongEntity)

    @Query("DELETE FROM liked_songs WHERE songId = :songId")
    suspend fun removeLikedSong(songId: String)

    @Query("SELECT * FROM recently_played ORDER BY playedAtEpochMillis DESC LIMIT 50")
    fun observeRecentlyPlayed(): Flow<List<RecentlyPlayedEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentlyPlayed(entity: RecentlyPlayedEntity)
}

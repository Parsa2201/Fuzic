package com.androidprj.fuzic.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.androidprj.fuzic.data.local.entity.HomeSongEntity

@Dao
interface HomeSongDao {

    @Query("SELECT * FROM home_songs WHERE category = :category ORDER BY orderIndex ASC")
    suspend fun getSongsByCategory(category: String): List<HomeSongEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<HomeSongEntity>)

    @Query("DELETE FROM home_songs WHERE category = :category")
    suspend fun clearCategory(category: String)

    @Transaction
    suspend fun replaceSongs(category: String, songs: List<HomeSongEntity>) {
        clearCategory(category)
        insertSongs(songs)
    }
}

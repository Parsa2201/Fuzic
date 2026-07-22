package com.androidprj.fuzic.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.androidprj.fuzic.data.local.entity.DownloadedSongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY downloadedAtEpochMillis DESC")
    fun observeAllDownloads(): Flow<List<DownloadedSongEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DownloadedSongEntity)

    @Query("DELETE FROM downloads WHERE songId = :songId")
    suspend fun delete(songId: String)

    @Query("UPDATE downloads SET isDownloadInProgress = :inProgress WHERE songId = :songId")
    suspend fun updateProgressState(songId: String, inProgress: Boolean)
}

package com.androidprj.fuzic.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.androidprj.fuzic.data.local.entity.DownloadEntity
import com.androidprj.fuzic.data.local.entity.DownloadStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY downloadedAt DESC")
    fun observeDownloadsByDate(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads ORDER BY title ASC")
    fun observeDownloadsByTitle(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads ORDER BY artist ASC")
    fun observeDownloadsByArtist(): Flow<List<DownloadEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadEntity)

    @Query("UPDATE downloads SET progress = :progress, downloadedBytes = :downloadedBytes, totalBytes = :totalBytes, status = :statusStr WHERE id = :id")
    suspend fun updateProgress(id: String, progress: Float, downloadedBytes: Long, totalBytes: Long, statusStr: String)

    @Query("UPDATE downloads SET filePath = :filePath, status = :statusStr, progress = 1.0 WHERE id = :id")
    suspend fun markCompleted(id: String, filePath: String, statusStr: String)

    @Query("UPDATE downloads SET status = :statusStr WHERE id = :id")
    suspend fun updateStatus(id: String, statusStr: String)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteDownload(id: String)
    
    @Query("SELECT * FROM downloads WHERE id = :id LIMIT 1")
    suspend fun getDownloadById(id: String): DownloadEntity?
}

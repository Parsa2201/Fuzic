package com.androidprj.fuzic.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.androidprj.fuzic.data.local.entity.LocalPlaylistEntity
import com.androidprj.fuzic.data.local.entity.LocalPlaylistSongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalPlaylistDao {
    @Query("SELECT * FROM local_playlists")
    fun observeAllPlaylists(): Flow<List<LocalPlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: LocalPlaylistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongToPlaylist(playlistSong: LocalPlaylistSongEntity)

    @Query("DELETE FROM local_playlists WHERE id = :id")
    suspend fun deletePlaylist(id: String)

    @Query("DELETE FROM local_playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: String, songId: String)
}

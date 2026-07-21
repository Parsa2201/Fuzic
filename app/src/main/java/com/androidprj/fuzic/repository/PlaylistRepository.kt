package com.androidprj.fuzic.repository

import com.androidprj.fuzic.model.ui.PlaylistItem
import com.androidprj.fuzic.model.ui.SongItem

interface PlaylistRepository {
    suspend fun getGlobalPlaylists(offset: Long = 0, limit: Long = 20): Result<List<PlaylistItem>>
    suspend fun getLocalPlaylists(offset: Long = 0, limit: Long = 20): Result<List<PlaylistItem>>
    suspend fun getUserPlaylists(userId: String, offset: Long = 0, limit: Long = 20): Result<List<PlaylistItem>>
    suspend fun getPlaylistSongs(playlistId: String, offset: Long = 0, limit: Long = 20): Result<List<SongItem>>
    
    suspend fun createPlaylist(title: String, type: String?, isPublic: Boolean): Result<PlaylistItem>
    suspend fun deletePlaylist(playlistId: String): Result<Unit>
    
    suspend fun addSongToPlaylist(playlistId: String, songId: String): Result<Unit>
    suspend fun removeSongFromPlaylist(playlistId: String, songId: String): Result<Unit>
}

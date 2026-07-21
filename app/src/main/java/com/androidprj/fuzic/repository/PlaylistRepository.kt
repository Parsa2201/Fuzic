package com.androidprj.fuzic.repository

import com.androidprj.fuzic.model.Playlist
import com.androidprj.fuzic.model.Song

interface PlaylistRepository {
    suspend fun getGlobalPlaylists(): Result<List<Playlist>>
    suspend fun getLocalPlaylists(): Result<List<Playlist>>
    suspend fun getUserPlaylists(userId: String): Result<List<Playlist>>
    suspend fun getPlaylistSongs(playlistId: String): Result<List<Song>>
    
    suspend fun createPlaylist(title: String, type: String?, isPublic: Boolean): Result<Playlist>
    suspend fun deletePlaylist(playlistId: String): Result<Unit>
    
    suspend fun addSongToPlaylist(playlistId: String, songId: String): Result<Unit>
    suspend fun removeSongFromPlaylist(playlistId: String, songId: String): Result<Unit>
}

package com.androidprj.fuzic.repository

import com.androidprj.fuzic.model.ui.CreatePlaylistRequest
import com.androidprj.fuzic.model.ui.UpdatePlaylistRequest
import com.androidprj.fuzic.model.ui.PlaylistItem
import com.androidprj.fuzic.model.ui.SongItem

/**
 * Playlist data needed by non-paged screen summaries and direct playlist mutations.
 * Long scrolling surfaces should use PagedPlaylistRepository.
 */
interface PlaylistRepository {
    suspend fun getGlobalPlaylists(offset: Long = 0, limit: Long = 20): Result<List<PlaylistItem>>
    suspend fun getLocalPlaylists(offset: Long = 0, limit: Long = 20): Result<List<PlaylistItem>>
    suspend fun getUserPlaylists(userId: String, offset: Long = 0, limit: Long = 20): Result<List<PlaylistItem>>
    suspend fun getPlaylistSongs(playlistId: String, offset: Long = 0, limit: Long = 20): Result<List<SongItem>>
    
    suspend fun createPlaylist(request: CreatePlaylistRequest): Result<PlaylistItem>
    suspend fun updatePlaylist(playlistId: String, request: UpdatePlaylistRequest): Result<PlaylistItem>
    suspend fun deletePlaylist(playlistId: String): Result<Unit>
    
    suspend fun addSongToPlaylist(playlistId: String, songId: String): Result<Unit>
    suspend fun removeSongFromPlaylist(playlistId: String, songId: String): Result<Unit>
}

package com.androidprj.fuzic.repository

import com.androidprj.fuzic.model.ui.CreatePlaylistRequest
import com.androidprj.fuzic.model.ui.PlaylistItem
import com.androidprj.fuzic.model.ui.SongItem
import kotlinx.coroutines.flow.Flow
import androidx.paging.PagingData

/**
 * Playlist data needed by non-paged screen summaries and direct playlist mutations.
 * Long scrolling surfaces should use PagedPlaylistRepository.
 */
interface PlaylistRepository {
    fun observeGlobalPlaylists(): Flow<PagingData<PlaylistItem>>
    fun observeLocalPlaylists(): Flow<PagingData<PlaylistItem>>
    fun observeUserPlaylists(userId: String): Flow<PagingData<PlaylistItem>>
    fun observePlaylistSongs(playlistId: String): Flow<PagingData<SongItem>>
    
    suspend fun createPlaylist(request: CreatePlaylistRequest): Result<PlaylistItem>
    suspend fun deletePlaylist(playlistId: String): Result<Unit>
    
    suspend fun addSongToPlaylist(playlistId: String, songId: String): Result<Unit>
    suspend fun removeSongFromPlaylist(playlistId: String, songId: String): Result<Unit>
}

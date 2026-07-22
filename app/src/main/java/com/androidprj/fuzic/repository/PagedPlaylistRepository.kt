package com.androidprj.fuzic.repository

import androidx.paging.PagingData
import com.androidprj.fuzic.model.ui.PlaylistItem
import com.androidprj.fuzic.model.ui.SongItem
import kotlinx.coroutines.flow.Flow

/** Paging contract for long playlist surfaces; UI must not request offset/limit pages directly. */
interface PagedPlaylistRepository {
    fun observeGlobalPlaylists(): Flow<PagingData<PlaylistItem>>
    fun observeLocalPlaylists(): Flow<PagingData<PlaylistItem>>
    fun observeUserPlaylists(userId: String): Flow<PagingData<PlaylistItem>>
    fun observePlaylistSongs(playlistId: String): Flow<PagingData<SongItem>>
}

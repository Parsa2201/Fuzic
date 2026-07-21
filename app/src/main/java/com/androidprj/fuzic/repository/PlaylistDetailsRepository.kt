package com.androidprj.fuzic.repository

import com.androidprj.fuzic.model.ui.PlaylistDetails

interface PlaylistDetailsRepository {
    suspend fun getPlaylistDetails(playlistId: String): Result<PlaylistDetails>
}

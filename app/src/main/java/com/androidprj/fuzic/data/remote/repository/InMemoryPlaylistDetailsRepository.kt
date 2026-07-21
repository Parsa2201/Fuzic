package com.androidprj.fuzic.data.remote.repository

import com.androidprj.fuzic.model.ui.PlaylistDetails
import com.androidprj.fuzic.repository.PlaylistDetailsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InMemoryPlaylistDetailsRepository @Inject constructor() : PlaylistDetailsRepository {
    override suspend fun getPlaylistDetails(playlistId: String): Result<PlaylistDetails> {
        return Result.failure(IllegalStateException("Playlist details are not implemented yet"))
    }
}

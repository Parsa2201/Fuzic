package com.androidprj.fuzic.data.remote.repository

import com.androidprj.fuzic.model.remote.PlaylistDto
import com.androidprj.fuzic.model.remote.PlaylistDtoSong
import com.androidprj.fuzic.model.remote.SongDto
import com.androidprj.fuzic.model.ui.CreatePlaylistRequest
import com.androidprj.fuzic.model.ui.PlaylistItem
import com.androidprj.fuzic.model.ui.PlaylistVisibility
import com.androidprj.fuzic.model.ui.SongItem
import com.androidprj.fuzic.model.mapper.toPlaylistItem
import com.androidprj.fuzic.model.mapper.toSongItem
import com.androidprj.fuzic.repository.PlaylistRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData

class RemotePlaylistRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) : PlaylistRepository {

    override fun observeGlobalPlaylists(): Flow<PagingData<PlaylistItem>> {
        return Pager(PagingConfig(pageSize = 20)) {
            com.androidprj.fuzic.data.remote.paging.GenericSupabasePagingSource { offset, limit ->
                supabaseClient.postgrest["playlists"]
                    .select { 
                        filter { eq("is_public", true) } 
                        range(offset, offset + limit - 1)
                    }
                    .decodeList<PlaylistDto>()
                    .map { it.toPlaylistItem() }
            }
        }.flow
    }

    override fun observeLocalPlaylists(): Flow<PagingData<PlaylistItem>> {
        return Pager(PagingConfig(pageSize = 20)) {
            com.androidprj.fuzic.data.remote.paging.GenericSupabasePagingSource { _, _ -> emptyList() }
        }.flow
    }

    override fun observeUserPlaylists(userId: String): Flow<PagingData<PlaylistItem>> {
        return Pager(PagingConfig(pageSize = 20)) {
            com.androidprj.fuzic.data.remote.paging.GenericSupabasePagingSource { offset, limit ->
                supabaseClient.postgrest["playlists"]
                    .select { 
                        filter { eq("owner_id", userId) }
                        range(offset, offset + limit - 1)
                    }
                    .decodeList<PlaylistDto>()
                    .map { it.toPlaylistItem() }
            }
        }.flow
    }

    override fun observePlaylistSongs(playlistId: String): Flow<PagingData<SongItem>> {
        return Pager(PagingConfig(pageSize = 20)) {
            com.androidprj.fuzic.data.remote.paging.GenericSupabasePagingSource { offset, limit ->
                supabaseClient.postgrest["playlist_songs"]
                    .select(columns = io.github.jan.supabase.postgrest.query.Columns.raw("song_id, songs(*)")) {
                        filter { eq("playlist_id", playlistId) }
                        range(offset, offset + limit - 1)
                    }
                    .decodeList<SongWrapper>()
                    .map { it.song.toSongItem() }
            }
        }.flow
    }

    override suspend fun createPlaylist(request: CreatePlaylistRequest): Result<PlaylistItem> {
        return try {
            val userId = supabaseClient.auth.currentUserOrNull()?.id ?: throw Exception("Not logged in")
            val newPlaylist = InsertPlaylistDto(
                title = request.title,
                ownerId = userId,
                type = USER_PLAYLIST_TYPE,
                isPublic = request.visibility == PlaylistVisibility.Public,
                coverImageUrl = request.coverImageUrl
            )
            val result = supabaseClient.postgrest["playlists"]
                .insert(newPlaylist) { select() }
                .decodeSingle<PlaylistDto>()
            Result.success(result.toPlaylistItem())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deletePlaylist(playlistId: String): Result<Unit> {
        return try {
            supabaseClient.postgrest["playlists"]
                .delete { filter { eq("id", playlistId) } }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addSongToPlaylist(playlistId: String, songId: String): Result<Unit> {
        return try {
            val ps = PlaylistDtoSong(playlistId, songId)
            supabaseClient.postgrest["playlist_songs"].upsert(ps)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeSongFromPlaylist(playlistId: String, songId: String): Result<Unit> {
        return try {
            supabaseClient.postgrest["playlist_songs"].delete {
                filter {
                    eq("playlist_id", playlistId)
                    eq("song_id", songId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @kotlinx.serialization.Serializable
    private data class SongWrapper(
        @kotlinx.serialization.SerialName("songs") val song: SongDto
    )
    
    @kotlinx.serialization.Serializable
    private data class InsertPlaylistDto(
        val title: String,
        @kotlinx.serialization.SerialName("owner_id") val ownerId: String,
        val type: String?,
        @kotlinx.serialization.SerialName("is_public") val isPublic: Boolean,
        @kotlinx.serialization.SerialName("cover_image_url") val coverImageUrl: String? = null
    )

    private companion object {
        const val USER_PLAYLIST_TYPE = "user"
    }
}

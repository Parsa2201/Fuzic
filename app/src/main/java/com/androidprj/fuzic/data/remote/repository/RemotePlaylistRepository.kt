package com.androidprj.fuzic.data.remote.repository

import com.androidprj.fuzic.model.remote.PlaylistDto
import com.androidprj.fuzic.model.remote.PlaylistDtoSong
import com.androidprj.fuzic.model.remote.SongDto
import com.androidprj.fuzic.model.ui.PlaylistItem
import com.androidprj.fuzic.model.ui.SongItem
import com.androidprj.fuzic.model.mapper.toPlaylistItem
import com.androidprj.fuzic.model.mapper.toSongItem
import com.androidprj.fuzic.repository.PlaylistRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject
import java.util.UUID

class RemotePlaylistRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) : PlaylistRepository {

    override suspend fun getGlobalPlaylists(offset: Long, limit: Long): Result<List<PlaylistItem>> {
        return try {
            val playlists = supabaseClient.postgrest["playlists"]
                .select { 
                    filter { eq("is_public", true) } 
                    range(offset, offset + limit - 1)
                }
                .decodeList<PlaylistDto>()
                .map { it.toPlaylistItem() }
            Result.success(playlists)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getLocalPlaylists(offset: Long, limit: Long): Result<List<PlaylistItem>> {
        return Result.success(emptyList()) // Local Room cache to be implemented
    }

    override suspend fun getUserPlaylists(userId: String, offset: Long, limit: Long): Result<List<PlaylistItem>> {
        return try {
            val playlists = supabaseClient.postgrest["playlists"]
                .select { 
                    filter { eq("owner_id", userId) }
                    range(offset, offset + limit - 1)
                }
                .decodeList<PlaylistDto>()
                .map { it.toPlaylistItem() }
            Result.success(playlists)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPlaylistSongs(playlistId: String, offset: Long, limit: Long): Result<List<SongItem>> {
        return try {
            // Note: In Supabase, you can use inner joins. 
            // For simplicity with the standard SDK, we fetch pivot table then songs, or use select("..., songs(*)")
            val songs = supabaseClient.postgrest["playlist_songs"]
                .select(columns = io.github.jan.supabase.postgrest.query.Columns.raw("song_id, songs(*)")) {
                    filter { eq("playlist_id", playlistId) }
                    range(offset, offset + limit - 1)
                }
                .decodeList<SongWrapper>()
                .map { it.song.toSongItem() }
            Result.success(songs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createPlaylist(title: String, type: String?, isPublic: Boolean): Result<PlaylistItem> {
        return try {
            val userId = supabaseClient.auth.currentUserOrNull()?.id ?: throw Exception("Not logged in")
            val newPlaylist = InsertPlaylistDto(
                title = title,
                ownerId = userId,
                type = type,
                isPublic = isPublic
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
        @kotlinx.serialization.SerialName("is_public") val isPublic: Boolean
    )
}

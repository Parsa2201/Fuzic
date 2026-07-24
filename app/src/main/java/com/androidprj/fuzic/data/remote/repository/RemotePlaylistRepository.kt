package com.androidprj.fuzic.data.remote.repository

import com.androidprj.fuzic.model.remote.PlaylistDto
import com.androidprj.fuzic.model.remote.PlaylistDtoSong
import com.androidprj.fuzic.model.remote.SongDto
import com.androidprj.fuzic.model.ui.CreatePlaylistRequest
import com.androidprj.fuzic.model.ui.UpdatePlaylistRequest
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
import java.util.UUID

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.storage.storage

class RemotePlaylistRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
    @ApplicationContext private val appContext: Context
) : PlaylistRepository {

    override suspend fun getGlobalPlaylists(offset: Long, limit: Long): Result<List<PlaylistItem>> {
        return try {
            val playlists = supabaseClient.postgrest["playlists"]
                .select(columns = io.github.jan.supabase.postgrest.query.Columns.raw("*, playlist_songs(count)")) { 
                    filter { eq("type", "global") } 
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
        return try {
            val playlists = supabaseClient.postgrest["playlists"]
                .select(columns = io.github.jan.supabase.postgrest.query.Columns.raw("*, playlist_songs(count)")) { 
                    filter { eq("type", "local") }
                    range(offset, offset + limit - 1)
                }
                .decodeList<PlaylistDto>()
                .map { it.toPlaylistItem() }
            Result.success(playlists)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUserPlaylists(userId: String, offset: Long, limit: Long): Result<List<PlaylistItem>> {
        return try {
            val playlists = supabaseClient.postgrest["playlists"]
                .select(columns = io.github.jan.supabase.postgrest.query.Columns.raw("*, playlist_songs(count)")) { 
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

    override suspend fun createPlaylist(request: CreatePlaylistRequest): Result<PlaylistItem> {
        return try {
            val userId = supabaseClient.auth.currentUserOrNull()?.id ?: throw Exception("Not logged in")
            
            var finalCoverUrl = request.coverImageUrl
            if (finalCoverUrl != null && finalCoverUrl.startsWith("content://")) {
                val imageBytes = appContext.contentResolver.openInputStream(android.net.Uri.parse(finalCoverUrl))
                    ?.use { inputStream ->
                        val original = android.graphics.BitmapFactory.decodeStream(inputStream)
                        val ratio = 500f / maxOf(original.width, original.height)
                        val scaled = if (ratio < 1f) {
                            android.graphics.Bitmap.createScaledBitmap(
                                original, 
                                (original.width * ratio).toInt(), 
                                (original.height * ratio).toInt(), 
                                true
                            )
                        } else original
                        
                        val out = java.io.ByteArrayOutputStream()
                        scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, out)
                        out.toByteArray()
                    } ?: throw Exception("Failed to process selected cover image")
                val path = "$userId/${java.util.UUID.randomUUID()}.jpg"
                val bucket = supabaseClient.storage.from("covers")
                bucket.upload(path, imageBytes)
                finalCoverUrl = bucket.publicUrl(path)
            }
            
            val isGlobal = request.category == com.androidprj.fuzic.model.ui.PlaylistCategory.Global
            val isPublic = if (isGlobal) true else request.visibility == com.androidprj.fuzic.model.ui.PlaylistVisibility.Public

            val newPlaylist = InsertPlaylistDto(
                title = request.title,
                ownerId = userId,
                type = if (request.category == com.androidprj.fuzic.model.ui.PlaylistCategory.None) null else request.category.name.lowercase(),
                isPublic = isPublic,
                coverImageUrl = finalCoverUrl
            )
            val result = supabaseClient.postgrest["playlists"]
                .insert(newPlaylist) { select() }
                .decodeSingle<PlaylistDto>()
            Result.success(result.toPlaylistItem())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updatePlaylist(playlistId: String, request: UpdatePlaylistRequest): Result<PlaylistItem> {
        return try {
            val userId = supabaseClient.auth.currentUserOrNull()?.id ?: throw Exception("Not logged in")
            
            var finalCoverUrl = request.coverImageUrl
            if (finalCoverUrl != null && finalCoverUrl.startsWith("content://")) {
                val imageBytes = appContext.contentResolver.openInputStream(android.net.Uri.parse(finalCoverUrl))
                    ?.use { inputStream ->
                        val original = android.graphics.BitmapFactory.decodeStream(inputStream)
                        val ratio = 500f / maxOf(original.width, original.height)
                        val scaled = if (ratio < 1f) {
                            android.graphics.Bitmap.createScaledBitmap(
                                original, 
                                (original.width * ratio).toInt(), 
                                (original.height * ratio).toInt(), 
                                true
                            )
                        } else original
                        
                        val out = java.io.ByteArrayOutputStream()
                        scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, out)
                        out.toByteArray()
                    } ?: throw Exception("Failed to process selected cover image")
                val path = "$userId/${java.util.UUID.randomUUID()}.jpg"
                val bucket = supabaseClient.storage.from("covers")
                bucket.upload(path, imageBytes)
                finalCoverUrl = bucket.publicUrl(path)
            }
            
            val isGlobal = request.category == com.androidprj.fuzic.model.ui.PlaylistCategory.Global
            val isPublic = if (isGlobal) true else request.visibility == com.androidprj.fuzic.model.ui.PlaylistVisibility.Public

            val updatedPlaylist = UpdatePlaylistDto(
                title = request.title,
                isPublic = isPublic,
                type = if (request.category == com.androidprj.fuzic.model.ui.PlaylistCategory.None) null else request.category.name.lowercase(),
                coverImageUrl = finalCoverUrl
            )
            val result = supabaseClient.postgrest["playlists"]
                .update(updatedPlaylist) { 
                    filter { eq("id", playlistId) } 
                    select() 
                }
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

    @kotlinx.serialization.Serializable
    private data class UpdatePlaylistDto(
        val title: String? = null,
        @kotlinx.serialization.SerialName("is_public") val isPublic: Boolean? = null,
        val type: String? = null,
        @kotlinx.serialization.SerialName("cover_image_url") val coverImageUrl: String? = null
    )

    private companion object {
        const val USER_PLAYLIST_TYPE = "user"
    }
}

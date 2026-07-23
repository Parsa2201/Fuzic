package com.androidprj.fuzic.data.remote.repository

import com.androidprj.fuzic.model.remote.InteractionDto
import com.androidprj.fuzic.model.remote.SongDto
import com.androidprj.fuzic.model.ui.SongItem
import com.androidprj.fuzic.model.mapper.toSongItem
import com.androidprj.fuzic.repository.InteractionRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject
import java.util.UUID

class RemoteInteractionRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) : InteractionRepository {

    override suspend fun getRecentlyPlayed(userId: String, offset: Long, limit: Long): Result<List<SongItem>> {
        return try {
            val songs = supabaseClient.postgrest["interactions"]
                .select(columns = io.github.jan.supabase.postgrest.query.Columns.raw("song_id, songs(*)")) {
                    filter {
                        eq("user_id", userId)
                        eq("interaction_type", "play")
                    }
                    order("created_at", order = Order.DESCENDING)
                    range(offset, offset + limit - 1)
                }
                .decodeList<SongWrapper>()
                .map { it.song.toSongItem() }
            val distinctSongs = songs.distinctBy { it.id }
            Result.success(distinctSongs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getLikedSongs(userId: String, offset: Long, limit: Long): Result<List<SongItem>> {
        return try {
            val songs = supabaseClient.postgrest["interactions"]
                .select(columns = io.github.jan.supabase.postgrest.query.Columns.raw("song_id, songs(*)")) {
                    filter {
                        eq("user_id", userId)
                        eq("interaction_type", "like")
                    }
                    order("created_at", order = Order.DESCENDING)
                    range(offset, offset + limit - 1)
                }
                .decodeList<SongWrapper>()
                .map { it.song.toSongItem() }
            val distinctSongs = songs.distinctBy { it.id }
            Result.success(distinctSongs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun recordPlay(songId: String): Result<Unit> {
        return insertInteraction(songId, "play")
    }

    override suspend fun likeSong(songId: String): Result<Unit> {
        val alreadyLiked = isSongLiked(songId)
        if (alreadyLiked.getOrNull() == true) return Result.success(Unit)
        return insertInteraction(songId, "like").recoverCatching { error ->
            if (error.message.orEmpty().contains("unique_active_like")) Unit else throw error
        }
    }

    override suspend fun isSongLiked(songId: String): Result<Boolean> = runCatching {
        val userId = supabaseClient.auth.currentUserOrNull()?.id ?: error("Not logged in")
        supabaseClient.postgrest["interactions"]
            .select {
                filter {
                    eq("user_id", userId)
                    eq("song_id", songId)
                    eq("interaction_type", "like")
                }
            }
            .decodeList<InteractionDto>()
            .isNotEmpty()
    }

    override suspend fun unlikeSong(songId: String): Result<Unit> {
        return deleteInteraction(songId, "like")
    }

    override suspend fun removeRecentlyPlayed(songId: String): Result<Unit> {
        return deleteInteraction(songId, "play")
    }

    private suspend fun deleteInteraction(songId: String, type: String): Result<Unit> {
        return try {
            val userId = supabaseClient.auth.currentUserOrNull()?.id ?: throw Exception("Not logged in")
            supabaseClient.postgrest["interactions"].delete {
                filter {
                    eq("user_id", userId)
                    eq("song_id", songId)
                    eq("interaction_type", type)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun insertInteraction(songId: String, type: String): Result<Unit> {
        return try {
            val userId = supabaseClient.auth.currentUserOrNull()?.id ?: throw Exception("Not logged in")
            val interaction = InsertInteractionDto(
                userId = userId,
                songId = songId,
                interactionType = type
            )
            // Use upsert = true to handle unique constraint (user_id, song_id, interaction_type) gracefully
            supabaseClient.postgrest["interactions"].upsert(interaction)
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
    private data class InsertInteractionDto(
        @kotlinx.serialization.SerialName("user_id") val userId: String,
        @kotlinx.serialization.SerialName("song_id") val songId: String,
        @kotlinx.serialization.SerialName("interaction_type") val interactionType: String
    )
}

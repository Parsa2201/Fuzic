package com.androidprj.fuzic.data.remote.repository

import com.androidprj.fuzic.model.Interaction
import com.androidprj.fuzic.model.Song
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

    override suspend fun getRecentlyPlayed(userId: String): Result<List<Song>> {
        return try {
            val songs = supabaseClient.postgrest["interactions"]
                .select(columns = io.github.jan.supabase.postgrest.query.Columns.raw("song_id, songs(*)")) {
                    filter {
                        eq("user_id", userId)
                        eq("interaction_type", "play")
                    }
                    order("created_at", order = Order.DESCENDING)
                    limit(20)
                }
                .decodeList<SongWrapper>()
                .map { it.song }
            Result.success(songs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getLikedSongs(userId: String): Result<List<Song>> {
        return try {
            val songs = supabaseClient.postgrest["interactions"]
                .select(columns = io.github.jan.supabase.postgrest.query.Columns.raw("song_id, songs(*)")) {
                    filter {
                        eq("user_id", userId)
                        eq("interaction_type", "like")
                    }
                    order("created_at", order = Order.DESCENDING)
                }
                .decodeList<SongWrapper>()
                .map { it.song }
            Result.success(songs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun recordPlay(songId: String): Result<Unit> {
        return insertInteraction(songId, "play")
    }

    override suspend fun likeSong(songId: String): Result<Unit> {
        return insertInteraction(songId, "like")
    }

    override suspend fun unlikeSong(songId: String): Result<Unit> {
        return try {
            val userId = supabaseClient.auth.currentUserOrNull()?.id ?: throw Exception("Not logged in")
            supabaseClient.postgrest["interactions"].delete {
                filter {
                    eq("user_id", userId)
                    eq("song_id", songId)
                    eq("interaction_type", "like")
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
            val interaction = Interaction(
                id = UUID.randomUUID().toString(),
                userId = userId,
                songId = songId,
                interactionType = type
            )
            supabaseClient.postgrest["interactions"].insert(interaction)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @kotlinx.serialization.Serializable
    private data class SongWrapper(
        @kotlinx.serialization.SerialName("songs") val song: Song
    )
}

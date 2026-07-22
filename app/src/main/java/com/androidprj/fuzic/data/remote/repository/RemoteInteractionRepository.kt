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
import kotlinx.coroutines.flow.Flow
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData

class RemoteInteractionRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) : InteractionRepository {

    override fun observeRecentlyPlayed(userId: String): Flow<PagingData<SongItem>> {
        return Pager(PagingConfig(pageSize = 20)) {
            com.androidprj.fuzic.data.remote.paging.GenericSupabasePagingSource { offset, limit ->
                supabaseClient.postgrest["interactions"]
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
            }
        }.flow
    }

    override fun observeLikedSongs(userId: String): Flow<PagingData<SongItem>> {
        return Pager(PagingConfig(pageSize = 20)) {
            com.androidprj.fuzic.data.remote.paging.GenericSupabasePagingSource { offset, limit ->
                supabaseClient.postgrest["interactions"]
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
            }
        }.flow
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

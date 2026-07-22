package com.androidprj.fuzic.data.remote.repository

import com.androidprj.fuzic.model.remote.SongDto
import com.androidprj.fuzic.model.ui.SongItem
import com.androidprj.fuzic.model.mapper.toSongItem
import com.androidprj.fuzic.repository.MusicRepository
import io.ktor.client.plugins.HttpRequestTimeoutException
import java.io.IOException
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData

class RemoteMusicRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) : MusicRepository {

    override fun observeDailyPicks(): Flow<PagingData<SongItem>> {
        return Pager(PagingConfig(pageSize = 20)) {
            com.androidprj.fuzic.data.remote.paging.GenericSupabasePagingSource { offset, limit ->
                supabaseClient.postgrest["songs"]
                    .select { range(offset, offset + limit - 1) }
                    .decodeList<SongDto>()
                    .map { it.toSongItem() }
            }
        }.flow
    }

    override fun observeTrendingSongs(): Flow<PagingData<SongItem>> {
        return Pager(PagingConfig(pageSize = 20)) {
            com.androidprj.fuzic.data.remote.paging.GenericSupabasePagingSource { offset, limit ->
                supabaseClient.postgrest["songs"]
                    .select { 
                        order("play_count", order = Order.DESCENDING)
                        range(offset, offset + limit - 1)
                    }
                    .decodeList<SongDto>()
                    .map { it.toSongItem() }
            }
        }.flow
    }

    override fun observeNewReleases(): Flow<PagingData<SongItem>> {
        return Pager(PagingConfig(pageSize = 20)) {
            com.androidprj.fuzic.data.remote.paging.GenericSupabasePagingSource { offset, limit ->
                supabaseClient.postgrest["songs"]
                    .select { 
                        order("release_date", order = Order.DESCENDING)
                        range(offset, offset + limit - 1)
                    }
                    .decodeList<SongDto>()
                    .map { it.toSongItem() }
            }
        }.flow
    }

    override fun observeMostPopular(): Flow<PagingData<SongItem>> {
        return observeTrendingSongs()
    }

    override suspend fun getSongById(songId: String): Result<SongItem> {
        return try {
            val song = supabaseClient.postgrest["songs"]
                .select { filter { eq("id", songId) } }
                .decodeSingle<SongDto>()
            Result.success(song.toSongItem())
        } catch (e: Exception) {
            Result.failure(mapToDomainError(e))
        }
    }

    override fun searchSongs(query: String): Flow<PagingData<SongItem>> {
        return Pager(PagingConfig(pageSize = 20)) {
            com.androidprj.fuzic.data.remote.paging.GenericSupabasePagingSource { offset, limit ->
                supabaseClient.postgrest["songs"]
                    .select { 
                        filter { 
                            or {
                                ilike("title", "%$query%")
                                ilike("artist_name", "%$query%")
                            }
                        }
                        range(offset, offset + limit - 1)
                    }
                    .decodeList<SongDto>()
                    .map { it.toSongItem() }
            }
        }.flow
    }

    private fun mapToDomainError(e: Exception): Exception {
        return when (e) {
            is IOException, is HttpRequestTimeoutException -> Exception("Network error: Please check your connection", e)
            else -> Exception("An unexpected error occurred", e)
        }
    }
}

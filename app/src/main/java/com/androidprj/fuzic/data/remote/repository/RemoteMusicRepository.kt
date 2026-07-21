package com.androidprj.fuzic.data.remote.repository

import com.androidprj.fuzic.model.Song
import com.androidprj.fuzic.repository.MusicRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject

class RemoteMusicRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) : MusicRepository {

    override suspend fun getDailyPicks(offset: Long, limit: Long): Result<List<Song>> {
        return try {
            val songs = supabaseClient.postgrest["songs"]
                .select { 
                    range(offset, offset + limit - 1)
                }
                .decodeList<Song>()
            Result.success(songs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTrendingSongs(offset: Long, limit: Long): Result<List<Song>> {
        return try {
            val songs = supabaseClient.postgrest["songs"]
                .select { 
                    order("play_count", order = Order.DESCENDING)
                    range(offset, offset + limit - 1)
                }
                .decodeList<Song>()
            Result.success(songs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getNewReleases(offset: Long, limit: Long): Result<List<Song>> {
        return try {
            val songs = supabaseClient.postgrest["songs"]
                .select { 
                    order("release_date", order = Order.DESCENDING)
                    range(offset, offset + limit - 1)
                }
                .decodeList<Song>()
            Result.success(songs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMostPopular(offset: Long, limit: Long): Result<List<Song>> {
        return getTrendingSongs(offset, limit)
    }

    override suspend fun getSongById(songId: String): Result<Song> {
        return try {
            val song = supabaseClient.postgrest["songs"]
                .select { filter { eq("id", songId) } }
                .decodeSingle<Song>()
            Result.success(song)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchSongs(query: String, offset: Long, limit: Long): Result<List<Song>> {
        return try {
            val songs = supabaseClient.postgrest["songs"]
                .select { 
                    filter { 
                        or {
                            ilike("title", "%$query%")
                            ilike("artist_name", "%$query%")
                        }
                    }
                    range(offset, offset + limit - 1)
                }
                .decodeList<Song>()
            Result.success(songs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

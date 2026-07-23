package com.androidprj.fuzic.data.remote.repository

import com.androidprj.fuzic.data.local.dao.HomeSongDao
import com.androidprj.fuzic.data.local.entity.HomeSongEntity
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

class RemoteMusicRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val homeSongDao: HomeSongDao
) : MusicRepository {

    override suspend fun getDailyPicks(offset: Long, limit: Long): Result<List<SongItem>> {
        return fetchAndCache("DAILY_PICKS", offset, limit) {
            supabaseClient.postgrest["songs"]
                .select { 
                    range(offset, offset + limit - 1)
                }
                .decodeList<SongDto>()
                .map { it.toSongItem() }
        }
    }

    override suspend fun getTrendingSongs(offset: Long, limit: Long): Result<List<SongItem>> {
        return fetchAndCache("TRENDING", offset, limit) {
            supabaseClient.postgrest["songs"]
                .select { 
                    order("play_count", order = Order.DESCENDING)
                    range(offset, offset + limit - 1)
                }
                .decodeList<SongDto>()
                .map { it.toSongItem() }
        }
    }

    override suspend fun getNewReleases(offset: Long, limit: Long): Result<List<SongItem>> {
        return fetchAndCache("NEW_RELEASES", offset, limit) {
            supabaseClient.postgrest["songs"]
                .select { 
                    order("release_date", order = Order.DESCENDING)
                    range(offset, offset + limit - 1)
                }
                .decodeList<SongDto>()
                .map { it.toSongItem() }
        }
    }

    private suspend fun fetchAndCache(
        category: String,
        offset: Long,
        limit: Long,
        fetchCall: suspend () -> List<SongItem>
    ): Result<List<SongItem>> {
        return try {
            val songs = fetchCall()
            if (offset == 0L) {
                homeSongDao.replaceSongs(category, songs.mapIndexed { index, song -> song.toHomeSongEntity(category, index) })
            } else {
                homeSongDao.insertSongs(songs.mapIndexed { index, song -> song.toHomeSongEntity(category, offset.toInt() + index) })
            }
            Result.success(songs)
        } catch (e: Exception) {
            val cachedEntities = homeSongDao.getSongsByCategory(category)
            if (cachedEntities.isNotEmpty() && offset == 0L) {
                // If offline and we have cache, return the first page from cache instead of failing.
                // We only support cached results for the first page (offset == 0) to avoid pagination issues offline.
                val cachedSongs = cachedEntities.take(limit.toInt()).map { it.toSongItem() }
                Result.success(cachedSongs)
            } else {
                Result.failure(mapToDomainError(e))
            }
        }
    }

    private fun SongItem.toHomeSongEntity(category: String, orderIndex: Int) = HomeSongEntity(
        id = id,
        category = category,
        title = title,
        artist = artist,
        artworkUrl = artworkUrl,
        audioUrl = audioUrl,
        album = album,
        durationLabel = durationLabel,
        isExplicit = isExplicit,
        orderIndex = orderIndex
    )

    private fun HomeSongEntity.toSongItem() = SongItem(
        id = id,
        title = title,
        artist = artist,
        artworkUrl = artworkUrl,
        audioUrl = audioUrl,
        album = album,
        durationLabel = durationLabel,
        isExplicit = isExplicit
    )

    override suspend fun getMostPopular(offset: Long, limit: Long): Result<List<SongItem>> {
        return getTrendingSongs(offset, limit)
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

    override suspend fun searchSongs(query: String, offset: Long, limit: Long): Result<List<SongItem>> {
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
                .decodeList<SongDto>()
                .map { it.toSongItem() }
            Result.success(songs)
        } catch (e: Exception) {
            Result.failure(mapToDomainError(e))
        }
    }

    private fun mapToDomainError(e: Exception): Exception {
        return when (e) {
            is IOException, is HttpRequestTimeoutException -> Exception("Network error: Please check your connection", e)
            else -> Exception("An unexpected error occurred", e)
        }
    }
}

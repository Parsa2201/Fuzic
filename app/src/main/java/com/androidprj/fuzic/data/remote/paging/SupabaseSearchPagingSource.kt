package com.androidprj.fuzic.data.remote.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.androidprj.fuzic.model.remote.AlbumDto
import com.androidprj.fuzic.model.remote.ArtistDto
import com.androidprj.fuzic.model.remote.PlaylistDto
import com.androidprj.fuzic.model.remote.SongDto
import com.androidprj.fuzic.model.remote.UserDto
import com.androidprj.fuzic.model.ui.SearchFilter
import com.androidprj.fuzic.model.ui.SearchResultItem
import com.androidprj.fuzic.model.mapper.toSearchResultItem
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order

class SupabaseSearchPagingSource(
    private val supabaseClient: SupabaseClient,
    private val query: String,
    private val filter: SearchFilter
) : PagingSource<Long, SearchResultItem>() {

    override fun getRefreshKey(state: PagingState<Long, SearchResultItem>): Long? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(PAGE_SIZE) ?: anchorPage?.nextKey?.minus(PAGE_SIZE)
        }
    }

    override suspend fun load(params: LoadParams<Long>): LoadResult<Long, SearchResultItem> {
        val offset = params.key ?: 0L
        val limit = params.loadSize.toLong()

        if (query.isBlank()) {
            return LoadResult.Page(emptyList(), prevKey = null, nextKey = null)
        }

        return try {
            val results = when (filter) {
                SearchFilter.Songs -> searchSongs(query, offset, limit)
                SearchFilter.Artists -> searchArtists(query, offset, limit)
                SearchFilter.Playlists -> searchPlaylists(query, offset, limit)
                SearchFilter.Users -> searchUsers(query, offset, limit)
            }

            LoadResult.Page(
                data = results,
                prevKey = if (offset == 0L) null else offset - PAGE_SIZE,
                nextKey = if (results.isEmpty() || results.size < limit) null else offset + limit
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    private suspend fun searchSongs(q: String, offset: Long, limit: Long): List<SearchResultItem> {
        return supabaseClient.postgrest["songs"]
            .select {
                filter {
                    ilike("title", "%$q%")
                }
                range(offset, offset + limit - 1)
            }
            .decodeList<SongDto>()
            .map { it.toSearchResultItem() }
    }

    private suspend fun searchArtists(q: String, offset: Long, limit: Long): List<SearchResultItem> {
        return supabaseClient.postgrest["artists"]
            .select {
                filter {
                    ilike("name", "%$q%")
                }
                range(offset, offset + limit - 1)
            }
            .decodeList<ArtistDto>()
            .map { it.toSearchResultItem() }
    }

    private suspend fun searchPlaylists(q: String, offset: Long, limit: Long): List<SearchResultItem> {
        return supabaseClient.postgrest["playlists"]
            .select {
                filter {
                    ilike("title", "%$q%")
                }
                range(offset, offset + limit - 1)
            }
            .decodeList<PlaylistDto>()
            .map { it.toSearchResultItem() }
    }

    private suspend fun searchUsers(q: String, offset: Long, limit: Long): List<SearchResultItem> {
        return supabaseClient.postgrest["users"]
            .select {
                filter {
                    or {
                        ilike("username", "%$q%")
                        ilike("name", "%$q%")
                    }
                }
                range(offset, offset + limit - 1)
            }
            .decodeList<UserDto>()
            .map { it.toSearchResultItem() }
    }

    companion object {
        const val PAGE_SIZE = 20L
    }
}

package com.androidprj.fuzic.data.remote.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.androidprj.fuzic.model.remote.PlaylistDto
import com.androidprj.fuzic.model.remote.SongDto
import com.androidprj.fuzic.model.remote.UserDto
import com.androidprj.fuzic.model.ui.SearchFilter
import com.androidprj.fuzic.model.ui.SearchResultItem
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest

class SearchPagingSource(
    private val supabase: SupabaseClient,
    private val query: String,
    private val filter: SearchFilter
) : PagingSource<Int, SearchResultItem>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SearchResultItem> {
        val page = params.key ?: 0
        val pageSize = params.loadSize
        val offset = page * pageSize

        return try {
            val items = when (filter) {
                SearchFilter.Songs -> {
                    val dtos = supabase.postgrest["songs"]
                        .select {
                            filter {
                                or {
                                    ilike("title", "%$query%")
                                    ilike("artist_name", "%$query%")
                                }
                            }
                            range(offset.toLong(), (offset + pageSize - 1).toLong())
                        }.decodeList<SongDto>()
                    
                    dtos.map {
                        SearchResultItem(
                            id = it.id,
                            title = it.title,
                            subtitle = it.artistName,
                            type = SearchFilter.Songs,
                            artworkUrl = it.coverImageUrl
                        )
                    }
                }
                SearchFilter.Playlists -> {
                    val dtos = supabase.postgrest["playlists"]
                        .select {
                            filter {
                                ilike("title", "%$query%")
                            }
                            range(offset.toLong(), (offset + pageSize - 1).toLong())
                        }.decodeList<PlaylistDto>()

                    dtos.map {
                        SearchResultItem(
                            id = it.id,
                            title = it.title,
                            subtitle = it.type ?: "Playlist",
                            type = SearchFilter.Playlists,
                            artworkUrl = it.coverImageUrl
                        )
                    }
                }
                SearchFilter.Users -> {
                    val dtos = supabase.postgrest["users"]
                        .select {
                            filter {
                                or {
                                    ilike("name", "%$query%")
                                    ilike("username", "%$query%")
                                }
                            }
                            range(offset.toLong(), (offset + pageSize - 1).toLong())
                        }.decodeList<UserDto>()

                    dtos.map {
                        SearchResultItem(
                            id = it.id,
                            title = it.name ?: it.username ?: "Unknown User",
                            subtitle = "User",
                            type = SearchFilter.Users,
                            artworkUrl = it.avatarUrl
                        )
                    }
                }
                SearchFilter.Artists -> {
                    val dtos = supabase.postgrest["songs"]
                        .select {
                            filter {
                                ilike("artist_name", "%$query%")
                            }
                            range(offset.toLong(), (offset + pageSize - 1).toLong())
                        }.decodeList<SongDto>()
                    
                    dtos.distinctBy { it.artistName }.map {
                        SearchResultItem(
                            id = it.artistName, // using artistName as ID since there's no dedicated artist table
                            title = it.artistName,
                            subtitle = "Artist",
                            type = SearchFilter.Artists,
                            artworkUrl = it.coverImageUrl
                        )
                    }
                }
            }

            val nextKey = if (items.isEmpty() || items.size < pageSize) null else page + 1
            val prevKey = if (page == 0) null else page - 1

            LoadResult.Page(
                data = items,
                prevKey = prevKey,
                nextKey = nextKey
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, SearchResultItem>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}

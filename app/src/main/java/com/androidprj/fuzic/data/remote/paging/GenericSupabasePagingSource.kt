package com.androidprj.fuzic.data.remote.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState

class GenericSupabasePagingSource<T : Any>(
    private val fetch: suspend (offset: Long, limit: Long) -> List<T>
) : PagingSource<Long, T>() {

    override fun getRefreshKey(state: PagingState<Long, T>): Long? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(PAGE_SIZE) ?: anchorPage?.nextKey?.minus(PAGE_SIZE)
        }
    }

    override suspend fun load(params: LoadParams<Long>): LoadResult<Long, T> {
        val offset = params.key ?: 0L
        val limit = params.loadSize.toLong()

        return try {
            val results = fetch(offset, limit)
            LoadResult.Page(
                data = results,
                prevKey = if (offset == 0L) null else offset - PAGE_SIZE,
                nextKey = if (results.isEmpty() || results.size < limit) null else offset + limit
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    companion object {
        const val PAGE_SIZE = 20L
    }
}

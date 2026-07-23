package com.androidprj.fuzic.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.androidprj.fuzic.data.local.dao.SearchHistoryDao
import com.androidprj.fuzic.data.local.entity.SearchHistoryEntity
import com.androidprj.fuzic.data.remote.repository.SearchPagingSource
import com.androidprj.fuzic.model.ui.SearchFilter
import com.androidprj.fuzic.model.ui.SearchResultItem
import com.androidprj.fuzic.repository.SearchRepository
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient,
    private val searchHistoryDao: SearchHistoryDao
) : SearchRepository {

    override fun search(query: String, filter: SearchFilter): Flow<PagingData<SearchResultItem>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { SearchPagingSource(supabase, query, filter) }
        ).flow
    }

    override fun observeSearchHistory(): Flow<List<String>> {
        return searchHistoryDao.observeHistory()
    }

    override suspend fun saveSearchQuery(query: String): Result<Unit> = runCatching {
        searchHistoryDao.insert(SearchHistoryEntity(query = query))
    }

    override suspend fun deleteSearchQuery(query: String): Result<Unit> = runCatching {
        searchHistoryDao.delete(query)
    }

    override suspend fun clearSearchHistory(): Result<Unit> = runCatching {
        searchHistoryDao.clear()
    }
}

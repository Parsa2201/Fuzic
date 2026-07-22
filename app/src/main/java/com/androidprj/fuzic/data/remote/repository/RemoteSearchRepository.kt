package com.androidprj.fuzic.data.remote.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.androidprj.fuzic.data.local.dao.SearchHistoryDao
import com.androidprj.fuzic.data.local.entity.SearchHistoryEntity
import com.androidprj.fuzic.data.remote.paging.SupabaseSearchPagingSource
import com.androidprj.fuzic.model.ui.SearchFilter
import com.androidprj.fuzic.model.ui.SearchResultItem
import com.androidprj.fuzic.repository.SearchRepository
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RemoteSearchRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val searchHistoryDao: SearchHistoryDao
) : SearchRepository {

    override fun search(query: String, filter: SearchFilter): Flow<PagingData<SearchResultItem>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                SupabaseSearchPagingSource(supabaseClient, query, filter)
            }
        ).flow
    }

    override fun observeSearchHistory(): Flow<List<String>> {
        return searchHistoryDao.observeHistory(limit = 10).map { entities ->
            entities.map { it.query }
        }
    }

    override suspend fun saveSearchQuery(query: String): Result<Unit> {
        return try {
            if (query.isNotBlank()) {
                searchHistoryDao.insert(
                    SearchHistoryEntity(
                        query = query.trim(),
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteSearchQuery(query: String): Result<Unit> {
        return try {
            searchHistoryDao.delete(query)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearSearchHistory(): Result<Unit> {
        return try {
            searchHistoryDao.clear()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

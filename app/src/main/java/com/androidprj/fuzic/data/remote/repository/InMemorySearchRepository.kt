package com.androidprj.fuzic.data.remote.repository

import androidx.paging.PagingData
import com.androidprj.fuzic.model.ui.SearchFilter
import com.androidprj.fuzic.model.ui.SearchResultItem
import com.androidprj.fuzic.repository.SearchRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

@Singleton
class InMemorySearchRepository @Inject constructor() : SearchRepository {
    private val history = MutableStateFlow(emptyList<String>())

    override fun search(query: String, filter: SearchFilter): Flow<PagingData<SearchResultItem>> {
        return flowOf(PagingData.empty())
    }

    override fun observeSearchHistory(): Flow<List<String>> = history

    override suspend fun saveSearchQuery(query: String): Result<Unit> {
        val cleaned = query.trim()
        if (cleaned.isNotEmpty()) {
            history.value = listOf(cleaned) + history.value.filterNot { it.equals(cleaned, ignoreCase = true) }
        }
        return Result.success(Unit)
    }

    override suspend fun deleteSearchQuery(query: String): Result<Unit> {
        history.value = history.value.filterNot { it == query }
        return Result.success(Unit)
    }

    override suspend fun clearSearchHistory(): Result<Unit> {
        history.value = emptyList()
        return Result.success(Unit)
    }
}

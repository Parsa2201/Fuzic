package com.androidprj.fuzic.repository

import androidx.paging.PagingData
import com.androidprj.fuzic.model.ui.SearchFilter
import com.androidprj.fuzic.model.ui.SearchResultItem
import kotlinx.coroutines.flow.Flow

interface SearchRepository {
    fun search(query: String, filter: SearchFilter): Flow<PagingData<SearchResultItem>>
    fun observeSearchHistory(): Flow<List<String>>
    suspend fun saveSearchQuery(query: String): Result<Unit>
    suspend fun deleteSearchQuery(query: String): Result<Unit>
    suspend fun clearSearchHistory(): Result<Unit>
}

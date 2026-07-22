package com.androidprj.fuzic.model.ui

import androidx.annotation.StringRes
import androidx.paging.PagingData
import com.androidprj.fuzic.R

data class SearchUiState(
    val query: String = "",
    val selectedFilter: SearchFilter = SearchFilter.Songs,
    val filters: List<SearchFilter> = SearchFilter.entries,
    val history: List<String> = emptyList(),
    val results: PagingData<SearchResultItem> = PagingData.empty(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val shouldShowHistory: Boolean
        get() = query.isBlank() && !isLoading && errorMessage == null && history.isNotEmpty()

    val shouldShowEmptyQuery: Boolean
        get() = query.isBlank() && !isLoading && errorMessage == null && history.isEmpty()

    val shouldShowNoResults: Boolean
        get() = false
}

data class SearchResultItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val type: SearchFilter,
    val artworkUrl: String? = null
)

enum class SearchFilter(@StringRes val labelRes: Int) {
    Songs(R.string.search_filter_songs),
    Artists(R.string.search_filter_artists),
    Playlists(R.string.search_filter_playlists),
    Users(R.string.search_filter_users)
}

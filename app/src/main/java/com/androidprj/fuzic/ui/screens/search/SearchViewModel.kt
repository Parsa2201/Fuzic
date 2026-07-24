package com.androidprj.fuzic.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.androidprj.fuzic.R
import com.androidprj.fuzic.di.IoDispatcher
import com.androidprj.fuzic.model.ui.SearchFilter
import com.androidprj.fuzic.model.ui.SearchResultItem
import com.androidprj.fuzic.model.ui.SearchUiState
import com.androidprj.fuzic.repository.SearchRepository
import com.androidprj.fuzic.util.StringProvider
import com.androidprj.fuzic.util.toUserFriendlyMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface SearchIntent {
    data class QueryChanged(val value: String) : SearchIntent
    data class FilterSelected(val filter: SearchFilter) : SearchIntent
    data class HistorySelected(val query: String) : SearchIntent
    data class DeleteHistory(val query: String) : SearchIntent
    data object ClearHistory : SearchIntent
    data class ResultSelected(val item: SearchResultItem) : SearchIntent
    data object Retry : SearchIntent
    data object ClearError : SearchIntent
    data object SubmitSearch : SearchIntent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val stringProvider: StringProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val pagingSource = MutableStateFlow<Flow<PagingData<SearchResultItem>>>(
        flowOf(PagingData.empty()),
    )
    val results: Flow<PagingData<SearchResultItem>> = pagingSource
        .flatMapLatest { it }
        .cachedIn(viewModelScope)

    private var searchJob: Job? = null
    private var lastSubmittedQuery: String = ""

    init {
        observeHistory()
    }

    fun onIntent(intent: SearchIntent) {
        when (intent) {
            is SearchIntent.QueryChanged -> {
                _uiState.update { it.copy(query = intent.value, errorMessage = null) }
                scheduleSearch()
            }
            is SearchIntent.FilterSelected -> {
                _uiState.update { it.copy(selectedFilter = intent.filter, errorMessage = null) }
                scheduleSearch(immediate = true)
            }
            is SearchIntent.HistorySelected -> {
                _uiState.update { it.copy(query = intent.query, errorMessage = null) }
                scheduleSearch(immediate = true)
            }
            is SearchIntent.DeleteHistory -> mutateHistory { searchRepository.deleteSearchQuery(intent.query) }
            SearchIntent.ClearHistory -> mutateHistory { searchRepository.clearSearchHistory() }
            is SearchIntent.ResultSelected -> mutateHistory { searchRepository.saveSearchQuery(intent.item.title) }
            SearchIntent.Retry -> scheduleSearch(immediate = true)
            SearchIntent.ClearError -> _uiState.update { it.copy(errorMessage = null) }
            SearchIntent.SubmitSearch -> mutateHistory { searchRepository.saveSearchQuery(_uiState.value.query) }
        }
    }

    private fun observeHistory() {
        viewModelScope.launch {
            searchRepository.observeSearchHistory()
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            errorMessage = throwable.toUserFriendlyMessage(
                                stringProvider,
                                R.string.search_error_message,
                            ),
                        )
                    }
                }
                .collect { history ->
                    _uiState.update { it.copy(history = history) }
                }
        }
    }

    private fun scheduleSearch(immediate: Boolean = false) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (!immediate) delay(SEARCH_DEBOUNCE_MS)
            val query = _uiState.value.query.trim()
            if (query.isBlank()) {
                lastSubmittedQuery = ""
                pagingSource.value = flowOf(PagingData.empty())
                _uiState.update { it.copy(isLoading = false, results = PagingData.empty(), errorMessage = null) }
                return@launch
            }
            if (query.length > MAX_QUERY_LENGTH) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = stringProvider.get(R.string.search_no_results_message))
                }
                return@launch
            }

            lastSubmittedQuery = query
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            pagingSource.value = runCatching {
                searchRepository.search(query, _uiState.value.selectedFilter)
                    .onStart { _uiState.update { it.copy(isLoading = false, errorMessage = null) } }
                    .catch { throwable ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = throwable.toUserFriendlyMessage(
                                    stringProvider,
                                    R.string.search_error_message,
                                ),
                            )
                        }
                        emit(PagingData.empty())
                    }
            }.getOrElse { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = throwable.toUserFriendlyMessage(
                            stringProvider,
                            R.string.search_error_message,
                        ),
                    )
                }
                flowOf(PagingData.empty())
            }
        }
    }

    private fun mutateHistory(block: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            val result = withContext(ioDispatcher) { block() }
            if (result.isFailure) {
                _uiState.update {
                    it.copy(
                        errorMessage = result.exceptionOrNull()?.toUserFriendlyMessage(
                            stringProvider,
                            R.string.search_error_message,
                        ),
                    )
                }
            }
        }
    }

    companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
        const val MAX_QUERY_LENGTH = 100
    }
}

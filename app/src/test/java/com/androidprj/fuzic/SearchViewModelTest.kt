package com.androidprj.fuzic

import com.androidprj.fuzic.R
import com.androidprj.fuzic.model.ui.SearchFilter
import com.androidprj.fuzic.ui.screens.search.SearchIntent
import com.androidprj.fuzic.ui.screens.search.SearchViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun observesHistoryOnStart() = runTest {
        val viewModel = SearchViewModel(FakeSearchRepository(listOf("liked songs")), dispatcher, FakeStringProvider)
        advanceUntilIdle()

        assertEquals(listOf("liked songs"), viewModel.uiState.value.history)
    }

    @Test
    fun queryIsDebouncedBeforeSearching() = runTest {
        val repository = FakeSearchRepository()
        val viewModel = SearchViewModel(repository, dispatcher, FakeStringProvider)

        viewModel.onIntent(SearchIntent.QueryChanged("midnight"))
        advanceTimeBy(SearchViewModel.SEARCH_DEBOUNCE_MS - 1)
        assertEquals(0, repository.searchCalls)
        advanceTimeBy(1)
        advanceUntilIdle()

        assertEquals(1, repository.searchCalls)
        assertEquals("midnight", repository.lastQuery)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun changingFilterSearchesImmediately() = runTest {
        val repository = FakeSearchRepository()
        val viewModel = SearchViewModel(repository, dispatcher, FakeStringProvider)
        advanceUntilIdle()

        viewModel.onIntent(SearchIntent.QueryChanged("luna"))
        viewModel.onIntent(SearchIntent.FilterSelected(SearchFilter.Artists))
        advanceUntilIdle()

        assertEquals(SearchFilter.Artists, repository.lastFilter)
        assertEquals("luna", repository.lastQuery)
    }

    @Test
    fun selectingHistoryUsesItAsQuery() = runTest {
        val repository = FakeSearchRepository(listOf("tehran"))
        val viewModel = SearchViewModel(repository, dispatcher, FakeStringProvider)
        advanceUntilIdle()

        viewModel.onIntent(SearchIntent.HistorySelected("tehran"))
        advanceUntilIdle()

        assertEquals("tehran", viewModel.uiState.value.query)
        assertEquals("tehran", repository.lastQuery)
    }

    @Test
    fun historyDeleteAndClearMutateRepository() = runTest {
        val repository = FakeSearchRepository(listOf("a", "b"))
        val viewModel = SearchViewModel(repository, dispatcher, FakeStringProvider)
        advanceUntilIdle()

        viewModel.onIntent(SearchIntent.DeleteHistory("a"))
        advanceUntilIdle()
        assertEquals(listOf("b"), viewModel.uiState.value.history)
        viewModel.onIntent(SearchIntent.ClearHistory)
        advanceUntilIdle()

        assertEquals(emptyList<String>(), viewModel.uiState.value.history)
    }

    @Test
    fun tooLongQueryDoesNotSearch() = runTest {
        val repository = FakeSearchRepository()
        val viewModel = SearchViewModel(repository, dispatcher, FakeStringProvider)

        viewModel.onIntent(SearchIntent.QueryChanged("x".repeat(SearchViewModel.MAX_QUERY_LENGTH + 1)))
        advanceTimeBy(SearchViewModel.SEARCH_DEBOUNCE_MS)
        advanceUntilIdle()

        assertEquals(0, repository.searchCalls)
        assertEquals(FakeStringProvider.get(R.string.search_no_results_message), viewModel.uiState.value.errorMessage)
    }

    @Test
    fun searchFailureShowsErrorAndStopsLoading() = runTest {
        val repository = FakeSearchRepository().apply {
            searchThrows = IllegalStateException("search failed")
        }
        val viewModel = SearchViewModel(repository, dispatcher, FakeStringProvider)

        viewModel.onIntent(SearchIntent.QueryChanged("midnight"))
        advanceTimeBy(SearchViewModel.SEARCH_DEBOUNCE_MS)
        advanceUntilIdle()

        assertEquals("search failed", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun resultSelectionSavesSearchTerm() = runTest {
        val viewModel = SearchViewModel(FakeSearchRepository(), dispatcher, FakeStringProvider)
        advanceUntilIdle()

        viewModel.onIntent(SearchIntent.ResultSelected(testSearchResult))
        advanceUntilIdle()

        assertEquals(listOf(testSearchResult.title), viewModel.uiState.value.history)
        assertNull(viewModel.uiState.value.errorMessage)
    }
}

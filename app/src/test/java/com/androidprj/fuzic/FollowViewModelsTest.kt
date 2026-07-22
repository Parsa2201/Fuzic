package com.androidprj.fuzic

import com.androidprj.fuzic.model.ui.FollowListType
import com.androidprj.fuzic.ui.screens.follow.FollowListIntent
import com.androidprj.fuzic.ui.screens.follow.FollowListViewModel
import com.androidprj.fuzic.ui.screens.follow.FollowSearchIntent
import com.androidprj.fuzic.ui.screens.follow.FollowSearchViewModel
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FollowViewModelsTest {
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
    fun searchDebouncesUserQueries() = runTest {
        val userRepository = FakeUserRepository()
        val viewModel = FollowSearchViewModel(userRepository, FakeFollowRepository(), dispatcher, FakeStringProvider)

        viewModel.onIntent(FollowSearchIntent.QueryChanged("nika"))
        advanceTimeBy(FollowSearchViewModel.SEARCH_DEBOUNCE_MS - 1)
        assertEquals(0, userRepository.searchCalls)
        advanceTimeBy(1)
        advanceUntilIdle()

        assertEquals("nika", userRepository.lastSearchQuery)
        assertEquals("Nika", viewModel.uiState.value.results.first().displayName)
    }

    @Test
    fun searchFailureShowsError() = runTest {
        val userRepository = FakeUserRepository().apply {
            searchResult = Result.failure(IllegalStateException("search users failed"))
        }
        val viewModel = FollowSearchViewModel(userRepository, FakeFollowRepository(), dispatcher, FakeStringProvider)

        viewModel.onIntent(FollowSearchIntent.QueryChanged("nika"))
        advanceTimeBy(FollowSearchViewModel.SEARCH_DEBOUNCE_MS)
        advanceUntilIdle()

        assertEquals("search users failed", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun followSearchToggleRollsBackOnFailure() = runTest {
        val followRepository = FakeFollowRepository().apply {
            followResult = Result.failure(IllegalStateException("cannot follow"))
        }
        val viewModel = FollowSearchViewModel(FakeUserRepository(), followRepository, dispatcher, FakeStringProvider)

        viewModel.onIntent(FollowSearchIntent.QueryChanged("nika"))
        advanceTimeBy(FollowSearchViewModel.SEARCH_DEBOUNCE_MS)
        advanceUntilIdle()
        val user = viewModel.uiState.value.results.first()
        viewModel.onIntent(FollowSearchIntent.ToggleFollow(user))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.results.first().isFollowing)
        assertEquals("cannot follow", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun listLoadsFollowersAndRetries() = runTest {
        val followRepository = FakeFollowRepository()
        val viewModel = FollowListViewModel(followRepository, dispatcher, FakeStringProvider)

        viewModel.onIntent(FollowListIntent.Load("user-1", FollowListType.Followers))
        advanceUntilIdle()
        viewModel.onIntent(FollowListIntent.Retry)
        advanceUntilIdle()

        assertEquals(2, followRepository.followersCalls)
        assertEquals(FollowListType.Followers, viewModel.uiState.value.type)
        assertEquals(listOf(testFollowUser), viewModel.uiState.value.users)
    }

    @Test
    fun listLoadsFollowing() = runTest {
        val followRepository = FakeFollowRepository()
        val viewModel = FollowListViewModel(followRepository, dispatcher, FakeStringProvider)

        viewModel.onIntent(FollowListIntent.Load("user-1", FollowListType.Following))
        advanceUntilIdle()

        assertEquals(1, followRepository.followingCalls)
        assertTrue(viewModel.uiState.value.users.first().isFollowing)
    }

    @Test
    fun listDoesNotFollowCurrentUser() = runTest {
        val followRepository = FakeFollowRepository()
        val viewModel = FollowListViewModel(followRepository, dispatcher, FakeStringProvider)

        viewModel.onIntent(FollowListIntent.Load("user-1", FollowListType.Followers))
        advanceUntilIdle()
        viewModel.onIntent(FollowListIntent.ToggleFollow(testFollowUser.copy(isCurrentUser = true)))
        advanceUntilIdle()

        assertEquals(0, followRepository.followCalls)
    }
}

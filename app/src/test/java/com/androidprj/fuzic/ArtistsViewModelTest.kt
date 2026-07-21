package com.androidprj.fuzic

import com.androidprj.fuzic.ui.screens.artists.ArtistsIntent
import com.androidprj.fuzic.ui.screens.artists.ArtistsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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
class ArtistsViewModelTest {
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
    fun observeArtistsClearsLoadingState() = runTest {
        val artistRepository = FakeArtistRepository()
        val viewModel = ArtistsViewModel(artistRepository, FakeFollowRepository(), dispatcher, FakeStringProvider)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(1, artistRepository.observeCalls)
    }

    @Test
    fun retryObservesArtistsAgainAfterFailure() = runTest {
        val artistRepository = FakeArtistRepository(
            artistDetailsResult = Result.success(testArtistDetails),
        ).apply {
            observeFailure = IllegalStateException("artists failed")
        }
        val viewModel = ArtistsViewModel(artistRepository, FakeFollowRepository(), dispatcher, FakeStringProvider)
        advanceUntilIdle()

        assertEquals("artists failed", viewModel.uiState.value.errorMessage)
        artistRepository.observeFailure = null
        viewModel.onIntent(ArtistsIntent.Retry)
        advanceUntilIdle()

        assertEquals(null, viewModel.uiState.value.errorMessage)
        assertEquals(2, artistRepository.observeCalls)
    }

    @Test
    fun toggleFollowOptimisticallyUpdatesVisibleArtist() = runTest {
        val followRepository = FakeFollowRepository()
        val viewModel = ArtistsViewModel(FakeArtistRepository(), followRepository, dispatcher, FakeStringProvider)
        viewModel.setArtistsForTesting(listOf(testArtistCollectionItem))

        viewModel.onIntent(ArtistsIntent.ToggleFollow(testArtistCollectionItem))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.artists.single().isFollowing)
        assertEquals(1, followRepository.followCalls)
    }

    @Test
    fun failedUnfollowRollsBackVisibleArtist() = runTest {
        val followedArtist = testArtistCollectionItem.copy(isFollowing = true)
        val followRepository = FakeFollowRepository().apply {
            unfollowResult = Result.failure(IllegalStateException("unfollow failed"))
        }
        val viewModel = ArtistsViewModel(FakeArtistRepository(), followRepository, dispatcher, FakeStringProvider)
        viewModel.setArtistsForTesting(listOf(followedArtist))

        viewModel.onIntent(ArtistsIntent.ToggleFollow(followedArtist))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.artists.single().isFollowing)
        assertEquals("unfollow failed", viewModel.uiState.value.errorMessage)
        assertEquals(1, followRepository.unfollowCalls)
    }
}

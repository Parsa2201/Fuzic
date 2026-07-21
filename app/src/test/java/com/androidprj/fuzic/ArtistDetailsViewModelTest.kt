package com.androidprj.fuzic

import com.androidprj.fuzic.ui.screens.artist.ArtistDetailsIntent
import com.androidprj.fuzic.ui.screens.artist.ArtistDetailsViewModel
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ArtistDetailsViewModelTest {
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
    fun loadArtistDetailsShowsArtistAndSongs() = runTest {
        val repository = FakeArtistRepository()
        val viewModel = ArtistDetailsViewModel(repository, FakeFollowRepository(), FakePlayerRepository(), dispatcher, FakeStringProvider)

        viewModel.onIntent(ArtistDetailsIntent.Load("artist-1"))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(testArtist, viewModel.uiState.value.artist)
        assertEquals(listOf(testSong), viewModel.uiState.value.popularSongs)
        assertEquals("artist-1", repository.lastArtistId)
    }

    @Test
    fun loadFailureShowsError() = runTest {
        val viewModel = ArtistDetailsViewModel(
            FakeArtistRepository(Result.failure(IllegalStateException("artist offline"))),
            FakeFollowRepository(),
            FakePlayerRepository(),
            dispatcher,
            FakeStringProvider,
        )

        viewModel.onIntent(ArtistDetailsIntent.Load("artist-1"))
        advanceUntilIdle()

        assertEquals("artist offline", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun retryReloadsLastArtist() = runTest {
        val repository = FakeArtistRepository()
        val viewModel = ArtistDetailsViewModel(repository, FakeFollowRepository(), FakePlayerRepository(), dispatcher, FakeStringProvider)

        viewModel.onIntent(ArtistDetailsIntent.Load("artist-1"))
        advanceUntilIdle()
        viewModel.onIntent(ArtistDetailsIntent.Retry)
        advanceUntilIdle()

        assertEquals(2, repository.detailsCalls)
    }

    @Test
    fun followFailureRollsBackOptimisticState() = runTest {
        val followRepository = FakeFollowRepository().apply {
            followResult = Result.failure(IllegalStateException("cannot follow"))
        }
        val viewModel = ArtistDetailsViewModel(FakeArtistRepository(), followRepository, FakePlayerRepository(), dispatcher, FakeStringProvider)

        viewModel.onIntent(ArtistDetailsIntent.Load("artist-1"))
        advanceUntilIdle()
        viewModel.onIntent(ArtistDetailsIntent.ToggleFollow)
        advanceUntilIdle()

        assertEquals(1, followRepository.followCalls)
        assertFalse(viewModel.uiState.value.isFollowing)
        assertEquals("cannot follow", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun playSongFailureShowsErrorWithoutLosingContent() = runTest {
        val playerRepository = FakePlayerRepository().apply {
            commandResult = Result.failure(IllegalStateException("play failed"))
        }
        val viewModel = ArtistDetailsViewModel(FakeArtistRepository(), FakeFollowRepository(), playerRepository, dispatcher, FakeStringProvider)

        viewModel.onIntent(ArtistDetailsIntent.Load("artist-1"))
        advanceUntilIdle()
        viewModel.onIntent(ArtistDetailsIntent.PlaySong(testSong))
        advanceUntilIdle()

        assertEquals(testArtist, viewModel.uiState.value.artist)
        assertEquals("play failed", viewModel.uiState.value.errorMessage)
        viewModel.onIntent(ArtistDetailsIntent.ClearError)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun successfulFollowUpdatesState() = runTest {
        val viewModel = ArtistDetailsViewModel(FakeArtistRepository(), FakeFollowRepository(), FakePlayerRepository(), dispatcher, FakeStringProvider)

        viewModel.onIntent(ArtistDetailsIntent.Load("artist-1"))
        advanceUntilIdle()
        viewModel.onIntent(ArtistDetailsIntent.ToggleFollow)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isFollowing)
    }
}

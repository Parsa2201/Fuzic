package com.androidprj.fuzic

import com.androidprj.fuzic.ui.screens.playlistdetail.PlaylistDetailsIntent
import com.androidprj.fuzic.ui.screens.playlistdetail.PlaylistDetailsViewModel
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaylistDetailsViewModelTest {
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
    fun loadPlaylistDetailsShowsContent() = runTest {
        val repository = FakePlaylistDetailsRepository()
        val viewModel = PlaylistDetailsViewModel(repository, FakePlayerRepository(), dispatcher, FakeStringProvider)

        viewModel.onIntent(PlaylistDetailsIntent.Load("playlist-1"))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(testPlaylistDetails, viewModel.uiState.value.playlist)
        assertEquals("playlist-1", repository.lastPlaylistId)
    }

    @Test
    fun loadFailureShowsError() = runTest {
        val viewModel = PlaylistDetailsViewModel(
            FakePlaylistDetailsRepository(Result.failure(IllegalStateException("missing playlist"))),
            FakePlayerRepository(),
            dispatcher,
            FakeStringProvider,
        )

        viewModel.onIntent(PlaylistDetailsIntent.Load("playlist-404"))
        advanceUntilIdle()

        assertEquals("missing playlist", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun retryReloadsLastPlaylist() = runTest {
        val repository = FakePlaylistDetailsRepository()
        val viewModel = PlaylistDetailsViewModel(repository, FakePlayerRepository(), dispatcher, FakeStringProvider)

        viewModel.onIntent(PlaylistDetailsIntent.Load("playlist-1"))
        advanceUntilIdle()
        viewModel.onIntent(PlaylistDetailsIntent.Retry)
        advanceUntilIdle()

        assertEquals(2, repository.loadCalls)
    }

    @Test
    fun playAllSendsPlaylistSongsToPlayer() = runTest {
        val playerRepository = FakePlayerRepository()
        val viewModel = PlaylistDetailsViewModel(FakePlaylistDetailsRepository(), playerRepository, dispatcher, FakeStringProvider)

        viewModel.onIntent(PlaylistDetailsIntent.PlayAll(testPlaylistDetails))
        advanceUntilIdle()

        assertEquals(testSong, playerRepository.playerState.value.currentSong)
    }

    @Test
    fun playAllFailureKeepsPlaylistAndShowsError() = runTest {
        val playerRepository = FakePlayerRepository().apply {
            commandResult = Result.failure(IllegalStateException("cannot play"))
        }
        val viewModel = PlaylistDetailsViewModel(FakePlaylistDetailsRepository(), playerRepository, dispatcher, FakeStringProvider)

        viewModel.onIntent(PlaylistDetailsIntent.Load("playlist-1"))
        advanceUntilIdle()
        viewModel.onIntent(PlaylistDetailsIntent.PlayAll(testPlaylistDetails))
        advanceUntilIdle()

        assertEquals(testPlaylistDetails, viewModel.uiState.value.playlist)
        assertEquals("cannot play", viewModel.uiState.value.errorMessage)
        viewModel.onIntent(PlaylistDetailsIntent.ClearError)
        assertNull(viewModel.uiState.value.errorMessage)
    }
}

package com.androidprj.fuzic

import com.androidprj.fuzic.model.ui.PlayerOverlay
import com.androidprj.fuzic.model.ui.PlayerUiState
import com.androidprj.fuzic.model.ui.RepeatMode
import com.androidprj.fuzic.ui.screens.player.PlayerIntent
import com.androidprj.fuzic.ui.screens.player.PlayerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelTest {
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
    fun mirrorsRepositoryStateOnStart() = runTest {
        val repository = FakePlayerRepository(PlayerUiState(currentSong = testSong, isPlaying = true))
        val viewModel = PlayerViewModel(repository, dispatcher, FakeStringProvider)
        advanceUntilIdle()

        assertEquals(testSong, viewModel.uiState.value.currentSong)
        assertTrue(viewModel.uiState.value.isPlaying)
    }

    @Test
    fun playAndToggleForwardToRepository() = runTest {
        val repository = FakePlayerRepository()
        val viewModel = PlayerViewModel(repository, dispatcher, FakeStringProvider)
        advanceUntilIdle()

        viewModel.onIntent(PlayerIntent.Play(testSong))
        advanceUntilIdle()
        viewModel.onIntent(PlayerIntent.TogglePlayPause)
        advanceUntilIdle()

        assertEquals(1, repository.playCalls)
        assertEquals(1, repository.toggleCalls)
    }

    @Test
    fun shuffleAndRepeatUseNextState() = runTest {
        val repository = FakePlayerRepository(PlayerUiState(repeatMode = RepeatMode.Off))
        val viewModel = PlayerViewModel(repository, dispatcher, FakeStringProvider)
        advanceUntilIdle()

        viewModel.onIntent(PlayerIntent.ToggleShuffle)
        advanceUntilIdle()
        viewModel.onIntent(PlayerIntent.CycleRepeatMode)
        advanceUntilIdle()

        assertEquals(true, repository.lastShuffleEnabled)
        assertEquals(RepeatMode.All, repository.lastRepeatMode)
    }

    @Test
    fun overlaysAreLocalUiState() = runTest {
        val viewModel = PlayerViewModel(FakePlayerRepository(), dispatcher, FakeStringProvider)
        advanceUntilIdle()

        viewModel.onIntent(PlayerIntent.ShowOverlay(PlayerOverlay.Queue))
        assertEquals(PlayerOverlay.Queue, viewModel.uiState.value.selectedOverlay)
        viewModel.onIntent(PlayerIntent.DismissOverlay)
        assertEquals(PlayerOverlay.None, viewModel.uiState.value.selectedOverlay)
    }

    @Test
    fun sleepTimerAndSpeedDismissOverlayAfterSuccess() = runTest {
        val repository = FakePlayerRepository()
        val viewModel = PlayerViewModel(repository, dispatcher, FakeStringProvider)
        advanceUntilIdle()

        viewModel.onIntent(PlayerIntent.ShowOverlay(PlayerOverlay.SleepTimer))
        viewModel.onIntent(PlayerIntent.SleepTimerSelected(30))
        advanceUntilIdle()
        viewModel.onIntent(PlayerIntent.ShowOverlay(PlayerOverlay.PlaybackSpeed))
        viewModel.onIntent(PlayerIntent.PlaybackSpeedSelected(1.5f))
        advanceUntilIdle()

        assertEquals(30, repository.lastSleepTimer)
        assertEquals(1.5f, repository.lastSpeed)
        assertEquals(PlayerOverlay.None, viewModel.uiState.value.selectedOverlay)
    }

    @Test
    fun repositoryFailureShowsError() = runTest {
        val repository = FakePlayerRepository().apply {
            commandResult = Result.failure(IllegalStateException("player failed"))
        }
        val viewModel = PlayerViewModel(repository, dispatcher, FakeStringProvider)
        advanceUntilIdle()

        viewModel.onIntent(PlayerIntent.Next)
        advanceUntilIdle()

        assertEquals("player failed", viewModel.uiState.value.errorMessage)
        viewModel.onIntent(PlayerIntent.ClearError)
        assertNull(viewModel.uiState.value.errorMessage)
    }
}

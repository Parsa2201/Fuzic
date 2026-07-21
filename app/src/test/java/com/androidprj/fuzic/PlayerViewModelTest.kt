package com.androidprj.fuzic

import com.androidprj.fuzic.model.ui.PlayerOverlay
import com.androidprj.fuzic.model.ui.PlayerUiState
import com.androidprj.fuzic.model.ui.RepeatMode
import com.androidprj.fuzic.model.ui.AudioVisualizerFrame
import com.androidprj.fuzic.ui.screens.player.PlayerIntent
import com.androidprj.fuzic.ui.screens.player.PlayerViewModel
import com.androidprj.fuzic.ui.screens.player.normalizeAmplitudes
import com.androidprj.fuzic.ui.screens.player.smoothAmplitudes
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

    @Test
    fun visualizerFramesAreClampedAndExposedInUiState() = runTest {
        val repository = FakePlayerRepository(PlayerUiState(currentSong = testSong, isPlaying = true))
        val viewModel = PlayerViewModel(repository, dispatcher, FakeStringProvider)
        advanceUntilIdle()

        repository.visualizerFrames.emit(
            AudioVisualizerFrame(
                amplitudes = listOf(-1f, 0.25f, 2f),
                timestampEpochMillis = 1L,
            ),
        )
        advanceUntilIdle()

        assertEquals(32, viewModel.uiState.value.visualizerAmplitudes.size)
        assertEquals(0f, viewModel.uiState.value.visualizerAmplitudes.first())
        assertEquals(1f, viewModel.uiState.value.visualizerAmplitudes.last())
    }

    @Test
    fun visualizerFramesAreIgnoredWhilePausedAndClearedOnPause() = runTest {
        val repository = FakePlayerRepository(PlayerUiState(currentSong = testSong, isPlaying = false))
        val viewModel = PlayerViewModel(repository, dispatcher, FakeStringProvider)
        advanceUntilIdle()

        repository.visualizerFrames.emit(AudioVisualizerFrame(listOf(1f), 1L))
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.visualizerAmplitudes.isEmpty())

        repository.play(testSong)
        advanceUntilIdle()
        repository.visualizerFrames.emit(AudioVisualizerFrame(listOf(1f), 2L))
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.visualizerAmplitudes.isNotEmpty())

        repository.togglePlayPause()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.visualizerAmplitudes.isEmpty())
    }

    @Test
    fun visualizerNormalizationResamplesAndSmoothingBoundsValues() {
        assertEquals(listOf(0f, 0.5f, 1f), normalizeAmplitudes(listOf(-1f, 0.5f, 2f), 3))
        assertEquals(listOf(0.25f, 0.75f), smoothAmplitudes(listOf(0f, 1f), listOf(1f, 0f), 0.25f))
    }
}

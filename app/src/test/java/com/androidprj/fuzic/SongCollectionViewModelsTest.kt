package com.androidprj.fuzic

import com.androidprj.fuzic.ui.screens.songcollection.LikedSongsViewModel
import com.androidprj.fuzic.ui.screens.songcollection.RecentlyPlayedViewModel
import com.androidprj.fuzic.ui.screens.songcollection.SongCollectionIntent
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SongCollectionViewModelsTest {
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
    fun likedSongsLoadsSongs() = runTest {
        val interactionRepository = FakeInteractionRepository()
        val viewModel = LikedSongsViewModel(FakeAuthRepository(), interactionRepository, dispatcher, FakeStringProvider)
        advanceUntilIdle()

        assertEquals(listOf(testSong), viewModel.uiState.value.songs)
        assertEquals(1, interactionRepository.likedCalls)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun recentlyPlayedLoadsSongs() = runTest {
        val interactionRepository = FakeInteractionRepository()
        val viewModel = RecentlyPlayedViewModel(FakeAuthRepository(), interactionRepository, dispatcher, FakeStringProvider)
        advanceUntilIdle()

        assertEquals(listOf(testSong), viewModel.uiState.value.songs)
        assertEquals(1, interactionRepository.recentlyCalls)
    }

    @Test
    fun authFailureShowsError() = runTest {
        val viewModel = LikedSongsViewModel(FakeAuthRepository(currentUserId = null), FakeInteractionRepository(), dispatcher, FakeStringProvider)
        advanceUntilIdle()

        assertEquals(FakeStringProvider.get(com.androidprj.fuzic.R.string.auth_error_message), viewModel.uiState.value.errorMessage)
    }

    @Test
    fun repositoryFailureShowsErrorAndRetryReloads() = runTest {
        val interactionRepository = FakeInteractionRepository().apply {
            likedResult = Result.failure(IllegalStateException("liked failed"))
        }
        val viewModel = LikedSongsViewModel(FakeAuthRepository(), interactionRepository, dispatcher, FakeStringProvider)
        advanceUntilIdle()

        assertEquals("liked failed", viewModel.uiState.value.errorMessage)
        interactionRepository.likedResult = Result.success(listOf(testSong))
        viewModel.onIntent(SongCollectionIntent.Retry)
        advanceUntilIdle()

        assertEquals(listOf(testSong), viewModel.uiState.value.songs)
        assertEquals(2, interactionRepository.likedCalls)
    }
}

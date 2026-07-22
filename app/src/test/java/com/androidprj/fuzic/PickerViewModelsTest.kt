package com.androidprj.fuzic

import com.androidprj.fuzic.ui.screens.chat.ChatPickerViewModel
import com.androidprj.fuzic.ui.screens.playlists.AddToPlaylistViewModel
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
class PickerViewModelsTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun addToPlaylistLoadsCurrentUsersPlaylistsAndCompletesOnSuccess() = runTest {
        val playlists = FakePlaylistRepository()
        val viewModel = AddToPlaylistViewModel(FakeAuthRepository(), playlists, dispatcher, FakeStringProvider)

        viewModel.load()
        advanceUntilIdle()
        viewModel.addSong(testPlaylist, testSong.id)
        advanceUntilIdle()

        assertEquals(listOf(testPlaylist), viewModel.uiState.value.playlists)
        assertEquals(testPlaylist.id, playlists.lastAddedPlaylistId)
        assertEquals(testSong.id, playlists.lastAddedSongId)
        assertTrue(viewModel.uiState.value.isComplete)
    }

    @Test
    fun addToPlaylistShowsRepositoryFailure() = runTest {
        val playlists = FakePlaylistRepository().apply {
            addSongResult = Result.failure(IllegalStateException("add failed"))
        }
        val viewModel = AddToPlaylistViewModel(FakeAuthRepository(), playlists, dispatcher, FakeStringProvider)

        viewModel.load()
        advanceUntilIdle()
        viewModel.addSong(testPlaylist, testSong.id)
        advanceUntilIdle()

        assertEquals("add failed", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isComplete)
    }

    @Test
    fun chatPickerSharesSongAndCompletesOnSuccess() = runTest {
        val chat = FakeChatRepository()
        val viewModel = ChatPickerViewModel(chat, dispatcher, FakeStringProvider)
        advanceUntilIdle()

        viewModel.share(testConversation, testSong.id)
        advanceUntilIdle()

        assertEquals(testSong.id, chat.lastSharedSongId)
        assertTrue(viewModel.uiState.value.isComplete)
    }

    @Test
    fun chatPickerKeepsScreenOpenWhenShareFails() = runTest {
        val chat = FakeChatRepository().apply {
            sendSongResult = Result.failure(IllegalStateException("share failed"))
        }
        val viewModel = ChatPickerViewModel(chat, dispatcher, FakeStringProvider)
        advanceUntilIdle()

        viewModel.share(testConversation, testSong.id)
        advanceUntilIdle()

        assertEquals("share failed", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isComplete)
    }
}

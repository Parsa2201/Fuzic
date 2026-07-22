package com.androidprj.fuzic

import com.androidprj.fuzic.model.ui.TypingStatus
import com.androidprj.fuzic.ui.screens.chat.ChatDetailIntent
import com.androidprj.fuzic.ui.screens.chat.ChatDetailViewModel
import com.androidprj.fuzic.ui.screens.chat.ChatListIntent
import com.androidprj.fuzic.ui.screens.chat.ChatListViewModel
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
class ChatViewModelsTest {
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
    fun chatListObservesConversations() = runTest {
        val repository = FakeChatRepository()
        val viewModel = ChatListViewModel(repository, FakeStringProvider)
        advanceUntilIdle()

        assertEquals(listOf(testConversation), viewModel.uiState.value.conversations)
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(1, repository.conversationObserveCalls)
    }

    @Test
    fun chatListRetryObservesAgainAfterFailure() = runTest {
        val repository = FakeChatRepository().apply {
            conversationsFailure = IllegalStateException("chat failed")
        }
        val viewModel = ChatListViewModel(repository, FakeStringProvider)
        advanceUntilIdle()

        assertEquals("chat failed", viewModel.uiState.value.errorMessage)
        repository.conversationsFailure = null
        viewModel.onIntent(ChatListIntent.Retry)
        advanceUntilIdle()

        assertEquals(listOf(testConversation), viewModel.uiState.value.conversations)
        assertEquals(2, repository.conversationObserveCalls)
    }

    @Test
    fun loadingConversationRefreshesAndTracksTyping() = runTest {
        val repository = FakeChatRepository()
        val viewModel = ChatDetailViewModel(repository, dispatcher, FakeStringProvider)

        viewModel.onIntent(ChatDetailIntent.LoadConversation(testConversation))
        repository.typingStatus.value = TypingStatus(testConversation.id, testConversation.participant.id, 1L)
        advanceUntilIdle()

        assertEquals(testConversation, viewModel.uiState.value.conversation)
        assertTrue(viewModel.uiState.value.isOtherUserTyping)
        assertEquals(1, repository.messageObserveCalls)
        assertEquals(1, repository.refreshCalls)
    }

    @Test
    fun loadingConversationDisplaysMessagesFromRepository() = runTest {
        val repository = FakeChatRepository()
        val viewModel = ChatDetailViewModel(repository, dispatcher, FakeStringProvider)

        viewModel.onIntent(ChatDetailIntent.LoadConversation(testConversation))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun draftChangeTogglesTypingWhenBlankStateChanges() = runTest {
        val repository = FakeChatRepository()
        val viewModel = ChatDetailViewModel(repository, dispatcher, FakeStringProvider)
        viewModel.onIntent(ChatDetailIntent.LoadConversation(testConversation))
        advanceUntilIdle()

        viewModel.onIntent(ChatDetailIntent.DraftChanged("hello"))
        advanceUntilIdle()

        assertEquals("hello", viewModel.uiState.value.draft)
        assertTrue(repository.lastTypingValue == true)
        viewModel.onIntent(ChatDetailIntent.DraftChanged(""))
        advanceUntilIdle()

        assertEquals(false, repository.lastTypingValue)
        assertEquals(2, repository.typingCalls)
    }

    @Test
    fun sendDraftTrimsTextAppendsMessageAndClearsDraft() = runTest {
        val repository = FakeChatRepository()
        val viewModel = ChatDetailViewModel(repository, dispatcher, FakeStringProvider)
        viewModel.onIntent(ChatDetailIntent.LoadConversation(testConversation))
        viewModel.onIntent(ChatDetailIntent.DraftChanged("  hello  "))
        advanceUntilIdle()

        viewModel.onIntent(ChatDetailIntent.SendDraft)
        advanceUntilIdle()

        assertEquals("hello", repository.lastSentText)
        assertEquals("", viewModel.uiState.value.draft)
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(false, repository.lastTypingValue)
    }

    @Test
    fun sendDraftFailureKeepsDraftAndShowsError() = runTest {
        val repository = FakeChatRepository().apply {
            sendTextResult = Result.failure(IllegalStateException("send failed"))
        }
        val viewModel = ChatDetailViewModel(repository, dispatcher, FakeStringProvider)
        viewModel.onIntent(ChatDetailIntent.LoadConversation(testConversation))
        viewModel.onIntent(ChatDetailIntent.DraftChanged("hello"))
        advanceUntilIdle()

        viewModel.onIntent(ChatDetailIntent.SendDraft)
        advanceUntilIdle()

        assertEquals("hello", viewModel.uiState.value.draft)
        assertEquals("send failed", viewModel.uiState.value.errorMessage)
        assertEquals(1, repository.sendTextCalls)
    }

    @Test
    fun shareSongAppendsReturnedSongMessage() = runTest {
        val repository = FakeChatRepository()
        val viewModel = ChatDetailViewModel(repository, dispatcher, FakeStringProvider)
        viewModel.onIntent(ChatDetailIntent.LoadConversation(testConversation))
        advanceUntilIdle()

        viewModel.onIntent(ChatDetailIntent.ShareSong(testSong))
        advanceUntilIdle()

        assertEquals(testSong.id, repository.lastSharedSongId)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun markMessagesReadDelegatesMessageIds() = runTest {
        val repository = FakeChatRepository()
        val viewModel = ChatDetailViewModel(repository, dispatcher, FakeStringProvider)
        viewModel.onIntent(ChatDetailIntent.LoadConversation(testConversation))
        advanceUntilIdle()

        viewModel.onIntent(ChatDetailIntent.MarkMessagesRead(listOf("one", "two")))
        advanceUntilIdle()

        assertEquals(listOf("one", "two"), repository.lastReadMessageIds)
        assertEquals(1, repository.markReadCalls)
    }

    @Test
    fun refreshFailureKeepsConversationAndMarksOffline() = runTest {
        val repository = FakeChatRepository().apply {
            refreshResult = Result.failure(IllegalStateException("offline"))
        }
        val viewModel = ChatDetailViewModel(repository, dispatcher, FakeStringProvider)

        viewModel.onIntent(ChatDetailIntent.LoadConversation(testConversation))
        advanceUntilIdle()

        assertEquals(testConversation, viewModel.uiState.value.conversation)
        assertTrue(viewModel.uiState.value.isOffline)
    }
}

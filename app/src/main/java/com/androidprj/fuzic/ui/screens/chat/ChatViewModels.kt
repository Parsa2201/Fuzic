package com.androidprj.fuzic.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidprj.fuzic.R
import com.androidprj.fuzic.di.IoDispatcher
import com.androidprj.fuzic.model.ui.ChatConversation
import com.androidprj.fuzic.model.ui.ChatDetailUiState
import com.androidprj.fuzic.model.ui.ChatListUiState
import com.androidprj.fuzic.model.ui.ChatMessage
import com.androidprj.fuzic.model.ui.SongItem
import com.androidprj.fuzic.repository.ChatRepository
import com.androidprj.fuzic.util.StringProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import androidx.paging.PagingData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface ChatListIntent {
    data object Retry : ChatListIntent
    data object ClearError : ChatListIntent
}

sealed interface ChatDetailIntent {
    data class LoadConversation(val conversation: ChatConversation) : ChatDetailIntent
    data class DraftChanged(val draft: String) : ChatDetailIntent
    data object SendDraft : ChatDetailIntent
    data class ShareSong(val song: SongItem) : ChatDetailIntent
    data class MarkMessagesRead(val messageIds: List<String>) : ChatDetailIntent
    data object Retry : ChatDetailIntent
    data object ClearError : ChatDetailIntent
}

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val stringProvider: StringProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChatListUiState(isLoading = true))
    val uiState: StateFlow<ChatListUiState> = _uiState.asStateFlow()

    init {
        observeConversations()
    }

    fun onIntent(intent: ChatListIntent) {
        when (intent) {
            ChatListIntent.Retry -> observeConversations()
            ChatListIntent.ClearError -> _uiState.value = _uiState.value.copy(errorMessage = null)
        }
    }

    private fun observeConversations() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching {
                chatRepository.observeConversations().collect { conversations ->
                    _uiState.value = ChatListUiState(conversations = conversations)
                }
            }.onFailure { throwable ->
                _uiState.value = ChatListUiState(
                    errorMessage = throwable.message ?: stringProvider.get(R.string.chat_error_title),
                )
            }
        }
    }
}

@HiltViewModel
class ChatDetailViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val stringProvider: StringProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChatDetailUiState())
    val uiState: StateFlow<ChatDetailUiState> = _uiState.asStateFlow()

    private var messagesJob: Job? = null
    private var typingJob: Job? = null
    private val markedReadMessageIds = mutableSetOf<String>()

    fun onIntent(intent: ChatDetailIntent) {
        when (intent) {
            is ChatDetailIntent.LoadConversation -> loadConversation(intent.conversation)
            is ChatDetailIntent.DraftChanged -> updateDraft(intent.draft)
            ChatDetailIntent.SendDraft -> sendDraft()
            is ChatDetailIntent.ShareSong -> shareSong(intent.song)
            is ChatDetailIntent.MarkMessagesRead -> markMessagesRead(intent.messageIds)
            ChatDetailIntent.Retry -> _uiState.value.conversation?.let(::loadConversation)
            ChatDetailIntent.ClearError -> _uiState.value = _uiState.value.copy(errorMessage = null)
        }
    }

    internal fun setMessagesForTesting(messages: List<ChatMessage>) {
        _uiState.value = _uiState.value.copy(messages = PagingData.from(messages), isLoading = false, errorMessage = null)
    }

    private fun loadConversation(conversation: ChatConversation) {
        markedReadMessageIds.clear()
        _uiState.value = _uiState.value.copy(conversation = conversation, isLoading = true, errorMessage = null)
        messagesJob?.cancel()
        typingJob?.cancel()
        messagesJob = viewModelScope.launch {
            runCatching {
                chatRepository.observeMessages(conversation.id).collect { messages ->
                    _uiState.value = _uiState.value.copy(
                        messages = messages,
                        optimisticMessages = emptyList(),
                        isLoading = false,
                        isOffline = false,
                        errorMessage = null,
                    )
                }
            }.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isOffline = true,
                    errorMessage = throwable.message ?: stringProvider.get(R.string.chat_error_title),
                )
            }
        }
        typingJob = viewModelScope.launch {
            runCatching {
                chatRepository.observeTypingStatus(conversation.id).collect { status ->
                    _uiState.value = _uiState.value.copy(
                        isOtherUserTyping = status != null && status.userId == conversation.participant.id,
                    )
                }
            }
        }
        refreshConversation(conversation.id)
    }

    private fun updateDraft(draft: String) {
        val previousDraft = _uiState.value.draft
        _uiState.value = _uiState.value.copy(draft = draft)
        val conversation = _uiState.value.conversation ?: return
        if (previousDraft.isBlank() == draft.isBlank()) return
        viewModelScope.launch {
            withContext(ioDispatcher) {
                chatRepository.setTyping(conversation.id, draft.isNotBlank())
            }
        }
    }

    private fun sendDraft() {
        val state = _uiState.value
        val conversation = state.conversation ?: return
        val text = state.draft.trim()
        if (text.isBlank()) return
        viewModelScope.launch {
            val result = withContext(ioDispatcher) {
                chatRepository.sendTextMessage(conversation.id, conversation.participant.id, text)
            }
            result.fold(
                onSuccess = { message ->
                    _uiState.value = _uiState.value.copy(
                        draft = "",
                        optimisticMessages = _uiState.value.optimisticMessages + message,
                        errorMessage = null,
                    )
                    withContext(ioDispatcher) {
                        chatRepository.setTyping(conversation.id, false)
                    }
                },
                onFailure = { throwable ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = throwable.message ?: stringProvider.get(R.string.chat_error_title),
                    )
                },
            )
        }
    }

    private fun shareSong(song: SongItem) {
        val conversation = _uiState.value.conversation ?: return
        viewModelScope.launch {
            val result = withContext(ioDispatcher) {
                chatRepository.sendSongMessage(conversation.id, conversation.participant.id, song.id)
            }
            result.fold(
                onSuccess = { message ->
                    _uiState.value = _uiState.value.copy(
                        optimisticMessages = _uiState.value.optimisticMessages + message,
                        errorMessage = null,
                    )
                },
                onFailure = { throwable ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = throwable.message ?: stringProvider.get(R.string.chat_error_title),
                    )
                },
            )
        }
    }

    private fun markMessagesRead(messageIds: List<String>) {
        val conversation = _uiState.value.conversation ?: return
        val unreadIds = messageIds.filter { markedReadMessageIds.add(it) }
        if (unreadIds.isEmpty()) return
        viewModelScope.launch {
            withContext(ioDispatcher) {
                chatRepository.markMessagesAsRead(conversation.id, unreadIds)
            }.onFailure { throwable ->
                markedReadMessageIds.removeAll(unreadIds.toSet())
                _uiState.value = _uiState.value.copy(
                    errorMessage = throwable.message ?: stringProvider.get(R.string.chat_error_title),
                )
            }
        }
    }

    private fun refreshConversation(conversationId: String) {
        viewModelScope.launch {
            withContext(ioDispatcher) {
                chatRepository.refreshConversation(conversationId)
            }.onFailure {
                _uiState.value = _uiState.value.copy(isOffline = true)
            }
        }
    }
}

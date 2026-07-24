package com.androidprj.fuzic.ui.screens.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.androidprj.fuzic.R
import com.androidprj.fuzic.model.ui.ChatConversation
import com.androidprj.fuzic.ui.components.DetailTopAppBar
import com.androidprj.fuzic.ui.components.MusicArtwork
import com.androidprj.fuzic.ui.components.ScreenMessage
import com.androidprj.fuzic.ui.components.fuzicShimmer
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidprj.fuzic.di.IoDispatcher
import com.androidprj.fuzic.repository.ChatRepository
import com.androidprj.fuzic.util.StringProvider
import com.androidprj.fuzic.model.ui.FollowUser
import com.androidprj.fuzic.ui.theme.FuzicTheme
import com.androidprj.fuzic.ui.theme.spacing
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ChatPickerUiState(val conversations: List<ChatConversation> = emptyList(), val isLoading: Boolean = true, val errorMessage: String? = null, val isComplete: Boolean = false)

@HiltViewModel
class ChatPickerViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val stringProvider: StringProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChatPickerUiState())
    val uiState: StateFlow<ChatPickerUiState> = _uiState.asStateFlow()
    init { viewModelScope.launch { chatRepository.observeConversations().catch { _uiState.value = ChatPickerUiState(isLoading = false, errorMessage = it.message ?: stringProvider.get(R.string.share_to_chat_error)) }.collect { _uiState.value = ChatPickerUiState(conversations = it, isLoading = false) } } }
    fun share(conversation: ChatConversation, songId: String) = viewModelScope.launch { val result = withContext(ioDispatcher) { chatRepository.sendSongMessage(conversation.id, conversation.participant.id, songId) }; _uiState.value = result.fold(onSuccess = { _uiState.value.copy(isComplete = true) }, onFailure = { _uiState.value.copy(errorMessage = it.message ?: stringProvider.get(R.string.share_to_chat_error)) }) }
}

@Composable
fun ChatPickerScreen(uiState: ChatPickerUiState, onBackClick: () -> Unit, onConversationClick: (ChatConversation) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize()) {
        DetailTopAppBar(stringResource(R.string.share_to_chat_title), onBackClick)
        when {
            uiState.errorMessage != null -> ScreenMessage(Icons.Default.Share, stringResource(R.string.share_to_chat_title), uiState.errorMessage)
            uiState.isLoading -> ChatPickerLoadingContent()
            !uiState.isLoading && uiState.conversations.isEmpty() -> ScreenMessage(Icons.Default.Share, stringResource(R.string.share_to_chat_title), stringResource(R.string.share_to_chat_empty))
            else -> LazyColumn {
                items(uiState.conversations, key = { it.id }) { conversation ->
                    ListItem(
                        headlineContent = { Text(conversation.participant.displayName) },
                        supportingContent = { Text(conversation.lastMessagePreview) },
                        leadingContent = {
                            MusicArtwork(
                                artworkUrl = conversation.participant.avatarUrl,
                                fallbackIcon = Icons.Default.Person,
                                contentDescription = conversation.participant.displayName,
                                modifier = Modifier.size(48.dp),
                                shape = CircleShape,
                            )
                        },
                        modifier = Modifier.fillMaxWidth().clickable { onConversationClick(conversation) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatPickerLoadingContent() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(MaterialTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
    ) {
        repeat(5) {
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)) {
                Spacer(Modifier.size(48.dp).fuzicShimmer(CircleShape))
                Column(
                    modifier = Modifier.width(180.dp),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                ) {
                    Spacer(Modifier.fillMaxWidth(0.5f).height(16.dp).fuzicShimmer())
                    Spacer(Modifier.fillMaxWidth(0.8f).height(12.dp).fuzicShimmer())
                }
            }
        }
    }
}

@Preview(name = "Share to chat - English", showBackground = true)
@Composable
private fun ChatPickerEnglishPreview() {
    FuzicTheme {
        ChatPickerScreen(
            uiState = ChatPickerUiState(
                conversations = listOf(
                    ChatConversation(
                        id = "conversation-preview",
                        participant = FollowUser(
                            "user-preview",
                            "nika",
                            stringResource(R.string.preview_artist_nika),
                        ),
                        lastMessagePreview = stringResource(R.string.preview_chat_picker_message),
                        lastMessageTimeLabel = stringResource(R.string.preview_chat_picker_now),
                    ),
                ),
                isLoading = false,
            ),
            onBackClick = {},
            onConversationClick = {},
        )
    }
}

@Preview(name = "Share to chat empty - Persian", locale = "fa", showBackground = true)
@Composable
private fun ChatPickerEmptyPersianPreview() {
    FuzicTheme {
        ChatPickerScreen(
            uiState = ChatPickerUiState(isLoading = false),
            onBackClick = {},
            onConversationClick = {},
        )
    }
}

@Preview(name = "Share to chat loading - Persian", locale = "fa", showBackground = true)
@Composable
private fun ChatPickerLoadingPreview() {
    FuzicTheme {
        ChatPickerScreen(ChatPickerUiState(), onBackClick = {}, onConversationClick = {})
    }
}

@Preview(name = "Share to chat error - Persian", locale = "fa", showBackground = true)
@Composable
private fun ChatPickerErrorPreview() {
    FuzicTheme {
        ChatPickerScreen(
            uiState = ChatPickerUiState(isLoading = false, errorMessage = stringResource(R.string.share_to_chat_error)),
            onBackClick = {},
            onConversationClick = {},
        )
    }
}

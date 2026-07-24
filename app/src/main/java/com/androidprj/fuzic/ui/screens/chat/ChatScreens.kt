package com.androidprj.fuzic.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import androidx.paging.cachedIn
import androidx.paging.PagingData
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.androidprj.fuzic.R
import com.androidprj.fuzic.model.ui.ChatConversation
import com.androidprj.fuzic.model.ui.ChatDetailUiState
import com.androidprj.fuzic.model.ui.ChatListUiState
import com.androidprj.fuzic.model.ui.ChatMessage
import com.androidprj.fuzic.model.ui.ChatMessageStatus
import com.androidprj.fuzic.model.ui.ChatMessageType
import com.androidprj.fuzic.model.ui.FollowUser
import com.androidprj.fuzic.model.ui.SongItem
import com.androidprj.fuzic.ui.components.DetailTopAppBar
import com.androidprj.fuzic.ui.components.MusicArtwork
import com.androidprj.fuzic.ui.components.ScreenMessage
import com.androidprj.fuzic.ui.components.previewArtworkUri
import com.androidprj.fuzic.ui.components.fuzicShimmer
import com.androidprj.fuzic.ui.theme.FuzicTheme
import com.androidprj.fuzic.ui.theme.spacing
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@Composable
fun ChatListRoute(
    uiState: ChatListUiState,
    onBackClick: (() -> Unit)?,
    onConversationClick: (ChatConversation) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ChatListScreen(uiState, onBackClick, onConversationClick, onRetryClick, modifier)
}

@Composable
fun ChatListScreen(
    uiState: ChatListUiState,
    onBackClick: (() -> Unit)?,
    onConversationClick: (ChatConversation) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) {
        if (onBackClick != null) {
            DetailTopAppBar(stringResource(R.string.chat_title), onBackClick)
        }
        when {
            uiState.isLoading -> ChatLoading()
            uiState.errorMessage != null -> ChatStateMessage(
                icon = Icons.Default.ErrorOutline,
                title = stringResource(R.string.chat_error_title),
                message = uiState.errorMessage,
                onRetryClick = onRetryClick,
            )
            uiState.isEmpty -> ChatStateMessage(
                icon = Icons.Default.Person,
                title = stringResource(R.string.chat_empty_title),
                message = stringResource(R.string.chat_empty_message),
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.medium),
            ) {
                items(uiState.conversations, key = { it.id }) { conversation ->
                    ConversationRow(
                        conversation = conversation,
                        onClick = { onConversationClick(conversation) },
                    )
                }
            }
        }
    }
}

@Composable
fun ChatDetailRoute(
    uiState: ChatDetailUiState,
    onBackClick: () -> Unit,
    onDraftChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onSongClick: (SongItem) -> Unit,
    onRetryClick: () -> Unit,
    onVisibleUnreadMessages: (List<ChatMessage>) -> Unit = {},
    modifier: Modifier = Modifier,
    messages: Flow<PagingData<ChatMessage>> = flowOf(uiState.messages),
) {
    ChatDetailScreen(
        uiState = uiState,
        messages = messages,
        onBackClick = onBackClick,
        onDraftChange = onDraftChange,
        onSendClick = onSendClick,
        onSongClick = onSongClick,
        onRetryClick = onRetryClick,
        onVisibleUnreadMessages = onVisibleUnreadMessages,
        modifier = modifier,
    )
}

@Composable
fun ChatDetailScreen(
    uiState: ChatDetailUiState,
    onBackClick: () -> Unit,
    onDraftChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onSongClick: (SongItem) -> Unit,
    onRetryClick: () -> Unit,
    onVisibleUnreadMessages: (List<ChatMessage>) -> Unit = {},
    modifier: Modifier = Modifier,
    messages: Flow<PagingData<ChatMessage>> = flowOf(uiState.messages),
) {
    val conversation = uiState.conversation
    if (conversation == null) {
        ChatStateMessage(
            icon = Icons.Default.ErrorOutline,
            title = stringResource(R.string.chat_error_title),
            message = uiState.errorMessage ?: stringResource(R.string.chat_empty_message),
            onRetryClick = onRetryClick,
        )
        return
    }
    Column(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) {
        ChatHeader(conversation = conversation, onBackClick = onBackClick)
        if (uiState.isOffline) {
            OfflineBanner()
        }
        when {
            uiState.isLoading -> ChatLoading()
            uiState.errorMessage != null -> ChatStateMessage(
                icon = Icons.Default.ErrorOutline,
                title = stringResource(R.string.chat_error_title),
                message = uiState.errorMessage,
                onRetryClick = onRetryClick,
            )
            else -> {
                val listState = rememberLazyListState()
                val pagingScope = rememberCoroutineScope()
                val cachedMessages = remember(messages, pagingScope) { messages.cachedIn(pagingScope) }
                val pagedMessages = cachedMessages.collectAsLazyPagingItems()
                val visibleUnreadMessages = pagedMessages.itemSnapshotList.items.filter {
                    !it.isMine && it.status != ChatMessageStatus.Read
                }
                LaunchedEffect(visibleUnreadMessages.map(ChatMessage::id)) {
                    onVisibleUnreadMessages(visibleUnreadMessages)
                }
                val messageCount = pagedMessages.itemCount + uiState.optimisticMessages.size
                var previousMessageCount by remember(conversation.id) { mutableIntStateOf(0) }
                LaunchedEffect(messageCount) {
                    if (messageCount == 0) return@LaunchedEffect
                    val targetIndex = (messageCount - 1).coerceAtLeast(0)
                    if (previousMessageCount == 0) {
                        listState.scrollToItem(targetIndex)
                    } else {
                        val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                        val isNearBottom = lastVisibleIndex >= listState.layoutInfo.totalItemsCount - 3
                        if (messageCount > previousMessageCount && isNearBottom) {
                            listState.animateScrollToItem(targetIndex)
                        }
                    }
                    previousMessageCount = messageCount
                }
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    state = listState,
                    contentPadding = PaddingValues(MaterialTheme.spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                ) {
                    if (pagedMessages.itemCount == 0 && uiState.optimisticMessages.isEmpty()) {
                        item {
                            ChatStateMessage(
                                icon = Icons.Default.MusicNote,
                                title = stringResource(R.string.chat_no_messages_title),
                                message = stringResource(R.string.chat_no_messages_message),
                            )
                        }
                    } else {
                        items(
                            count = pagedMessages.itemCount,
                            key = pagedMessages.itemKey { it.id },
                        ) { index ->
                            val message = pagedMessages[index] ?: return@items
                            ChatMessageBubble(
                                message = message,
                                onSongClick = onSongClick,
                            )
                        }
                        items(uiState.optimisticMessages, key = { "optimistic-${it.id}" }) { message ->
                            ChatMessageBubble(
                                message = message,
                                onSongClick = onSongClick,
                            )
                        }
                    }
                }
                if (uiState.isOtherUserTyping) {
                    TypingIndicator()
                }
                ChatComposer(
                    draft = uiState.draft,
                    onDraftChange = onDraftChange,
                    onSendClick = onSendClick,
                )
            }
        }
    }
}

@Composable
private fun ConversationRow(
    conversation: ChatConversation,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = MaterialTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
    ) {
        Box {
            MusicArtwork(
                artworkUrl = conversation.participant.avatarUrl,
                fallbackIcon = Icons.Default.Person,
                contentDescription = conversation.participant.displayName,
                modifier = Modifier.size(56.dp).clip(CircleShape),
            )
            if (conversation.isOnline) {
                Box(
                    modifier = Modifier.size(14.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary)
                        .align(Alignment.BottomEnd),
                )
            }
        }
        Column(Modifier.weight(1f)) {
            Text(conversation.participant.displayName, style = MaterialTheme.typography.titleMedium)
            Text(
                conversation.lastMessagePreview,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(conversation.lastMessageTimeLabel, style = MaterialTheme.typography.labelSmall)
            if (conversation.unreadCount > 0) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Text(
                        text = conversation.unreadCount.toString(),
                        modifier = Modifier.padding(horizontal = MaterialTheme.spacing.small),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatHeader(
    conversation: ChatConversation,
    onBackClick: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(MaterialTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
            }
            MusicArtwork(
                artworkUrl = conversation.participant.avatarUrl,
                fallbackIcon = Icons.Default.Person,
                contentDescription = conversation.participant.displayName,
                modifier = Modifier.size(44.dp).clip(CircleShape),
            )
            Column {
                Text(conversation.participant.displayName, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (conversation.isOnline) stringResource(R.string.follow_search_title) else "@${conversation.participant.username}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun OfflineBanner() {
    Row(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.tertiaryContainer).padding(MaterialTheme.spacing.small),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.WifiOff, contentDescription = null)
        Spacer(Modifier.size(MaterialTheme.spacing.small))
        Text(stringResource(R.string.chat_offline), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun ChatMessageBubble(
    message: ChatMessage,
    onSongClick: (SongItem) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isMine) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            horizontalAlignment = if (message.isMine) Alignment.End else Alignment.Start,
        ) {
            when (message.type) {
                ChatMessageType.Text -> Surface(
                    color = if (message.isMine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Text(
                        text = message.text.orEmpty(),
                        modifier = Modifier.padding(horizontal = MaterialTheme.spacing.medium, vertical = MaterialTheme.spacing.small),
                        color = if (message.isMine) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ChatMessageType.SongShare -> {
                    val song = message.song
                    if (song != null) {
                        SongShareCard(song = song, onClick = { onSongClick(song) })
                    } else {
                        SharedSongUnavailableCard()
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(message.timeLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (message.isMine) {
                    Spacer(Modifier.size(MaterialTheme.spacing.extraSmall))
                    MessageStatusIcon(message.status)
                }
            }
        }
    }
}

@Composable
private fun MessageStatusIcon(status: ChatMessageStatus) {
    val icon = when (status) {
        ChatMessageStatus.Sending -> Icons.Default.History
        ChatMessageStatus.Sent -> Icons.Default.Check
        ChatMessageStatus.Delivered,
        ChatMessageStatus.Read -> Icons.Default.DoneAll
    }
    Icon(
        imageVector = icon,
        contentDescription = when (status) {
            ChatMessageStatus.Sending -> stringResource(R.string.chat_status_sending)
            ChatMessageStatus.Sent -> stringResource(R.string.chat_status_sent)
            ChatMessageStatus.Delivered -> stringResource(R.string.chat_status_delivered)
            ChatMessageStatus.Read -> stringResource(R.string.chat_status_read)
        },
        modifier = Modifier.size(14.dp),
        tint = if (status == ChatMessageStatus.Read) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SharedSongUnavailableCard() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(18.dp),
    ) {
        Text(
            text = stringResource(R.string.chat_shared_song_unavailable),
            modifier = Modifier.padding(MaterialTheme.spacing.medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SongShareCard(
    song: SongItem,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.width(260.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(
            modifier = Modifier.padding(MaterialTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        ) {
            MusicArtwork(
                artworkUrl = song.artworkUrl,
                fallbackIcon = Icons.Default.MusicNote,
                contentDescription = song.title,
                modifier = Modifier.size(52.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(song.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(song.artist, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Default.PlayArrow, contentDescription = null)
        }
    }
}

@Composable
private fun TypingIndicator() {
    val transition = rememberInfiniteTransition(label = "typing-indicator")
    val firstAlpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "typing-dot-1",
    )
    val secondAlpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(500, delayMillis = 140), RepeatMode.Reverse),
        label = "typing-dot-2",
    )
    val thirdAlpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(500, delayMillis = 280), RepeatMode.Reverse),
        label = "typing-dot-3",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.spacing.medium),
        horizontalArrangement = Arrangement.Start,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(18.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = MaterialTheme.spacing.medium, vertical = MaterialTheme.spacing.small),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                listOf(firstAlpha, secondAlpha, thirdAlpha).forEach { alpha ->
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)),
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatComposer(
    draft: String,
    onDraftChange: (String) -> Unit,
    onSendClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(MaterialTheme.spacing.small),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(start = MaterialTheme.spacing.medium, end = MaterialTheme.spacing.small),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        ) {
            BasicTextField(
                value = draft,
                onValueChange = onDraftChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = MaterialTheme.spacing.medium),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                maxLines = 5,
                decorationBox = { innerTextField ->
                    if (draft.isBlank()) {
                        Text(
                            text = stringResource(R.string.chat_message_placeholder),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    innerTextField()
                },
            )
            IconButton(
                onClick = onSendClick,
                enabled = draft.isNotBlank(),
                modifier = Modifier
                    .padding(bottom = MaterialTheme.spacing.small)
                    .size(40.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.chat_send))
            }
        }
    }
}

@Composable
private fun ChatLoading() {
    Column(Modifier.fillMaxWidth().padding(MaterialTheme.spacing.medium), verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)) {
        repeat(5) {
            Spacer(Modifier.fillMaxWidth().size(width = 1.dp, height = 64.dp).fuzicShimmer(MaterialTheme.shapes.medium))
        }
    }
}

@Composable
private fun ChatStateMessage(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
    onRetryClick: (() -> Unit)? = null,
) {
    ScreenMessage(
        icon = icon,
        title = title,
        message = message,
        action = onRetryClick?.let {
            {
                Button(onClick = it) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Text(stringResource(R.string.action_retry))
                }
            }
        },
    )
}

@Preview(name = "Chat list", showBackground = true)
@Composable
private fun ChatListPreview() {
    FuzicTheme {
        ChatListScreen(ChatListUiState(sampleConversations()), {}, {}, {})
    }
}

@Preview(name = "Chat list empty Persian", locale = "fa", showBackground = true)
@Composable
private fun ChatListEmptyPreview() {
    FuzicTheme {
        ChatListScreen(ChatListUiState(), {}, {}, {})
    }
}

@Preview(name = "Chat list loading Persian", locale = "fa", showBackground = true)
@Composable
private fun ChatListLoadingPreview() {
    FuzicTheme {
        ChatListScreen(ChatListUiState(isLoading = true), {}, {}, {})
    }
}

@Preview(name = "Chat detail read messages", showBackground = true)
@Composable
private fun ChatDetailPreview() {
    FuzicTheme {
        ChatDetailScreen(sampleChatDetailState(), {}, {}, {}, {}, {}, {})
    }
}

@Preview(name = "Chat detail offline typing Persian", locale = "fa", showBackground = true)
@Composable
private fun ChatDetailOfflinePreview() {
    FuzicTheme {
        ChatDetailScreen(
            sampleChatDetailState().copy(isOffline = true, isOtherUserTyping = true),
            {}, {}, {}, {}, {}, {},
        )
    }
}

@Preview(name = "Chat detail empty Persian", locale = "fa", showBackground = true)
@Composable
private fun ChatDetailEmptyPreview() {
    FuzicTheme {
        ChatDetailScreen(sampleChatDetailState().copy(messages = androidx.paging.PagingData.empty()), {}, {}, {}, {}, {}, {})
    }
}

@Preview(name = "Chat detail sending song share", showBackground = true)
@Composable
private fun ChatDetailSendingPreview() {
    FuzicTheme {
        ChatDetailScreen(
            sampleChatDetailState().copy(
                messages = androidx.paging.PagingData.from(listOf(
                    ChatMessage(
                        id = "sending",
                        senderId = "me",
                        text = "Check this out",
                        status = ChatMessageStatus.Sending,
                        timeLabel = "10:32",
                        isMine = true,
                    ),
                    ChatMessage(
                        id = "share",
                        senderId = "me",
                        type = ChatMessageType.SongShare,
                        song = sampleSong(),
                        status = ChatMessageStatus.Sent,
                        timeLabel = "10:33",
                        isMine = true,
                    ),
                )),
            ),
            {}, {}, {}, {}, {}, {},
        )
    }
}

@Preview(name = "Chat detail message types - English", showBackground = true)
@Preview(name = "Chat detail message types - Persian", locale = "fa", showBackground = true)
@Composable
private fun ChatDetailMessageTypesPreview() {
    FuzicTheme {
        ChatDetailScreen(
            uiState = sampleChatDetailState().copy(
                messages = androidx.paging.PagingData.empty(),
                optimisticMessages = listOf(
                        ChatMessage(
                            id = "incoming-text",
                            senderId = "raha",
                            text = "I found a new playlist for you.",
                            status = ChatMessageStatus.Delivered,
                            timeLabel = "10:30",
                            isMine = false,
                        ),
                        ChatMessage(
                            id = "sent-text",
                            senderId = "me",
                            text = "Nice, send it over!",
                            status = ChatMessageStatus.Sent,
                            timeLabel = "10:31",
                            isMine = true,
                        ),
                        ChatMessage(
                            id = "read-text",
                            senderId = "me",
                            text = "Added it to my library.",
                            status = ChatMessageStatus.Read,
                            timeLabel = "10:32",
                            isMine = true,
                        ),
                        ChatMessage(
                            id = "song-share",
                            senderId = "raha",
                            type = ChatMessageType.SongShare,
                            song = sampleSong(),
                            status = ChatMessageStatus.Delivered,
                            timeLabel = "10:33",
                            isMine = false,
                        ),
                        ChatMessage(
                            id = "sending-text",
                            senderId = "me",
                            text = "Listening now!",
                            status = ChatMessageStatus.Sending,
                            timeLabel = "10:34",
                            isMine = true,
                        ),
                    ),
            ),
            onBackClick = {},
            onDraftChange = {},
            onSendClick = {},
            onSongClick = {},
            onRetryClick = {},
        )
    }
}

@Preview(name = "Chat error Persian", locale = "fa", showBackground = true)
@Composable
private fun ChatErrorPreview() {
    FuzicTheme {
        ChatListScreen(
            ChatListUiState(errorMessage = stringResource(R.string.chat_error_title)),
            {}, {}, {},
        )
    }
}

@Composable
private fun sampleConversations() = listOf(
    ChatConversation(
        id = "conversation-raha",
        participant = FollowUser(
            id = "raha",
            username = "raha_band",
            displayName = stringResource(R.string.preview_artist_raha_band),
            avatarUrl = previewArtworkUri(R.drawable.preview_artwork_tehran),
        ),
        lastMessagePreview = stringResource(R.string.preview_chat_last_message),
        lastMessageTimeLabel = "10:32",
        unreadCount = 2,
        isOnline = true,
    ),
)

@Composable
private fun sampleChatDetailState() = ChatDetailUiState(
    conversation = sampleConversations().first(),
    messages = androidx.paging.PagingData.from(listOf(
        ChatMessage(
            id = "one",
            senderId = "raha",
            text = "I found a new playlist for you.",
            timeLabel = "10:30",
            isMine = false,
        ),
        ChatMessage(
            id = "two",
            senderId = "me",
            text = "Nice, send it over!",
            status = ChatMessageStatus.Read,
            timeLabel = "10:31",
            isMine = true,
        ),
    )),
    draft = "",
)

@Composable
private fun sampleSong() = SongItem(
    id = "song-midnight-drive",
    title = stringResource(R.string.preview_song_midnight_drive),
    artist = stringResource(R.string.preview_artist_luna_ray),
    artworkUrl = previewArtworkUri(R.drawable.preview_artwork_midnight),
)

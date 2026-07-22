package com.androidprj.fuzic.model.ui

import androidx.paging.PagingData

enum class ChatMessageStatus {
    Sending,
    Sent,
    Delivered,
    Read,
}

enum class ChatMessageType {
    Text,
    SongShare,
}

data class ChatConversation(
    val id: String,
    val participant: FollowUser,
    val lastMessagePreview: String,
    val lastMessageTimeLabel: String,
    val unreadCount: Int = 0,
    val isOnline: Boolean = false,
)

data class ChatMessage(
    val id: String,
    val senderId: String,
    val text: String? = null,
    val type: ChatMessageType = ChatMessageType.Text,
    val song: SongItem? = null,
    val status: ChatMessageStatus = ChatMessageStatus.Sent,
    val timeLabel: String,
    val isMine: Boolean,
)

data class ChatListUiState(
    val conversations: List<ChatConversation> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    val isEmpty: Boolean
        get() = !isLoading && errorMessage == null && conversations.isEmpty()
}

data class ChatDetailUiState(
    val conversation: ChatConversation? = null,
    val messages: PagingData<ChatMessage> = PagingData.empty(),
    val optimisticMessages: List<ChatMessage> = emptyList(),
    val draft: String = "",
    val isOtherUserTyping: Boolean = false,
    val isOffline: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
}

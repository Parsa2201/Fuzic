package com.androidprj.fuzic.model.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RecentConversationDto(
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("other_user_id") val otherUserId: String,
    @SerialName("other_user_name") val otherUserName: String? = null,
    @SerialName("other_user_username") val otherUserUsername: String? = null,
    @SerialName("other_user_avatar_url") val otherUserAvatarUrl: String? = null,
    @SerialName("last_message_preview") val lastMessagePreview: String? = null,
    @SerialName("last_message_time") val lastMessageTime: String? = null,
    @SerialName("unread_count") val unreadCount: Int = 0
)

package com.androidprj.fuzic.model.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MessageDto(
    val id: String,
    @SerialName("sender_id") val senderId: String,
    @SerialName("receiver_id") val receiverId: String,
    val content: String? = null,
    @SerialName("shared_song_id") val sharedSongId: String? = null,
    val status: String = "sent",
    @SerialName("message_type") val messageType: String = "text",
    @SerialName("conversation_id") val conversationId: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("delivered_at") val deliveredAt: String? = null,
    @SerialName("read_at") val readAt: String? = null
)

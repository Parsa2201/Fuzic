package com.androidprj.fuzic.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val id: String,
    @SerialName("sender_id") val senderId: String,
    @SerialName("receiver_id") val receiverId: String,
    val content: String? = null,
    @SerialName("shared_song_id") val sharedSongId: String? = null,
    val status: String = "sent",
    @SerialName("created_at") val createdAt: String? = null
)

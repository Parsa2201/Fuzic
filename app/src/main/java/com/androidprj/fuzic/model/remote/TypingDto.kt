package com.androidprj.fuzic.model.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TypingDto(
    @SerialName("user_id") val userId: String,
    @SerialName("participant_id") val participantId: String,
    @SerialName("is_typing") val isTyping: Boolean,
)

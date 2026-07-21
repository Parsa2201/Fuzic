package com.androidprj.fuzic.model.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InteractionDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("song_id") val songId: String,
    @SerialName("interaction_type") val interactionType: String,
    @SerialName("created_at") val createdAt: String? = null
)

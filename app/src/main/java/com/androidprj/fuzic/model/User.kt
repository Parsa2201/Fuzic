package com.androidprj.fuzic.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val name: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("is_premium") val isPremium: Boolean = false
)

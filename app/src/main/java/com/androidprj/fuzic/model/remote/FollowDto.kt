package com.androidprj.fuzic.model.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FollowDto(
    @SerialName("follower_id") val followerId: String,
    @SerialName("followee_id") val followeeId: String,
    @SerialName("created_at") val createdAt: String? = null
)

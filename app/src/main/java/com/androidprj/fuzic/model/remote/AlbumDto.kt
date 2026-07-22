package com.androidprj.fuzic.model.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AlbumDto(
    val id: String,
    val title: String,
    @SerialName("artist_id") val artistId: String,
    @SerialName("cover_image_url") val coverImageUrl: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

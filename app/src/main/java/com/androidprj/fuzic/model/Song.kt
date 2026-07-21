package com.androidprj.fuzic.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Song(
    val id: String,
    val title: String,
    @SerialName("artist_name") val artistName: String,
    @SerialName("cover_image_url") val coverImageUrl: String? = null,
    @SerialName("audio_url") val audioUrl: String? = null,
    @SerialName("play_count") val playCount: Int = 0,
    @SerialName("release_date") val releaseDate: String? = null
)

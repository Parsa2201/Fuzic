package com.androidprj.fuzic.model.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AggregateCount(val count: Int)

@Serializable
data class PlaylistDto(
    val id: String,
    val title: String,
    @SerialName("cover_image_url") val coverImageUrl: String? = null,
    @SerialName("owner_id") val ownerId: String,
    val type: String? = null,
    @SerialName("is_public") val isPublic: Boolean = false,
    @SerialName("playlist_songs") val playlistSongsCount: List<AggregateCount>? = null
)

@Serializable
data class PlaylistDtoSong(
    @SerialName("playlist_id") val playlistId: String,
    @SerialName("song_id") val songId: String
)

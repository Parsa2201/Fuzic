package com.androidprj.fuzic.model.mapper

import com.androidprj.fuzic.model.remote.MessageDto
import com.androidprj.fuzic.model.remote.PlaylistDto
import com.androidprj.fuzic.model.remote.SongDto
import com.androidprj.fuzic.model.remote.UserDto
import com.androidprj.fuzic.model.ui.ChatMessage
import com.androidprj.fuzic.model.ui.ChatMessageStatus
import com.androidprj.fuzic.model.ui.ChatMessageType
import com.androidprj.fuzic.model.ui.FollowUser
import com.androidprj.fuzic.model.ui.PlaylistItem
import com.androidprj.fuzic.model.ui.ProfileUser
import com.androidprj.fuzic.model.ui.SongItem

fun SongDto.toSongItem(): SongItem {
    return SongItem(
        id = this.id,
        title = this.title,
        artist = this.artistName,
        artworkUrl = this.coverImageUrl,
        album = null,
        durationLabel = null, // Will require formatting if DTO had duration
        isExplicit = false, // Not provided by current DTO
        audioUrl = this.audioUrl,
    )
}

fun UserDto.toProfileUser(): ProfileUser {
    val name = this.name ?: "Unknown"
    return ProfileUser(
        id = this.id,
        displayName = name,
        username = this.username ?: name.lowercase().replace(" ", "_"),
        avatarUrl = this.avatarUrl,
        isPremium = this.isPremium
    )
}

fun UserDto.toFollowUser(currentUserId: String? = null): FollowUser {
    val name = this.name ?: "Unknown"
    return FollowUser(
        id = this.id,
        username = this.username ?: name.lowercase().replace(" ", "_"),
        displayName = name,
        avatarUrl = this.avatarUrl,
        isFollowing = false, // Must be updated by the caller if needed
        isCurrentUser = this.id == currentUserId
    )
}

fun PlaylistDto.toPlaylistItem(ownerName: String = ""): PlaylistItem {
    val count = this.playlistSongsCount?.firstOrNull()?.count ?: 0
    val label = if (count == 1) "1 Song" else "$count Songs"
    return PlaylistItem(
        id = this.id,
        title = this.title,
        subtitle = ownerName,
        songCountLabel = label,
        artworkUrl = this.coverImageUrl
    )
}

fun MessageDto.toChatMessage(currentUserId: String): ChatMessage {
    return ChatMessage(
        id = this.id,
        senderId = this.senderId,
        text = this.content,
        type = if (this.sharedSongId != null) ChatMessageType.SongShare else ChatMessageType.Text,
        song = null, // Will need resolution by the caller
        status = when (this.status) {
            "sending" -> ChatMessageStatus.Sending
            "sent" -> ChatMessageStatus.Sent
            "delivered" -> ChatMessageStatus.Delivered
            "read" -> ChatMessageStatus.Read
            else -> ChatMessageStatus.Sent
        },
        timeLabel = this.createdAt ?: "", // Will require formatting if DTO had date
        isMine = this.senderId == currentUserId
    )
}

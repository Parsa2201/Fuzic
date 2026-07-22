package com.androidprj.fuzic.model.mapper

import com.androidprj.fuzic.model.remote.ArtistDto
import com.androidprj.fuzic.model.remote.MessageDto
import com.androidprj.fuzic.model.remote.PlaylistDto
import com.androidprj.fuzic.model.remote.SongDto
import com.androidprj.fuzic.model.remote.UserDto
import com.androidprj.fuzic.model.ui.ArtistItem
import com.androidprj.fuzic.model.ui.ChatMessage
import com.androidprj.fuzic.model.ui.ChatMessageStatus
import com.androidprj.fuzic.model.ui.ChatMessageType
import com.androidprj.fuzic.model.ui.FollowUser
import com.androidprj.fuzic.model.ui.PlaylistItem
import com.androidprj.fuzic.model.ui.ProfileUser
import com.androidprj.fuzic.model.ui.SearchFilter
import com.androidprj.fuzic.model.ui.SearchResultItem
import com.androidprj.fuzic.model.ui.SongItem

/**
 * Formats a duration in seconds into a human-readable label (e.g. "3:45").
 */
private fun formatDuration(seconds: Int?): String? {
    if (seconds == null || seconds < 0) return null
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "%d:%02d".format(minutes, remainingSeconds)
}

fun SongDto.toSongItem(): SongItem {
    return SongItem(
        id = this.id,
        title = this.title,
        artist = this.artistName,
        artworkUrl = this.coverImageUrl,
        album = this.albumName,
        durationLabel = formatDuration(this.durationSeconds),
        isExplicit = this.isExplicit
    )
}

fun UserDto.toProfileUser(): ProfileUser {
    return ProfileUser(
        id = this.id,
        displayName = this.name ?: "Unknown",
        username = this.username ?: this.id.take(8),
        avatarUrl = this.avatarUrl,
        isPremium = this.isPremium
    )
}

fun UserDto.toFollowUser(currentUserId: String? = null): FollowUser {
    return FollowUser(
        id = this.id,
        username = this.username ?: this.id.take(8),
        displayName = this.name ?: "Unknown",
        avatarUrl = this.avatarUrl,
        isFollowing = false, // Must be updated by the caller if needed
        isCurrentUser = this.id == currentUserId
    )
}

fun PlaylistDto.toPlaylistItem(ownerName: String = "", songCount: Int = 0): PlaylistItem {
    return PlaylistItem(
        id = this.id,
        title = this.title,
        subtitle = ownerName,
        songCountLabel = "$songCount Songs",
        artworkUrl = this.coverImageUrl
    )
}

fun ArtistDto.toArtistItem(): ArtistItem {
    return ArtistItem(
        id = this.id,
        name = this.name,
        avatarUrl = this.avatarUrl,
        monthlyListenersLabel = if (this.monthlyListeners > 0) {
            "%,d".format(this.monthlyListeners)
        } else {
            null
        }
    )
}

fun MessageDto.toChatMessage(currentUserId: String): ChatMessage {
    return ChatMessage(
        id = this.id,
        senderId = this.senderId,
        text = this.content,
        type = when (this.messageType) {
            "song_share" -> ChatMessageType.SongShare
            else -> if (this.sharedSongId != null) ChatMessageType.SongShare else ChatMessageType.Text
        },
        song = null, // Will need resolution by the caller
        status = when (this.status) {
            "sending" -> ChatMessageStatus.Sending
            "sent" -> ChatMessageStatus.Sent
            "delivered" -> ChatMessageStatus.Delivered
            "read" -> ChatMessageStatus.Read
            else -> ChatMessageStatus.Sent
        },
        timeLabel = this.createdAt ?: "",
        isMine = this.senderId == currentUserId
    )
}

// --- Search result mappers ---

fun SongDto.toSearchResultItem(): SearchResultItem {
    return SearchResultItem(
        id = this.id,
        title = this.title,
        subtitle = this.artistName,
        type = SearchFilter.Songs,
        artworkUrl = this.coverImageUrl
    )
}

fun ArtistDto.toSearchResultItem(): SearchResultItem {
    return SearchResultItem(
        id = this.id,
        title = this.name,
        subtitle = if (this.monthlyListeners > 0) "%,d listeners".format(this.monthlyListeners) else "Artist",
        type = SearchFilter.Artists,
        artworkUrl = this.avatarUrl
    )
}

fun PlaylistDto.toSearchResultItem(): SearchResultItem {
    return SearchResultItem(
        id = this.id,
        title = this.title,
        subtitle = this.description ?: "Playlist",
        type = SearchFilter.Playlists,
        artworkUrl = this.coverImageUrl
    )
}

fun UserDto.toSearchResultItem(): SearchResultItem {
    return SearchResultItem(
        id = this.id,
        title = this.name ?: this.username ?: "Unknown",
        subtitle = this.username?.let { "@$it" } ?: "",
        type = SearchFilter.Users,
        artworkUrl = this.avatarUrl
    )
}

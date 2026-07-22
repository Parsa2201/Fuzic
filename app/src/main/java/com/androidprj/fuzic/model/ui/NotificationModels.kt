package com.androidprj.fuzic.model.ui

import androidx.annotation.StringRes
import androidx.paging.PagingData
import com.androidprj.fuzic.R

data class NotificationsUiState(
    val notifications: PagingData<NotificationItem> = PagingData.empty(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

data class NotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val timeLabel: String,
    val type: NotificationType,
    val isRead: Boolean = false,
    val artworkUrl: String? = null,
    val target: NotificationTarget? = null,
)

/**
 * A UI navigation target supplied by notification data. Repository implementations map their
 * transport payloads to this stable model instead of exposing backend route details to Compose.
 */
sealed interface NotificationTarget {
    data class Song(val songId: String) : NotificationTarget
    data class Playlist(val playlistId: String) : NotificationTarget
    data class Artist(val artistId: String) : NotificationTarget
    data class UserProfile(val userId: String) : NotificationTarget
    data class Conversation(
        val conversationId: String,
        val participantId: String,
        val participantUsername: String,
        val participantDisplayName: String,
        val participantAvatarUrl: String? = null,
    ) : NotificationTarget
    data object Premium : NotificationTarget
}

enum class NotificationType(@StringRes val fallbackLabelRes: Int) {
    NewRelease(R.string.notifications_type_new_release),
    Follow(R.string.notifications_type_follow),
    Playlist(R.string.notifications_type_playlist),
    Premium(R.string.notifications_type_premium),
    System(R.string.notifications_type_system),
}

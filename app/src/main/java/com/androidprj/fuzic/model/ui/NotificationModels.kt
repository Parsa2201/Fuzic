package com.androidprj.fuzic.model.ui

import androidx.annotation.StringRes
import com.androidprj.fuzic.R

data class NotificationsUiState(
    val notifications: List<NotificationItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    val isEmpty: Boolean
        get() = !isLoading && errorMessage == null && notifications.isEmpty()

    val unreadCount: Int
        get() = notifications.count { !it.isRead }
}

data class NotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val timeLabel: String,
    val type: NotificationType,
    val isRead: Boolean = false,
    val artworkUrl: String? = null,
)

enum class NotificationType(@StringRes val fallbackLabelRes: Int) {
    NewRelease(R.string.notifications_type_new_release),
    Follow(R.string.notifications_type_follow),
    Playlist(R.string.notifications_type_playlist),
    Premium(R.string.notifications_type_premium),
    System(R.string.notifications_type_system),
}

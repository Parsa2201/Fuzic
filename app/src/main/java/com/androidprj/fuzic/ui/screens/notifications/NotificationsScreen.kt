package com.androidprj.fuzic.ui.screens.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FollowTheSigns
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.androidprj.fuzic.R
import com.androidprj.fuzic.model.ui.NotificationItem
import com.androidprj.fuzic.model.ui.NotificationType
import com.androidprj.fuzic.model.ui.NotificationsUiState
import com.androidprj.fuzic.ui.components.MusicArtwork
import com.androidprj.fuzic.ui.components.ScreenMessage
import com.androidprj.fuzic.ui.components.previewArtworkUri
import com.androidprj.fuzic.ui.theme.FuzicTheme
import com.androidprj.fuzic.ui.theme.spacing
import kotlinx.coroutines.flow.flowOf

@Composable
fun NotificationsRoute(
    uiState: NotificationsUiState,
    onNotificationClick: (NotificationItem) -> Unit,
    onMarkAllReadClick: () -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NotificationsScreen(
        uiState = uiState,
        onNotificationClick = onNotificationClick,
        onMarkAllReadClick = onMarkAllReadClick,
        onRetryClick = onRetryClick,
        modifier = modifier,
    )
}

@Composable
fun NotificationsScreen(
    uiState: NotificationsUiState,
    onNotificationClick: (NotificationItem) -> Unit,
    onMarkAllReadClick: () -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        uiState.isLoading -> NotificationsLoadingContent(modifier)
        uiState.errorMessage != null -> ScreenMessage(
            icon = Icons.Default.ErrorOutline,
            title = stringResource(R.string.notifications_error_title),
            message = uiState.errorMessage,
            action = {
                Button(onClick = onRetryClick) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(MaterialTheme.spacing.small))
                    Text(stringResource(R.string.action_retry))
                }
            },
            modifier = modifier,
        )
        else -> NotificationsContent(
            uiState = uiState,
            onNotificationClick = onNotificationClick,
            onMarkAllReadClick = onMarkAllReadClick,
            modifier = modifier,
        )
    }
}

@Composable
private fun NotificationsContent(
    uiState: NotificationsUiState,
    onNotificationClick: (NotificationItem) -> Unit,
    onMarkAllReadClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val notificationsFlow = remember(uiState.notifications) { flowOf(uiState.notifications) }
    val pagedNotifications = notificationsFlow.collectAsLazyPagingItems()
    val snapshot = pagedNotifications.itemSnapshotList.items
    val unreadCount = snapshot.count { !it.isRead }
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(MaterialTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
    ) {
        item {
            NotificationsHeader(
                unreadCount = unreadCount,
                onMarkAllReadClick = onMarkAllReadClick,
            )
        }
        if (pagedNotifications.itemCount == 0) {
            item {
                ScreenMessage(
                    icon = Icons.Default.Notifications,
                    title = stringResource(R.string.notifications_empty_title),
                    message = stringResource(R.string.notifications_empty_message),
                )
            }
        }
        items(
            count = pagedNotifications.itemCount,
            key = pagedNotifications.itemKey { it.id },
        ) { index ->
            val notification = pagedNotifications[index] ?: return@items
            NotificationRow(
                notification = notification,
                onClick = { onNotificationClick(notification) },
            )
        }
    }
}

@Composable
private fun NotificationsHeader(
    unreadCount: Int,
    onMarkAllReadClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = MaterialTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BadgedBox(
            badge = {
                if (unreadCount > 0) {
                    Badge { Text(unreadCount.toString()) }
                }
            },
        ) {
            Icon(
                Icons.Default.Notifications,
                contentDescription = stringResource(R.string.notifications_title),
            )
        }
        Spacer(Modifier.width(MaterialTheme.spacing.small))
        Text(
            text = stringResource(R.string.notifications_title),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.headlineLarge,
        )
        if (unreadCount > 0) {
            TextButton(onClick = onMarkAllReadClick) {
                Text(stringResource(R.string.notifications_mark_all_read))
            }
        }
    }
}

@Composable
private fun NotificationRow(
    notification: NotificationItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.primaryContainer
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(MaterialTheme.spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
        ) {
            NotificationLeadingIcon(
                notification = notification,
                modifier = Modifier.size(MaterialTheme.spacing.extraLarge),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = notification.timeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!notification.isRead) {
                Box(
                    modifier = Modifier
                        .size(MaterialTheme.spacing.small)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        }
    }
}

@Composable
private fun NotificationLeadingIcon(
    notification: NotificationItem,
    modifier: Modifier = Modifier,
) {
    val icon = when (notification.type) {
        NotificationType.NewRelease -> Icons.Default.LibraryMusic
        NotificationType.Follow -> Icons.AutoMirrored.Filled.FollowTheSigns
        NotificationType.Playlist -> Icons.Default.Favorite
        NotificationType.Premium -> Icons.Default.WorkspacePremium
        NotificationType.System -> Icons.Default.Campaign
    }
    if (notification.artworkUrl != null) {
        MusicArtwork(
            artworkUrl = notification.artworkUrl,
            fallbackIcon = icon,
            contentDescription = notification.title,
            modifier = modifier.clip(CircleShape),
        )
    } else {
        Surface(
            modifier = modifier,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.padding(MaterialTheme.spacing.small))
        }
    }
}

@Composable
private fun NotificationsLoadingContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(MaterialTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
    ) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        repeat(5) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(MaterialTheme.spacing.extraLarge),
                )
            }
        }
    }
}

@Composable
private fun sampleNotificationsState() = NotificationsUiState(
    notifications = androidx.paging.PagingData.from(listOf(
        NotificationItem(
        id = "release",
            title = stringResource(R.string.preview_notification_release_title),
            message = stringResource(R.string.preview_notification_release_message),
            timeLabel = stringResource(R.string.preview_notification_five_minutes),
            type = NotificationType.NewRelease,
            artworkUrl = previewArtworkUri(1),
        ),
        NotificationItem(
            id = "follow",
            title = stringResource(R.string.preview_notification_follow_title),
            message = stringResource(R.string.preview_notification_follow_message),
            timeLabel = stringResource(R.string.preview_notification_yesterday),
            type = NotificationType.Follow,
        ),
        NotificationItem(
            id = "playlist",
            title = stringResource(R.string.preview_notification_playlist_title),
            message = stringResource(R.string.preview_notification_playlist_message),
            timeLabel = stringResource(R.string.preview_notification_two_days),
            type = NotificationType.Playlist,
            isRead = true,
        ),
    )),
)

@Preview(name = "Notifications - English", showBackground = true)
@Composable
private fun NotificationsPreview() {
    FuzicTheme {
        NotificationsScreen(
            uiState = sampleNotificationsState(),
            onNotificationClick = {},
            onMarkAllReadClick = {},
            onRetryClick = {},
        )
    }
}

@Preview(name = "Notifications - Persian", locale = "fa", showBackground = true)
@Composable
private fun NotificationsPersianPreview() {
    FuzicTheme {
        NotificationsScreen(
            uiState = sampleNotificationsState(),
            onNotificationClick = {},
            onMarkAllReadClick = {},
            onRetryClick = {},
        )
    }
}

@Preview(name = "Notifications empty - Persian", locale = "fa", showBackground = true)
@Composable
private fun NotificationsEmptyPreview() {
    FuzicTheme {
        NotificationsScreen(
            uiState = NotificationsUiState(),
            onNotificationClick = {},
            onMarkAllReadClick = {},
            onRetryClick = {},
        )
    }
}

@Preview(name = "Notifications loading - Persian", locale = "fa", showBackground = true)
@Composable
private fun NotificationsLoadingPreview() {
    FuzicTheme {
        NotificationsScreen(
            uiState = NotificationsUiState(isLoading = true),
            onNotificationClick = {},
            onMarkAllReadClick = {},
            onRetryClick = {},
        )
    }
}

@Preview(name = "Notifications error - Persian", locale = "fa", showBackground = true)
@Composable
private fun NotificationsErrorPreview() {
    FuzicTheme {
        NotificationsScreen(
            uiState = NotificationsUiState(
                errorMessage = stringResource(R.string.preview_notifications_error_message),
            ),
            onNotificationClick = {},
            onMarkAllReadClick = {},
            onRetryClick = {},
        )
    }
}

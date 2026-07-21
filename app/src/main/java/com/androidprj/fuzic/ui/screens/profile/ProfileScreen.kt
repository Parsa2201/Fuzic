package com.androidprj.fuzic.ui.screens.profile

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.androidprj.fuzic.R
import com.androidprj.fuzic.model.ui.ProfileEntry
import com.androidprj.fuzic.model.ui.ProfileStat
import com.androidprj.fuzic.model.ui.ProfileStats
import com.androidprj.fuzic.model.ui.ProfileUiState
import com.androidprj.fuzic.model.ui.ProfileUser
import com.androidprj.fuzic.ui.components.MusicArtwork
import com.androidprj.fuzic.ui.components.ScreenMessage
import com.androidprj.fuzic.ui.components.fuzicShimmer
import com.androidprj.fuzic.ui.components.previewArtworkUri
import com.androidprj.fuzic.ui.theme.FuzicTheme
import com.androidprj.fuzic.ui.theme.spacing

@Composable
fun ProfileRoute(
    uiState: ProfileUiState,
    onEditProfileClick: () -> Unit,
    onEntryClick: (ProfileEntry) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ProfileScreen(
        uiState = uiState,
        onEditProfileClick = onEditProfileClick,
        onEntryClick = onEntryClick,
        onRetryClick = onRetryClick,
        modifier = modifier
    )
}

@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    onEditProfileClick: () -> Unit,
    onEntryClick: (ProfileEntry) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        uiState.isLoading -> ProfileLoadingContent(modifier)
        uiState.errorMessage != null -> ScreenMessage(
            icon = Icons.Default.ErrorOutline,
            title = stringResource(R.string.profile_error_title),
            message = uiState.errorMessage,
            action = {
                Button(onClick = onRetryClick) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(MaterialTheme.spacing.small))
                    Text(stringResource(R.string.action_retry))
                }
            },
            modifier = modifier
        )
        uiState.profile == null -> ScreenMessage(
            icon = Icons.Default.Person,
            title = stringResource(R.string.profile_empty_title),
            message = stringResource(R.string.profile_empty_message),
            modifier = modifier
        )
        else -> ProfileContent(
            uiState = uiState,
            onEditProfileClick = onEditProfileClick,
            onEntryClick = onEntryClick,
            modifier = modifier
        )
    }
}

@Composable
private fun ProfileContent(
    uiState: ProfileUiState,
    onEditProfileClick: () -> Unit,
    onEntryClick: (ProfileEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(MaterialTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
    ) {
        item {
            ProfileHeader(
                profile = uiState.profile ?: return@item,
                stats = uiState.stats,
                onEditProfileClick = onEditProfileClick
            )
        }
        items(uiState.entries) { entry ->
            ProfileEntryRow(
                entry = entry,
                onClick = { onEntryClick(entry) }
            )
        }
    }
}

@Composable
private fun ProfileHeader(
    profile: ProfileUser,
    stats: ProfileStats,
    onEditProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
        ) {
            ProfileAvatar(
                avatarUrl = profile.avatarUrl,
                username = profile.username
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = profile.displayName,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = profile.username,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (profile.isPremium) {
                PremiumBadge()
            }
            Button(onClick = onEditProfileClick) {
                Icon(Icons.Default.Edit, contentDescription = null)
                Spacer(Modifier.width(MaterialTheme.spacing.small))
                Text(stringResource(R.string.profile_edit))
            }
            ProfileStatsRow(stats = stats)
        }
    }
}

@Composable
private fun ProfileAvatar(
    avatarUrl: String?,
    username: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(ProfileSizes.AvatarSize)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        MusicArtwork(
            artworkUrl = avatarUrl,
            fallbackIcon = Icons.Default.Person,
            contentDescription = username,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun PremiumBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = MaterialTheme.spacing.medium,
                vertical = MaterialTheme.spacing.small
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            Icon(Icons.Default.WorkspacePremium, contentDescription = null)
            Text(
                text = stringResource(R.string.profile_premium_badge),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun ProfileStatsRow(
    stats: ProfileStats,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ProfileStatItem(value = stats.followersLabel, stat = ProfileStat.Followers)
        ProfileStatItem(value = stats.followingLabel, stat = ProfileStat.Following)
        ProfileStatItem(value = stats.playlistsLabel, stat = ProfileStat.Playlists)
    }
}

@Composable
private fun ProfileStatItem(
    value: String,
    stat: ProfileStat,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(stat.labelRes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProfileEntryRow(
    entry: ProfileEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
        ) {
            Icon(
                imageVector = entry.icon,
                contentDescription = null,
                tint = if (entry == ProfileEntry.Logout) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
            Text(
                text = stringResource(entry.labelRes),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                color = if (entry == ProfileEntry.Logout) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

@Composable
private fun ProfileLoadingContent(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(MaterialTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
            ) {
                Box(
                    modifier = Modifier
                        .size(ProfileSizes.AvatarSize)
                        .fuzicShimmer(CircleShape)
                )
                Box(
                    modifier = Modifier
                        .width(ProfileSizes.TitleSkeletonWidth)
                        .height(ProfileSizes.TextSkeletonHeight)
                        .fuzicShimmer(MaterialTheme.shapes.small)
                )
                Box(
                    modifier = Modifier
                        .width(ProfileSizes.SubtitleSkeletonWidth)
                        .height(ProfileSizes.TextSkeletonHeight)
                        .fuzicShimmer(MaterialTheme.shapes.small)
                )
            }
        }
        items(5) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ProfileSizes.RowSkeletonHeight)
                    .fuzicShimmer(MaterialTheme.shapes.medium)
            )
        }
    }
}

private val ProfileEntry.icon: ImageVector
    get() = when (this) {
        ProfileEntry.Followers -> Icons.Default.Groups
        ProfileEntry.Following -> Icons.AutoMirrored.Filled.TrendingUp
        ProfileEntry.LikedSongs -> Icons.Default.Favorite
        ProfileEntry.RecentlyPlayed -> Icons.Default.History
        ProfileEntry.Settings -> Icons.Default.Settings
        ProfileEntry.Chat -> Icons.AutoMirrored.Filled.Message
        ProfileEntry.Logout -> Icons.AutoMirrored.Filled.Logout
    }

private object ProfileSizes {
    val AvatarSize = 104.dp
    val TitleSkeletonWidth = 180.dp
    val SubtitleSkeletonWidth = 120.dp
    val TextSkeletonHeight = 16.dp
    val RowSkeletonHeight = 56.dp
}

@Preview(name = "Profile content - English", showBackground = true)
@Composable
private fun ProfileScreenContentPreview() {
    FuzicTheme {
        ProfilePreviewState(uiState = sampleProfileUiState())
    }
}

@Preview(name = "Profile content - Persian", locale = "fa", showBackground = true)
@Composable
private fun ProfileScreenContentPersianPreview() {
    FuzicTheme {
        ProfilePreviewState(uiState = sampleProfileUiState())
    }
}

@Preview(name = "Profile free user - Persian", locale = "fa", showBackground = true)
@Composable
private fun ProfileScreenFreePreview() {
    FuzicTheme {
        ProfilePreviewState(
            uiState = sampleProfileUiState().copy(
                profile = sampleProfileUser().copy(isPremium = false)
            )
        )
    }
}

@Preview(name = "Profile loading - Persian", locale = "fa", showBackground = true)
@Composable
private fun ProfileScreenLoadingPreview() {
    FuzicTheme {
        ProfilePreviewState(uiState = ProfileUiState(isLoading = true))
    }
}

@Preview(name = "Profile empty - Persian", locale = "fa", showBackground = true)
@Composable
private fun ProfileScreenEmptyPreview() {
    FuzicTheme {
        ProfilePreviewState(uiState = ProfileUiState())
    }
}

@Preview(name = "Profile error - Persian", locale = "fa", showBackground = true)
@Composable
private fun ProfileScreenErrorPreview() {
    FuzicTheme {
        ProfilePreviewState(
            uiState = ProfileUiState(errorMessage = stringResource(R.string.profile_error_message))
        )
    }
}

@Preview(name = "Profile header", showBackground = true)
@Composable
private fun ProfileHeaderPreview() {
    FuzicTheme {
        ProfileHeader(
            profile = sampleProfileUser(),
            stats = sampleProfileStats(),
            onEditProfileClick = { },
            modifier = Modifier.padding(MaterialTheme.spacing.medium)
        )
    }
}

@Preview(name = "Profile avatar", showBackground = true)
@Composable
private fun ProfileAvatarPreview() {
    FuzicTheme {
        ProfileAvatar(
            avatarUrl = previewArtworkUri(R.drawable.preview_artwork_pulse),
            username = stringResource(R.string.preview_user_parsa),
            modifier = Modifier.padding(MaterialTheme.spacing.medium)
        )
    }
}

@Preview(name = "Premium badge", showBackground = true)
@Composable
private fun PremiumBadgePreview() {
    FuzicTheme {
        PremiumBadge(modifier = Modifier.padding(MaterialTheme.spacing.medium))
    }
}

@Preview(name = "Profile stats row", showBackground = true)
@Composable
private fun ProfileStatsRowPreview() {
    FuzicTheme {
        ProfileStatsRow(
            stats = sampleProfileStats(),
            modifier = Modifier.padding(MaterialTheme.spacing.medium)
        )
    }
}

@Preview(name = "Profile entry row", showBackground = true)
@Composable
private fun ProfileEntryRowPreview() {
    FuzicTheme {
        var selectedEntry by remember { mutableStateOf(ProfileEntry.Settings) }
        ProfileEntryRow(
            entry = ProfileEntry.Settings,
            onClick = { selectedEntry = ProfileEntry.Settings },
            modifier = Modifier.padding(MaterialTheme.spacing.medium)
        )
    }
}

@Preview(name = "Profile loading content", showBackground = true)
@Composable
private fun ProfileLoadingContentPreview() {
    FuzicTheme {
        ProfileLoadingContent()
    }
}

@Composable
private fun ProfilePreviewState(uiState: ProfileUiState) {
    var state by remember { mutableStateOf(uiState) }
    var selectedEntry by remember { mutableStateOf(ProfileEntry.Settings) }
    ProfileScreen(
        uiState = state,
        onEditProfileClick = { selectedEntry = ProfileEntry.Settings },
        onEntryClick = { selectedEntry = it },
        onRetryClick = { state = state.copy(errorMessage = null) }
    )
}

@Composable
private fun sampleProfileUiState() = ProfileUiState(
    profile = sampleProfileUser(),
    stats = sampleProfileStats()
)

@Composable
private fun sampleProfileUser() = ProfileUser(
    id = "profile-1",
    username = stringResource(R.string.preview_profile_username),
    displayName = stringResource(R.string.preview_profile_display_name),
    avatarUrl = previewArtworkUri(R.drawable.preview_artwork_pulse),
    isPremium = true
)

@Composable
private fun sampleProfileStats() = ProfileStats(
    followersLabel = stringResource(R.string.preview_profile_followers_count),
    followingLabel = stringResource(R.string.preview_profile_following_count),
    playlistsLabel = stringResource(R.string.preview_profile_playlists_count)
)

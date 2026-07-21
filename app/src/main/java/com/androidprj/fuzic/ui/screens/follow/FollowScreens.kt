package com.androidprj.fuzic.ui.screens.follow

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.androidprj.fuzic.R
import com.androidprj.fuzic.model.FollowListType
import com.androidprj.fuzic.model.FollowListUiState
import com.androidprj.fuzic.model.FollowSearchUiState
import com.androidprj.fuzic.model.FollowUser
import com.androidprj.fuzic.ui.components.DetailTopAppBar
import com.androidprj.fuzic.ui.components.MusicArtwork
import com.androidprj.fuzic.ui.components.ScreenMessage
import com.androidprj.fuzic.ui.components.previewArtworkUri
import com.androidprj.fuzic.ui.theme.FuzicTheme
import com.androidprj.fuzic.ui.theme.spacing

@Composable
fun FollowSearchRoute(
    uiState: FollowSearchUiState,
    onBackClick: () -> Unit,
    onQueryChange: (String) -> Unit,
    onUserClick: (FollowUser) -> Unit,
    onFollowClick: (FollowUser) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FollowSearchScreen(uiState, onBackClick, onQueryChange, onUserClick, onFollowClick, onRetryClick, modifier)
}

@Composable
fun FollowSearchScreen(
    uiState: FollowSearchUiState,
    onBackClick: () -> Unit,
    onQueryChange: (String) -> Unit,
    onUserClick: (FollowUser) -> Unit,
    onFollowClick: (FollowUser) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) {
        DetailTopAppBar(stringResource(R.string.follow_search_title), onBackClick)
        OutlinedTextField(
            value = uiState.query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth().padding(MaterialTheme.spacing.medium),
            label = { Text(stringResource(R.string.follow_search_label)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
        )
        when {
            uiState.isLoading -> FollowLoading()
            uiState.errorMessage != null -> FollowMessage(
                Icons.Default.ErrorOutline, stringResource(R.string.follow_error_title),
                uiState.errorMessage, onRetryClick,
            )
            uiState.query.isBlank() -> FollowMessage(
                Icons.Default.Groups, stringResource(R.string.follow_search_idle_title),
                stringResource(R.string.follow_search_idle_message),
            )
            uiState.isEmpty -> FollowMessage(
                Icons.Default.Search, stringResource(R.string.follow_search_empty_title),
                stringResource(R.string.follow_search_empty_message),
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.medium),
            ) {
                items(uiState.results, key = { it.id }) { user ->
                    FollowUserRow(user, { onUserClick(user) }, { onFollowClick(user) })
                }
            }
        }
    }
}

@Composable
fun FollowListRoute(
    uiState: FollowListUiState,
    onBackClick: () -> Unit,
    onUserClick: (FollowUser) -> Unit,
    onFollowClick: (FollowUser) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FollowListScreen(uiState, onBackClick, onUserClick, onFollowClick, onRetryClick, modifier)
}

@Composable
fun FollowListScreen(
    uiState: FollowListUiState,
    onBackClick: () -> Unit,
    onUserClick: (FollowUser) -> Unit,
    onFollowClick: (FollowUser) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = stringResource(
        if (uiState.type == FollowListType.Followers) R.string.follow_followers_title
        else R.string.follow_following_title,
    )
    val emptyMessage = stringResource(
        if (uiState.type == FollowListType.Followers) R.string.follow_list_empty_followers
        else R.string.follow_list_empty_following,
    )
    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        DetailTopAppBar(title, onBackClick)
        when {
            uiState.isLoading -> FollowLoading()
            uiState.errorMessage != null -> FollowMessage(
                Icons.Default.ErrorOutline, stringResource(R.string.follow_error_title),
                uiState.errorMessage, onRetryClick,
            )
            uiState.isEmpty -> FollowMessage(Icons.Default.Groups, title, emptyMessage)
            else -> LazyColumn(
                contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.medium),
            ) {
                items(uiState.users, key = { it.id }) { user ->
                    FollowUserRow(user, { onUserClick(user) }, { onFollowClick(user) })
                }
            }
        }
    }
}

@Composable
private fun FollowUserRow(
    user: FollowUser,
    onClick: () -> Unit,
    onFollowClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = MaterialTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
    ) {
        MusicArtwork(
            artworkUrl = user.avatarUrl,
            fallbackIcon = Icons.Default.Person,
            contentDescription = user.displayName,
            modifier = Modifier.size(52.dp).clip(CircleShape),
        )
        Column(Modifier.weight(1f)) {
            Text(user.displayName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("@${user.username}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        when {
            user.isCurrentUser -> Text(stringResource(R.string.follow_self_label), style = MaterialTheme.typography.labelMedium)
            else -> FilledTonalButton(onClick = onFollowClick) {
                Text(stringResource(if (user.isFollowing) R.string.following_button else R.string.follow_button))
            }
        }
    }
}

@Composable
private fun FollowLoading() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(MaterialTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
    ) {
        repeat(5) {
            Spacer(Modifier.fillMaxWidth().size(width = 1.dp, height = 64.dp).background(MaterialTheme.colorScheme.surfaceVariant))
        }
    }
}

@Composable
private fun FollowMessage(
    icon: ImageVector,
    title: String,
    message: String,
    onRetryClick: (() -> Unit)? = null,
) {
    ScreenMessage(
        icon = icon,
        title = title,
        message = message,
        action = onRetryClick?.let {
            {
                Button(onClick = it) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Text(stringResource(R.string.action_retry))
                }
            }
        },
    )
}

@Preview(name = "Follow search", showBackground = true)
@Composable
private fun FollowSearchPreview() {
    FuzicTheme {
        FollowSearchScreen(
            sampleSearchState(),
            {}, {}, {}, {}, {},
        )
    }
}

@Preview(name = "Follow search Persian empty", locale = "fa", showBackground = true)
@Composable
private fun FollowSearchEmptyPreview() {
    FuzicTheme {
        FollowSearchScreen(FollowSearchUiState(query = "unknown"), {}, {}, {}, {}, {})
    }
}

@Preview(name = "Follow search idle Persian", locale = "fa", showBackground = true)
@Composable
private fun FollowSearchIdlePreview() {
    FuzicTheme {
        FollowSearchScreen(FollowSearchUiState(), {}, {}, {}, {}, {})
    }
}

@Preview(name = "Follow search error Persian", locale = "fa", showBackground = true)
@Composable
private fun FollowSearchErrorPreview() {
    FuzicTheme {
        FollowSearchScreen(
            FollowSearchUiState(errorMessage = stringResource(R.string.follow_error_title)),
            {}, {}, {}, {}, {},
        )
    }
}

@Preview(name = "Followers Persian", locale = "fa", showBackground = true)
@Composable
private fun FollowersPreview() {
    FuzicTheme {
        FollowListScreen(
            FollowListUiState(FollowListType.Followers, sampleUsers()),
            {}, {}, {}, {},
        )
    }
}

@Preview(name = "Following empty Persian", locale = "fa", showBackground = true)
@Composable
private fun FollowingEmptyPreview() {
    FuzicTheme {
        FollowListScreen(FollowListUiState(FollowListType.Following), {}, {}, {}, {})
    }
}

@Preview(name = "Following loading", showBackground = true)
@Composable
private fun FollowingLoadingPreview() {
    FuzicTheme {
        FollowListScreen(FollowListUiState(FollowListType.Following, isLoading = true), {}, {}, {}, {})
    }
}

@Preview(name = "Followers error Persian", locale = "fa", showBackground = true)
@Composable
private fun FollowersErrorPreview() {
    FuzicTheme {
        FollowListScreen(
            FollowListUiState(
                type = FollowListType.Followers,
                errorMessage = stringResource(R.string.follow_error_title),
            ),
            {}, {}, {}, {},
        )
    }
}

@Composable
private fun sampleSearchState() = FollowSearchUiState(
    query = "raha",
    results = sampleUsers(),
)

@Composable
private fun sampleUsers() = listOf(
    FollowUser(
        id = "user-raha",
        username = "raha_band",
        displayName = stringResource(R.string.preview_artist_raha_band),
        avatarUrl = previewArtworkUri(R.drawable.preview_artwork_tehran),
        isFollowing = true,
    ),
    FollowUser(
        id = "user-parsa",
        username = "parsa",
        displayName = stringResource(R.string.preview_profile_display_name),
        avatarUrl = previewArtworkUri(R.drawable.preview_artwork_pulse),
        isCurrentUser = true,
    ),
)

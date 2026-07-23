package com.androidprj.fuzic.ui.screens.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidprj.fuzic.R
import com.androidprj.fuzic.di.IoDispatcher
import com.androidprj.fuzic.model.ui.ProfileUser
import com.androidprj.fuzic.model.ui.PlaylistItem
import com.androidprj.fuzic.repository.AuthRepository
import com.androidprj.fuzic.repository.FollowRepository
import com.androidprj.fuzic.repository.PlaylistRepository
import com.androidprj.fuzic.repository.UserRepository
import com.androidprj.fuzic.ui.components.DetailTopAppBar
import com.androidprj.fuzic.ui.components.MusicArtwork
import com.androidprj.fuzic.ui.components.ScreenMessage
import com.androidprj.fuzic.ui.components.fuzicShimmer
import com.androidprj.fuzic.ui.theme.spacing
import com.androidprj.fuzic.ui.theme.FuzicTheme
import com.androidprj.fuzic.util.StringProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UserProfileUiState(
    val user: ProfileUser? = null,
    val publicPlaylists: List<PlaylistItem> = emptyList(),
    val isFollowing: Boolean = false,
    val isFollowActionLoading: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val followRepository: FollowRepository,
    private val playlistRepository: PlaylistRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val stringProvider: StringProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()
    private var userId: String? = null

    fun load(id: String) {
        userId = id
        viewModelScope.launch {
            _uiState.value = UserProfileUiState()
            val result = withContext(ioDispatcher) { userRepository.getUserProfile(id) }
            _uiState.value = result.fold(
                onSuccess = { user ->
                    val playlists = withContext(ioDispatcher) { playlistRepository.getUserPlaylists(id) }.getOrDefault(emptyList())
                    val currentUserId = withContext(ioDispatcher) { authRepository.getCurrentUserId() }
                    val isFollowing = currentUserId
                        ?.let { withContext(ioDispatcher) { followRepository.getFollowing(it) } }
                        ?.getOrDefault(emptyList())
                        ?.any { it.id == id }
                        ?: false
                    UserProfileUiState(
                        user = user,
                        publicPlaylists = playlists,
                        isFollowing = isFollowing,
                        isLoading = false,
                    )
                },
                onFailure = { UserProfileUiState(isLoading = false, errorMessage = it.message ?: stringProvider.get(R.string.user_profile_error)) },
            )
        }
    }

    fun retry() { userId?.let(::load) }

    fun toggleFollow() {
        val user = _uiState.value.user ?: return
        if (_uiState.value.isFollowActionLoading) return
        val wasFollowing = _uiState.value.isFollowing
        _uiState.value = _uiState.value.copy(
            isFollowing = !wasFollowing,
            isFollowActionLoading = true,
            errorMessage = null,
        )
        viewModelScope.launch {
            val result = withContext(ioDispatcher) {
                if (wasFollowing) followRepository.unfollowUser(user.id)
                else followRepository.followUser(user.id)
            }
            _uiState.value = if (result.isSuccess) {
                _uiState.value.copy(isFollowActionLoading = false)
            } else {
                _uiState.value.copy(
                    isFollowing = wasFollowing,
                    isFollowActionLoading = false,
                    errorMessage = result.exceptionOrNull()?.message
                        ?: stringProvider.get(R.string.user_profile_error),
                )
            }
        }
    }
}

@Preview(name = "User profile - English", showBackground = true)
@Composable
private fun UserProfileEnglishPreview() {
    FuzicTheme {
        UserProfileScreen(
            uiState = UserProfileUiState(
                user = ProfileUser(
                    id = "user-preview",
                    username = "parsa",
                    displayName = "Parsa",
                    isPremium = true,
                ),
                isLoading = false,
            ),
            onBackClick = {},
            onRetryClick = {},
            onPlaylistClick = {},
            onChatClick = {},
            onFollowClick = {},
        )
    }
}

@Preview(name = "User profile error - Persian", locale = "fa", showBackground = true)
@Composable
private fun UserProfileErrorPersianPreview() {
    FuzicTheme {
        UserProfileScreen(
            uiState = UserProfileUiState(isLoading = false, errorMessage = "Profile could not be loaded."),
            onBackClick = {},
            onRetryClick = {},
            onPlaylistClick = {},
            onChatClick = {},
            onFollowClick = {},
        )
    }
}

@Preview(name = "User profile loading - Persian", locale = "fa", showBackground = true)
@Composable
private fun UserProfileLoadingPreview() {
    FuzicTheme {
        UserProfileScreen(UserProfileUiState(), onBackClick = {}, onRetryClick = {}, onPlaylistClick = {}, onChatClick = {}, onFollowClick = {})
    }
}

@Preview(name = "User profile empty - Persian", locale = "fa", showBackground = true)
@Composable
private fun UserProfileEmptyPreview() {
    FuzicTheme {
        UserProfileScreen(
            uiState = UserProfileUiState(isLoading = false),
            onBackClick = {},
            onRetryClick = {},
            onPlaylistClick = {},
            onChatClick = {},
            onFollowClick = {},
        )
    }
}

@Composable
fun UserProfileScreen(
    uiState: UserProfileUiState,
    onBackClick: () -> Unit,
    onRetryClick: () -> Unit,
    onPlaylistClick: (PlaylistItem) -> Unit,
    onChatClick: (ProfileUser) -> Unit,
    onFollowClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        DetailTopAppBar(title = stringResource(R.string.user_profile_title), onBackClick = onBackClick)
        when {
            uiState.isLoading -> UserProfileLoadingContent()
            uiState.errorMessage != null -> ScreenMessage(
                icon = Icons.Default.ErrorOutline,
                title = stringResource(R.string.user_profile_title),
                message = uiState.errorMessage,
                action = { Button(onClick = onRetryClick) { Text(stringResource(R.string.action_retry)) } },
            )
            uiState.user != null -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(MaterialTheme.spacing.large),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
            ) {
                item {
                    MusicArtwork(
                        artworkUrl = uiState.user.avatarUrl,
                        fallbackIcon = Icons.Default.Person,
                        contentDescription = uiState.user.displayName,
                        modifier = Modifier.size(MaterialTheme.spacing.extraLarge * 6).clip(CircleShape),
                    )
                }
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(uiState.user.displayName, style = MaterialTheme.typography.headlineMedium)
                        Text(
                            "@${uiState.user.username}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
                    ) {
                        FilledTonalButton(
                            onClick = onFollowClick,
                            enabled = !uiState.isFollowActionLoading,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                stringResource(
                                    if (uiState.isFollowing) R.string.action_unfollow else R.string.action_follow,
                                ),
                            )
                        }
                        OutlinedButton(
                            onClick = { onChatClick(uiState.user) },
                            modifier = Modifier.weight(1f),
                        ) {
                            androidx.compose.material3.Icon(
                                Icons.Default.ChatBubbleOutline,
                                contentDescription = null,
                            )
                            Text(stringResource(R.string.profile_entry_chat))
                        }
                    }
                }
                if (uiState.publicPlaylists.isNotEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.user_profile_public_playlists),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    items(uiState.publicPlaylists.size) { index ->
                        val playlist = uiState.publicPlaylists[index]
                        Card(
                            onClick = { onPlaylistClick(playlist) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(Modifier.padding(MaterialTheme.spacing.medium)) {
                                Text(playlist.title, style = MaterialTheme.typography.titleMedium)
                                Text(playlist.songCountLabel, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
            else -> ScreenMessage(
                icon = Icons.Default.ErrorOutline,
                title = stringResource(R.string.profile_empty_title),
                message = stringResource(R.string.profile_empty_message),
            )
        }
    }
}

@Composable
private fun UserProfileLoadingContent(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(MaterialTheme.spacing.large),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
    ) {
        item {
            Box(
                modifier = Modifier
                    .size(MaterialTheme.spacing.extraLarge * 6)
                    .fuzicShimmer(CircleShape),
            )
        }
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            ) {
                Box(
                    modifier = Modifier
                        .width(MaterialTheme.spacing.extraLarge * 4)
                        .height(MaterialTheme.spacing.medium)
                        .fuzicShimmer(MaterialTheme.shapes.small),
                )
                Box(
                    modifier = Modifier
                        .width(MaterialTheme.spacing.extraLarge * 3)
                        .height(MaterialTheme.spacing.small)
                        .fuzicShimmer(MaterialTheme.shapes.small),
                )
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(MaterialTheme.spacing.large)
                        .fuzicShimmer(MaterialTheme.shapes.medium),
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(MaterialTheme.spacing.large)
                        .fuzicShimmer(MaterialTheme.shapes.medium),
                )
            }
        }
    }
}

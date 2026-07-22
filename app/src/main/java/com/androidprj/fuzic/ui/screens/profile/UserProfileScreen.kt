package com.androidprj.fuzic.ui.screens.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidprj.fuzic.R
import com.androidprj.fuzic.di.IoDispatcher
import com.androidprj.fuzic.model.ui.ProfileUser
import com.androidprj.fuzic.model.ui.PlaylistItem
import com.androidprj.fuzic.repository.PlaylistRepository
import com.androidprj.fuzic.repository.UserRepository
import com.androidprj.fuzic.ui.components.DetailTopAppBar
import com.androidprj.fuzic.ui.components.ScreenMessage
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
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
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
                    UserProfileUiState(user = user, publicPlaylists = playlists, isLoading = false)
                },
                onFailure = { UserProfileUiState(isLoading = false, errorMessage = it.message ?: stringProvider.get(R.string.user_profile_error)) },
            )
        }
    }

    fun retry() { userId?.let(::load) }
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
        )
    }
}

@Preview(name = "User profile loading - Persian", locale = "fa", showBackground = true)
@Composable
private fun UserProfileLoadingPreview() {
    FuzicTheme {
        UserProfileScreen(UserProfileUiState(), onBackClick = {}, onRetryClick = {}, onPlaylistClick = {})
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
        )
    }
}

@Composable
fun UserProfileScreen(
    uiState: UserProfileUiState,
    onBackClick: () -> Unit,
    onRetryClick: () -> Unit,
    onPlaylistClick: (PlaylistItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        DetailTopAppBar(title = stringResource(R.string.user_profile_title), onBackClick = onBackClick)
        when {
            uiState.isLoading -> CircularProgressIndicator(Modifier.padding(MaterialTheme.spacing.large))
            uiState.errorMessage != null -> ScreenMessage(
                icon = Icons.Default.ErrorOutline,
                title = stringResource(R.string.user_profile_title),
                message = uiState.errorMessage,
                action = { Button(onClick = onRetryClick) { Text(stringResource(R.string.action_retry)) } },
            )
            uiState.user != null -> Column(
                Modifier.padding(MaterialTheme.spacing.large),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
            ) {
                Text(uiState.user.displayName, style = MaterialTheme.typography.headlineMedium)
                Text(uiState.user.username, style = MaterialTheme.typography.bodyLarge)
                if (uiState.publicPlaylists.isNotEmpty()) {
                    Text(stringResource(R.string.user_profile_public_playlists), style = MaterialTheme.typography.titleLarge)
                    uiState.publicPlaylists.forEach { playlist ->
                        Card(onClick = { onPlaylistClick(playlist) }) {
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

package com.androidprj.fuzic.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidprj.fuzic.model.ui.ProfileStats
import com.androidprj.fuzic.di.IoDispatcher
import com.androidprj.fuzic.R
import com.androidprj.fuzic.util.StringProvider
import com.androidprj.fuzic.model.ui.ProfileUiState
import com.androidprj.fuzic.repository.AuthRepository
import com.androidprj.fuzic.repository.FollowRepository
import com.androidprj.fuzic.repository.PlaylistRepository
import com.androidprj.fuzic.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val followRepository: FollowRepository,
    private val playlistRepository: PlaylistRepository,
    @IoDispatcher private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher,
    private val stringProvider: StringProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState(isLoading = true)
            val userId = withContext(ioDispatcher) { authRepository.getCurrentUserId() }
            if (userId == null) {
                _uiState.value = ProfileUiState()
                return@launch
            }
            val result = withContext(ioDispatcher) {
                runCatching {
                    val profile = userRepository.getUserProfile(userId).getOrThrow()
                    val followers = followRepository.observeFollowersCount(userId).first()
                    val following = followRepository.observeFollowingCount(userId).first()
                    val playlists = playlistRepository.getUserPlaylists(userId).getOrThrow().size
                    profile to ProfileStats(
                        followersLabel = followers.toString(),
                        followingLabel = following.toString(),
                        playlistsLabel = playlists.toString(),
                    )
                }
            }
            _uiState.value = result.fold(
                onSuccess = { (profile, stats) -> ProfileUiState(profile = profile, stats = stats) },
                onFailure = {
                    ProfileUiState(
                        errorMessage = it.message ?: stringProvider.get(R.string.profile_error_message),
                    )
                },
            )
        }
    }
}

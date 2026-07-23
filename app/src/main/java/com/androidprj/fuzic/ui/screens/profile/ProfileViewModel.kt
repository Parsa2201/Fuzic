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
import com.androidprj.fuzic.util.toUserFriendlyMessage
import kotlinx.coroutines.flow.update

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
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val userId = withContext(ioDispatcher) { authRepository.getCurrentUserId() }
            if (userId == null) {
                _uiState.update { ProfileUiState() }
                return@launch
            }
            
            // 1. Fetch and show profile immediately (works offline if cached)
            val profileResult = withContext(ioDispatcher) { userRepository.getUserProfile(userId) }
            val profile = profileResult.getOrNull()
            
            if (profile != null) {
                _uiState.update { it.copy(profile = profile, isLoading = false, errorMessage = null) }
                
                // 2. Fetch stats (might fail offline)
                withContext(ioDispatcher) {
                    runCatching {
                        val followers = followRepository.observeFollowersCount(userId).first()
                        val following = followRepository.observeFollowingCount(userId).first()
                        val playlists = playlistRepository.getUserPlaylists(userId).getOrThrow().size
                        ProfileStats(
                            followersLabel = followers.toString(),
                            followingLabel = following.toString(),
                            playlistsLabel = playlists.toString(),
                        )
                    }.onSuccess { stats ->
                        _uiState.update { it.copy(stats = stats) }
                    }.onFailure { e ->
                        _uiState.update { 
                            it.copy(errorMessage = e.toUserFriendlyMessage(stringProvider, R.string.profile_error_message)) 
                        }
                    }
                }
            } else {
                // Profile fetch failed (not cached and offline)
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = profileResult.exceptionOrNull()?.toUserFriendlyMessage(stringProvider, R.string.profile_error_message) 
                            ?: stringProvider.get(R.string.profile_error_message)
                    )
                }
            }
        }
    }
}

package com.androidprj.fuzic.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidprj.fuzic.model.ui.ProfileUser
import com.androidprj.fuzic.repository.AuthRepository
import com.androidprj.fuzic.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.ExperimentalCoroutinesApi

sealed interface SessionUiState {
    data object Restoring : SessionUiState
    data class Ready(val currentUser: ProfileUser?) : SessionUiState
}

@HiltViewModel
class SessionViewModel @Inject constructor(
    authRepository: AuthRepository,
    private val userRepository: UserRepository,
) : ViewModel() {
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<SessionUiState> = authRepository.getCurrentUserFlow()
        .mapLatest { user ->
            if (user != null) {
                userRepository.getUserProfile(user.id).getOrNull() ?: user
            } else {
                null
            }
        }
        .map(SessionUiState::Ready)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionUiState.Restoring)
}

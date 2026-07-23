package com.androidprj.fuzic.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidprj.fuzic.model.ui.ProfileUser
import com.androidprj.fuzic.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface SessionUiState {
    data object Restoring : SessionUiState
    data class Ready(val currentUser: ProfileUser?) : SessionUiState
}

@HiltViewModel
class SessionViewModel @Inject constructor(
    authRepository: AuthRepository,
) : ViewModel() {
    val uiState: StateFlow<SessionUiState> = authRepository.getCurrentUserFlow()
        .map(SessionUiState::Ready)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionUiState.Restoring)
}

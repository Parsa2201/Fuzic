package com.androidprj.fuzic.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidprj.fuzic.R
import com.androidprj.fuzic.di.IoDispatcher
import com.androidprj.fuzic.model.ui.ProfileUser
import com.androidprj.fuzic.repository.AuthRepository
import com.androidprj.fuzic.repository.UserRepository
import com.androidprj.fuzic.util.StringProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ProfileEditorUiState(
    val profile: ProfileUser? = null,
    val displayName: String = "",
    val username: String = "",
    val avatarUrl: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val isSaved: Boolean = false,
)

sealed interface ProfileEditorIntent {
    data object Retry : ProfileEditorIntent
    data class DisplayNameChanged(val value: String) : ProfileEditorIntent
    data class UsernameChanged(val value: String) : ProfileEditorIntent
    data class AvatarUrlChanged(val value: String) : ProfileEditorIntent
    data object Save : ProfileEditorIntent
}

@HiltViewModel
class ProfileEditorViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val stringProvider: StringProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileEditorUiState())
    val uiState: StateFlow<ProfileEditorUiState> = _uiState.asStateFlow()

    init { load() }

    fun onIntent(intent: ProfileEditorIntent) {
        when (intent) {
            ProfileEditorIntent.Retry -> load()
            is ProfileEditorIntent.DisplayNameChanged -> _uiState.update { it.copy(displayName = intent.value, isSaved = false) }
            is ProfileEditorIntent.UsernameChanged -> _uiState.update { it.copy(username = intent.value, isSaved = false) }
            is ProfileEditorIntent.AvatarUrlChanged -> _uiState.update { it.copy(avatarUrl = intent.value, isSaved = false) }
            ProfileEditorIntent.Save -> save()
        }
    }

    private fun load() = viewModelScope.launch {
        _uiState.value = ProfileEditorUiState()
        val userId = withContext(ioDispatcher) { authRepository.getCurrentUserId() }
        if (userId == null) {
            _uiState.value = ProfileEditorUiState(isLoading = false, errorMessage = stringProvider.get(R.string.auth_error_message))
            return@launch
        }
        val result = withContext(ioDispatcher) { userRepository.getUserProfile(userId) }
        _uiState.value = result.fold(
            onSuccess = { profile -> ProfileEditorUiState(profile, profile.displayName, profile.username, profile.avatarUrl.orEmpty(), isLoading = false) },
            onFailure = { ProfileEditorUiState(isLoading = false, errorMessage = it.message ?: stringProvider.get(R.string.edit_profile_error)) },
        )
    }

    private fun save() {
        val profile = _uiState.value.profile ?: return
        val name = _uiState.value.displayName.trim()
        val username = _uiState.value.username.trim()
        if (name.isBlank() || username.isBlank()) {
            _uiState.update { it.copy(errorMessage = stringProvider.get(R.string.edit_profile_error)) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val updated = profile.copy(displayName = name, username = username, avatarUrl = _uiState.value.avatarUrl.trim().ifBlank { null })
            val result = withContext(ioDispatcher) { userRepository.updateProfile(updated) }
            _uiState.update { state -> result.fold(
                onSuccess = { state.copy(profile = it, isSaving = false, isSaved = true) },
                onFailure = { state.copy(isSaving = false, errorMessage = it.message ?: stringProvider.get(R.string.edit_profile_error)) },
            ) }
        }
    }
}

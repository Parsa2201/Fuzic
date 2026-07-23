package com.androidprj.fuzic.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidprj.fuzic.R
import com.androidprj.fuzic.di.IoDispatcher
import com.androidprj.fuzic.model.ui.AvatarEditorState
import com.androidprj.fuzic.model.ui.AvatarUploadRequest
import com.androidprj.fuzic.model.ui.ProfileUser
import com.androidprj.fuzic.repository.AuthRepository
import com.androidprj.fuzic.repository.UserRepository
import com.androidprj.fuzic.util.StringProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
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
    val avatar: AvatarEditorState = AvatarEditorState.None,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val isSaved: Boolean = false,
    val showDiscardConfirmation: Boolean = false,
    val shouldNavigateBack: Boolean = false,
) {
    val isAvatarBusy: Boolean
        get() = avatar is AvatarEditorState.Uploading ||
            (avatar is AvatarEditorState.Removing && avatar.url != profile?.avatarUrl)

    val displayedAvatarUrl: String?
        get() = when (val value = avatar) {
            is AvatarEditorState.Existing -> value.url
            is AvatarEditorState.Uploading -> value.localUri
            is AvatarEditorState.Uploaded -> value.url
            is AvatarEditorState.Removing,
            AvatarEditorState.None -> null
        }

    val canSave: Boolean
        get() = !isLoading && !isSaving && !isAvatarBusy && hasUnsavedChanges

    val hasUnsavedChanges: Boolean
        get() {
            val original = profile ?: return false
            if (displayName != original.displayName || username != original.username) return true
            return when (val value = avatar) {
                AvatarEditorState.None -> original.avatarUrl != null
                is AvatarEditorState.Existing -> value.url != original.avatarUrl
                is AvatarEditorState.Uploading,
                is AvatarEditorState.Uploaded,
                is AvatarEditorState.Removing -> true
            }
        }
}

sealed interface ProfileEditorIntent {
    data object Retry : ProfileEditorIntent
    data class DisplayNameChanged(val value: String) : ProfileEditorIntent
    data class UsernameChanged(val value: String) : ProfileEditorIntent
    data class AvatarSelected(val localUri: String) : ProfileEditorIntent
    data object DeleteAvatar : ProfileEditorIntent
    data object Save : ProfileEditorIntent
    data object BackRequested : ProfileEditorIntent
    data object DiscardChanges : ProfileEditorIntent
    data object DismissDiscardConfirmation : ProfileEditorIntent
    data object BackNavigationConsumed : ProfileEditorIntent
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
    private var avatarUploadJob: Job? = null

    init { load() }

    fun onIntent(intent: ProfileEditorIntent) {
        when (intent) {
            ProfileEditorIntent.Retry -> load()
            is ProfileEditorIntent.DisplayNameChanged -> _uiState.update { it.copy(displayName = intent.value, isSaved = false) }
            is ProfileEditorIntent.UsernameChanged -> _uiState.update { it.copy(username = intent.value, isSaved = false) }
            is ProfileEditorIntent.AvatarSelected -> selectAvatar(intent.localUri)
            ProfileEditorIntent.DeleteAvatar -> deleteAvatar()
            ProfileEditorIntent.Save -> save()
            ProfileEditorIntent.BackRequested -> requestBack()
            ProfileEditorIntent.DiscardChanges -> discardChanges()
            ProfileEditorIntent.DismissDiscardConfirmation -> _uiState.update { it.copy(showDiscardConfirmation = false) }
            ProfileEditorIntent.BackNavigationConsumed -> _uiState.update { it.copy(shouldNavigateBack = false) }
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
            onSuccess = { profile ->
                ProfileEditorUiState(
                    profile = profile,
                    displayName = profile.displayName,
                    username = profile.username,
                    avatar = profile.avatarUrl?.let(AvatarEditorState::Existing) ?: AvatarEditorState.None,
                    isLoading = false,
                )
            },
            onFailure = { ProfileEditorUiState(isLoading = false, errorMessage = it.message ?: stringProvider.get(R.string.edit_profile_error)) },
        )
    }

    private fun selectAvatar(localUri: String) {
        val profile = _uiState.value.profile ?: return
        val previousAvatar = _uiState.value.avatar
        avatarUploadJob?.cancel()
        avatarUploadJob = viewModelScope.launch {
            discardPendingAvatar(profile.id, previousAvatar)
            val uploadId = UUID.randomUUID().toString()
            _uiState.update {
                it.copy(
                    avatar = AvatarEditorState.Uploading(localUri, uploadId, 0f),
                    errorMessage = null,
                    isSaved = false,
                )
            }
            val result = withContext(ioDispatcher) {
                userRepository.uploadAvatar(profile.id, AvatarUploadRequest(localUri, uploadId)) { progress ->
                    _uiState.update { state ->
                        val avatar = state.avatar
                        if (avatar is AvatarEditorState.Uploading && avatar.uploadId == uploadId) {
                            state.copy(avatar = avatar.copy(progress = progress.coerceIn(0f, 1f)))
                        } else {
                            state
                        }
                    }
                }
            }
            _uiState.update { state ->
                result.fold(
                    onSuccess = { url -> state.copy(avatar = AvatarEditorState.Uploaded(url, uploadId)) },
                    onFailure = {
                        state.copy(
                            avatar = originalAvatarState(profile),
                            errorMessage = it.message ?: stringProvider.get(R.string.edit_profile_error),
                        )
                    },
                )
            }
        }
    }

    private fun deleteAvatar() {
        val state = _uiState.value
        val profile = state.profile ?: return
        when (val avatar = state.avatar) {
            AvatarEditorState.None -> Unit
            is AvatarEditorState.Existing -> _uiState.update { it.copy(avatar = AvatarEditorState.Removing(avatar.url), isSaved = false) }
            is AvatarEditorState.Removing -> _uiState.update { it.copy(avatar = AvatarEditorState.Existing(avatar.url)) }
            is AvatarEditorState.Uploading -> {
                avatarUploadJob?.cancel()
                viewModelScope.launch {
                    _uiState.update { it.copy(avatar = AvatarEditorState.Removing(it.displayedAvatarUrl.orEmpty())) }
                    withContext(ioDispatcher) { userRepository.deletePendingAvatar(profile.id, avatar.uploadId) }
                    _uiState.update { it.copy(avatar = originalAvatarState(profile)) }
                }
            }
            is AvatarEditorState.Uploaded -> viewModelScope.launch {
                _uiState.update { it.copy(avatar = AvatarEditorState.Removing(avatar.url)) }
                val result = withContext(ioDispatcher) { userRepository.deleteAvatar(profile.id, avatar.url) }
                _uiState.update { state ->
                    result.fold(
                        onSuccess = { state.copy(avatar = originalAvatarState(profile)) },
                        onFailure = { error -> state.copy(avatar = avatar, errorMessage = error.message ?: stringProvider.get(R.string.edit_profile_error)) },
                    )
                }
            }
        }
    }

    private fun requestBack() {
        if (_uiState.value.hasUnsavedChanges) {
            _uiState.update { it.copy(showDiscardConfirmation = true) }
        } else {
            _uiState.update { it.copy(shouldNavigateBack = true) }
        }
    }

    private fun discardChanges() {
        val profile = _uiState.value.profile ?: run {
            _uiState.update { it.copy(shouldNavigateBack = true, showDiscardConfirmation = false) }
            return
        }
        val avatar = _uiState.value.avatar
        avatarUploadJob?.cancel()
        viewModelScope.launch {
            discardPendingAvatar(profile.id, avatar)
            _uiState.update { it.copy(showDiscardConfirmation = false, shouldNavigateBack = true) }
        }
    }

    private suspend fun discardPendingAvatar(userId: String, avatar: AvatarEditorState) {
        when (avatar) {
            is AvatarEditorState.Uploading -> userRepository.deletePendingAvatar(userId, avatar.uploadId)
            is AvatarEditorState.Uploaded -> userRepository.deleteAvatar(userId, avatar.url)
            else -> Unit
        }
    }

    private fun save() {
        val state = _uiState.value
        val profile = state.profile ?: return
        val name = state.displayName.trim()
        val username = state.username.trim()
        if (name.isBlank() || username.isBlank() || !state.canSave) {
            if (name.isBlank() || username.isBlank()) {
                _uiState.update { it.copy(errorMessage = stringProvider.get(R.string.edit_profile_error)) }
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val avatarUrl = when (val avatar = state.avatar) {
                is AvatarEditorState.Existing -> avatar.url
                is AvatarEditorState.Uploaded -> avatar.url
                AvatarEditorState.None,
                is AvatarEditorState.Removing -> null
                is AvatarEditorState.Uploading -> return@launch
            }
            val updated = profile.copy(displayName = name, username = username, avatarUrl = avatarUrl)
            val result = withContext(ioDispatcher) { userRepository.updateProfile(updated) }
            result.onSuccess {
                val oldAvatarUrl = profile.avatarUrl
                if (oldAvatarUrl != null && oldAvatarUrl != avatarUrl) {
                    withContext(ioDispatcher) { userRepository.deleteAvatar(profile.id, oldAvatarUrl) }
                }
            }
            _uiState.update { current ->
                result.fold(
                    onSuccess = { saved -> current.copy(profile = saved, isSaving = false, isSaved = true) },
                    onFailure = { current.copy(isSaving = false, errorMessage = it.message ?: stringProvider.get(R.string.edit_profile_error)) },
                )
            }
        }
    }

    private fun originalAvatarState(profile: ProfileUser): AvatarEditorState =
        profile.avatarUrl?.let(AvatarEditorState::Existing) ?: AvatarEditorState.None
}

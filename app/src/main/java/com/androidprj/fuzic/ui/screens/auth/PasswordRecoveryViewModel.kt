package com.androidprj.fuzic.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidprj.fuzic.R
import com.androidprj.fuzic.di.IoDispatcher
import com.androidprj.fuzic.repository.PasswordRecoveryRepository
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
import com.androidprj.fuzic.util.toUserFriendlyMessage

data class PasswordRecoveryUiState(
    val email: String = "",
    val isLoading: Boolean = false,
    val isSubmitted: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class PasswordRecoveryViewModel @Inject constructor(
    private val repository: PasswordRecoveryRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val stringProvider: StringProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PasswordRecoveryUiState())
    val uiState: StateFlow<PasswordRecoveryUiState> = _uiState.asStateFlow()

    fun updateEmail(email: String) = _uiState.update { it.copy(email = email, errorMessage = null, isSubmitted = false) }

    fun submit() = viewModelScope.launch {
        val email = _uiState.value.email.trim()
        if (!email.contains('@')) return@launch
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        val result = withContext(ioDispatcher) { repository.requestPasswordReset(email) }
        _uiState.update { state -> result.fold(
            onSuccess = { state.copy(isLoading = false, isSubmitted = true) },
            onFailure = { state.copy(isLoading = false, errorMessage = it.toUserFriendlyMessage(stringProvider, R.string.password_recovery_error)) },
        ) }
    }
}

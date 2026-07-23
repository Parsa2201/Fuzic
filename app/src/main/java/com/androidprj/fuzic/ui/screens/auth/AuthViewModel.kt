package com.androidprj.fuzic.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidprj.fuzic.R
import com.androidprj.fuzic.di.IoDispatcher
import com.androidprj.fuzic.util.StringProvider
import com.androidprj.fuzic.model.ui.AuthUiState
import com.androidprj.fuzic.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface AuthIntent {
    data class NameChanged(val value: String) : AuthIntent
    data class EmailChanged(val value: String) : AuthIntent
    data class PasswordChanged(val value: String) : AuthIntent
    data class ConfirmPasswordChanged(val value: String) : AuthIntent
    data object ToggleMode : AuthIntent
    data object TogglePasswordVisibility : AuthIntent
    data object ToggleConfirmPasswordVisibility : AuthIntent
    data object Submit : AuthIntent
    data object Retry : AuthIntent
    data object ClearError : AuthIntent
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    @IoDispatcher private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher,
    private val stringProvider: StringProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onIntent(intent: AuthIntent) {
        when (intent) {
            is AuthIntent.NameChanged -> _uiState.update { it.copy(name = intent.value, nameErrorRes = null, errorMessage = null) }
            is AuthIntent.EmailChanged -> _uiState.update { it.copy(email = intent.value, emailErrorRes = null, errorMessage = null) }
            is AuthIntent.PasswordChanged -> _uiState.update { it.copy(password = intent.value, passwordErrorRes = null, errorMessage = null) }
            is AuthIntent.ConfirmPasswordChanged -> _uiState.update { it.copy(confirmPassword = intent.value, confirmPasswordErrorRes = null, errorMessage = null) }
            AuthIntent.ToggleMode -> _uiState.update {
                it.copy(
                    isSignUp = !it.isSignUp,
                    confirmationEmail = null,
                    errorMessage = null,
                    emailErrorRes = null,
                    nameErrorRes = null,
                    passwordErrorRes = null,
                    confirmPasswordErrorRes = null,
                )
            }
            AuthIntent.TogglePasswordVisibility -> _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            AuthIntent.ToggleConfirmPasswordVisibility -> _uiState.update { it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible) }
            AuthIntent.Submit, AuthIntent.Retry -> submit()
            AuthIntent.ClearError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun submit() {
        val state = _uiState.value
        if (state.isLoading || state.confirmationEmail != null) return

        val nameError = if (state.isSignUp && state.name.isBlank()) R.string.auth_name_required else null
        val emailError = when {
            state.email.isBlank() -> R.string.auth_email_required
            !EMAIL_REGEX.matches(state.email.trim()) -> R.string.auth_email_error
            else -> null
        }
        val passwordError = when {
            state.password.isBlank() -> R.string.auth_password_required
            state.password.length < 8 -> R.string.auth_password_error
            else -> null
        }
        val confirmError = if (state.isSignUp && state.password != state.confirmPassword) {
            R.string.auth_confirm_password_error
        } else {
            null
        }
        if (nameError != null || emailError != null || passwordError != null || confirmError != null) {
            _uiState.update {
                it.copy(
                    emailErrorRes = emailError,
                    nameErrorRes = nameError,
                    passwordErrorRes = passwordError,
                    confirmPasswordErrorRes = confirmError,
                )
            }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = withContext(ioDispatcher) {
                if (state.isSignUp) {
                    authRepository.signUp(state.email.trim(), state.password, state.name.trim())
                } else {
                    authRepository.login(state.email.trim(), state.password)
                }
            }
            _uiState.update { current ->
                result.fold(
                    onSuccess = {
                        current.copy(
                            isLoading = false,
                            confirmationEmail = if (state.isSignUp) state.email.trim() else null,
                        )
                    },
                    onFailure = {
                        current.copy(
                            isLoading = false,
                            errorMessage = it.message ?: stringProvider.get(R.string.auth_error_message),
                        )
                    },
                )
            }
        }
    }

    private companion object {
        val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    }
}

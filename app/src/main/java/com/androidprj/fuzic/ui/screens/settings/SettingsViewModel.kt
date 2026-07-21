package com.androidprj.fuzic.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidprj.fuzic.R
import com.androidprj.fuzic.di.IoDispatcher
import com.androidprj.fuzic.model.ui.AppLanguageOption
import com.androidprj.fuzic.model.ui.AppThemeOption
import com.androidprj.fuzic.model.ui.SettingsOverlay
import com.androidprj.fuzic.model.ui.SettingsUiState
import com.androidprj.fuzic.repository.AuthRepository
import com.androidprj.fuzic.repository.SettingsRepository
import com.androidprj.fuzic.util.StringProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface SettingsIntent {
    data object Retry : SettingsIntent
    data object ShowThemeOptions : SettingsIntent
    data object ShowLanguageOptions : SettingsIntent
    data object DismissOverlay : SettingsIntent
    data class ThemeSelected(val option: AppThemeOption) : SettingsIntent
    data class LanguageSelected(val option: AppLanguageOption) : SettingsIntent
    data object ShowLogoutConfirmation : SettingsIntent
    data object DismissLogoutConfirmation : SettingsIntent
    data object ConfirmLogout : SettingsIntent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val stringProvider: StringProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState(isLoading = true))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        observeSettings()
    }

    fun onIntent(intent: SettingsIntent) {
        when (intent) {
            SettingsIntent.Retry -> observeSettings()
            SettingsIntent.ShowThemeOptions -> _uiState.update { it.copy(selectedOverlay = SettingsOverlay.Theme) }
            SettingsIntent.ShowLanguageOptions -> _uiState.update { it.copy(selectedOverlay = SettingsOverlay.Language) }
            SettingsIntent.DismissOverlay -> _uiState.update { it.copy(selectedOverlay = SettingsOverlay.None) }
            is SettingsIntent.ThemeSelected -> setTheme(intent.option)
            is SettingsIntent.LanguageSelected -> setLanguage(intent.option)
            SettingsIntent.ShowLogoutConfirmation -> _uiState.update { it.copy(isLogoutConfirmationVisible = true) }
            SettingsIntent.DismissLogoutConfirmation -> _uiState.update { it.copy(isLogoutConfirmationVisible = false) }
            SettingsIntent.ConfirmLogout -> logout()
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            settingsRepository.observeSettings()
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: stringProvider.get(R.string.settings_error_title),
                        )
                    }
                }
                .collect { settings ->
                    _uiState.update {
                        it.copy(
                            theme = settings.theme,
                            language = settings.language,
                            isLoading = false,
                            errorMessage = null,
                        )
                    }
                }
        }
    }

    private fun setTheme(option: AppThemeOption) {
        viewModelScope.launch {
            val result = withContext(ioDispatcher) { settingsRepository.setTheme(option) }
            _uiState.update { current ->
                result.fold(
                    onSuccess = {
                        current.copy(
                            theme = option,
                            selectedOverlay = SettingsOverlay.None,
                            errorMessage = null,
                        )
                    },
                    onFailure = {
                        current.copy(
                            selectedOverlay = SettingsOverlay.None,
                            errorMessage = it.message ?: stringProvider.get(R.string.settings_error_title),
                        )
                    },
                )
            }
        }
    }

    private fun setLanguage(option: AppLanguageOption) {
        viewModelScope.launch {
            val result = withContext(ioDispatcher) { settingsRepository.setLanguage(option) }
            _uiState.update { current ->
                result.fold(
                    onSuccess = {
                        current.copy(
                            language = option,
                            selectedOverlay = SettingsOverlay.None,
                            errorMessage = null,
                        )
                    },
                    onFailure = {
                        current.copy(
                            selectedOverlay = SettingsOverlay.None,
                            errorMessage = it.message ?: stringProvider.get(R.string.settings_error_title),
                        )
                    },
                )
            }
        }
    }

    private fun logout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = withContext(ioDispatcher) { authRepository.logout() }
            _uiState.update { current ->
                result.fold(
                    onSuccess = {
                        current.copy(
                            isLoading = false,
                            isLogoutConfirmationVisible = false,
                            errorMessage = null,
                        )
                    },
                    onFailure = {
                        current.copy(
                            isLoading = false,
                            isLogoutConfirmationVisible = false,
                            errorMessage = it.message ?: stringProvider.get(R.string.settings_error_title),
                        )
                    },
                )
            }
        }
    }
}

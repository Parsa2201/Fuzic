package com.androidprj.fuzic.model

enum class AppThemeOption {
    System,
    Light,
    Dark,
}

enum class AppLanguageOption {
    System,
    English,
    Persian,
}

enum class SettingsOverlay {
    None,
    Theme,
    Language,
}

data class SettingsUiState(
    val theme: AppThemeOption = AppThemeOption.System,
    val language: AppLanguageOption = AppLanguageOption.System,
    val selectedOverlay: SettingsOverlay = SettingsOverlay.None,
    val isLogoutConfirmationVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

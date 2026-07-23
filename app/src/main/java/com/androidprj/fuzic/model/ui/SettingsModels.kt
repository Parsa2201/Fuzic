package com.androidprj.fuzic.model.ui

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

enum class AppFontScale(val multiplier: Float) {
    Small(0.9f),
    Default(1f),
    Large(1.15f),
}

enum class SettingsOverlay {
    None,
    Theme,
    Language,
    FontSize,
}

data class SettingsUiState(
    val theme: AppThemeOption = AppThemeOption.System,
    val language: AppLanguageOption = AppLanguageOption.System,
    val fontScale: AppFontScale = AppFontScale.Default,
    val selectedOverlay: SettingsOverlay = SettingsOverlay.None,
    val isLogoutConfirmationVisible: Boolean = false,
    val isLogoutComplete: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

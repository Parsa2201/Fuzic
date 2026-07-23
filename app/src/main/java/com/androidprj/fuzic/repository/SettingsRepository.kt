package com.androidprj.fuzic.repository

import com.androidprj.fuzic.model.ui.AppLanguageOption
import com.androidprj.fuzic.model.ui.AppFontScale
import com.androidprj.fuzic.model.ui.AppSettings
import com.androidprj.fuzic.model.ui.AppThemeOption
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeSettings(): Flow<AppSettings>
    suspend fun setTheme(theme: AppThemeOption): Result<Unit>
    suspend fun setLanguage(language: AppLanguageOption): Result<Unit>
    suspend fun setFontScale(fontScale: AppFontScale): Result<Unit>
    suspend fun clearSettings(): Result<Unit>
}

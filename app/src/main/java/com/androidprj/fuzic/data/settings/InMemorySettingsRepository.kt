package com.androidprj.fuzic.data.settings

import com.androidprj.fuzic.model.ui.AppLanguageOption
import com.androidprj.fuzic.model.ui.AppSettings
import com.androidprj.fuzic.model.ui.AppThemeOption
import com.androidprj.fuzic.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

@Singleton
class InMemorySettingsRepository @Inject constructor() : SettingsRepository {
    private val settings = MutableStateFlow(AppSettings())

    override fun observeSettings(): Flow<AppSettings> = settings

    override suspend fun setTheme(theme: AppThemeOption): Result<Unit> {
        settings.value = settings.value.copy(theme = theme)
        return Result.success(Unit)
    }

    override suspend fun setLanguage(language: AppLanguageOption): Result<Unit> {
        settings.value = settings.value.copy(language = language)
        return Result.success(Unit)
    }

    override suspend fun clearSettings(): Result<Unit> {
        settings.value = AppSettings()
        return Result.success(Unit)
    }
}

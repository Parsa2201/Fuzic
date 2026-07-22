package com.androidprj.fuzic.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.androidprj.fuzic.model.ui.AppLanguageOption
import com.androidprj.fuzic.model.ui.AppSettings
import com.androidprj.fuzic.model.ui.AppThemeOption
import com.androidprj.fuzic.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class LocalSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    override fun observeSettings(): Flow<AppSettings> {
        return context.dataStore.data.map { preferences ->
            val themeStr = preferences[PreferencesKeys.THEME] ?: AppThemeOption.System.name
            val langStr = preferences[PreferencesKeys.LANGUAGE] ?: AppLanguageOption.System.name
            AppSettings(
                theme = AppThemeOption.valueOf(themeStr),
                language = AppLanguageOption.valueOf(langStr)
            )
        }
    }

    override suspend fun setTheme(theme: AppThemeOption): Result<Unit> {
        return try {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.THEME] = theme.name
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setLanguage(language: AppLanguageOption): Result<Unit> {
        return try {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.LANGUAGE] = language.name
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearSettings(): Result<Unit> {
        return try {
            context.dataStore.edit { preferences ->
                preferences.clear()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private object PreferencesKeys {
        val THEME = stringPreferencesKey("theme")
        val LANGUAGE = stringPreferencesKey("language")
    }
}

package com.androidprj.fuzic

import com.androidprj.fuzic.model.ui.AppLanguageOption
import com.androidprj.fuzic.model.ui.AppSettings
import com.androidprj.fuzic.model.ui.AppThemeOption
import com.androidprj.fuzic.model.ui.SettingsOverlay
import com.androidprj.fuzic.ui.screens.settings.SettingsIntent
import com.androidprj.fuzic.ui.screens.settings.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun observesSettingsOnStart() = runTest {
        val repository = FakeSettingsRepository(
            AppSettings(theme = AppThemeOption.Dark, language = AppLanguageOption.Persian)
        )
        val viewModel = SettingsViewModel(repository, FakeAuthRepository(), dispatcher, FakeStringProvider)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(AppThemeOption.Dark, viewModel.uiState.value.theme)
        assertEquals(AppLanguageOption.Persian, viewModel.uiState.value.language)
    }

    @Test
    fun opensAndDismissesSelectionOverlays() = runTest {
        val viewModel = SettingsViewModel(FakeSettingsRepository(), FakeAuthRepository(), dispatcher, FakeStringProvider)
        advanceUntilIdle()

        viewModel.onIntent(SettingsIntent.ShowThemeOptions)
        assertEquals(SettingsOverlay.Theme, viewModel.uiState.value.selectedOverlay)
        viewModel.onIntent(SettingsIntent.DismissOverlay)
        assertEquals(SettingsOverlay.None, viewModel.uiState.value.selectedOverlay)

        viewModel.onIntent(SettingsIntent.ShowLanguageOptions)
        assertEquals(SettingsOverlay.Language, viewModel.uiState.value.selectedOverlay)
    }

    @Test
    fun selectingThemePersistsAndClosesOverlay() = runTest {
        val repository = FakeSettingsRepository()
        val viewModel = SettingsViewModel(repository, FakeAuthRepository(), dispatcher, FakeStringProvider)
        advanceUntilIdle()

        viewModel.onIntent(SettingsIntent.ShowThemeOptions)
        viewModel.onIntent(SettingsIntent.ThemeSelected(AppThemeOption.Light))
        advanceUntilIdle()

        assertEquals(1, repository.setThemeCalls)
        assertEquals(AppThemeOption.Light, viewModel.uiState.value.theme)
        assertEquals(SettingsOverlay.None, viewModel.uiState.value.selectedOverlay)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun languageFailureClosesOverlayAndShowsError() = runTest {
        val repository = FakeSettingsRepository().apply {
            languageResult = Result.failure(IllegalStateException("cannot save"))
        }
        val viewModel = SettingsViewModel(repository, FakeAuthRepository(), dispatcher, FakeStringProvider)
        advanceUntilIdle()

        viewModel.onIntent(SettingsIntent.ShowLanguageOptions)
        viewModel.onIntent(SettingsIntent.LanguageSelected(AppLanguageOption.English))
        advanceUntilIdle()

        assertEquals(SettingsOverlay.None, viewModel.uiState.value.selectedOverlay)
        assertEquals("cannot save", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun logoutConfirmationAndSuccessClearsLoading() = runTest {
        val authRepository = FakeAuthRepository()
        val viewModel = SettingsViewModel(FakeSettingsRepository(), authRepository, dispatcher, FakeStringProvider)
        advanceUntilIdle()

        viewModel.onIntent(SettingsIntent.ShowLogoutConfirmation)
        assertTrue(viewModel.uiState.value.isLogoutConfirmationVisible)
        viewModel.onIntent(SettingsIntent.ConfirmLogout)
        advanceUntilIdle()

        assertEquals(1, authRepository.logoutCalls)
        assertFalse(viewModel.uiState.value.isLogoutConfirmationVisible)
        assertFalse(viewModel.uiState.value.isLoading)
    }
}

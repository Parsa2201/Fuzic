package com.androidprj.fuzic

import com.androidprj.fuzic.model.mapper.toProfileUser
import com.androidprj.fuzic.model.remote.UserDto
import com.androidprj.fuzic.model.ui.PlaylistSectionType
import com.androidprj.fuzic.ui.screens.auth.AuthIntent
import com.androidprj.fuzic.ui.screens.auth.AuthViewModel
import com.androidprj.fuzic.ui.screens.home.HomeViewModel
import com.androidprj.fuzic.ui.screens.playlists.PlaylistsIntent
import com.androidprj.fuzic.ui.screens.playlists.PlaylistsViewModel
import com.androidprj.fuzic.ui.screens.profile.ProfileViewModel
import com.androidprj.fuzic.ui.screens.profile.ProfileEditorIntent
import com.androidprj.fuzic.ui.screens.profile.ProfileEditorViewModel
import com.androidprj.fuzic.ui.screens.song.SongDetailsViewModel
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
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelsTest {
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
    fun authRejectsInvalidCredentialsWithoutCallingRepository() = runTest {
        val repository = FakeAuthRepository()
        val viewModel = AuthViewModel(repository, dispatcher, FakeStringProvider)

        viewModel.onIntent(AuthIntent.Submit)
        advanceUntilIdle()

        assertEquals(0, repository.loginCalls)
        assertTrue(viewModel.uiState.value.emailErrorRes != null)
        assertTrue(viewModel.uiState.value.passwordErrorRes != null)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun authLoginCallsRepositoryAndStopsLoadingOnSuccess() = runTest {
        val repository = FakeAuthRepository()
        val viewModel = AuthViewModel(repository, dispatcher, FakeStringProvider)

        viewModel.onIntent(AuthIntent.EmailChanged("parsa@example.com"))
        viewModel.onIntent(AuthIntent.PasswordChanged("password123"))
        viewModel.onIntent(AuthIntent.Submit)
        advanceUntilIdle()

        assertEquals(1, repository.loginCalls)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun authSignupPassesNameToRepository() = runTest {
        val repository = FakeAuthRepository()
        val viewModel = AuthViewModel(repository, dispatcher, FakeStringProvider)

        viewModel.onIntent(AuthIntent.ToggleMode)
        viewModel.onIntent(AuthIntent.NameChanged("Parsa"))
        viewModel.onIntent(AuthIntent.EmailChanged("parsa@example.com"))
        viewModel.onIntent(AuthIntent.PasswordChanged("password123"))
        viewModel.onIntent(AuthIntent.ConfirmPasswordChanged("password123"))
        viewModel.onIntent(AuthIntent.Submit)
        advanceUntilIdle()

        assertEquals(1, repository.signUpCalls)
        assertEquals("Parsa", repository.lastSignUpName)
        assertEquals("parsa@example.com", viewModel.uiState.value.confirmationEmail)
    }

    @Test
    fun authDoesNotResubmitAfterSuccessfulSignup() = runTest {
        val repository = FakeAuthRepository()
        val viewModel = AuthViewModel(repository, dispatcher, FakeStringProvider)

        viewModel.onIntent(AuthIntent.ToggleMode)
        viewModel.onIntent(AuthIntent.NameChanged("Parsa"))
        viewModel.onIntent(AuthIntent.EmailChanged("parsa@example.com"))
        viewModel.onIntent(AuthIntent.PasswordChanged("password123"))
        viewModel.onIntent(AuthIntent.ConfirmPasswordChanged("password123"))
        viewModel.onIntent(AuthIntent.Submit)
        advanceUntilIdle()
        viewModel.onIntent(AuthIntent.Submit)
        viewModel.onIntent(AuthIntent.Retry)

        assertEquals(1, repository.signUpCalls)
    }

    @Test
    fun homeLoadsDailyPicksAndAllRequiredSections() = runTest {
        val viewModel = HomeViewModel(
            FakeMusicRepository(),
            FakePlaylistRepository(),
            dispatcher,
            FakeStringProvider,
        )

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(1, viewModel.uiState.value.dailyPicks.size)
        assertEquals(4, viewModel.uiState.value.sections.size)
    }

    @Test
    fun homeExposesRepositoryFailure() = runTest {
        val viewModel = HomeViewModel(
            FakeMusicRepository(dailyResult = Result.failure(IllegalStateException("offline"))),
            FakePlaylistRepository(),
            dispatcher,
            FakeStringProvider,
        )

        advanceUntilIdle()

        assertEquals("offline", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun playlistsLoadsGlobalLocalAndMineSections() = runTest {
        val viewModel = PlaylistsViewModel(
            FakePlaylistRepository(),
            FakeAuthRepository(),
            dispatcher,
            FakeStringProvider,
        )

        advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.sections.size)
        assertEquals(PlaylistSectionType.Mine, viewModel.uiState.value.sections.last().type)
    }

    @Test
    fun playlistsRejectsDuplicateMinePlaylistName() = runTest {
        val repository = FakePlaylistRepository()
        val viewModel = PlaylistsViewModel(repository, FakeAuthRepository(), dispatcher, FakeStringProvider)
        advanceUntilIdle()

        viewModel.onIntent(PlaylistsIntent.ShowCreate)
        viewModel.onIntent(PlaylistsIntent.NameChanged(" evening MIX "))
        viewModel.onIntent(PlaylistsIntent.Create)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.createPlaylistState.hasNameConflict)
        assertEquals(0, repository.createCalls)
    }

    @Test
    fun profileAggregatesProfileAndCounts() = runTest {
        val viewModel = ProfileViewModel(
            FakeAuthRepository(),
            FakeUserRepository(),
            FakeFollowRepository(followersCount = 12, followingCount = 5),
            FakePlaylistRepository(),
            dispatcher,
            FakeStringProvider,
        )

        advanceUntilIdle()

        assertEquals(testProfile, viewModel.uiState.value.profile)
        assertEquals("12", viewModel.uiState.value.stats.followersLabel)
        assertEquals("5", viewModel.uiState.value.stats.followingLabel)
    }

    @Test
    fun profileEditorMarksSaveCompleteWithUpdatedProfile() = runTest {
        val viewModel = ProfileEditorViewModel(
            FakeAuthRepository(),
            FakeUserRepository(),
            dispatcher,
            FakeStringProvider,
        )
        advanceUntilIdle()

        viewModel.onIntent(ProfileEditorIntent.DisplayNameChanged("Parsa Updated"))
        viewModel.onIntent(ProfileEditorIntent.UsernameChanged("parsa_updated"))
        viewModel.onIntent(ProfileEditorIntent.Save)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSaved)
        assertEquals("Parsa Updated", viewModel.uiState.value.profile?.displayName)
        assertEquals("parsa_updated", viewModel.uiState.value.profile?.username)
    }

    @Test
    fun songDetailsLoadsSongAndTogglesLikeAfterSuccess() = runTest {
        val interactionRepository = FakeInteractionRepository()
        val viewModel = SongDetailsViewModel(
            FakeMusicRepository(),
            interactionRepository,
            FakePremiumRepository(),
            FakeDownloadRepository(),
            dispatcher,
            FakeStringProvider,
        )

        viewModel.load("song-1")
        advanceUntilIdle()
        viewModel.toggleLike()
        advanceUntilIdle()

        assertEquals(testSong, viewModel.uiState.value.song)
        assertTrue(viewModel.uiState.value.isLiked)
        assertEquals(1, interactionRepository.likeCalls)
    }

    @Test
    fun userDtoMappingUsesDatabaseUsername() {
        val profile = UserDto(
            id = "user-1",
            name = "Parsa Example",
            username = "parsa_music",
        ).toProfileUser()

        assertEquals("parsa_music", profile.username)
    }
}

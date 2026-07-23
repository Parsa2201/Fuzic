package com.androidprj.fuzic

import com.androidprj.fuzic.model.ui.DownloadSortOption
import com.androidprj.fuzic.ui.screens.downloads.DownloadsIntent
import com.androidprj.fuzic.ui.screens.downloads.DownloadsViewModel
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadsViewModelTest {
    private val fakePremiumRepository = FakePremiumRepository()
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
    fun observesDownloadsOnStart() = runTest {
        val viewModel = DownloadsViewModel(FakeDownloadRepository(), fakePremiumRepository, dispatcher, FakeStringProvider)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(listOf(testDownload), viewModel.uiState.value.downloads)
    }

    @Test
    fun changingSortOptionRestartsObservation() = runTest {
        val repository = FakeDownloadRepository()
        val viewModel = DownloadsViewModel(repository, fakePremiumRepository, dispatcher, FakeStringProvider)
        advanceUntilIdle()

        viewModel.onIntent(DownloadsIntent.SortSelected(DownloadSortOption.Artist))
        advanceUntilIdle()

        assertEquals(DownloadSortOption.Artist, repository.observedSortOption)
        assertEquals(DownloadSortOption.Artist, viewModel.uiState.value.sortOption)
    }

    @Test
    fun deleteRemovesItemAndAllowsUndo() = runTest {
        val repository = FakeDownloadRepository()
        val viewModel = DownloadsViewModel(repository, fakePremiumRepository, dispatcher, FakeStringProvider)
        advanceUntilIdle()

        viewModel.onIntent(DownloadsIntent.Delete(testDownload))
        advanceUntilIdle()
        viewModel.onIntent(DownloadsIntent.UndoDelete)
        advanceUntilIdle()

        assertEquals(1, repository.deleteCalls)
        assertEquals(1, repository.restoreCalls)
    }

    @Test
    fun deleteFailureShowsError() = runTest {
        val repository = FakeDownloadRepository().apply {
            deleteResult = Result.failure(IllegalStateException("delete failed"))
        }
        val viewModel = DownloadsViewModel(repository, fakePremiumRepository, dispatcher, FakeStringProvider)
        advanceUntilIdle()

        viewModel.onIntent(DownloadsIntent.Delete(testDownload))
        advanceUntilIdle()

        assertEquals("delete failed", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun removeFileFailureMarksStorageFull() = runTest {
        val repository = FakeDownloadRepository().apply {
            removeFileResult = Result.failure(IllegalStateException("storage full"))
        }
        val viewModel = DownloadsViewModel(repository, fakePremiumRepository, dispatcher, FakeStringProvider)
        advanceUntilIdle()

        viewModel.onIntent(DownloadsIntent.RemoveFile(testDownload))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isStorageFull)
        assertEquals("storage full", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun nonPremiumUserShowsUpgradeGate() = runTest {
        val viewModel = DownloadsViewModel(FakeDownloadRepository(), fakePremiumRepository, dispatcher, FakeStringProvider)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isPremiumUser)
        assertFalse(viewModel.uiState.value.isPremiumLoading)
    }

    @Test
    fun upgradeToPremiumFlipsStatus() = runTest {
        val viewModel = DownloadsViewModel(FakeDownloadRepository(), fakePremiumRepository, dispatcher, FakeStringProvider)
        advanceUntilIdle()

        viewModel.onIntent(DownloadsIntent.UpgradeToPremium)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isPremiumUser)
    }
}
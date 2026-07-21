package com.androidprj.fuzic

import com.androidprj.fuzic.ui.screens.notifications.NotificationsIntent
import com.androidprj.fuzic.ui.screens.notifications.NotificationsViewModel
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
class NotificationsViewModelTest {
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
    fun observeStopsLoading() = runTest {
        val viewModel = NotificationsViewModel(FakeNotificationRepository(), dispatcher, FakeStringProvider)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun selectingUnreadNotificationMarksItRead() = runTest {
        val repository = FakeNotificationRepository()
        val viewModel = NotificationsViewModel(repository, dispatcher, FakeStringProvider)
        advanceUntilIdle()
        viewModel.setNotificationsForTesting(listOf(testNotification))

        viewModel.onIntent(NotificationsIntent.NotificationSelected(testNotification))
        advanceUntilIdle()

        assertEquals(1, repository.markCalls)
        assertTrue(viewModel.uiState.value.notifications.first().isRead)
    }

    @Test
    fun markReadFailureRollsBackNotification() = runTest {
        val repository = FakeNotificationRepository().apply {
            markResult = Result.failure(IllegalStateException("mark failed"))
        }
        val viewModel = NotificationsViewModel(repository, dispatcher, FakeStringProvider)
        advanceUntilIdle()
        viewModel.setNotificationsForTesting(listOf(testNotification))

        viewModel.onIntent(NotificationsIntent.NotificationSelected(testNotification))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.notifications.first().isRead)
        assertEquals("mark failed", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun markAllReadUpdatesAllItems() = runTest {
        val repository = FakeNotificationRepository()
        val viewModel = NotificationsViewModel(repository, dispatcher, FakeStringProvider)
        advanceUntilIdle()
        viewModel.setNotificationsForTesting(listOf(testNotification, testNotification.copy(id = "notification-2")))

        viewModel.onIntent(NotificationsIntent.MarkAllRead)
        advanceUntilIdle()

        assertEquals(1, repository.markAllCalls)
        assertTrue(viewModel.uiState.value.notifications.all { it.isRead })
    }

    @Test
    fun markAllFailureRollsBackAllItems() = runTest {
        val repository = FakeNotificationRepository().apply {
            markAllResult = Result.failure(IllegalStateException("mark all failed"))
        }
        val viewModel = NotificationsViewModel(repository, dispatcher, FakeStringProvider)
        advanceUntilIdle()
        viewModel.setNotificationsForTesting(listOf(testNotification))

        viewModel.onIntent(NotificationsIntent.MarkAllRead)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.notifications.first().isRead)
        assertEquals("mark all failed", viewModel.uiState.value.errorMessage)
        viewModel.onIntent(NotificationsIntent.ClearError)
        assertNull(viewModel.uiState.value.errorMessage)
    }
}

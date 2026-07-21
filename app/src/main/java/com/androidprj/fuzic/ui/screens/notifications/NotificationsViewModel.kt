package com.androidprj.fuzic.ui.screens.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidprj.fuzic.R
import com.androidprj.fuzic.di.IoDispatcher
import com.androidprj.fuzic.model.ui.NotificationItem
import com.androidprj.fuzic.model.ui.NotificationsUiState
import com.androidprj.fuzic.repository.NotificationRepository
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

sealed interface NotificationsIntent {
    data object Retry : NotificationsIntent
    data class NotificationSelected(val item: NotificationItem) : NotificationsIntent
    data object MarkAllRead : NotificationsIntent
    data object ClearError : NotificationsIntent
}

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val stringProvider: StringProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(NotificationsUiState(isLoading = true))
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init {
        observeNotifications()
    }

    fun onIntent(intent: NotificationsIntent) {
        when (intent) {
            NotificationsIntent.Retry -> observeNotifications()
            is NotificationsIntent.NotificationSelected -> markRead(intent.item)
            NotificationsIntent.MarkAllRead -> markAllRead()
            NotificationsIntent.ClearError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun observeNotifications() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            notificationRepository.observeNotifications()
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: stringProvider.get(R.string.notifications_error_title),
                        )
                    }
                }
                .collect {
                    _uiState.update { state -> state.copy(isLoading = false, errorMessage = null) }
                }
        }
    }

    private fun markRead(item: NotificationItem) {
        if (item.isRead) return
        _uiState.update { state ->
            state.copy(notifications = state.notifications.map { if (it.id == item.id) it.copy(isRead = true) else it })
        }
        viewModelScope.launch {
            val result = withContext(ioDispatcher) { notificationRepository.markNotificationAsRead(item.id) }
            if (result.isFailure) {
                _uiState.update { state ->
                    state.copy(
                        notifications = state.notifications.map { if (it.id == item.id) item else it },
                        errorMessage = result.exceptionOrNull()?.message ?: stringProvider.get(R.string.notifications_error_title),
                    )
                }
            }
        }
    }

    private fun markAllRead() {
        val previous = _uiState.value.notifications
        _uiState.update { state -> state.copy(notifications = state.notifications.map { it.copy(isRead = true) }) }
        viewModelScope.launch {
            val result = withContext(ioDispatcher) { notificationRepository.markAllNotificationsAsRead() }
            if (result.isFailure) {
                _uiState.update {
                    it.copy(
                        notifications = previous,
                        errorMessage = result.exceptionOrNull()?.message ?: stringProvider.get(R.string.notifications_error_title),
                    )
                }
            }
        }
    }

    fun setNotificationsForTesting(items: List<NotificationItem>) {
        _uiState.value = NotificationsUiState(notifications = items)
    }
}

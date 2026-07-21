package com.androidprj.fuzic.repository

import androidx.paging.PagingData
import com.androidprj.fuzic.model.ui.NotificationItem
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun observeNotifications(): Flow<PagingData<NotificationItem>>
    suspend fun markNotificationAsRead(notificationId: String): Result<Unit>
    suspend fun markAllNotificationsAsRead(): Result<Unit>
}

package com.androidprj.fuzic.data.remote.repository

import androidx.paging.PagingData
import com.androidprj.fuzic.model.ui.NotificationItem
import com.androidprj.fuzic.repository.NotificationRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@Singleton
class InMemoryNotificationRepository @Inject constructor() : NotificationRepository {
    override fun observeNotifications(): Flow<PagingData<NotificationItem>> = flowOf(PagingData.empty())
    override suspend fun markNotificationAsRead(notificationId: String): Result<Unit> = Result.success(Unit)
    override suspend fun markAllNotificationsAsRead(): Result<Unit> = Result.success(Unit)
}

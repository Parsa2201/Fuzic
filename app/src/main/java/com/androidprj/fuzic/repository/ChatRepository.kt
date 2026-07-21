package com.androidprj.fuzic.repository

import com.androidprj.fuzic.model.Message
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    suspend fun getChatHistory(userId: String, offset: Long = 0, limit: Long = 50): Result<List<Message>>
    suspend fun sendMessage(receiverId: String, content: String?, sharedSongId: String?): Result<Message>
    suspend fun markMessageAsRead(messageId: String): Result<Unit>
    
    fun observeMessages(userId: String): Flow<Message>
}

package com.androidprj.fuzic.repository

import com.androidprj.fuzic.model.ui.ChatMessage
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    suspend fun getChatHistory(userId: String, offset: Long = 0, limit: Long = 50): Result<List<ChatMessage>>
    suspend fun sendMessage(receiverId: String, content: String?, sharedSongId: String?): Result<ChatMessage>
    suspend fun markMessageAsRead(messageId: String): Result<Unit>
    
    fun observeMessages(userId: String): Flow<ChatMessage>
}

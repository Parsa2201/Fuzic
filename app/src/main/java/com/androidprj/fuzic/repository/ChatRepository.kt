package com.androidprj.fuzic.repository

import androidx.paging.PagingData
import com.androidprj.fuzic.model.ui.ChatConversation
import com.androidprj.fuzic.model.ui.ChatMessage
import com.androidprj.fuzic.model.ui.TypingStatus
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for chat UI data. Conversation ids are app-level identifiers;
 * implementations may resolve them through realtime channels, local cache, or remote queries.
 */
interface ChatRepository {
    fun observeConversations(): Flow<List<ChatConversation>>
    fun observeMessages(conversationId: String): Flow<PagingData<ChatMessage>>
    fun observeTypingStatus(conversationId: String): Flow<TypingStatus?>
    suspend fun sendTextMessage(conversationId: String, receiverId: String, text: String): Result<ChatMessage>
    suspend fun sendSongMessage(conversationId: String, receiverId: String, songId: String): Result<ChatMessage>
    suspend fun markMessagesAsRead(conversationId: String, messageIds: List<String>): Result<Unit>
    suspend fun setTyping(conversationId: String, isTyping: Boolean): Result<Unit>
    suspend fun refreshConversation(conversationId: String): Result<Unit>
}

package com.androidprj.fuzic.data.remote.repository

import androidx.paging.PagingData
import com.androidprj.fuzic.model.remote.MessageDto
import com.androidprj.fuzic.model.ui.ChatConversation
import com.androidprj.fuzic.model.ui.ChatMessage
import com.androidprj.fuzic.model.mapper.toChatMessage
import com.androidprj.fuzic.model.ui.TypingStatus
import com.androidprj.fuzic.repository.ChatRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.decodeRecord
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.map
import com.androidprj.fuzic.data.local.dao.ChatDao
import com.androidprj.fuzic.model.mapper.toChatMessageEntity
import com.androidprj.fuzic.model.ui.ChatMessageStatus
import com.androidprj.fuzic.model.ui.ChatMessageType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import com.androidprj.fuzic.model.remote.RecentConversationDto
import com.androidprj.fuzic.model.mapper.toChatConversation

class RemoteChatRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val chatDao: ChatDao
) : ChatRepository {

    override fun observeConversations(): Flow<List<ChatConversation>> = flow {
        val currentUserId = supabaseClient.auth.currentUserOrNull()?.id ?: return@flow
        try {
            val conversations = supabaseClient.postgrest["recent_conversations"]
                .select {
                    filter { eq("user_id", currentUserId) }
                }
                .decodeList<RecentConversationDto>()
                .map { it.toChatConversation() }
            emit(conversations)
        } catch (e: Exception) {
            // Emit empty or handle error
            emit(emptyList())
        }
    }

    override fun observeMessages(conversationId: String): Flow<PagingData<ChatMessage>> {
        val currentUserId = supabaseClient.auth.currentUserOrNull()?.id ?: return flowOf(PagingData.empty())
        
        // Sync in background
        CoroutineScope(Dispatchers.IO).launch {
            syncChatHistory(conversationId)
        }
        
        return Pager(
            config = PagingConfig(pageSize = 50, enablePlaceholders = false),
            pagingSourceFactory = { chatDao.getMessagesPagingSource(conversationId) }
        ).flow.map { pagingData ->
            pagingData.map { it.toChatMessage(currentUserId) }
        }
    }

    override fun observeTypingStatus(conversationId: String): Flow<TypingStatus?> = flowOf(null)

    override suspend fun sendTextMessage(
        conversationId: String,
        receiverId: String,
        text: String
    ): Result<ChatMessage> = sendMessageOptimistic(
        conversationId = conversationId,
        receiverId = receiverId,
        content = text,
        sharedSongId = null,
        type = ChatMessageType.Text
    )

    override suspend fun sendSongMessage(
        conversationId: String,
        receiverId: String,
        songId: String
    ): Result<ChatMessage> = sendMessageOptimistic(
        conversationId = conversationId,
        receiverId = receiverId,
        content = null,
        sharedSongId = songId,
        type = ChatMessageType.SongShare
    )

    override suspend fun markMessagesAsRead(
        conversationId: String,
        messageIds: List<String>
    ): Result<Unit> = runCatching {
        messageIds.forEach { messageId ->
            chatDao.updateMessageStatus(messageId, ChatMessageStatus.Read.name)
            markMessageAsRead(messageId).getOrThrow()
        }
    }

    override suspend fun setTyping(conversationId: String, isTyping: Boolean): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun refreshConversation(conversationId: String): Result<Unit> {
        syncChatHistory(conversationId)
        return Result.success(Unit)
    }

    private suspend fun syncChatHistory(conversationId: String) {
        try {
            val messages = supabaseClient.postgrest["messages"]
                .select {
                    filter { eq("conversation_id", conversationId) }
                    order("created_at", order = Order.DESCENDING)
                    limit(50)
                }
                .decodeList<MessageDto>()
                .map { it.toChatMessageEntity() }
            chatDao.insertMessages(messages)
        } catch (e: Exception) {
            // Ignore background sync errors
        }
    }

    private suspend fun sendMessageOptimistic(
        conversationId: String, 
        receiverId: String, 
        content: String?, 
        sharedSongId: String?,
        type: ChatMessageType
    ): Result<ChatMessage> {
        return try {
            val currentUserId = supabaseClient.auth.currentUserOrNull()?.id ?: throw Exception("Not logged in")
            
            // 1. Optimistic insertion to local DB
            val tempId = UUID.randomUUID().toString()
            val optimisticMessage = ChatMessage(
                id = tempId,
                senderId = currentUserId,
                text = content ?: "",
                type = type,
                song = null, // Can't resolve optimistically
                status = ChatMessageStatus.Sending,
                timeLabel = "Just now",
                isMine = true
            )
            chatDao.insertMessage(optimisticMessage.toChatMessageEntity(conversationId, currentUserId, sharedSongId))

            // 2. Send to network
            val message = InsertMessageDto(
                senderId = currentUserId,
                receiverId = receiverId,
                content = content,
                sharedSongId = sharedSongId,
                conversationId = conversationId,
                messageType = type.name
            )
            val insertedMessage = supabaseClient.postgrest["messages"]
                .insert(message) { select() }
                .decodeSingle<MessageDto>()
            
            // 3. Update local DB with real data from network
            val realEntity = insertedMessage.toChatMessageEntity()
            chatDao.insertMessage(realEntity)
            
            Result.success(realEntity.toChatMessage(currentUserId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun markMessageAsRead(messageId: String): Result<Unit> {
        return try {
            supabaseClient.postgrest["messages"].update(
                { "status" to "read" }
            ) {
                filter { eq("id", messageId) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun observeMessageInserts(conversationId: String): Flow<ChatMessage> {
        val currentUserId = supabaseClient.auth.currentUserOrNull()?.id 
            ?: throw Exception("Not logged in")
            
        val channel = supabaseClient.channel("public:messages")
        return channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "messages"
        }.map { it.decodeRecord<MessageDto>() }
         .filter { message -> message.conversationId == conversationId }
         .map { it.toChatMessageEntity() }
         .map { 
             chatDao.insertMessage(it) // Write directly to cache
             it.toChatMessage(currentUserId) 
         }
    }

    @kotlinx.serialization.Serializable
    private data class InsertMessageDto(
        @kotlinx.serialization.SerialName("sender_id") val senderId: String,
        @kotlinx.serialization.SerialName("receiver_id") val receiverId: String,
        val content: String? = null,
        @kotlinx.serialization.SerialName("shared_song_id") val sharedSongId: String? = null,
        @kotlinx.serialization.SerialName("conversation_id") val conversationId: String,
        @kotlinx.serialization.SerialName("message_type") val messageType: String
    )
}

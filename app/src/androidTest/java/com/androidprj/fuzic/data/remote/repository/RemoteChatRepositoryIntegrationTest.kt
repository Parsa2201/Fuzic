package com.androidprj.fuzic.data.remote.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.androidprj.fuzic.BuildConfig
import com.androidprj.fuzic.data.local.database.AppDatabase
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class RemoteChatRepositoryIntegrationTest {

    private lateinit var supabaseClient: SupabaseClient
    private lateinit var db: AppDatabase
    private lateinit var chatRepository: RemoteChatRepository

    private val testEmail1 = "chat_user1_${UUID.randomUUID()}@example.com"
    private val testEmail2 = "chat_user2_${UUID.randomUUID()}@example.com"
    private val testPassword = "Password123!"

    private var user1Id: String? = null
    private var user2Id: String? = null

    @Before
    fun setup() = runBlocking {
        // Init Supabase with Realtime
        supabaseClient = createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Postgrest)
            install(Auth)
            install(Realtime)
        }

        // Init InMemory Room DB
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        chatRepository = RemoteChatRepository(supabaseClient, db.chatDao())

        // Create User 2
        supabaseClient.auth.signUpWith(Email) {
            email = testEmail2
            password = testPassword
        }
        user2Id = supabaseClient.auth.currentUserOrNull()?.id
        
        supabaseClient.auth.signOut()

        // Create User 1 (Active User)
        supabaseClient.auth.signUpWith(Email) {
            email = testEmail1
            password = testPassword
        }
        user1Id = supabaseClient.auth.currentUserOrNull()?.id
    }

    @Test
    fun testSendTextMessage() = runBlocking {
        val receiverId = user2Id!!
        val conversationId = UUID.randomUUID().toString()
        val text = "Hello from integration test!"

        // Sending text message
        val result = chatRepository.sendTextMessage(
            conversationId = conversationId,
            receiverId = receiverId,
            text = text
        )

        // Might fail if Supabase requires a valid conversation_id in a related table,
        // but if the schema allows it or automatically handles it, it will succeed.
        assertTrue("Send message failed: ${result.exceptionOrNull()?.message}", result.isSuccess)
    }

    @Test
    fun testSetTypingStatus() = runBlocking {
        val conversationId = UUID.randomUUID().toString()
        val result = chatRepository.setTyping(conversationId, true)
        
        assertTrue("Set typing failed: ${result.exceptionOrNull()?.message}", result.isSuccess)
    }

    @After
    fun tearDown() = runBlocking {
        try {
            db.close()
            supabaseClient.auth.signOut()
        } catch (e: Exception) {
            // Ignore
        }
    }
}

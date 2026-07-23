package com.androidprj.fuzic.data.remote.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.androidprj.fuzic.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@RunWith(AndroidJUnit4::class)
class RemoteFollowRepositoryIntegrationTest {

    private lateinit var supabaseClient: SupabaseClient
    private lateinit var followRepository: RemoteFollowRepository

    private val testEmail1 = "testuser1_${UUID.randomUUID()}@example.com"
    private val testEmail2 = "testuser2_${UUID.randomUUID()}@example.com"
    private val testPassword = "Password123!"

    private var user1Id: String? = null
    private var user2Id: String? = null

    @Before
    fun setup() = runBlocking {
        supabaseClient = createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Postgrest)
            install(Auth)
        }
        
        followRepository = RemoteFollowRepository(supabaseClient)

        // Sign up User 2
        supabaseClient.auth.signUpWith(Email) {
            email = testEmail2
            password = testPassword
            data = buildJsonObject { put("full_name", "Test User 2") }
        }
        user2Id = supabaseClient.auth.currentUserOrNull()?.id
        
        // Sign out User 2 so we can sign up User 1
        supabaseClient.auth.signOut()

        // Sign up User 1 (The active user for the test)
        supabaseClient.auth.signUpWith(Email) {
            email = testEmail1
            password = testPassword
            data = buildJsonObject { put("full_name", "Test User 1") }
        }
        user1Id = supabaseClient.auth.currentUserOrNull()?.id
    }

    @Test
    fun testFollowAndUnfollowUser() = runBlocking {
        assertTrue("User 2 must exist", user2Id != null)
        assertTrue("User 1 must be logged in", supabaseClient.auth.currentUserOrNull() != null)

        // 1. Follow User 2
        val followResult = followRepository.followUser(user2Id!!)
        assertTrue(
            "Follow user should succeed but failed with: ${followResult.exceptionOrNull()?.message}", 
            followResult.isSuccess
        )

        // 2. Unfollow User 2
        val unfollowResult = followRepository.unfollowUser(user2Id!!)
        assertTrue(
            "Unfollow user should succeed but failed with: ${unfollowResult.exceptionOrNull()?.message}", 
            unfollowResult.isSuccess
        )
    }

    @After
    fun tearDown() = runBlocking {
        try {
            supabaseClient.auth.signOut()
        } catch (e: Exception) {
            // Ignore teardown errors
        }
    }
}

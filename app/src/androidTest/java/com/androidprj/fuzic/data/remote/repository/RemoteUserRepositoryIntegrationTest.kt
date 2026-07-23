package com.androidprj.fuzic.data.remote.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.androidprj.fuzic.BuildConfig
import com.androidprj.fuzic.model.ui.ProfileUser
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@RunWith(AndroidJUnit4::class)
class RemoteUserRepositoryIntegrationTest {

    private lateinit var supabaseClient: SupabaseClient
    private lateinit var userRepository: RemoteUserRepository

    private val testEmail = "user_repo_test_${UUID.randomUUID()}@example.com"
    private val testPassword = "Password123!"

    private var currentUserId: String? = null

    @Before
    fun setup() = runBlocking {
        supabaseClient = createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Postgrest)
            install(Auth)
        }
        
        userRepository = RemoteUserRepository(supabaseClient)

        // Sign up User
        supabaseClient.auth.signUpWith(Email) {
            email = testEmail
            password = testPassword
            data = buildJsonObject { put("full_name", "Original Name") }
        }
        currentUserId = supabaseClient.auth.currentUserOrNull()?.id
    }

    @Test
    fun testGetUserProfile() = runBlocking {
        assertTrue("User must be logged in", currentUserId != null)

        val result = userRepository.getUserProfile(currentUserId!!)
        
        // This might fail if the `users` table doesn't have an automatic trigger 
        // that inserts a row when an auth user is created.
        assertTrue("Get user profile should succeed: ${result.exceptionOrNull()?.message}", result.isSuccess)
    }

    @Test
    fun testUpdateProfile() = runBlocking {
        assertTrue("User must be logged in", currentUserId != null)

        val updatedUser = ProfileUser(
            id = currentUserId!!,
            displayName = "Updated Test Name",
            username = "updated_test_user",
            avatarUrl = null
        )

        val result = userRepository.updateProfile(updatedUser)
        assertTrue("Update profile should succeed: ${result.exceptionOrNull()?.message}", result.isSuccess)

        val returnedUser = result.getOrThrow()
        assertEquals("Updated Test Name", returnedUser.displayName)
    }

    @Test
    fun testSearchUsers() = runBlocking {
        val result = userRepository.searchUsers("Updated")
        assertTrue("Search users should succeed: ${result.exceptionOrNull()?.message}", result.isSuccess)
    }

    @After
    fun tearDown() = runBlocking {
        try {
            supabaseClient.auth.signOut()
        } catch (e: Exception) {
            // Ignore
        }
    }
}

package com.androidprj.fuzic.data.remote.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.androidprj.fuzic.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * Integration test for RemoteAuthRepository that hits the real backend.
 * Ensure your local.properties has a valid SUPABASE_URL and SUPABASE_ANON_KEY.
 * For local testing, use http://10.0.2.2:54321 for the URL if running in an emulator,
 * or http://127.0.0.1:54321 if running standard JVM tests.
 */
@RunWith(AndroidJUnit4::class)
class RemoteAuthRepositoryIntegrationTest {

    private lateinit var supabaseClient: SupabaseClient
    private lateinit var authRepository: RemoteAuthRepository

    private val testEmail = "testuser_${UUID.randomUUID()}@example.com"
    private val testPassword = "Password123!"
    private val testName = "Test User"

    @Before
    fun setup() {
        supabaseClient = createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Postgrest)
            install(Auth)
        }
        
        authRepository = RemoteAuthRepository(supabaseClient)
    }

    @Test
    fun testSignUpAndLogin() = runBlocking {
        // 1. Sign up the user
        val signUpResult = authRepository.signUp(testEmail, testPassword, testName)
        assertTrue("SignUp should succeed", signUpResult.isSuccess)
        
        // At this point we might be logged in automatically depending on email confirmation settings.
        // Let's force a logout to test login specifically.
        authRepository.logout()

        // 2. Login the user
        val loginResult = authRepository.login(testEmail, testPassword)
        assertTrue("Login should succeed", loginResult.isSuccess)

        // 3. Verify user ID
        val userId = authRepository.getCurrentUserId()
        assertTrue("User ID should not be null after login", userId != null)
    }

    @After
    fun tearDown() = runBlocking {
        // Clean up by signing out. 
        // Note: Realistically, you want a way to delete test users (e.g., via edge function or db reset).
        try {
            authRepository.logout()
        } catch (e: Exception) {
            // Ignore errors on teardown
        }
    }
}

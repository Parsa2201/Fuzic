package com.androidprj.fuzic.data.remote.repository

import androidx.paging.testing.asSnapshot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.androidprj.fuzic.BuildConfig
import com.androidprj.fuzic.model.ui.CreatePlaylistRequest
import com.androidprj.fuzic.model.ui.PlaylistVisibility
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class RemotePlaylistRepositoryIntegrationTest {

    private lateinit var supabaseClient: SupabaseClient
    private lateinit var playlistRepository: RemotePlaylistRepository

    private val testEmail = "testuser_${UUID.randomUUID()}@example.com"
    private val testPassword = "Password123!"

    private var createdPlaylistId: String? = null

    @Before
    fun setup() = runBlocking {
        supabaseClient = createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Postgrest)
            install(Auth)
        }
        
        playlistRepository = RemotePlaylistRepository(supabaseClient)

        // Sign up a user (auto-logs in)
        supabaseClient.auth.signUpWith(Email) {
            email = testEmail
            password = testPassword
        }
    }

    @Test
    fun testCreateAndObservePlaylist() = runBlocking {
        // 1. Create a playlist
        val request = CreatePlaylistRequest(
            title = "Test Playlist ${UUID.randomUUID()}",
            visibility = PlaylistVisibility.Public,
            coverImageUrl = null
        )
        
        val createResult = playlistRepository.createPlaylist(request)
        assertTrue("Playlist creation failed: ${createResult.exceptionOrNull()?.message}", createResult.isSuccess)
        
        val playlist = createResult.getOrThrow()
        createdPlaylistId = playlist.id

        // 2. Observe User Playlists (should include the newly created one)
        val userId = supabaseClient.auth.currentUserOrNull()?.id!!
        val userPlaylists = playlistRepository.observeUserPlaylists(userId).asSnapshot {
            scrollTo(index = 0)
        }
        assertTrue("User playlists should contain the newly created playlist", userPlaylists.any { it.id == playlist.id })
    }

    @Test
    fun testObserveGlobalPlaylists() = runBlocking {
        val globalPlaylists = playlistRepository.observeGlobalPlaylists().asSnapshot {
            scrollTo(index = 0)
        }
        // Just verify it doesn't crash
        assertNotNull(globalPlaylists)
    }

    @After
    fun tearDown() = runBlocking {
        try {
            // Delete playlist if one was created
            createdPlaylistId?.let {
                playlistRepository.deletePlaylist(it)
            }
            supabaseClient.auth.signOut()
        } catch (e: Exception) {
            // Ignore teardown errors
        }
    }
}

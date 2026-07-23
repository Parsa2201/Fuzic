package com.androidprj.fuzic.data.remote.repository

import androidx.paging.testing.asSnapshot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.androidprj.fuzic.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RemoteMusicRepositoryIntegrationTest {

    private lateinit var supabaseClient: SupabaseClient
    private lateinit var musicRepository: RemoteMusicRepository

    @Before
    fun setup() {
        supabaseClient = createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Postgrest)
            install(Auth)
        }
        
        musicRepository = RemoteMusicRepository(supabaseClient)
    }

    @Test
    fun testObserveDailyPicks() = runBlocking {
        // Collect the PagingData as a snapshot list
        val items = musicRepository.observeDailyPicks().asSnapshot {
            // We just fetch the first page
            scrollTo(index = 0)
        }
        
        // Assert it doesn't crash and returns a non-null snapshot.
        // It might be empty if the DB has no songs yet.
        assertNotNull(items)
    }

    @Test
    fun testObserveTrendingSongs() = runBlocking {
        val items = musicRepository.observeTrendingSongs().asSnapshot {
            scrollTo(index = 0)
        }
        assertNotNull(items)
    }

    @Test
    fun testSearchSongs() = runBlocking {
        // Even if no items match, it should return an empty snapshot instead of throwing
        val items = musicRepository.searchSongs("random_test_query").asSnapshot {
            scrollTo(index = 0)
        }
        assertNotNull(items)
    }

    @Test
    fun testGetSongById_NotFound() = runBlocking {
        // Expect a failure when searching for a non-existent UUID
        val result = musicRepository.getSongById("00000000-0000-0000-0000-000000000000")
        assertTrue(result.isFailure)
    }
}

package com.androidprj.fuzic.data.remote.repository

import com.androidprj.fuzic.model.Follow
import com.androidprj.fuzic.model.User
import com.androidprj.fuzic.repository.FollowRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

class RemoteFollowRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) : FollowRepository {

    override suspend fun followUser(followeeId: String): Result<Unit> {
        return try {
            val followerId = supabaseClient.auth.currentUserOrNull()?.id ?: throw Exception("Not logged in")
            val follow = Follow(followerId, followeeId)
            supabaseClient.postgrest["follows"].insert(follow)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unfollowUser(followeeId: String): Result<Unit> {
        return try {
            val followerId = supabaseClient.auth.currentUserOrNull()?.id ?: throw Exception("Not logged in")
            supabaseClient.postgrest["follows"].delete {
                filter {
                    eq("follower_id", followerId)
                    eq("followee_id", followeeId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getFollowers(userId: String): Result<List<User>> {
        return try {
            val users = supabaseClient.postgrest["follows"]
                .select(columns = io.github.jan.supabase.postgrest.query.Columns.raw("follower_id, users!follows_follower_id_fkey(*)")) {
                    filter { eq("followee_id", userId) }
                }
                .decodeList<UserWrapper>()
                .map { it.user }
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getFollowing(userId: String): Result<List<User>> {
        return try {
            val users = supabaseClient.postgrest["follows"]
                .select(columns = io.github.jan.supabase.postgrest.query.Columns.raw("followee_id, users!follows_followee_id_fkey(*)")) {
                    filter { eq("follower_id", userId) }
                }
                .decodeList<UserWrapper>()
                .map { it.user }
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeFollowersCount(userId: String): Flow<Int> {
        // Fallback for realtime counts using standard count queries
        // A complete implementation would combine initial count + realtime deltas
        throw NotImplementedError("To be implemented with complex realtime flow")
    }

    override fun observeFollowingCount(userId: String): Flow<Int> {
        throw NotImplementedError("To be implemented with complex realtime flow")
    }

    @kotlinx.serialization.Serializable
    private data class UserWrapper(
        @kotlinx.serialization.SerialName("users") val user: User
    )
}

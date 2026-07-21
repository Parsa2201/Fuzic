package com.androidprj.fuzic.data.remote.repository

import com.androidprj.fuzic.model.remote.FollowDto
import com.androidprj.fuzic.model.remote.UserDto
import com.androidprj.fuzic.model.ui.FollowUser
import com.androidprj.fuzic.model.mapper.toFollowUser
import com.androidprj.fuzic.repository.FollowRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest

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
            val follow = FollowDto(followerId, followeeId)
            supabaseClient.postgrest["follows"].upsert(follow)
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

    override suspend fun getFollowers(userId: String, offset: Long, limit: Long): Result<List<FollowUser>> {
        return try {
            val users = supabaseClient.postgrest["follows"]
                .select(columns = io.github.jan.supabase.postgrest.query.Columns.raw("follower_id, users!follows_follower_id_fkey(*)")) {
                    filter { eq("followee_id", userId) }
                    range(offset, offset + limit - 1)
                }
                .decodeList<UserWrapper>()
                .map { 
                    val currentUserId = supabaseClient.auth.currentUserOrNull()?.id
                    it.user.toFollowUser(currentUserId) 
                }
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getFollowing(userId: String, offset: Long, limit: Long): Result<List<FollowUser>> {
        return try {
            val users = supabaseClient.postgrest["follows"]
                .select(columns = io.github.jan.supabase.postgrest.query.Columns.raw("followee_id, users!follows_followee_id_fkey(*)")) {
                    filter { eq("follower_id", userId) }
                    range(offset, offset + limit - 1)
                }
                .decodeList<UserWrapper>()
                .map { 
                    val currentUserId = supabaseClient.auth.currentUserOrNull()?.id
                    it.user.toFollowUser(currentUserId) 
                }
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeFollowersCount(userId: String): Flow<Int> {
        return kotlinx.coroutines.flow.flow {
            val count = try {
                supabaseClient.postgrest["follows"]
                    .select {
                        filter { eq("followee_id", userId) }
                    }.decodeList<FollowDto>().size
            } catch (e: Exception) { 0 }
            emit(count)
        }
    }

    override fun observeFollowingCount(userId: String): Flow<Int> {
        return kotlinx.coroutines.flow.flow {
            val count = try {
                supabaseClient.postgrest["follows"]
                    .select {
                        filter { eq("follower_id", userId) }
                    }.decodeList<FollowDto>().size
            } catch (e: Exception) { 0 }
            emit(count)
        }
    }

    @kotlinx.serialization.Serializable
    private data class UserWrapper(
        @kotlinx.serialization.SerialName("users") val user: UserDto
    )
}

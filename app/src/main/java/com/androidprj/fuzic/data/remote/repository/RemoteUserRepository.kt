package com.androidprj.fuzic.data.remote.repository

import com.androidprj.fuzic.model.User
import com.androidprj.fuzic.repository.UserRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject

class RemoteUserRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) : UserRepository {

    override suspend fun getUserProfile(userId: String): Result<User> {
        return try {
            val user = supabaseClient.postgrest["users"]
                .select { filter { eq("id", userId) } }
                .decodeSingle<User>()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProfile(user: User): Result<User> {
        return try {
            val updatedUser = supabaseClient.postgrest["users"]
                .update(user) { filter { eq("id", user.id) } }
                .decodeSingle<User>()
            Result.success(updatedUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchUsers(query: String): Result<List<User>> {
        return try {
            val users = supabaseClient.postgrest["users"]
                .select { filter { ilike("name", "%$query%") } }
                .decodeList<User>()
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

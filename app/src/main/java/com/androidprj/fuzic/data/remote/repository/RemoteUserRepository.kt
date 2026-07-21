package com.androidprj.fuzic.data.remote.repository

import com.androidprj.fuzic.model.remote.UserDto
import com.androidprj.fuzic.model.ui.ProfileUser
import com.androidprj.fuzic.model.mapper.toProfileUser
import com.androidprj.fuzic.repository.UserRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject

class RemoteUserRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) : UserRepository {

    override suspend fun getUserProfile(userId: String): Result<ProfileUser> {
        return try {
            val user = supabaseClient.postgrest["users"]
                .select { filter { eq("id", userId) } }
                .decodeSingle<UserDto>()
            Result.success(user.toProfileUser())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProfile(user: ProfileUser): Result<ProfileUser> {
        return try {
            val updateDto = UpdateProfileDto(
                name = user.displayName,
                username = user.username,
                avatarUrl = user.avatarUrl
            )
            val updatedUser = supabaseClient.postgrest["users"]
                .update(updateDto) { 
                    filter { eq("id", user.id) } 
                    select() // Make sure to return updated row
                }
                .decodeSingle<UserDto>()
            Result.success(updatedUser.toProfileUser())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchUsers(query: String): Result<List<ProfileUser>> {
        return try {
            val users = supabaseClient.postgrest["users"]
                .select { 
                    filter { 
                        or {
                            ilike("name", "%$query%")
                            ilike("username", "%$query%")
                        }
                    } 
                }
                .decodeList<UserDto>()
                .map { it.toProfileUser() }
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    @kotlinx.serialization.Serializable
    private data class UpdateProfileDto(
        val name: String,
        val username: String,
        @kotlinx.serialization.SerialName("avatar_url") val avatarUrl: String?
    )
}

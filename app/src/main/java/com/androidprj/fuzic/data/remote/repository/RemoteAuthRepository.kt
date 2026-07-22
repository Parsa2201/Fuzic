package com.androidprj.fuzic.data.remote.repository

import com.androidprj.fuzic.model.remote.UserDto
import com.androidprj.fuzic.model.mapper.toProfileUser
import com.androidprj.fuzic.model.ui.ProfileUser
import com.androidprj.fuzic.repository.AuthRepository
import com.androidprj.fuzic.repository.PasswordRecoveryRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class RemoteAuthRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) : AuthRepository, PasswordRecoveryRepository {

    override suspend fun requestPasswordReset(email: String): Result<Unit> = runCatching {
        supabaseClient.auth.resetPasswordForEmail(email)
    }

    override suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            supabaseClient.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signUp(email: String, password: String, name: String): Result<Unit> {
        return try {
            supabaseClient.auth.signUpWith(Email) {
                this.email = email
                this.password = password
                this.data = buildJsonObject {
                    put("full_name", name)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            supabaseClient.auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getCurrentUserFlow(): Flow<ProfileUser?> {
        return supabaseClient.auth.sessionStatus.map { status ->
            when (status) {
                is io.github.jan.supabase.auth.status.SessionStatus.Authenticated -> {
                    val authUser = status.session.user
                    authUser?.let {
                        try {
                            // Fetch full profile from users table to get the real DB username
                            supabaseClient.postgrest["users"]
                                .select { filter { eq("id", it.id) } }
                                .decodeSingle<UserDto>()
                                .toProfileUser()
                        } catch (e: Exception) {
                            // Fallback to auth metadata if DB query fails
                            val name = it.userMetadata?.get("full_name")?.let { json ->
                                if (json is kotlinx.serialization.json.JsonPrimitive) json.content else null
                            } ?: "Unknown"
                            ProfileUser(
                                id = it.id,
                                displayName = name,
                                username = it.id.take(8),
                                avatarUrl = it.userMetadata?.get("avatar_url")?.let { json ->
                                    if (json is kotlinx.serialization.json.JsonPrimitive) json.content else null
                                }
                            )
                        }
                    }
                }
                else -> null
            }
        }
    }

    override suspend fun getCurrentUserId(): String? {
        return supabaseClient.auth.currentUserOrNull()?.id
    }
}

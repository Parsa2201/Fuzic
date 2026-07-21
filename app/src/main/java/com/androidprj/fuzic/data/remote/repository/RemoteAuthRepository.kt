package com.androidprj.fuzic.data.remote.repository

import com.androidprj.fuzic.model.User
import com.androidprj.fuzic.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class RemoteAuthRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) : AuthRepository {

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

    override fun getCurrentUserFlow(): Flow<User?> {
        return supabaseClient.auth.sessionStatus.map { status ->
            when (status) {
                is io.github.jan.supabase.auth.status.SessionStatus.Authenticated -> {
                    val user = status.session.user
                    user?.let {
                        User(
                            id = it.id,
                            name = it.userMetadata?.get("full_name")?.kotlinx.serialization.json.jsonPrimitive?.content,
                            avatarUrl = it.userMetadata?.get("avatar_url")?.kotlinx.serialization.json.jsonPrimitive?.content
                        )
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

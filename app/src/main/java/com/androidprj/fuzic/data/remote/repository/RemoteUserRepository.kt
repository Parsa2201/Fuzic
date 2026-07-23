package com.androidprj.fuzic.data.remote.repository

import android.content.Context
import android.net.Uri
import com.androidprj.fuzic.R
import com.androidprj.fuzic.model.ui.AvatarUploadRequest
import com.androidprj.fuzic.model.remote.UserDto
import com.androidprj.fuzic.model.ui.ProfileUser
import com.androidprj.fuzic.model.mapper.toProfileUser
import com.androidprj.fuzic.repository.UserRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class RemoteUserRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
    @ApplicationContext private val appContext: Context,
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

    override suspend fun uploadAvatar(
        userId: String,
        request: AvatarUploadRequest,
        onProgress: (Float) -> Unit,
    ): Result<String> = runCatching {
        onProgress(0.1f)
        val imageBytes = appContext.contentResolver.openInputStream(Uri.parse(request.localUri))
            ?.use { it.readBytes() }
            ?: error(appContext.getString(R.string.edit_profile_avatar_read_error))
        onProgress(0.65f)
        val path = avatarPath(userId, request.uploadId)
        val bucket = supabaseClient.storage.from(AVATARS_BUCKET)
        bucket.upload(path, imageBytes) { upsert = true }
        onProgress(1f)
        bucket.publicUrl(path)
    }

    override suspend fun deleteAvatar(userId: String, avatarUrl: String): Result<Unit> = runCatching {
        avatarPathFromUrl(userId, avatarUrl)?.let { path ->
            supabaseClient.storage.from(AVATARS_BUCKET).delete(path)
        }
    }

    override suspend fun deletePendingAvatar(userId: String, uploadId: String): Result<Unit> = runCatching {
        supabaseClient.storage.from(AVATARS_BUCKET).delete(avatarPath(userId, uploadId))
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

    private fun avatarPath(userId: String, uploadId: String) = "$userId/$uploadId"

    private fun avatarPathFromUrl(userId: String, avatarUrl: String): String? {
        val path = avatarUrl.substringAfter("/$AVATARS_BUCKET/", missingDelimiterValue = "")
            .substringBefore('?')
        return path.takeIf { it.startsWith("$userId/") }
    }

    private companion object {
        const val AVATARS_BUCKET = "avatars"
    }
}

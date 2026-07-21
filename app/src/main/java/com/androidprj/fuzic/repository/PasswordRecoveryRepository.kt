package com.androidprj.fuzic.repository

interface PasswordRecoveryRepository {
    suspend fun requestPasswordReset(email: String): Result<Unit>
}

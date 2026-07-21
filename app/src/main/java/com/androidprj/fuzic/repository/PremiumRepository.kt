package com.androidprj.fuzic.repository

import com.androidprj.fuzic.model.ui.PremiumPlan
import kotlinx.coroutines.flow.Flow

interface PremiumRepository {
    fun observePremiumStatus(): Flow<Boolean>
    suspend fun getPlans(): Result<List<PremiumPlan>>
    suspend fun purchasePlan(planId: String): Result<Unit>
    suspend fun restorePurchase(): Result<Unit>
}

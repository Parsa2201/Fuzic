package com.androidprj.fuzic.data.remote.repository

import com.androidprj.fuzic.model.ui.PremiumPlan
import com.androidprj.fuzic.repository.PremiumRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

@Singleton
class InMemoryPremiumRepository @Inject constructor() : PremiumRepository {
    private val premiumStatus = MutableStateFlow(false)

    override fun observePremiumStatus(): Flow<Boolean> = premiumStatus

    override suspend fun getPlans(): Result<List<PremiumPlan>> = Result.success(emptyList())

    override suspend fun purchasePlan(planId: String): Result<Unit> {
        premiumStatus.value = true
        return Result.success(Unit)
    }

    override suspend fun restorePurchase(): Result<Unit> = Result.success(Unit)
}

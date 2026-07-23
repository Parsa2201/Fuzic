package com.androidprj.fuzic.data.remote.repository

import com.androidprj.fuzic.R
import com.androidprj.fuzic.model.remote.UserDto
import com.androidprj.fuzic.model.ui.PremiumPlan
import com.androidprj.fuzic.repository.PremiumRepository
import com.androidprj.fuzic.util.StringProvider
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Demo purchase implementation: no payment provider is involved, but the resulting
 * premium entitlement is stored in the authenticated user's Supabase profile.
 */
@Singleton
class RemotePremiumRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val stringProvider: StringProvider,
) : PremiumRepository {

    private val premiumStatus = MutableStateFlow(false)

    override fun observePremiumStatus(): StateFlow<Boolean> = premiumStatus

    override suspend fun fetchPremiumStatus(): Result<Boolean> = runCatching {
        refreshPremiumStatus()
        premiumStatus.value
    }

    override suspend fun getPlans(): Result<List<PremiumPlan>> = runCatching {
        refreshPremiumStatus()
        listOf(
            PremiumPlan(
                id = MONTHLY_PLAN_ID,
                title = stringProvider.get(R.string.preview_premium_monthly),
                priceLabel = stringProvider.get(R.string.preview_premium_monthly_price),
                billingLabel = stringProvider.get(R.string.preview_premium_monthly_billing),
            ),
            PremiumPlan(
                id = YEARLY_PLAN_ID,
                title = stringProvider.get(R.string.preview_premium_yearly),
                priceLabel = stringProvider.get(R.string.preview_premium_yearly_price),
                billingLabel = stringProvider.get(R.string.preview_premium_yearly_billing),
                isRecommended = true,
            ),
        )
    }

    override suspend fun purchasePlan(planId: String): Result<Unit> = runCatching {
        require(planId == MONTHLY_PLAN_ID || planId == YEARLY_PLAN_ID)
        val userId = requireCurrentUserId()
        val updatedUser = supabaseClient.postgrest[USERS_TABLE]
            .update(PremiumUpdateDto(isPremium = true)) {
                filter { eq("id", userId) }
                select()
            }
            .decodeSingle<UserDto>()
        premiumStatus.value = updatedUser.isPremium
        check(updatedUser.isPremium)
    }

    override suspend fun restorePurchase(): Result<Unit> = runCatching {
        refreshPremiumStatus()
    }

    private suspend fun refreshPremiumStatus() {
        val user = supabaseClient.postgrest[USERS_TABLE]
            .select { filter { eq("id", requireCurrentUserId()) } }
            .decodeSingle<UserDto>()
        premiumStatus.value = user.isPremium
    }

    private fun requireCurrentUserId(): String =
        requireNotNull(supabaseClient.auth.currentUserOrNull()?.id) { "No authenticated user" }

    @Serializable
    private data class PremiumUpdateDto(
        @SerialName("is_premium") val isPremium: Boolean,
    )

    private companion object {
        const val USERS_TABLE = "users"
        const val MONTHLY_PLAN_ID = "monthly"
        const val YEARLY_PLAN_ID = "yearly"
    }
}

package com.androidprj.fuzic.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.androidprj.fuzic.model.ui.PremiumPlan
import com.androidprj.fuzic.repository.PremiumRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private val Context.premiumDataStore: DataStore<Preferences> by preferencesDataStore(name = "premium_cache")

@Singleton
class LocalPremiumRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val supabaseClient: SupabaseClient
) : PremiumRepository {

    override fun observePremiumStatus(): Flow<Boolean> {
        // Optimistically sync from network in the background
        CoroutineScope(Dispatchers.IO).launch {
            syncPremiumStatus()
        }
        
        return context.premiumDataStore.data.map { preferences ->
            preferences[PreferencesKeys.IS_PREMIUM] ?: false
        }
    }

    private suspend fun syncPremiumStatus() {
        try {
            val userId = supabaseClient.auth.currentUserOrNull()?.id ?: return
            val user = supabaseClient.postgrest["users"]
                .select(columns = io.github.jan.supabase.postgrest.query.Columns.raw("is_premium")) {
                    filter { eq("id", userId) }
                }.decodeSingle<PremiumResponse>()
            
            context.premiumDataStore.edit { prefs ->
                prefs[PreferencesKeys.IS_PREMIUM] = user.isPremium
            }
        } catch (e: Exception) {
            // Ignore network errors during background sync
        }
    }

    override suspend fun getPlans(): Result<List<PremiumPlan>> {
        // Stub plans for now, would typically fetch from RevenueCat/Play Billing
        return Result.success(
            listOf(
                PremiumPlan(id = "1_month", name = "1 Month Premium", priceLabel = "$4.99"),
                PremiumPlan(id = "1_year", name = "1 Year Premium", priceLabel = "$49.99")
            )
        )
    }

    override suspend fun purchasePlan(planId: String): Result<Unit> {
        // Simulated purchase flow
        return try {
            val userId = supabaseClient.auth.currentUserOrNull()?.id ?: throw Exception("Not logged in")
            supabaseClient.postgrest["users"].update(
                { "is_premium" to true }
            ) {
                filter { eq("id", userId) }
            }
            syncPremiumStatus()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun restorePurchase(): Result<Unit> {
        // In a real app, this would query the app store.
        // For now, we just force a sync from Supabase.
        syncPremiumStatus()
        return Result.success(Unit)
    }

    private object PreferencesKeys {
        val IS_PREMIUM = booleanPreferencesKey("is_premium")
    }
    
    @kotlinx.serialization.Serializable
    private data class PremiumResponse(
        @kotlinx.serialization.SerialName("is_premium") val isPremium: Boolean
    )
}

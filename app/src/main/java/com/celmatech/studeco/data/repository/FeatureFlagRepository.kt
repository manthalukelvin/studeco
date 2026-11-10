package com.celmatech.studeco.data.repository

import com.celmatech.studeco.data.model.FeatureFlag
import com.celmatech.studeco.data.model.FeatureFlags
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeatureFlagRepository @Inject constructor(
    private val client: SupabaseClient
) {
    private val _flags = MutableStateFlow(FeatureFlags())
    val flags: StateFlow<FeatureFlags> = _flags.asStateFlow()

    suspend fun refresh() {
        try {
            val rows = client.from("feature_flags")
                .select()
                .decodeList<FeatureFlag>()
            val map = rows.associate { it.key to it.enabled }
            _flags.value = FeatureFlags.fromMap(map)
        } catch (_: Exception) {
            // Keep defaults if fetch fails (offline / RLS)
        }
    }

    fun isEnabled(key: String): Boolean {
        val f = _flags.value
        return when (key) {
            "marketplace" -> f.marketplace
            "messaging" -> f.messaging
            "groups" -> f.groups
            "resources" -> f.resources
            "opportunities" -> f.opportunities
            "ai_tutor" -> f.aiTutor
            "registrations" -> f.registrations
            "public_profiles" -> f.publicProfiles
            "planner" -> f.planner
            "friends" -> f.friends
            "notifications" -> f.notifications
            else -> true
        }
    }
}

package com.celmatech.studeco.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json

object SupabaseClientProvider {
    fun create(url: String, anonKey: String): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = url,
            supabaseKey = anonKey
        ) {
            install(Auth) {
                // Auto-refresh & persist session
            }
            install(Postgrest)
            install(Realtime)
            install(Storage)
            defaultSerializer = KotlinXSerializer(Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
            })
        }
    }
}

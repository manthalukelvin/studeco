package com.celmatech.studeco.data.repository

import com.celmatech.studeco.data.model.NotificationItem
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val client: SupabaseClient
) {
    private fun currentUserId(): String? = client.auth.currentUserOrNull()?.id

    suspend fun getNotifications(limit: Long = 40): Result<List<NotificationItem>> {
        return try {
            val userId = currentUserId() ?: return Result.failure(Exception("Not authenticated"))
            val list = client.from("notifications")
                .select {
                    filter { eq("user_id", userId) }
                    order("created_at", Order.DESCENDING)
                    limit(limit)
                }
                .decodeList<NotificationItem>()
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markRead(id: String): Result<Unit> {
        return try {
            client.from("notifications").update(mapOf("is_read" to true)) {
                filter { eq("id", id) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markAllRead(): Result<Unit> {
        return try {
            val userId = currentUserId() ?: return Result.failure(Exception("Not authenticated"))
            client.from("notifications").update(mapOf("is_read" to true)) {
                filter {
                    eq("user_id", userId)
                    eq("is_read", false)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

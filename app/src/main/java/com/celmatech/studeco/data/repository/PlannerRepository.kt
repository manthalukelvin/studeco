package com.celmatech.studeco.data.repository

import com.celmatech.studeco.data.model.TaskItem
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlannerRepository @Inject constructor(
    private val client: SupabaseClient
) {
    private fun currentUserId(): String? = client.auth.currentUserOrNull()?.id

    suspend fun getTasks(): Result<List<TaskItem>> {
        return try {
            val userId = currentUserId() ?: return Result.failure(Exception("Not authenticated"))
            val list = client.from("tasks")
                .select {
                    filter { eq("user_id", userId) }
                    order("due_date", Order.ASCENDING)
                }
                .decodeList<TaskItem>()
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createTask(title: String, notes: String?, dueDate: String?, category: String = "general"): Result<TaskItem> {
        return try {
            val userId = currentUserId() ?: return Result.failure(Exception("Not authenticated"))
            if (title.isBlank()) return Result.failure(Exception("Title required"))
            val inserted = client.from("tasks")
                .insert(
                    mapOf(
                        "user_id" to userId,
                        "title" to title.trim(),
                        "notes" to notes,
                        "due_date" to dueDate,
                        "category" to category,
                        "is_done" to false
                    )
                ) { select() }
                .decodeSingle<TaskItem>()
            Result.success(inserted)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleDone(task: TaskItem): Result<Boolean> {
        return try {
            val newVal = !task.isDone
            client.from("tasks").update(mapOf("is_done" to newVal)) {
                filter { eq("id", task.id) }
            }
            Result.success(newVal)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTask(id: String): Result<Unit> {
        return try {
            client.from("tasks").delete { filter { eq("id", id) } }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

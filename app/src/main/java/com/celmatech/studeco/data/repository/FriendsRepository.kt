package com.celmatech.studeco.data.repository

import com.celmatech.studeco.data.model.Profile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class Friendship(
    val id: String = "",
    @SerialName("requester_id") val requesterId: String = "",
    @SerialName("addressee_id") val addresseeId: String = "",
    val status: String = "pending",
    @SerialName("created_at") val createdAt: String? = null,
    var otherUser: Profile? = null
)

@Singleton
class FriendsRepository @Inject constructor(
    private val client: SupabaseClient
) {
    private fun currentUserId(): String? = client.auth.currentUserOrNull()?.id

    suspend fun getFriends(): Result<List<Friendship>> {
        return try {
            val userId = currentUserId() ?: return Result.failure(Exception("Not authenticated"))
            val rows = client.from("friendships")
                .select {
                    filter {
                        or {
                            eq("requester_id", userId)
                            eq("addressee_id", userId)
                        }
                        eq("status", "accepted")
                    }
                }
                .decodeList<Friendship>()

            val otherIds = rows.map {
                if (it.requesterId == userId) it.addresseeId else it.requesterId
            }.distinct()

            val profiles = if (otherIds.isNotEmpty()) {
                try {
                    client.from("profiles")
                        .select { filter { isIn("id", otherIds) } }
                        .decodeList<Profile>()
                        .associateBy { it.id }
                } catch (_: Exception) { emptyMap() }
            } else emptyMap()

            Result.success(rows.map { f ->
                val oid = if (f.requesterId == userId) f.addresseeId else f.requesterId
                f.copy(otherUser = profiles[oid])
            })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPendingRequests(): Result<List<Friendship>> {
        return try {
            val userId = currentUserId() ?: return Result.failure(Exception("Not authenticated"))
            val rows = client.from("friendships")
                .select {
                    filter {
                        eq("addressee_id", userId)
                        eq("status", "pending")
                    }
                }
                .decodeList<Friendship>()

            val ids = rows.map { it.requesterId }.distinct()
            val profiles = if (ids.isNotEmpty()) {
                try {
                    client.from("profiles")
                        .select { filter { isIn("id", ids) } }
                        .decodeList<Profile>()
                        .associateBy { it.id }
                } catch (_: Exception) { emptyMap() }
            } else emptyMap()

            Result.success(rows.map { it.copy(otherUser = profiles[it.requesterId]) })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun respond(friendshipId: String, accept: Boolean): Result<Unit> {
        return try {
            client.from("friendships").update(
                mapOf("status" to if (accept) "accepted" else "rejected")
            ) {
                filter { eq("id", friendshipId) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendRequest(toUserId: String): Result<Unit> {
        return try {
            val userId = currentUserId() ?: return Result.failure(Exception("Not authenticated"))
            client.from("friendships").insert(
                mapOf(
                    "requester_id" to userId,
                    "addressee_id" to toUserId,
                    "status" to "pending"
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

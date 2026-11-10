package com.celmatech.studeco.data.repository

import com.celmatech.studeco.data.model.Group
import com.celmatech.studeco.data.model.GroupMember
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupRepository @Inject constructor(
    private val client: SupabaseClient
) {
    private fun currentUserId(): String? = client.auth.currentUserOrNull()?.id

    suspend fun getGroups(limit: Long = 50): Result<List<Group>> {
        return try {
            val groups = client.from("groups")
                .select {
                    order("created_at", Order.DESCENDING)
                    limit(limit)
                }
                .decodeList<Group>()

            val myId = currentUserId()
            val myMemberships = if (myId != null) {
                try {
                    client.from("group_members")
                        .select { filter { eq("user_id", myId) } }
                        .decodeList<GroupMember>()
                        .map { it.groupId }
                        .toSet()
                } catch (_: Exception) {
                    emptySet()
                }
            } else emptySet()

            Result.success(groups.map { g ->
                g.copy(isMember = myMemberships.contains(g.id))
            })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun joinGroup(groupId: String): Result<Unit> {
        return try {
            val userId = currentUserId() ?: return Result.failure(Exception("Not authenticated"))
            client.from("group_members").insert(
                mapOf(
                    "group_id" to groupId,
                    "user_id" to userId,
                    "role" to "member"
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun leaveGroup(groupId: String): Result<Unit> {
        return try {
            val userId = currentUserId() ?: return Result.failure(Exception("Not authenticated"))
            client.from("group_members").delete {
                filter {
                    eq("group_id", groupId)
                    eq("user_id", userId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createGroup(
        name: String,
        description: String?,
        isPrivate: Boolean = false,
        requiresApproval: Boolean = false
    ): Result<Group> {
        return try {
            val userId = currentUserId() ?: return Result.failure(Exception("Not authenticated"))
            if (name.isBlank()) return Result.failure(Exception("Group name required"))
            val inserted = client.from("groups")
                .insert(
                    mapOf(
                        "name" to name.trim(),
                        "description" to (description ?: ""),
                        "is_private" to isPrivate,
                        "requires_approval" to requiresApproval,
                        "created_by" to userId,
                        "group_type" to "study"
                    )
                ) { select() }
                .decodeSingle<Group>()

            // Auto-join as admin
            try {
                client.from("group_members").insert(
                    mapOf(
                        "group_id" to inserted.id,
                        "user_id" to userId,
                        "role" to "admin"
                    )
                )
            } catch (_: Exception) {}

            Result.success(inserted.copy(isMember = true))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

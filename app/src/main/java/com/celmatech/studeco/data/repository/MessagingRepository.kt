package com.celmatech.studeco.data.repository

import com.celmatech.studeco.data.model.ChatMessage
import com.celmatech.studeco.data.model.Conversation
import com.celmatech.studeco.data.model.Profile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessagingRepository @Inject constructor(
    private val client: SupabaseClient
) {
    private fun currentUserId(): String? = client.auth.currentUserOrNull()?.id

    suspend fun getMyConversations(): Result<List<Conversation>> {
        return try {
            val userId = currentUserId() ?: return Result.failure(Exception("Not authenticated"))

            // Get conversation IDs the user belongs to
            val memberships = client.from("conversation_members")
                .select { filter { eq("user_id", userId) } }
                .decodeList<Map<String, String?>>()

            val convIds = memberships.mapNotNull { it["conversation_id"] }
            if (convIds.isEmpty()) return Result.success(emptyList())

            val conversations = client.from("conversations")
                .select {
                    filter { isIn("id", convIds) }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<Conversation>()

            // Enrich with other participant names for 1:1 chats
            val enriched = conversations.map { conv ->
                try {
                    val members = client.from("conversation_members")
                        .select { filter { eq("conversation_id", conv.id) } }
                        .decodeList<Map<String, String?>>()
                    val otherId = members.mapNotNull { it["user_id"] }.firstOrNull { it != userId }
                    val otherName = if (otherId != null) {
                        client.from("profiles")
                            .select { filter { eq("id", otherId) } }
                            .decodeSingleOrNull<Profile>()?.fullName
                    } else null
                    conv.copy(otherUserName = otherName ?: conv.title ?: "Chat")
                } catch (_: Exception) {
                    conv.copy(otherUserName = conv.title ?: "Chat")
                }
            }
            Result.success(enriched)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMessages(conversationId: String, limit: Long = 80): Result<List<ChatMessage>> {
        return try {
            val messages = client.from("messages")
                .select {
                    filter {
                        eq("conversation_id", conversationId)
                        eq("is_deleted", false)
                    }
                    order("created_at", Order.ASCENDING)
                    limit(limit)
                }
                .decodeList<ChatMessage>()

            val senderIds = messages.map { it.senderId }.distinct()
            val senders = if (senderIds.isNotEmpty()) {
                try {
                    client.from("profiles")
                        .select { filter { isIn("id", senderIds) } }
                        .decodeList<Profile>()
                        .associateBy { it.id }
                } catch (_: Exception) {
                    emptyMap()
                }
            } else emptyMap()

            Result.success(messages.map {
                it.copy(senderName = senders[it.senderId]?.fullName ?: "Student")
            })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendMessage(conversationId: String, content: String): Result<ChatMessage> {
        return try {
            val userId = currentUserId() ?: return Result.failure(Exception("Not authenticated"))
            if (content.isBlank()) return Result.failure(Exception("Message cannot be empty"))
            val inserted = client.from("messages")
                .insert(
                    mapOf(
                        "conversation_id" to conversationId,
                        "sender_id" to userId,
                        "content" to content.trim()
                    )
                ) { select() }
                .decodeSingle<ChatMessage>()
            Result.success(inserted.copy(senderName = "You"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Start or find a 1:1 conversation with another user */
    suspend fun startDirectChat(otherUserId: String): Result<String> {
        return try {
            val userId = currentUserId() ?: return Result.failure(Exception("Not authenticated"))
            if (otherUserId == userId) return Result.failure(Exception("Cannot chat with yourself"))

            // Look for existing shared conversation (simple approach)
            val myMemberships = client.from("conversation_members")
                .select { filter { eq("user_id", userId) } }
                .decodeList<Map<String, String?>>()
            val myConvIds = myMemberships.mapNotNull { it["conversation_id"] }.toSet()

            if (myConvIds.isNotEmpty()) {
                val otherMemberships = client.from("conversation_members")
                    .select {
                        filter {
                            eq("user_id", otherUserId)
                            isIn("conversation_id", myConvIds.toList())
                        }
                    }
                    .decodeList<Map<String, String?>>()
                val shared = otherMemberships.mapNotNull { it["conversation_id"] }.firstOrNull()
                if (shared != null) return Result.success(shared)
            }

            // Create new conversation
            val conv = client.from("conversations")
                .insert(mapOf("is_group" to false)) { select() }
                .decodeSingle<Map<String, String?>>()
            val convId = conv["id"] ?: return Result.failure(Exception("Failed to create conversation"))

            client.from("conversation_members").insert(
                listOf(
                    mapOf("conversation_id" to convId, "user_id" to userId),
                    mapOf("conversation_id" to convId, "user_id" to otherUserId)
                )
            )
            Result.success(convId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

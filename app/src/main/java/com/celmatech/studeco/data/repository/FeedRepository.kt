package com.celmatech.studeco.data.repository

import com.celmatech.studeco.data.model.Comment
import com.celmatech.studeco.data.model.Post
import com.celmatech.studeco.data.model.PostLike
import com.celmatech.studeco.data.model.Profile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedRepository @Inject constructor(
    private val client: SupabaseClient
) {
    private fun currentUserId(): String? = client.auth.currentUserOrNull()?.id

    suspend fun getFeed(limit: Long = 40): Result<List<Post>> {
        return try {
            val posts = client.from("posts")
                .select {
                    order("created_at", Order.DESCENDING)
                    limit(limit)
                }
                .decodeList<Post>()

            val authorIds = posts.map { it.authorId }.distinct()
            val authors = if (authorIds.isNotEmpty()) {
                try {
                    client.from("profiles")
                        .select { filter { isIn("id", authorIds) } }
                        .decodeList<Profile>()
                        .associateBy { it.id }
                } catch (_: Exception) {
                    emptyMap()
                }
            } else emptyMap()

            val myId = currentUserId()
            val likedIds = if (myId != null) {
                try {
                    client.from("post_likes")
                        .select { filter { eq("user_id", myId) } }
                        .decodeList<PostLike>()
                        .map { it.postId }
                        .toSet()
                } catch (_: Exception) {
                    emptySet()
                }
            } else emptySet()

            val enriched = posts.map { p ->
                p.copy(
                    authorName = authors[p.authorId]?.fullName ?: "Student",
                    likedByMe = likedIds.contains(p.id)
                )
            }
            Result.success(enriched)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createPost(
        content: String,
        institution: String?,
        visibility: String = "institution",
        educationLevel: String? = null
    ): Result<Post> {
        return try {
            val userId = currentUserId() ?: return Result.failure(Exception("Not authenticated"))
            if (content.isBlank()) return Result.failure(Exception("Post cannot be empty"))

            val row = mapOf(
                "author_id" to userId,
                "content" to content.trim(),
                "institution" to institution,
                "visibility" to visibility,
                "education_level" to educationLevel,
                "post_type" to "discussion"
            )
            val inserted = client.from("posts")
                .insert(row) { select() }
                .decodeSingle<Post>()
            Result.success(inserted.copy(authorName = "You", likedByMe = false))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleLike(postId: String): Result<Boolean> {
        return try {
            val userId = currentUserId() ?: return Result.failure(Exception("Not authenticated"))
            val existing = client.from("post_likes")
                .select {
                    filter {
                        eq("post_id", postId)
                        eq("user_id", userId)
                    }
                }
                .decodeList<PostLike>()

            if (existing.isNotEmpty()) {
                client.from("post_likes").delete {
                    filter {
                        eq("post_id", postId)
                        eq("user_id", userId)
                    }
                }
                Result.success(false)
            } else {
                client.from("post_likes").insert(
                    mapOf("post_id" to postId, "user_id" to userId)
                )
                Result.success(true)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getComments(postId: String): Result<List<Comment>> {
        return try {
            val comments = client.from("post_comments")
                .select {
                    filter { eq("post_id", postId) }
                    order("created_at", Order.ASCENDING)
                }
                .decodeList<Comment>()

            val authorIds = comments.map { it.authorId }.distinct()
            val authors = if (authorIds.isNotEmpty()) {
                try {
                    client.from("profiles")
                        .select { filter { isIn("id", authorIds) } }
                        .decodeList<Profile>()
                        .associateBy { it.id }
                } catch (_: Exception) {
                    emptyMap()
                }
            } else emptyMap()

            Result.success(comments.map {
                it.copy(authorName = authors[it.authorId]?.fullName ?: "Student")
            })
        } catch (e: Exception) {
            Result.success(emptyList())
        }
    }

    suspend fun addComment(postId: String, content: String): Result<Comment> {
        return try {
            val userId = currentUserId() ?: return Result.failure(Exception("Not authenticated"))
            if (content.isBlank()) return Result.failure(Exception("Comment cannot be empty"))
            val inserted = client.from("post_comments")
                .insert(
                    mapOf(
                        "post_id" to postId,
                        "author_id" to userId,
                        "content" to content.trim()
                    )
                ) { select() }
                .decodeSingle<Comment>()
            Result.success(inserted.copy(authorName = "You"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

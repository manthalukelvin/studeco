package com.celmatech.studeco.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String = "",
    @SerialName("full_name") val fullName: String = "",
    val email: String? = null,
    @SerialName("education_level") val educationLevel: String? = null,
    val country: String = "MW",
    val institution: String? = null,
    val programme: String = "",
    val bio: String = "",
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val age: Int? = null,
    @SerialName("is_public") val isPublic: Boolean = true,
    val plan: String = "free",
    val role: String = "student",
    @SerialName("is_verified") val isVerified: Boolean = false,
    @SerialName("email_verified") val emailVerified: Boolean = false,
    @SerialName("institution_verified") val institutionVerified: Boolean = false,
    @SerialName("pending_institution") val pendingInstitution: String? = null,
    @SerialName("is_suspended") val isSuspended: Boolean = false,
    @SerialName("is_minor") val isMinor: Boolean = false,
    @SerialName("last_seen") val lastSeen: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class FeatureFlag(
    val key: String,
    val enabled: Boolean = true,
    val label: String? = null
)

@Serializable
data class Post(
    val id: String = "",
    @SerialName("author_id") val authorId: String = "",
    val content: String = "",
    @SerialName("post_type") val postType: String = "discussion",
    val visibility: String = "institution",
    val institution: String? = null,
    @SerialName("education_level") val educationLevel: String? = null,
    val programme: String? = null,
    @SerialName("group_id") val groupId: String? = null,
    @SerialName("likes_count") val likesCount: Int = 0,
    @SerialName("comments_count") val commentsCount: Int = 0,
    @SerialName("created_at") val createdAt: String? = null,
    // Client-side enrichment
    var authorName: String? = null,
    var likedByMe: Boolean = false
)

@Serializable
data class PostLike(
    val id: String = "",
    @SerialName("post_id") val postId: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class Comment(
    val id: String = "",
    @SerialName("post_id") val postId: String = "",
    @SerialName("author_id") val authorId: String = "",
    val content: String = "",
    @SerialName("created_at") val createdAt: String? = null,
    var authorName: String? = null
)

@Serializable
data class Group(
    val id: String = "",
    val name: String = "",
    val description: String? = null,
    @SerialName("education_level") val educationLevel: String? = null,
    @SerialName("group_type") val groupType: String = "study",
    @SerialName("is_private") val isPrivate: Boolean = false,
    @SerialName("requires_approval") val requiresApproval: Boolean = false,
    @SerialName("is_institution_level") val isInstitutionLevel: Boolean = false,
    @SerialName("cover_url") val coverUrl: String? = null,
    @SerialName("created_by") val createdBy: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    var memberCount: Int = 0,
    var isMember: Boolean = false
)

@Serializable
data class GroupMember(
    @SerialName("group_id") val groupId: String = "",
    @SerialName("user_id") val userId: String = "",
    val role: String = "member",
    @SerialName("joined_at") val joinedAt: String? = null
)

@Serializable
data class MarketplaceListing(
    val id: String = "",
    @SerialName("seller_id") val sellerId: String = "",
    val title: String = "",
    val description: String? = null,
    val price: Double? = null,
    val currency: String = "MWK",
    val category: String? = null,
    val institution: String? = null,
    val status: String = "active",
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    var sellerName: String? = null
)

@Serializable
data class Opportunity(
    val id: String = "",
    val title: String = "",
    val description: String? = null,
    val category: String = "event",
    val institution: String? = null,
    @SerialName("education_level") val educationLevel: String? = null,
    val visibility: String = "public",
    val deadline: String? = null,
    val link: String? = null,
    @SerialName("created_by") val createdBy: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class ResourceItem(
    val id: String = "",
    @SerialName("uploader_id") val uploaderId: String = "",
    val title: String = "",
    val description: String? = null,
    val category: String? = null,
    @SerialName("file_url") val fileUrl: String? = null,
    val institution: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class Conversation(
    val id: String = "",
    @SerialName("is_group") val isGroup: Boolean = false,
    val title: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    var otherUserName: String? = null,
    var lastMessage: String? = null
)

@Serializable
data class ChatMessage(
    val id: String = "",
    @SerialName("conversation_id") val conversationId: String = "",
    @SerialName("sender_id") val senderId: String = "",
    val content: String = "",
    @SerialName("is_deleted") val isDeleted: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    var senderName: String? = null
)

@Serializable
data class TaskItem(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    val title: String = "",
    val notes: String? = null,
    val category: String = "general",
    @SerialName("due_date") val dueDate: String? = null,
    @SerialName("is_done") val isDone: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class NotificationItem(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("actor_id") val actorId: String? = null,
    val type: String = "",
    val title: String = "",
    val body: String? = null,
    val link: String? = null,
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null
)

data class AuthState(
    val isAuthenticated: Boolean = false,
    val isLoading: Boolean = true,
    val user: Profile? = null,
    val needsEmailVerification: Boolean = false,
    val needsProfileCompletion: Boolean = false,
    val error: String? = null
)

data class FeatureFlags(
    val marketplace: Boolean = true,
    val messaging: Boolean = true,
    val groups: Boolean = true,
    val resources: Boolean = true,
    val opportunities: Boolean = true,
    val aiTutor: Boolean = false,
    val registrations: Boolean = true,
    val publicProfiles: Boolean = true,
    val planner: Boolean = true,
    val friends: Boolean = true,
    val notifications: Boolean = true
) {
    companion object {
        fun fromMap(map: Map<String, Boolean>): FeatureFlags = FeatureFlags(
            marketplace = map["marketplace"] ?: true,
            messaging = map["messaging"] ?: true,
            groups = map["groups"] ?: true,
            resources = map["resources"] ?: true,
            opportunities = map["opportunities"] ?: true,
            aiTutor = map["ai_tutor"] ?: false,
            registrations = map["registrations"] ?: true,
            publicProfiles = map["public_profiles"] ?: true,
            planner = map["planner"] ?: true,
            friends = map["friends"] ?: true,
            notifications = map["notifications"] ?: true
        )
    }
}

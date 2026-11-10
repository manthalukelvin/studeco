package com.celmatech.studeco.data.repository

import com.celmatech.studeco.data.model.MarketplaceListing
import com.celmatech.studeco.data.model.Opportunity
import com.celmatech.studeco.data.model.Profile
import com.celmatech.studeco.data.model.ResourceItem
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MarketplaceRepository @Inject constructor(
    private val client: SupabaseClient
) {
    private fun currentUserId(): String? = client.auth.currentUserOrNull()?.id

    suspend fun getListings(limit: Long = 40): Result<List<MarketplaceListing>> {
        return try {
            val listings = client.from("marketplace_listings")
                .select {
                    filter { eq("status", "active") }
                    order("created_at", Order.DESCENDING)
                    limit(limit)
                }
                .decodeList<MarketplaceListing>()

            val sellerIds = listings.map { it.sellerId }.distinct()
            val sellers = if (sellerIds.isNotEmpty()) {
                try {
                    client.from("profiles")
                        .select { filter { isIn("id", sellerIds) } }
                        .decodeList<Profile>()
                        .associateBy { it.id }
                } catch (_: Exception) {
                    emptyMap()
                }
            } else emptyMap()

            Result.success(listings.map {
                it.copy(sellerName = sellers[it.sellerId]?.fullName ?: "Student")
            })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createListing(
        title: String,
        description: String?,
        price: Double?,
        category: String?,
        institution: String?
    ): Result<MarketplaceListing> {
        return try {
            val userId = currentUserId() ?: return Result.failure(Exception("Not authenticated"))
            if (title.isBlank()) return Result.failure(Exception("Title required"))
            val inserted = client.from("marketplace_listings")
                .insert(
                    mapOf(
                        "seller_id" to userId,
                        "title" to title.trim(),
                        "description" to (description ?: ""),
                        "price" to price,
                        "currency" to "MWK",
                        "category" to category,
                        "institution" to institution,
                        "status" to "active"
                    )
                ) { select() }
                .decodeSingle<MarketplaceListing>()
            Result.success(inserted.copy(sellerName = "You"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOpportunities(limit: Long = 40): Result<List<Opportunity>> {
        return try {
            val list = client.from("opportunities")
                .select {
                    order("created_at", Order.DESCENDING)
                    limit(limit)
                }
                .decodeList<Opportunity>()
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getResources(limit: Long = 40): Result<List<ResourceItem>> {
        return try {
            val list = client.from("resources")
                .select {
                    order("created_at", Order.DESCENDING)
                    limit(limit)
                }
                .decodeList<ResourceItem>()
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

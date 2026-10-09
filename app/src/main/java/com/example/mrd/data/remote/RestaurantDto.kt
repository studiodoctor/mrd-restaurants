package com.example.mrd.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * The list is kept as raw [JsonElement]s on purpose: one malformed record (say a string where a
 * number should be) must not fail the entire sync. Records are decoded individually in the mapper.
 */
@Serializable
data class RestaurantsResponse(
    val restaurants: List<JsonElement> = emptyList(),
)

/** Wire format. Everything is nullable because "fields may be missing or null in real data". */
@Serializable
data class RestaurantDto(
    val id: String? = null,
    val name: String? = null,
    val cuisines: List<String>? = null,
    val rating: Double? = null,
    @SerialName("delivery_fee_cents") val deliveryFeeCents: Int? = null,
    @SerialName("eta_minutes") val etaMinutes: Int? = null,
    @SerialName("is_open") val isOpen: Boolean? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    val neighbourhood: String? = null,
    val description: String? = null,
    val address: String? = null,
    /** "HH:mm", 24-hour. */
    @SerialName("opens_at") val opensAt: String? = null,
    @SerialName("closes_at") val closesAt: String? = null,
    @SerialName("price_level") val priceLevel: Int? = null,
    @SerialName("review_count") val reviewCount: Int? = null,
    @SerialName("min_order_cents") val minOrderCents: Int? = null,
    val tags: List<String>? = null,
    @SerialName("distance_km") val distanceKm: Double? = null,
)

package com.example.mrd.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** Categories and their items stay raw so one broken entry only drops itself, not the menu. */
@Serializable
data class MenuResponseDto(
    @SerialName("restaurant_id") val restaurantId: String? = null,
    val categories: List<JsonElement> = emptyList(),
)

@Serializable
data class MenuCategoryDto(
    val id: String? = null,
    val name: String? = null,
    val items: List<JsonElement> = emptyList(),
)

@Serializable
data class MenuItemDto(
    val id: String? = null,
    val name: String? = null,
    val description: String? = null,
    @SerialName("price_cents") val priceCents: Int? = null,
    val available: Boolean? = null,
    val options: List<MenuOptionDto>? = null,
)

@Serializable
data class MenuOptionDto(
    val id: String? = null,
    val name: String? = null,
    @SerialName("price_cents") val priceCents: Int? = null,
)

package com.example.mrd.data.mapper

import com.example.mrd.data.local.MenuItemEntity
import com.example.mrd.data.local.RestaurantEntity
import com.example.mrd.data.local.RestaurantWithFavorite
import com.example.mrd.data.remote.MenuCategoryDto
import com.example.mrd.data.remote.MenuItemDto
import com.example.mrd.data.remote.MenuOptionDto
import com.example.mrd.data.remote.MenuResponseDto
import com.example.mrd.data.remote.RestaurantDto
import com.example.mrd.domain.MenuCategory
import com.example.mrd.domain.MenuItem
import com.example.mrd.domain.MenuOption
import com.example.mrd.domain.Restaurant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/** Separator for lists flattened into one TEXT column (cuisines, tags, menu options). */
internal const val LIST_SEPARATOR = "|"
private const val OPTION_FIELD_SEPARATOR = "~"
private const val MAX_TAGS = 6
private const val MAX_DISTANCE_KM = 500.0
private val HOURS_PATTERN = Regex("""^([01]?\d|2[0-3]):([0-5]\d)$""")

// ---------------------------------------------------------------------------------------------
// Restaurants
// ---------------------------------------------------------------------------------------------

/**
 * Validates and normalises one wire record. Returns null when the record is unusable (no id or
 * no name). Out-of-range optional values become null ("unknown") rather than being displayed.
 */
internal fun RestaurantDto.toEntity(position: Int, syncedAt: Long): RestaurantEntity? {
    val cleanId = id?.trim().orEmpty()
    val cleanName = name?.trim().orEmpty()
    if (cleanId.isEmpty() || cleanName.isEmpty()) return null

    return RestaurantEntity(
        id = cleanId,
        name = cleanName,
        cuisines = cuisines.cleanList().joinToString(LIST_SEPARATOR),
        rating = rating?.takeIf { it in 0.0..5.0 },
        deliveryFeeCents = deliveryFeeCents?.takeIf { it >= 0 },
        etaMinutes = etaMinutes?.takeIf { it > 0 },
        isOpen = isOpen ?: false,
        imageUrl = imageUrl.blankToNull(),
        neighbourhood = neighbourhood.blankToNull(),
        position = position,
        syncedAt = syncedAt,
        description = description.blankToNull(),
        address = address.blankToNull(),
        opensAtMinutes = opensAt.toMinutesOfDay(),
        closesAtMinutes = closesAt.toMinutesOfDay(),
        priceLevel = priceLevel?.takeIf { it in 1..4 },
        reviewCount = reviewCount?.takeIf { it >= 0 },
        minOrderCents = minOrderCents?.takeIf { it >= 0 },
        tags = tags.cleanList().take(MAX_TAGS).joinToString(LIST_SEPARATOR),
        distanceKm = distanceKm?.takeIf { it in 0.0..MAX_DISTANCE_KM },
    )
}

/**
 * Decodes records one by one so a single poisoned record cannot fail the whole sync, drops
 * unusable ones, de-duplicates by id (first wins) and numbers positions contiguously.
 */
internal fun Json.parseRestaurants(elements: List<JsonElement>, syncedAt: Long): List<RestaurantEntity> {
    val seen = HashSet<String>(elements.size)
    val result = ArrayList<RestaurantEntity>(elements.size)
    for (element in elements) {
        val dto = runCatching { decodeFromJsonElement(RestaurantDto.serializer(), element) }.getOrNull()
            ?: continue
        val entity = dto.toEntity(position = result.size, syncedAt = syncedAt) ?: continue
        if (seen.add(entity.id)) result += entity
    }
    return result
}

internal fun RestaurantWithFavorite.toDomain(): Restaurant = Restaurant(
    id = restaurant.id,
    name = restaurant.name,
    cuisines = restaurant.cuisines.splitList(),
    rating = restaurant.rating,
    deliveryFeeCents = restaurant.deliveryFeeCents,
    etaMinutes = restaurant.etaMinutes,
    isOpen = restaurant.isOpen,
    imageUrl = restaurant.imageUrl,
    neighbourhood = restaurant.neighbourhood,
    isFavorite = isFavorite,
    description = restaurant.description,
    address = restaurant.address,
    opensAtMinutes = restaurant.opensAtMinutes,
    closesAtMinutes = restaurant.closesAtMinutes,
    priceLevel = restaurant.priceLevel,
    reviewCount = restaurant.reviewCount,
    minOrderCents = restaurant.minOrderCents,
    tags = restaurant.tags.splitList(),
    distanceKm = restaurant.distanceKm,
)

// ---------------------------------------------------------------------------------------------
// Menus
// ---------------------------------------------------------------------------------------------

/**
 * Flattens a menu response into rows. Categories and items are decoded one at a time so a broken
 * entry only drops itself; items without a name are dropped, duplicate item ids keep the first,
 * a missing price stays null ("price on request") and categories that end up empty vanish
 * naturally because they contribute no rows.
 */
internal fun Json.parseMenu(restaurantId: String, response: MenuResponseDto): List<MenuItemEntity> {
    val result = ArrayList<MenuItemEntity>()
    val seen = HashSet<String>()
    response.categories.forEachIndexed { categoryIndex, categoryElement ->
        val category = runCatching { decodeFromJsonElement(MenuCategoryDto.serializer(), categoryElement) }
            .getOrNull() ?: return@forEachIndexed
        val categoryId = category.id.blankToNull() ?: "c-$categoryIndex"
        val categoryName = category.name.blankToNull() ?: "More"

        for (itemElement in category.items) {
            val dto = runCatching { decodeFromJsonElement(MenuItemDto.serializer(), itemElement) }
                .getOrNull() ?: continue
            val name = dto.name.blankToNull() ?: continue
            val itemId = dto.id.blankToNull() ?: "$categoryId-${result.size}"
            if (!seen.add(itemId)) continue

            result += MenuItemEntity(
                restaurantId = restaurantId,
                id = itemId,
                categoryId = categoryId,
                categoryName = categoryName,
                name = name,
                description = dto.description.blankToNull(),
                priceCents = dto.priceCents?.takeIf { it >= 0 },
                available = dto.available ?: true,
                options = dto.options.encodeOptions(),
                position = result.size,
            )
        }
    }
    return result
}

internal fun MenuItemEntity.toDomain(): MenuItem = MenuItem(
    id = id,
    name = name,
    description = description,
    priceCents = priceCents,
    available = available,
    options = options.decodeOptions(),
)

/** Regroups ordered rows into categories, keeping the server's category and item order. */
internal fun List<MenuItemEntity>.toMenuCategories(): List<MenuCategory> {
    val grouped = LinkedHashMap<String, MutableList<MenuItemEntity>>()
    for (row in this) grouped.getOrPut(row.categoryId) { mutableListOf() } += row
    return grouped.map { (categoryId, rows) ->
        MenuCategory(id = categoryId, name = rows.first().categoryName, items = rows.map { it.toDomain() })
    }
}

private fun List<MenuOptionDto>?.encodeOptions(): String =
    orEmpty().mapNotNull { option ->
        val name = option.name.blankToNull()
            ?.replace(LIST_SEPARATOR, " ")?.replace(OPTION_FIELD_SEPARATOR, " ")
            ?: return@mapNotNull null
        "$name$OPTION_FIELD_SEPARATOR${(option.priceCents ?: 0).coerceAtLeast(0)}"
    }.joinToString(LIST_SEPARATOR)

private fun String.decodeOptions(): List<MenuOption> =
    splitList().mapNotNull { encoded ->
        val name = encoded.substringBeforeLast(OPTION_FIELD_SEPARATOR).trim()
        val price = encoded.substringAfterLast(OPTION_FIELD_SEPARATOR, missingDelimiterValue = "0").toIntOrNull() ?: 0
        if (name.isEmpty()) null else MenuOption(name = name, priceCents = price)
    }

// ---------------------------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------------------------

/** Escapes LIKE wildcards so user input like "100%" or "a_b" is matched literally. */
internal fun String.escapeLike(): String =
    replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

private fun String?.blankToNull(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

/** Trims, drops blanks and duplicates and strips the separator character from each entry. */
private fun List<String>?.cleanList(): List<String> =
    orEmpty().map { it.replace(LIST_SEPARATOR, " ").trim() }.filter { it.isNotEmpty() }.distinct()

private fun String.splitList(): List<String> = split(LIST_SEPARATOR).filter { it.isNotEmpty() }

/** "22:30" -> 1350. Anything that isn't a valid 24-hour HH:mm becomes null (unknown). */
internal fun String?.toMinutesOfDay(): Int? {
    val match = this?.trim()?.let { HOURS_PATTERN.matchEntire(it) } ?: return null
    return match.groupValues[1].toInt() * 60 + match.groupValues[2].toInt()
}

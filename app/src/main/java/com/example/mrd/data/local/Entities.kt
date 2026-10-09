package com.example.mrd.data.local

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "restaurants",
    indices = [Index("position"), Index("isOpen")],
)
data class RestaurantEntity(
    @PrimaryKey val id: String,
    val name: String,
    /** Cuisines joined with [com.example.mrd.data.mapper.LIST_SEPARATOR]. */
    val cuisines: String,
    val rating: Double?,
    val deliveryFeeCents: Int?,
    val etaMinutes: Int?,
    val isOpen: Boolean,
    val imageUrl: String?,
    val neighbourhood: String?,
    /** Server order; gives a stable, index-backed sort for keyset-friendly paging. */
    val position: Int,
    /** Sync generation stamp. Rows not touched by the latest sync are stale and get deleted. */
    val syncedAt: Long,

    // --- added in schema v2 (details page) ------------------------------------------------
    val description: String? = null,
    val address: String? = null,
    val opensAtMinutes: Int? = null,
    val closesAtMinutes: Int? = null,
    val priceLevel: Int? = null,
    val reviewCount: Int? = null,
    val minOrderCents: Int? = null,
    /** Joined with LIST_SEPARATOR. The default lets Room auto-migrate existing v1 rows. */
    @ColumnInfo(defaultValue = "''") val tags: String = "",

    // --- added in schema v3 (distance filter) ----------------------------------------------
    val distanceKm: Double? = null,
)

/**
 * Favourites live in their own table so a list refresh (which REPLACEs restaurant rows) can never
 * clobber user data, and a favourite survives even if a restaurant temporarily disappears.
 */
@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val restaurantId: String,
    val createdAt: Long,
)

data class RestaurantWithFavorite(
    @Embedded val restaurant: RestaurantEntity,
    val isFavorite: Boolean,
)

/**
 * One menu item, flattened with its category so a whole menu is a single indexed query.
 * Menus are cached per restaurant the first time they are opened and replaced on every refresh.
 */
@Entity(
    tableName = "menu_items",
    primaryKeys = ["restaurantId", "id"],
    indices = [Index("restaurantId")],
)
data class MenuItemEntity(
    val restaurantId: String,
    val id: String,
    val categoryId: String,
    val categoryName: String,
    val name: String,
    val description: String?,
    val priceCents: Int?,
    val available: Boolean,
    /** `name~priceCents` pairs joined with LIST_SEPARATOR. */
    val options: String,
    /** Global order within the menu, so categories and items keep the server's order. */
    val position: Int,
)

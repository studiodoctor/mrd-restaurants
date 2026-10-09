package com.example.mrd.domain

/**
 * UI-facing restaurant. Fields that real backends may omit are nullable so the UI is forced
 * to decide how to present "unknown" instead of showing a misleading default (e.g. 0 rating).
 *
 * The new "about" fields all default to "unknown" so callers (previews, tests) only set what
 * they care about.
 */
data class Restaurant(
    val id: String,
    val name: String,
    val cuisines: List<String>,
    val rating: Double?,
    val deliveryFeeCents: Int?,
    val etaMinutes: Int?,
    val isOpen: Boolean,
    val imageUrl: String?,
    val neighbourhood: String?,
    val isFavorite: Boolean,
    val description: String? = null,
    val address: String? = null,
    /** Minutes after midnight, e.g. 22:30 = 1350. Null when the hours are unknown. */
    val opensAtMinutes: Int? = null,
    val closesAtMinutes: Int? = null,
    /** 1 (cheap) .. 4 (expensive). */
    val priceLevel: Int? = null,
    val reviewCount: Int? = null,
    val minOrderCents: Int? = null,
    val tags: List<String> = emptyList(),
    /** Straight-line distance from the user, in km. Null when unknown. */
    val distanceKm: Double? = null,
)

enum class SortOrder { RECOMMENDED, TOP_RATED, FASTEST, NEAREST, CHEAPEST_DELIVERY, MOST_REVIEWED }

/**
 * Everything beyond name search and "open now". "Any" is encoded with a neutral value so the SQL
 * can ignore a filter without nullable parameters: 0 for the minimums / maximums, [ANY_FEE] for the
 * delivery fee (0 is a meaningful value there: free delivery) and "" for the text filters.
 * Serializable so it survives process death in SavedStateHandle.
 */
data class AdvancedFilters(
    val sort: SortOrder = SortOrder.RECOMMENDED,
    val minRating: Double = 0.0,
    val minReviews: Int = 0,
    val maxDistanceKm: Double = 0.0,
    val maxEtaMinutes: Int = 0,
    val maxFeeCents: Int = ANY_FEE,
    val maxPriceLevel: Int = 0,
    val cuisine: String = "",
    val tag: String = "",
) : java.io.Serializable {

    /** Number of narrowing filters in use (sorting does not narrow anything). */
    val activeCount: Int
        get() = listOf(
            minRating > 0, minReviews > 0, maxDistanceKm > 0, maxEtaMinutes > 0,
            maxFeeCents != ANY_FEE, maxPriceLevel > 0, cuisine.isNotEmpty(), tag.isNotEmpty(),
        ).count { it }

    /** What the filter button's badge counts: narrowing filters plus a non-default sort. */
    val badgeCount: Int get() = activeCount + if (sort != SortOrder.RECOMMENDED) 1 else 0

    val isActive: Boolean get() = badgeCount > 0

    companion object {
        const val ANY_FEE = -1
        private const val serialVersionUID = 1L
    }
}

/** Name search and "open now", plus the optional extra filters and sort order. */
data class SearchFilters(
    val query: String = "",
    val openOnly: Boolean = false,
    val advanced: AdvancedFilters = AdvancedFilters(),
) : java.io.Serializable {
    val isActive: Boolean get() = query.isNotBlank() || openOnly || advanced.isActive
}

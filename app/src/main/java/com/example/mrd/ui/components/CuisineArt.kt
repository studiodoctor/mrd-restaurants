package com.example.mrd.ui.components

import androidx.compose.ui.graphics.Color

/**
 * Generated "cover art" for a restaurant: a cuisine emoji on a warm gradient.
 *
 * Why not rely on photos alone? Photos are optional in the data, slow on a bad connection and
 * absent offline. Deterministic artwork means every card looks intentional in every state, costs
 * no network or memory, and is the placeholder while a real photo loads.
 */
internal data class CuisineArt(val emoji: String, val start: Color, val end: Color)

private val Gradients = listOf(
    Color(0xFFFFB199) to Color(0xFFFF6A5B), // coral
    Color(0xFFFFD27F) to Color(0xFFF59E0B), // amber
    Color(0xFF9BE3C7) to Color(0xFF1FA37E), // mint
    Color(0xFFFFC2D4) to Color(0xFFE5578A), // rose
    Color(0xFFB9C8FF) to Color(0xFF6D83F2), // periwinkle
    Color(0xFFFFE08A) to Color(0xFFF29D38), // sunset
    Color(0xFFC9F0A8) to Color(0xFF6FBF3B), // lime
    Color(0xFFE0C3FC) to Color(0xFF9B6BE0), // lilac
)

/** First keyword that matches any of the restaurant's cuisines wins; order = specificity. */
private val Emojis = listOf(
    listOf("sushi", "japanese") to "🍣",
    listOf("pizza", "pasta", "italian") to "🍕",
    listOf("burger", "american") to "🍔",
    listOf("steak", "grill", "braai") to "🥩",
    listOf("curry", "indian") to "🍛",
    listOf("korean") to "🍲",
    listOf("thai", "noodle", "chinese", "asian") to "🍜",
    listOf("mexican", "tex-mex") to "🌮",
    listOf("chicken", "portuguese") to "🍗",
    listOf("vegan", "vegetarian", "healthy") to "🥗",
    listOf("seafood", "fish") to "🦐",
    listOf("dessert", "ice cream", "bakery") to "🍦",
    listOf("cafe", "breakfast", "coffee") to "☕",
    listOf("mediterranean", "greek", "middle eastern") to "🥙",
)

internal fun cuisineArtFor(cuisines: List<String>, fallbackSeed: String): CuisineArt {
    val lowered = cuisines.map { it.lowercase() }
    val emoji = Emojis.firstOrNull { (keywords, _) ->
        lowered.any { cuisine -> keywords.any { keyword -> cuisine.contains(keyword) } }
    }?.second ?: "🍽️"

    // Same primary cuisine => same colours, so the list reads as a coherent system, not noise.
    val seed = cuisines.firstOrNull() ?: fallbackSeed
    val (start, end) = Gradients[(seed.hashCode() and Int.MAX_VALUE) % Gradients.size]
    return CuisineArt(emoji, start, end)
}

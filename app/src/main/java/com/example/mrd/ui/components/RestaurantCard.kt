@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.mrd.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mrd.domain.Restaurant
import com.example.mrd.ui.theme.MrDTheme

/**
 * Pure, stateless list item. It takes plain values and lambdas only, so it is previewable, cheap to
 * skip during recomposition, and trivially reusable by other squads' screens.
 *
 * Layout: a 16:9 cover (artwork or photo) carrying the glanceable badges — status top-left,
 * favourite top-right, delivery time bottom-right — over a compact text block.
 */
@Composable
fun RestaurantCard(
    restaurant: Restaurant,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.5f)),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
        ) {
            RestaurantImage(
                url = restaurant.imageUrl,
                name = restaurant.name,
                cuisines = restaurant.cuisines,
                dimmed = !restaurant.isOpen,
                modifier = Modifier.fillMaxSize(),
            )
            // Keeps the top badges legible on bright photos.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.28f), Color.Transparent))),
            )
            StatusChip(
                isOpen = restaurant.isOpen,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp),
            )
            FavoriteButton(
                isFavorite = restaurant.isFavorite,
                restaurantName = restaurant.name,
                onClick = onFavoriteClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp),
            )
            InfoPill(
                text = etaLabel(restaurant.etaMinutes),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp),
            )
        }

        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = restaurant.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                RatingBadge(restaurant.rating)
            }

            val subtitle = listOfNotNull(
                restaurant.cuisines.takeIf { it.isNotEmpty() }?.joinToString(" · "),
                restaurant.neighbourhood,
            ).joinToString("  •  ")
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            val free = restaurant.deliveryFeeCents == 0
            Text(
                text = "🛵  " + listOfNotNull(
                    deliveryFeeLabel(restaurant.deliveryFeeCents),
                    distanceLabel(restaurant.distanceKm),
                ).joinToString("  •  "),
                style = MaterialTheme.typography.labelLarge,
                color = if (free) colors.secondary else colors.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF8F4)
@Composable
private fun RestaurantCardPreview() {
    MrDTheme {
        RestaurantCard(
            restaurant = Restaurant(
                id = "r-1001",
                name = "Cape Town Grill",
                cuisines = listOf("Steakhouse", "Grill"),
                rating = 4.6,
                deliveryFeeCents = 0,
                etaMinutes = 32,
                isOpen = true,
                imageUrl = null,
                neighbourhood = "Sea Point",
                isFavorite = true,
            ),
            onClick = {},
            onFavoriteClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF8F4)
@Composable
private fun ClosedRestaurantCardPreview() {
    MrDTheme {
        RestaurantCard(
            restaurant = Restaurant(
                id = "r-1002",
                name = "Sushi on Sea Point",
                cuisines = listOf("Sushi", "Japanese"),
                rating = null,
                deliveryFeeCents = 1500,
                etaMinutes = null,
                isOpen = false,
                imageUrl = null,
                neighbourhood = null,
                isFavorite = false,
            ),
            onClick = {},
            onFavoriteClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

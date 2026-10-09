@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.mrd.ui.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.mrd.R
import com.example.mrd.domain.AdvancedFilters
import com.example.mrd.domain.SortOrder
import com.example.mrd.ui.components.formatRand

private val RatingOptions = listOf(0.0, 3.5, 4.0, 4.5)
private val ReviewOptions = listOf(0, 100, 500, 1000)
private val DistanceOptions = listOf(0.0, 1.0, 3.0, 5.0, 10.0)
private val EtaOptions = listOf(0, 20, 30, 45)
private val FeeOptions = listOf(AdvancedFilters.ANY_FEE, 0, 2000, 3000)
private val PriceOptions = listOf(0, 1, 2, 3, 4)
private val CuisineOptions = listOf(
    "Burgers", "Pizza", "Sushi", "Indian", "Chinese", "Thai", "Korean", "Mexican", "Seafood",
    "Steakhouse", "Chicken", "Vegan", "Mediterranean", "Dessert", "Cafe",
)
private val TagOptions = listOf("Halaal", "Vegetarian friendly", "Vegan options", "Gluten-free options")

/**
 * Bottom sheet with every optional filter. Changes apply live (the list behind it updates as you
 * tap), so there is no separate "apply" step to forget; "Reset" clears them and "Done" closes.
 */
@Composable
fun FilterSheet(
    filters: AdvancedFilters,
    onChange: (AdvancedFilters) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            Row(
                modifier = Modifier.padding(start = 24.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.filter_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { heading() },
                )
                TextButton(onClick = { onChange(AdvancedFilters()) }, enabled = filters.isActive) {
                    Text(stringResource(R.string.filter_reset))
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Group(stringResource(R.string.filter_sort)) {
                    SortOrder.entries.forEach { order ->
                        Option(stringResource(order.labelRes()), filters.sort == order) { onChange(filters.copy(sort = order)) }
                    }
                }
                Group(stringResource(R.string.filter_rating)) {
                    RatingOptions.forEach { value ->
                        val label = if (value == 0.0) stringResource(R.string.filter_any) else stringResource(R.string.filter_rating_value, value.toString())
                        Option(label, filters.minRating == value) { onChange(filters.copy(minRating = value)) }
                    }
                }
                Group(stringResource(R.string.filter_reviews)) {
                    ReviewOptions.forEach { value ->
                        val label = if (value == 0) stringResource(R.string.filter_any) else stringResource(R.string.filter_reviews_value, value)
                        Option(label, filters.minReviews == value) { onChange(filters.copy(minReviews = value)) }
                    }
                }
                Group(stringResource(R.string.filter_distance)) {
                    DistanceOptions.forEach { value ->
                        val label = if (value == 0.0) stringResource(R.string.filter_any) else stringResource(R.string.filter_distance_value, value.toInt())
                        Option(label, filters.maxDistanceKm == value) { onChange(filters.copy(maxDistanceKm = value)) }
                    }
                }
                Group(stringResource(R.string.filter_eta)) {
                    EtaOptions.forEach { value ->
                        val label = if (value == 0) stringResource(R.string.filter_any) else stringResource(R.string.filter_eta_value, value)
                        Option(label, filters.maxEtaMinutes == value) { onChange(filters.copy(maxEtaMinutes = value)) }
                    }
                }
                Group(stringResource(R.string.filter_fee)) {
                    FeeOptions.forEach { value ->
                        val label = when (value) {
                            AdvancedFilters.ANY_FEE -> stringResource(R.string.filter_any)
                            0 -> stringResource(R.string.fee_free)
                            else -> stringResource(R.string.filter_up_to, formatRand(value))
                        }
                        Option(label, filters.maxFeeCents == value) { onChange(filters.copy(maxFeeCents = value)) }
                    }
                }
                Group(stringResource(R.string.filter_price)) {
                    PriceOptions.forEach { value ->
                        val label = if (value == 0) stringResource(R.string.filter_any) else "R".repeat(value)
                        Option(label, filters.maxPriceLevel == value) { onChange(filters.copy(maxPriceLevel = value)) }
                    }
                }
                Group(stringResource(R.string.filter_cuisine)) {
                    Option(stringResource(R.string.filter_any), filters.cuisine.isEmpty()) { onChange(filters.copy(cuisine = "")) }
                    CuisineOptions.forEach { value ->
                        Option(value, filters.cuisine == value) { onChange(filters.copy(cuisine = value)) }
                    }
                }
                Group(stringResource(R.string.filter_dietary)) {
                    Option(stringResource(R.string.filter_any), filters.tag.isEmpty()) { onChange(filters.copy(tag = "")) }
                    TagOptions.forEach { value ->
                        Option(value, filters.tag == value) { onChange(filters.copy(tag = value)) }
                    }
                }
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .fillMaxWidthCompat(),
            ) { Text(stringResource(R.string.filter_done)) }
        }
    }
}

private fun Modifier.fillMaxWidthCompat(): Modifier = this.then(Modifier.fillMaxWidth())

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            content()
        }
    }
}

@Composable
private fun Option(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        shape = CircleShape,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = colors.surfaceContainerLowest,
            selectedContainerColor = colors.primaryContainer,
            selectedLabelColor = colors.onPrimaryContainer,
        ),
    )
}

private fun SortOrder.labelRes(): Int = when (this) {
    SortOrder.RECOMMENDED -> R.string.sort_recommended
    SortOrder.TOP_RATED -> R.string.sort_top_rated
    SortOrder.FASTEST -> R.string.sort_fastest
    SortOrder.NEAREST -> R.string.sort_nearest
    SortOrder.CHEAPEST_DELIVERY -> R.string.sort_cheapest_delivery
    SortOrder.MOST_REVIEWED -> R.string.sort_most_reviewed
}

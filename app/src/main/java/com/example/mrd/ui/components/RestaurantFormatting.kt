package com.example.mrd.ui.components

import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.example.mrd.R
import java.util.Locale

/** Prices are integer cents end to end; formatting happens only here, at the edge. */
internal fun formatRand(cents: Int): String {
    val rands = cents / 100
    val remainder = cents % 100
    return "R$rands.${remainder.toString().padStart(2, '0')}"
}

internal fun formatRating(rating: Double): String = String.format(Locale.US, "%.1f", rating)

@Composable
fun deliveryFeeLabel(cents: Int?): String = when {
    cents == null -> stringResource(R.string.fee_unknown)
    cents == 0 -> stringResource(R.string.fee_free)
    else -> stringResource(R.string.fee_format, formatRand(cents))
}

@Composable
fun etaLabel(minutes: Int?): String =
    if (minutes == null) stringResource(R.string.eta_unknown) else stringResource(R.string.eta_format, minutes)

@Composable
fun ratingLabel(rating: Double?): String =
    if (rating == null) stringResource(R.string.rating_unrated) else stringResource(R.string.rating_format, formatRating(rating))

@Composable
fun ratingContentDescription(rating: Double?): String =
    if (rating == null) stringResource(R.string.rating_unrated)
    else stringResource(R.string.rating_content_description, formatRating(rating))

@Composable
fun relativeTimeLabel(epochMillis: Long): String =
    remember(epochMillis) {
        DateUtils.getRelativeTimeSpanString(
            epochMillis,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
        ).toString()
    }

/** 1350 -> "22:30". */
internal fun formatMinutesOfDay(minutes: Int): String =
    String.format(Locale.US, "%02d:%02d", minutes / 60, minutes % 60)

/** Value for the "delivery fee" stat tile: short, because the tile is small. */
@Composable
fun deliveryFeeTileLabel(cents: Int?): String = when {
    cents == null -> stringResource(R.string.tile_unknown)
    cents == 0 -> stringResource(R.string.tile_free)
    else -> formatRand(cents)
}

@Composable
fun minOrderTileLabel(cents: Int?): String = when {
    cents == null -> stringResource(R.string.tile_unknown)
    cents == 0 -> stringResource(R.string.tile_none)
    else -> formatRand(cents)
}

/** "Closes at 22:00" while open, "Opens at 09:00" while closed; null when the hours are unknown. */
@Composable
fun hoursStatusLabel(isOpen: Boolean, opensAtMinutes: Int?, closesAtMinutes: Int?): String? = when {
    isOpen && closesAtMinutes != null -> stringResource(R.string.detail_closes_at, formatMinutesOfDay(closesAtMinutes))
    !isOpen && opensAtMinutes != null -> stringResource(R.string.detail_opens_at, formatMinutesOfDay(opensAtMinutes))
    else -> null
}

internal fun formatDistance(km: Double): String = String.format(Locale.US, "%.1f", km)

@Composable
fun distanceLabel(km: Double?): String? = km?.let { stringResource(R.string.distance_km, formatDistance(it)) }

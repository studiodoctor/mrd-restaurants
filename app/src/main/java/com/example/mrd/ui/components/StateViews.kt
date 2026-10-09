package com.example.mrd.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mrd.R
import com.example.mrd.domain.FailureReason

/** Centered emoji badge + title + body + optional action. Backbone of empty / error / not-found states. */
@Composable
fun MessageState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    emoji: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (emoji != null) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                    .clearAndSetSemantics { },
                contentAlignment = Alignment.Center,
            ) {
                Text(text = emoji, fontSize = 44.sp)
            }
            Spacer(Modifier.height(20.dp))
        }
        Text(text = title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(24.dp))
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

/** Full-screen failure when there is nothing cached to fall back on. */
@Composable
fun ErrorState(reason: FailureReason, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    MessageState(
        title = stringResource(R.string.error_title),
        body = stringResource(reason.bodyRes()),
        emoji = reason.emoji(),
        actionLabel = stringResource(R.string.action_retry),
        onAction = onRetry,
        modifier = modifier,
    )
}

@Composable
fun EmptyResultsState(filtersActive: Boolean, onClearFilters: () -> Unit, modifier: Modifier = Modifier) {
    MessageState(
        title = stringResource(R.string.empty_title),
        body = stringResource(R.string.empty_filtered_body),
        emoji = "🔍",
        actionLabel = if (filtersActive) stringResource(R.string.empty_clear) else null,
        onAction = if (filtersActive) onClearFilters else null,
        modifier = modifier,
    )
}

/**
 * Shown above the list when a refresh failed but cached data is still on screen. This is the
 * visible half of the offline baseline: the user keeps browsing and can retry in place. Amber
 * rather than red: it's a heads-up, not a dead end.
 */
@Composable
fun StaleDataBanner(
    reason: FailureReason,
    lastSyncedAt: Long?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = Icons.Filled.Warning, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stringResource(reason.bannerRes()), style = MaterialTheme.typography.bodyMedium)
                if (lastSyncedAt != null) {
                    Text(
                        text = stringResource(R.string.last_updated, relativeTimeLabel(lastSyncedAt)),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            TextButton(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
        }
    }
}

/** Shared pulse for every skeleton on screen; the value is read in draw scope only (no recomposition). */
@Composable
private fun rememberSkeletonPulse(): androidx.compose.runtime.State<Float> {
    val transition = rememberInfiniteTransition(label = "skeleton")
    return transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 800), RepeatMode.Reverse),
        label = "pulse",
    )
}

/** Placeholder cards (same shape as the real ones) shown while the first load is in flight. */
@Composable
fun RestaurantListSkeleton(modifier: Modifier = Modifier, rows: Int = 3) {
    val pulse = rememberSkeletonPulse()
    val description = stringResource(R.string.loading_restaurants)
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .semantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        repeat(rows) { SkeletonCard(alpha = { pulse.value }) }
    }
}

@Composable
private fun SkeletonCard(alpha: () -> Float) {
    val block = MaterialTheme.colorScheme.surfaceVariant
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { this.alpha = alpha() }
            .background(MaterialTheme.colorScheme.surfaceContainerLowest, MaterialTheme.shapes.large),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(block, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
        )
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.fillMaxWidth(0.55f).height(16.dp).background(block, RoundedCornerShape(6.dp)))
            Box(Modifier.fillMaxWidth(0.4f).height(12.dp).background(block, RoundedCornerShape(6.dp)))
            Box(Modifier.fillMaxWidth(0.3f).height(12.dp).background(block, RoundedCornerShape(6.dp)))
        }
    }
}

/** Compact menu-row placeholders for the details page. */
@Composable
fun MenuSkeleton(modifier: Modifier = Modifier, rows: Int = 4) {
    val pulse = rememberSkeletonPulse()
    val block = MaterialTheme.colorScheme.surfaceVariant
    val description = stringResource(R.string.menu_loading)
    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .semantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        repeat(rows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = pulse.value }
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest, MaterialTheme.shapes.medium)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.fillMaxWidth(0.6f).height(14.dp).background(block, RoundedCornerShape(6.dp)))
                    Box(Modifier.fillMaxWidth(0.9f).height(10.dp).background(block, RoundedCornerShape(6.dp)))
                }
                Box(Modifier.width(48.dp).height(14.dp).background(block, RoundedCornerShape(6.dp)))
            }
        }
    }
}

private fun FailureReason.bodyRes(): Int = when (this) {
    FailureReason.NETWORK -> R.string.error_network_body
    FailureReason.SERVER -> R.string.error_server_body
    FailureReason.INVALID_DATA -> R.string.error_invalid_body
}

private fun FailureReason.bannerRes(): Int = when (this) {
    FailureReason.NETWORK -> R.string.banner_offline
    FailureReason.SERVER -> R.string.banner_server
    FailureReason.INVALID_DATA -> R.string.banner_invalid
}

private fun FailureReason.emoji(): String = when (this) {
    FailureReason.NETWORK -> "📡"
    FailureReason.SERVER -> "🛠️"
    FailureReason.INVALID_DATA -> "🤔"
}

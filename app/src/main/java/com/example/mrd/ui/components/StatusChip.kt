package com.example.mrd.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.mrd.R

/**
 * Pill with a status dot. Open is conveyed by text and shape as well as colour, so it is
 * readable without colour vision, and it holds its contrast over photos and artwork.
 */
@Composable
fun StatusChip(isOpen: Boolean, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val container = if (isOpen) colors.secondaryContainer else colors.surfaceVariant
    val content = if (isOpen) colors.onSecondaryContainer else colors.onSurfaceVariant
    val dot = if (isOpen) colors.secondary else colors.outline

    Surface(
        modifier = modifier,
        color = container,
        contentColor = content,
        shape = CircleShape,
    ) {
        Row(
            modifier = Modifier.padding(start = 8.dp, end = 10.dp, top = 3.dp, bottom = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(7.dp).background(dot, CircleShape))
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(if (isOpen) R.string.status_open else R.string.status_closed),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

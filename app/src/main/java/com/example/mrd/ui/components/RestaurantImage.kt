package com.example.mrd.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * Real food photography first. The restaurant's own [url] is tried, then a cuisine-matched stock
 * photo (see [stockPhotoUrl]), so every card shows a photo. While a photo loads you see a plain
 * brand-coloured gradient (no icons); the cuisine emoji appears only if every photo failed, e.g.
 * on first launch with no connection. Coil caches downloaded photos on disk, so after the first
 * online visit they also show offline. Coil decodes at the composable's measured size, keeping
 * list thumbnails small in memory.
 *
 * @param dimmed greys the tile out (used for closed restaurants).
 */
@Composable
fun RestaurantImage(
    url: String?,
    name: String,
    cuisines: List<String>,
    modifier: Modifier = Modifier,
    emojiSize: TextUnit = 56.sp,
    dimmed: Boolean = false,
) {
    val art = remember(cuisines, name) { cuisineArtFor(cuisines, name) }
    val candidates = remember(url, cuisines, name) {
        listOfNotNull(url?.takeIf { it.isNotBlank() }, stockPhotoUrl(cuisines, name)).distinct()
    }
    var attempt by remember(candidates) { mutableIntStateOf(0) }
    val current = candidates.getOrNull(attempt)

    Box(modifier = modifier.clipToBounds(), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Brush.linearGradient(listOf(art.start, art.end), start = Offset.Zero, end = Offset(size.width, size.height)))
        }
        if (current == null) {
            // Every photo failed (offline, first run): fall back to the cuisine emoji.
            Text(text = art.emoji, fontSize = emojiSize)
        } else {
            val context = LocalContext.current
            val request = remember(current) { ImageRequest.Builder(context).data(current).crossfade(true).build() }
            AsyncImage(
                model = request,
                contentDescription = null, // decorative: the name is always next to it
                contentScale = ContentScale.Crop,
                colorFilter = if (dimmed) GreyscaleFilter else null,
                onError = { attempt++ },
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (dimmed) {
            Box(Modifier.fillMaxSize().background(DimColor))
        }
    }
}

private val GreyscaleFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
private val DimColor = Color(0xFF2A1A15).copy(alpha = 0.38f)

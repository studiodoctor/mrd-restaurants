@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package com.example.mrd.ui.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import com.example.mrd.ui.components.ratingLabel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mrd.R
import com.example.mrd.domain.FailureReason
import com.example.mrd.domain.MenuCategory
import com.example.mrd.domain.MenuItem
import com.example.mrd.domain.Restaurant
import com.example.mrd.ui.components.ErrorState
import com.example.mrd.ui.components.FavoriteButton
import com.example.mrd.ui.components.MenuSkeleton
import com.example.mrd.ui.components.MessageState
import com.example.mrd.ui.components.PriceLevel
import com.example.mrd.ui.components.RatingBadge
import com.example.mrd.ui.components.RestaurantImage
import com.example.mrd.ui.components.StatTile
import com.example.mrd.ui.components.StatusChip
import com.example.mrd.ui.components.TagPill
import com.example.mrd.ui.components.deliveryFeeTileLabel
import com.example.mrd.ui.components.distanceLabel
import com.example.mrd.ui.components.etaLabel
import com.example.mrd.ui.components.formatMinutesOfDay
import com.example.mrd.ui.components.formatRand
import com.example.mrd.ui.components.hoursStatusLabel
import com.example.mrd.ui.components.minOrderTileLabel
import kotlinx.coroutines.launch

@Composable
fun DetailRoute(
    onBack: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val menuState by viewModel.menuState.collectAsStateWithLifecycle()
    DetailScreen(
        state = state,
        menuState = menuState,
        onBack = onBack,
        onRetry = viewModel::retry,
        onRetryMenu = viewModel::retryMenu,
        onFavoriteClick = viewModel::onFavoriteClick,
    )
}

@Composable
fun DetailScreen(
    state: DetailUiState,
    menuState: MenuUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onRetryMenu: () -> Unit,
    onFavoriteClick: () -> Unit,
) {
    // A Surface (not a bare Box) so text defaults to onBackground, not black; without it every
    // heading without an explicit colour was unreadable in dark theme.
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
      Box(Modifier.fillMaxSize()) {
        when (state) {
            DetailUiState.Loading -> {
                val description = stringResource(R.string.loading_restaurants)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .semantics { contentDescription = description },
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
                FloatingBackButton(onBack)
            }

            is DetailUiState.Content -> DetailContent(
                restaurant = state.restaurant,
                menuState = menuState,
                onBack = onBack,
                onFavoriteClick = onFavoriteClick,
                onRetryMenu = onRetryMenu,
            )

            DetailUiState.NotFound -> {
                MessageState(
                    title = stringResource(R.string.detail_not_found_title),
                    body = stringResource(R.string.detail_not_found_body),
                    emoji = "🍽️",
                    actionLabel = stringResource(R.string.detail_back_to_list),
                    onAction = onBack,
                )
                FloatingBackButton(onBack)
            }

            is DetailUiState.Error -> {
                ErrorState(reason = state.reason, onRetry = onRetry)
                FloatingBackButton(onBack)
            }
        }
      }
    }
}

@Composable
private fun FloatingBackButton(onBack: () -> Unit) {
    Box(
        Modifier
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) { BackButton(onBack) }
}

@Composable
private fun BackButton(onBack: () -> Unit) {
    IconButton(
        onClick = onBack,
        modifier = Modifier.size(40.dp),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.92f),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.detail_back))
    }
}

/** One scrollable page = a flat list of rows, so the menu can be lazy and category chips can jump to it. */
private sealed interface Row_ {
    val key: String

    data object Hero : Row_ { override val key = "hero" }
    data object Header : Row_ { override val key = "header" }
    data object Stats : Row_ { override val key = "stats" }
    data object About : Row_ { override val key = "about" }
    data object MenuTitle : Row_ { override val key = "menu-title" }
    data object Chips : Row_ { override val key = "chips" }
    data object MenuLoading : Row_ { override val key = "menu-loading" }
    data object MenuEmpty : Row_ { override val key = "menu-empty" }
    data class MenuError(val reason: FailureReason) : Row_ { override val key = "menu-error" }
    data class CategoryTitle(val category: MenuCategory) : Row_ { override val key = "cat-${category.id}" }
    data class Dish(val category: MenuCategory, val item: MenuItem) : Row_ {
        override val key = "item-${category.id}-${item.id}"
    }
    data object Footnote : Row_ { override val key = "footnote" }
}

@Composable
private fun DetailContent(
    restaurant: Restaurant,
    menuState: MenuUiState,
    onBack: () -> Unit,
    onFavoriteClick: () -> Unit,
    onRetryMenu: () -> Unit,
) {
    val rows = remember(menuState) {
        buildList<Row_> {
            add(Row_.Hero)
            add(Row_.Header)
            add(Row_.Stats)
            add(Row_.About)
            add(Row_.MenuTitle)
            when (menuState) {
                MenuUiState.Loading -> add(Row_.MenuLoading)
                MenuUiState.Empty -> add(Row_.MenuEmpty)
                is MenuUiState.Error -> add(Row_.MenuError(menuState.reason))
                is MenuUiState.Loaded -> {
                    if (menuState.categories.size > 1) add(Row_.Chips)
                    menuState.categories.forEach { category ->
                        add(Row_.CategoryTitle(category))
                        category.items.forEach { add(Row_.Dish(category, it)) }
                    }
                    add(Row_.Footnote)
                }
            }
        }
    }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // The top bar is transparent over the hero photo and turns solid (with the restaurant's name)
    // as the page scrolls, so you always know where you are and can go back or favourite.
    val heroFadePx = with(LocalDensity.current) { 180.dp.toPx() }
    val barAlpha by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / heroFadePx).coerceIn(0f, 1f)
        }
    }
    // Once the in-page category chips scroll away, the same chips move into the pinned bar.
    val chipsIndex = rows.indexOfFirst { it is Row_.Chips }
    val showPinnedChips by remember(chipsIndex) {
        derivedStateOf { chipsIndex >= 0 && listState.firstVisibleItemIndex >= chipsIndex }
    }
    var barHeightPx by remember { mutableIntStateOf(0) }
    val jumpTo: (MenuCategory) -> Unit = { category ->
        val target = rows.indexOfFirst { it is Row_.CategoryTitle && it.category.id == category.id }
        // A negative offset leaves room for the pinned bar so the heading isn't hidden under it.
        if (target >= 0) scope.launch { listState.animateScrollToItem(target, -barHeightPx) }
    }

    Box(Modifier.fillMaxSize()) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        items(rows, key = { it.key }) { row ->
            when (row) {
                Row_.Hero -> Hero(restaurant)
                Row_.Header -> Header(restaurant)
                Row_.Stats -> Stats(restaurant)
                Row_.About -> About(restaurant)
                Row_.MenuTitle -> MenuTitle(menuState)
                Row_.Chips -> {
                    val categories = (menuState as MenuUiState.Loaded).categories
                    CategoryChips(categories, jumpTo)
                }
                Row_.MenuLoading -> MenuSkeleton(Modifier.padding(top = 4.dp))
                Row_.MenuEmpty -> InlineMessage(
                    emoji = "🍳",
                    title = stringResource(R.string.menu_empty_title),
                    body = stringResource(R.string.menu_empty_body),
                )
                is Row_.MenuError -> InlineMessage(
                    emoji = "📡",
                    title = stringResource(R.string.menu_error_title),
                    body = stringResource(
                        when (row.reason) {
                            FailureReason.NETWORK -> R.string.error_network_body
                            FailureReason.SERVER -> R.string.error_server_body
                            FailureReason.INVALID_DATA -> R.string.error_invalid_body
                        },
                    ),
                    actionLabel = stringResource(R.string.action_retry),
                    onAction = onRetryMenu,
                )
                is Row_.CategoryTitle -> CategoryTitle(row.category)
                is Row_.Dish -> DishRow(row.item)
                Row_.Footnote -> Text(
                    text = stringResource(R.string.menu_prices_note),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                )
            }
        }
    }

    DetailTopBar(
        restaurant = restaurant,
        categories = (menuState as? MenuUiState.Loaded)?.categories.orEmpty(),
        alpha = barAlpha,
        showChips = showPinnedChips,
        onBack = onBack,
        onFavoriteClick = onFavoriteClick,
        onJumpTo = jumpTo,
        onHeightChange = { barHeightPx = it },
    )
    }
}

/** Back + name + favourite, optionally with the menu's category chips underneath. */
@Composable
private fun DetailTopBar(
    restaurant: Restaurant,
    categories: List<MenuCategory>,
    alpha: Float,
    showChips: Boolean,
    onBack: () -> Unit,
    onFavoriteClick: () -> Unit,
    onJumpTo: (MenuCategory) -> Unit,
    onHeightChange: (Int) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { onHeightChange(it.height) }
            .background(colors.background.copy(alpha = alpha))
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackButton(onBack)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
                    .alpha(alpha),
            ) {
                Text(
                    text = restaurant.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = listOf(
                        ratingLabel(restaurant.rating),
                        stringResource(if (restaurant.isOpen) R.string.status_open else R.string.status_closed),
                    ).joinToString("  ·  "),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            FavoriteButton(
                isFavorite = restaurant.isFavorite,
                restaurantName = restaurant.name,
                onClick = onFavoriteClick,
            )
        }
        AnimatedVisibility(visible = showChips && categories.size > 1) {
            Column {
                CategoryChips(categories, onJumpTo)
                HorizontalDivider(
                    modifier = Modifier.padding(top = 8.dp),
                    color = colors.outlineVariant.copy(alpha = 0.6f),
                )
            }
        }
        if (!(showChips && categories.size > 1)) {
            HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.6f * alpha))
        }
    }
}

@Composable
private fun Hero(restaurant: Restaurant) {
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f),
    ) {
        RestaurantImage(
            url = restaurant.imageUrl,
            name = restaurant.name,
            cuisines = restaurant.cuisines,
            dimmed = !restaurant.isOpen,
            emojiSize = 96.sp,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(110.dp)
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.35f), Color.Transparent))),
        )
        // The sheet below "rises" into the hero with a rounded top edge.
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(28.dp)
                .background(
                    MaterialTheme.colorScheme.background,
                    RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                ),
        )
    }
}

@Composable
private fun Header(restaurant: Restaurant) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatusChip(isOpen = restaurant.isOpen)
            hoursStatusLabel(restaurant.isOpen, restaurant.opensAtMinutes, restaurant.closesAtMinutes)?.let {
                Text(text = it, style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
            }
        }

        Text(text = restaurant.name, style = MaterialTheme.typography.headlineLarge, color = colors.onSurface)

        val subtitle = listOfNotNull(
            restaurant.cuisines.takeIf { it.isNotEmpty() }?.joinToString(" · "),
            restaurant.neighbourhood,
            distanceLabel(restaurant.distanceKm),
        ).joinToString("  •  ")
        if (subtitle.isNotEmpty()) {
            Text(text = subtitle, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RatingBadge(restaurant.rating)
            restaurant.reviewCount?.takeIf { it > 0 }?.let {
                Text(
                    text = pluralStringResource(R.plurals.detail_reviews, it, it),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
            restaurant.priceLevel?.takeIf { it in 1..4 }?.let { PriceLevel(it) }
        }

        if (restaurant.tags.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                restaurant.tags.forEachIndexed { index, tag -> TagPill(text = tag, index = index) }
            }
        }
    }
}

@Composable
private fun Stats(restaurant: Restaurant) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatTile(
            emoji = "⏱️",
            label = stringResource(R.string.detail_eta),
            value = etaLabel(restaurant.etaMinutes),
            modifier = Modifier.weight(1f),
        )
        StatTile(
            emoji = "🛵",
            label = stringResource(R.string.detail_fee),
            value = deliveryFeeTileLabel(restaurant.deliveryFeeCents),
            modifier = Modifier.weight(1f),
        )
        StatTile(
            emoji = "🧾",
            label = stringResource(R.string.detail_min_order),
            value = minOrderTileLabel(restaurant.minOrderCents),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun About(restaurant: Restaurant) {
    val hasHours = restaurant.opensAtMinutes != null && restaurant.closesAtMinutes != null
    if (restaurant.description.isNullOrBlank() && restaurant.address.isNullOrBlank() && !hasHours) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = stringResource(R.string.detail_about), style = MaterialTheme.typography.headlineSmall)
        restaurant.description?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (hasHours || !restaurant.address.isNullOrBlank()) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    if (hasHours) {
                        InfoRow(
                            emoji = "🕒",
                            label = stringResource(R.string.detail_hours),
                            value = stringResource(
                                R.string.detail_hours_value,
                                formatMinutesOfDay(restaurant.opensAtMinutes!!),
                                formatMinutesOfDay(restaurant.closesAtMinutes!!),
                            ),
                        )
                    }
                    restaurant.address?.takeIf { it.isNotBlank() }?.let {
                        InfoRow(emoji = "📍", label = stringResource(R.string.detail_address), value = it)
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(emoji: String, label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Text(text = emoji, fontSize = 18.sp) }
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text = value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun MenuTitle(menuState: MenuUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 12.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = stringResource(R.string.menu_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.weight(1f),
        )
        if (menuState is MenuUiState.Loaded) {
            val count = menuState.categories.sumOf { it.items.size }
            Text(
                text = pluralStringResource(R.plurals.menu_item_count, count, count),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CategoryChips(categories: List<MenuCategory>, onSelect: (MenuCategory) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 8.dp),
    ) {
        items(categories, key = { it.id }) { category ->
            AssistChip(
                onClick = { onSelect(category) },
                label = { Text(category.name) },
                shape = CircleShape,
            )
        }
    }
}

@Composable
private fun CategoryTitle(category: MenuCategory) {
    Text(
        text = category.name,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun DishRow(item: MenuItem) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .alpha(if (item.available) 1f else 0.55f),
        shape = MaterialTheme.shapes.medium,
        color = colors.surfaceContainerLowest,
        border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.4f)),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = item.name, style = MaterialTheme.typography.titleSmall)
                item.description?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (item.options.isNotEmpty()) {
                    Text(
                        text = item.options.joinToString("  ·  ") { "${it.name} +${formatRand(it.priceCents)}" },
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.secondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = item.priceCents?.let(::formatRand) ?: stringResource(R.string.menu_price_on_request),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (item.priceCents != null) colors.primary else colors.onSurfaceVariant,
                )
                if (!item.available) {
                    Text(
                        text = stringResource(R.string.menu_sold_out),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun InlineMessage(
    emoji: String,
    title: String,
    body: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text = emoji, fontSize = 36.sp)
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

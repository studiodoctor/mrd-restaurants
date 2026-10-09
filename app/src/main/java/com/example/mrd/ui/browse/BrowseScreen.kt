@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.mrd.ui.browse

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.filter
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.example.mrd.R
import com.example.mrd.domain.AdvancedFilters
import com.example.mrd.domain.Restaurant
import com.example.mrd.ui.components.EmptyResultsState
import com.example.mrd.ui.components.PopularCard
import com.example.mrd.ui.components.ErrorState
import com.example.mrd.ui.components.RestaurantCard
import com.example.mrd.ui.components.RestaurantListSkeleton
import com.example.mrd.ui.components.StaleDataBanner
import kotlinx.coroutines.Dispatchers

/** Stateful entry point: owns the ViewModel, hands plain state + lambdas to [BrowseScreen]. */
@Composable
fun BrowseRoute(
    onRestaurantClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: BrowseViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val popular by viewModel.popular.collectAsStateWithLifecycle()
    // Collected on Main.immediate so the text field never sees a stale value between keystrokes
    // (a dispatch hop here is what makes cursors jump in state-flow-backed text fields).
    val query by viewModel.query.collectAsStateWithLifecycle(context = Dispatchers.Main.immediate)
    val restaurants = viewModel.restaurants.collectAsLazyPagingItems()

    BrowseScreen(
        state = state,
        query = query,
        restaurants = restaurants,
        popular = popular,
        onQueryChange = viewModel::onQueryChange,
        onOpenOnlyChange = viewModel::onOpenOnlyChange,
        onAdvancedFiltersChange = viewModel::onAdvancedFiltersChange,
        onClearFilters = viewModel::clearFilters,
        onRefresh = viewModel::refresh,
        onFavoriteClick = viewModel::onFavoriteClick,
        onRestaurantClick = onRestaurantClick,
        onSettingsClick = onSettingsClick,
    )
}

@Composable
fun BrowseScreen(
    state: BrowseUiState,
    query: String,
    restaurants: LazyPagingItems<Restaurant>,
    popular: List<Restaurant>,
    onQueryChange: (String) -> Unit,
    onOpenOnlyChange: (Boolean) -> Unit,
    onAdvancedFiltersChange: (AdvancedFilters) -> Unit,
    onClearFilters: () -> Unit,
    onRefresh: () -> Unit,
    onFavoriteClick: (id: String, favorite: Boolean) -> Unit,
    onRestaurantClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
) {
    // rememberLazyListState is saveable: scroll position survives rotation, process death and
    // navigating to the detail screen and back. It is hoisted above the state `when` below so
    // swapping skeleton/empty/list doesn't throw it away.
    val listState = rememberLazyListState()

    // A *change* in filters should show results from the top; first composition (including a
    // restore) must not reset the restored scroll position.
    val filterKey = "${state.filters.query}|${state.filters.openOnly}|${state.filters.advanced}"
    var appliedFilterKey by rememberSaveable { mutableStateOf(filterKey) }
    LaunchedEffect(filterKey) {
        if (appliedFilterKey != filterKey) {
            appliedFilterKey = filterKey
            listState.scrollToItem(0)
        }
    }

    var showFilters by rememberSaveable { mutableStateOf(false) }
    if (showFilters) {
        FilterSheet(
            filters = state.filters.advanced,
            onChange = onAdvancedFiltersChange,
            onDismiss = { showFilters = false },
        )
    }

    // The title, search bar and filter chips slide away and fade as the list scrolls forward, and
    // come back as soon as the user scrolls the other way (or reaches the top).
    val collapsing = remember { CollapsingHeaderState() }
    LaunchedEffect(filterKey) { collapsing.reset() }
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0 }
            .filter { atTop -> atTop }
            .collect { collapsing.reset() }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .nestedScroll(collapsing.connection),
        ) {
            // Only the big headline slides away; search, filters and settings stay put.
            Column(modifier = Modifier.collapsing(collapsing)) {
                BrowseHeader(restaurantCount = state.cachedCount)
            }
            PinnedBar(
                query = query,
                onQueryChange = onQueryChange,
                onSettingsClick = onSettingsClick,
            )
            FilterRow(
                openOnly = state.filters.openOnly,
                onOpenOnlyChange = onOpenOnlyChange,
                filterCount = state.filters.advanced.badgeCount,
                onFiltersClick = { showFilters = true },
            )

            val refresh = state.refresh
            if (refresh is RefreshUi.Failed && (state.cachedCount ?: 0) > 0) {
                StaleDataBanner(
                    reason = refresh.reason,
                    lastSyncedAt = state.lastSyncedAt,
                    onRetry = onRefresh,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                BrowseContent(
                    state = state,
                    restaurants = restaurants,
                    popular = popular,
                    listState = listState,
                    onRefresh = onRefresh,
                    onClearFilters = onClearFilters,
                    onFavoriteClick = onFavoriteClick,
                    onRestaurantClick = onRestaurantClick,
                )
            }
        }
    }
}

/**
 * Quick-return header: [offset] goes from 0 (fully shown) to -height (hidden). The connection
 * only observes scrolling (it consumes nothing), so the list scrolls exactly as before.
 */
@Stable
private class CollapsingHeaderState {
    var offset by mutableFloatStateOf(0f)
    // Plain field on purpose: written during measure, read in draw/scroll callbacks.
    var height = 0f

    fun reset() {
        offset = 0f
    }

    val connection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (height > 0f) offset = (offset + available.y).coerceIn(-height, 0f)
            return Offset.Zero
        }
    }
}

/** Shrinks the layout height by the collapsed amount, slides the content up and fades it. */
private fun Modifier.collapsing(state: CollapsingHeaderState): Modifier = this
    .layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        state.height = placeable.height.toFloat()
        val visible = (placeable.height + state.offset).roundToInt().coerceAtLeast(0)
        layout(placeable.width, visible) { placeable.placeRelative(0, state.offset.roundToInt()) }
    }
    .clipToBounds()
    .graphicsLayer {
        // Fade a little faster than the slide so nothing is left half-hidden under the list.
        val fraction = if (state.height > 0f) 1f + state.offset / state.height else 1f
        alpha = (fraction * 1.6f - 0.6f).coerceIn(0f, 1f)
    }

/**
 * The state machine for the main area. Order matters: "do we have *anything* cached?" is decided
 * first (skeleton vs hard error), then "does the *filtered* list have rows?" (empty vs list).
 */
@Composable
private fun BrowseContent(
    state: BrowseUiState,
    restaurants: LazyPagingItems<Restaurant>,
    popular: List<Restaurant>,
    listState: LazyListState,
    onRefresh: () -> Unit,
    onClearFilters: () -> Unit,
    onFavoriteClick: (id: String, favorite: Boolean) -> Unit,
    onRestaurantClick: (String) -> Unit,
) {
    val cached = state.cachedCount
    val refresh = state.refresh
    when {
        cached == null -> RestaurantListSkeleton()
        cached == 0 && refresh is RefreshUi.Loading -> RestaurantListSkeleton()
        cached == 0 && refresh is RefreshUi.Failed -> ErrorState(reason = refresh.reason, onRetry = onRefresh)
        restaurants.itemCount == 0 && restaurants.loadState.refresh is LoadState.Loading -> RestaurantListSkeleton()
        restaurants.itemCount == 0 -> EmptyResultsState(
            filtersActive = state.filters.isActive,
            onClearFilters = onClearFilters,
        )
        else -> PullToRefreshBox(
            isRefreshing = refresh is RefreshUi.Loading,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            RestaurantList(
                restaurants = restaurants,
                // The carousel is a browsing aid: hide it as soon as the user is searching/filtering.
                popular = if (state.filters.isActive) emptyList() else popular,
                listState = listState,
                onFavoriteClick = onFavoriteClick,
                onRestaurantClick = onRestaurantClick,
            )
        }
    }
}

@Composable
private fun RestaurantList(
    restaurants: LazyPagingItems<Restaurant>,
    popular: List<Restaurant>,
    listState: LazyListState,
    onFavoriteClick: (id: String, favorite: Boolean) -> Unit,
    onRestaurantClick: (String) -> Unit,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (popular.size >= MIN_POPULAR_TO_SHOW) {
            item(key = "popular", contentType = "popular") {
                PopularSection(popular, onRestaurantClick, onFavoriteClick)
            }
        }
        items(
            count = restaurants.itemCount,
            // Stable keys keep scroll position and item state correct when the list re-queries
            // (filter change, favourite toggle, background refresh) and let rows animate in place.
            key = restaurants.itemKey { it.id },
            contentType = restaurants.itemContentType { "restaurant" },
        ) { index ->
            val restaurant = restaurants[index]
            if (restaurant != null) {
                RestaurantCard(
                    restaurant = restaurant,
                    onClick = { onRestaurantClick(restaurant.id) },
                    onFavoriteClick = { onFavoriteClick(restaurant.id, !restaurant.isFavorite) },
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .animateItem(),
                )
            }
        }
    }
}

private const val MIN_POPULAR_TO_SHOW = 3

@Composable
private fun PopularSection(
    popular: List<Restaurant>,
    onRestaurantClick: (String) -> Unit,
    onFavoriteClick: (id: String, favorite: Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text(
                text = stringResource(R.string.popular_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.popular_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            // Cards need room for their elevation shadow.
            modifier = Modifier.padding(bottom = 4.dp),
        ) {
            itemsIndexed(popular, key = { _, r -> r.id }) { index, restaurant ->
                PopularCard(
                    restaurant = restaurant,
                    rank = index + 1,
                    onClick = { onRestaurantClick(restaurant.id) },
                    onFavoriteClick = { onFavoriteClick(restaurant.id, !restaurant.isFavorite) },
                )
            }
        }
        Text(
            text = stringResource(R.string.all_restaurants),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp)
                .semantics { heading() },
        )
    }
}

/** The big, friendly headline. This is the part that collapses on scroll. */
@Composable
private fun BrowseHeader(restaurantCount: Int?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp),
    ) {
        Text(
            text = stringResource(R.string.browse_kicker),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.browse_headline),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics { heading() },
        )
        if (restaurantCount != null && restaurantCount > 0) {
            val resources = LocalContext.current.resources
            val subtitle = remember(restaurantCount) {
                resources.getQuantityString(R.plurals.restaurants_count, restaurantCount, restaurantCount)
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Always visible: search and settings. */
@Composable
private fun PinnedBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSettingsClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SearchField(query = query, onQueryChange = onQueryChange, modifier = Modifier.weight(1f))
        Surface(
            onClick = onSettingsClick,
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            shadowElevation = 4.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = stringResource(R.string.settings_open),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val focusManager = LocalFocusManager.current
    val colors = MaterialTheme.colorScheme
    var focused by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(
        targetValue = if (focused) colors.primary else Color.Transparent,
        label = "searchBorder",
    )
    val iconBackground by animateColorAsState(
        targetValue = if (focused) colors.primary else colors.primaryContainer,
        label = "searchIconBg",
    )
    val iconTint by animateColorAsState(
        targetValue = if (focused) colors.onPrimary else colors.onPrimaryContainer,
        label = "searchIconTint",
    )
    val searchDescription = stringResource(R.string.search_hint)

    // A floating "pill": soft shadow, large radius, a tinted icon badge that fills with the brand
    // colour on focus, and a quiet clear button. BasicTextField gives full control of the layout.
    Surface(
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(24.dp),
        color = colors.surfaceContainerLowest,
        shadowElevation = if (focused) 8.dp else 4.dp,
        border = BorderStroke(2.dp, borderColor),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 6.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(iconBackground, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp)
                    .onFocusChanged { focused = it.isFocused }
                    .semantics { contentDescription = searchDescription },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
                cursorBrush = SolidColor(colors.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
                            Text(
                                text = stringResource(R.string.search_hint),
                                style = MaterialTheme.typography.bodyLarge,
                                color = colors.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        inner()
                    }
                },
            )
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Filled.Clear,
                        contentDescription = stringResource(R.string.clear_search),
                        tint = colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterRow(
    openOnly: Boolean,
    onOpenOnlyChange: (Boolean) -> Unit,
    filterCount: Int,
    onFiltersClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = filterCount > 0,
            onClick = onFiltersClick,
            label = {
                Text(
                    if (filterCount > 0) stringResource(R.string.filters_chip_count, filterCount)
                    else stringResource(R.string.filters_chip),
                )
            },
            leadingIcon = {
                Icon(
                    painterResource(R.drawable.ic_tune),
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                )
            },
            shape = CircleShape,
            colors = FilterChipDefaults.filterChipColors(
                containerColor = colors.surfaceContainerLowest,
                selectedContainerColor = colors.primaryContainer,
                selectedLabelColor = colors.onPrimaryContainer,
            ),
        )
        FilterChip(
            selected = openOnly,
            onClick = { onOpenOnlyChange(!openOnly) },
            label = { Text(stringResource(R.string.filter_open_now)) },
            leadingIcon = if (openOnly) {
                { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
            } else {
                null
            },
            shape = CircleShape,
            colors = FilterChipDefaults.filterChipColors(
                containerColor = colors.surfaceContainerLowest,
                selectedContainerColor = colors.secondaryContainer,
                selectedLabelColor = colors.onSecondaryContainer,
                selectedLeadingIconColor = colors.onSecondaryContainer,
            ),
        )
    }
}

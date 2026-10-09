package com.example.mrd.testutil

import androidx.paging.PagingData
import com.example.mrd.domain.FilterStore
import com.example.mrd.domain.MenuCategory
import com.example.mrd.domain.RefreshResult
import com.example.mrd.domain.Restaurant
import com.example.mrd.domain.RestaurantRepository
import com.example.mrd.domain.SearchFilters
import com.example.mrd.domain.ThemeMode
import com.example.mrd.domain.ThemeStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

fun restaurant(
    id: String = "r-1",
    name: String = "Cape Town Grill",
    isOpen: Boolean = true,
    isFavorite: Boolean = false,
) = Restaurant(
    id = id,
    name = name,
    cuisines = listOf("Steakhouse", "Grill"),
    rating = 4.6,
    deliveryFeeCents = 1500,
    etaMinutes = 32,
    isOpen = isOpen,
    imageUrl = null,
    neighbourhood = "Sea Point",
    isFavorite = isFavorite,
)

class FakeRestaurantRepository : RestaurantRepository {
    val restaurants = MutableStateFlow<Map<String, Restaurant>>(emptyMap())
    val count = MutableStateFlow(0)
    override val lastSyncedAt = MutableStateFlow<Long?>(null)

    var nextRefresh: RefreshResult = RefreshResult.Success(0)
    var onRefresh: () -> Unit = {}
    var refreshCalls = 0
        private set
    val requestedFilters = mutableListOf<SearchFilters>()

    override fun pagedRestaurants(filters: SearchFilters): Flow<PagingData<Restaurant>> {
        requestedFilters += filters
        return flowOf(PagingData.from(restaurants.value.values.toList()))
    }

    override fun observeRestaurant(id: String): Flow<Restaurant?> = restaurants.map { it[id] }

    override fun observeCount(): Flow<Int> = count

    override fun observePopular(limit: Int): Flow<List<Restaurant>> =
        restaurants.map { map -> map.values.filter { it.isOpen && (it.rating ?: 0.0) >= 4.3 }.take(limit) }

    override fun observeFavorites(): Flow<List<Restaurant>> =
        restaurants.map { map -> map.values.filter { it.isFavorite } }

    val menus = MutableStateFlow<Map<String, List<MenuCategory>>>(emptyMap())
    var nextMenuRefresh: RefreshResult = RefreshResult.Success(0)
    var onMenuRefresh: () -> Unit = {}
    var menuRefreshCalls = 0
        private set

    override fun observeMenu(restaurantId: String): Flow<List<MenuCategory>> =
        menus.map { it[restaurantId].orEmpty() }

    override suspend fun refreshMenu(restaurantId: String): RefreshResult {
        menuRefreshCalls++
        onMenuRefresh()
        return nextMenuRefresh
    }

    override suspend fun refresh(): RefreshResult {
        refreshCalls++
        onRefresh()
        return nextRefresh
    }

    override suspend fun setFavorite(id: String, favorite: Boolean) {
        restaurants.update { map ->
            val current = map[id] ?: return@update map
            map + (id to current.copy(isFavorite = favorite))
        }
    }
}

class FakeFilterStore(var saved: SearchFilters = SearchFilters()) : FilterStore {
    override suspend fun load(): SearchFilters = saved
    override suspend fun save(filters: SearchFilters) {
        saved = filters
    }
}

class FakeThemeStore(initial: ThemeMode = ThemeMode.SYSTEM) : ThemeStore {
    private val state = MutableStateFlow(initial)
    override val themeMode: Flow<ThemeMode> = state
    override suspend fun setThemeMode(mode: ThemeMode) {
        state.value = mode
    }
}

/** Swaps Dispatchers.Main so `viewModelScope` runs under the test scheduler. */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}

package com.example.mrd.domain

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for restaurant data. The database is authoritative; the network only
 * ever refreshes it. That is what gives us the offline baseline: every read path works without
 * connectivity once a sync has succeeded.
 */
interface RestaurantRepository {

    /** Filtered, paged view over the local cache. Cheap to re-create when filters change. */
    fun pagedRestaurants(filters: SearchFilters): Flow<PagingData<Restaurant>>

    /** Emits null when the restaurant is not (yet) in the cache. */
    fun observeRestaurant(id: String): Flow<Restaurant?>

    /** A handful of open, highly rated, much-reviewed restaurants for the "Popular" carousel. */
    fun observePopular(limit: Int = 5): Flow<List<Restaurant>>

    /** The user's favourites (most recent first), from the cache so they work offline. */
    fun observeFavorites(): Flow<List<Restaurant>>

    /** Number of cached restaurants, independent of any filter. */
    fun observeCount(): Flow<Int>

    /** Epoch millis of the last successful sync, or null if we have never synced. */
    val lastSyncedAt: Flow<Long?>

    /** Pulls the latest list from the network into the cache. Never throws for expected failures. */
    suspend fun refresh(): RefreshResult

    suspend fun setFavorite(id: String, favorite: Boolean)

    /** Cached menu for a restaurant, grouped into categories. Empty when nothing is cached. */
    fun observeMenu(restaurantId: String): Flow<List<MenuCategory>>

    /** Fetches the menu and replaces the cached copy. Same no-throw contract as [refresh]. */
    suspend fun refreshMenu(restaurantId: String): RefreshResult
}

sealed interface RefreshResult {
    data class Success(val count: Int) : RefreshResult
    data class Failure(val reason: FailureReason) : RefreshResult
}

enum class FailureReason {
    /** No connectivity / timeout / DNS. Retrying later is likely to work. */
    NETWORK,

    /** The server answered with an error status. */
    SERVER,

    /** The server answered 200 but with nothing we could use. */
    INVALID_DATA,
}

/** Persists the user's last search inputs across cold starts. */
interface FilterStore {
    suspend fun load(): SearchFilters
    suspend fun save(filters: SearchFilters)
}

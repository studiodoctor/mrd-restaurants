package com.example.mrd.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.mrd.domain.AdvancedFilters
import com.example.mrd.domain.FilterStore
import com.example.mrd.domain.SearchFilters
import com.example.mrd.domain.SortOrder
import com.example.mrd.domain.ThemeMode
import com.example.mrd.domain.ThemeStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Small key/value state: last search inputs, last successful sync time and theme choice. */
@Singleton
class UserPreferences @Inject constructor(
    private val store: DataStore<Preferences>,
) : FilterStore, ThemeStore {

    override val themeMode: Flow<ThemeMode> = store.data.map { prefs ->
        // Unknown / missing values fall back to following the system.
        ThemeMode.entries.firstOrNull { it.name == prefs[THEME_MODE] } ?: ThemeMode.SYSTEM
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[THEME_MODE] = mode.name }
    }


    val lastSyncedAt: Flow<Long?> = store.data.map { it[LAST_SYNCED_AT] }

    suspend fun setLastSyncedAt(epochMillis: Long) {
        store.edit { it[LAST_SYNCED_AT] = epochMillis }
    }

    override suspend fun load(): SearchFilters {
        val prefs = store.data.first()
        return SearchFilters(
            query = prefs[QUERY].orEmpty(),
            openOnly = prefs[OPEN_ONLY] ?: false,
            advanced = AdvancedFilters(
                sort = SortOrder.entries.firstOrNull { it.name == prefs[SORT] } ?: SortOrder.RECOMMENDED,
                minRating = prefs[MIN_RATING] ?: 0.0,
                minReviews = prefs[MIN_REVIEWS] ?: 0,
                maxDistanceKm = prefs[MAX_DISTANCE] ?: 0.0,
                maxEtaMinutes = prefs[MAX_ETA] ?: 0,
                maxFeeCents = prefs[MAX_FEE] ?: AdvancedFilters.ANY_FEE,
                maxPriceLevel = prefs[MAX_PRICE] ?: 0,
                cuisine = prefs[CUISINE].orEmpty(),
                tag = prefs[TAG].orEmpty(),
            ),
        )
    }

    override suspend fun save(filters: SearchFilters) {
        store.edit {
            it[QUERY] = filters.query
            it[OPEN_ONLY] = filters.openOnly
            val a = filters.advanced
            it[SORT] = a.sort.name
            it[MIN_RATING] = a.minRating
            it[MIN_REVIEWS] = a.minReviews
            it[MAX_DISTANCE] = a.maxDistanceKm
            it[MAX_ETA] = a.maxEtaMinutes
            it[MAX_FEE] = a.maxFeeCents
            it[MAX_PRICE] = a.maxPriceLevel
            it[CUISINE] = a.cuisine
            it[TAG] = a.tag
        }
    }

    private companion object {
        val QUERY = stringPreferencesKey("filter_query")
        val OPEN_ONLY = booleanPreferencesKey("filter_open_only")
        val SORT = stringPreferencesKey("filter_sort")
        val MIN_RATING = doublePreferencesKey("filter_min_rating")
        val MIN_REVIEWS = intPreferencesKey("filter_min_reviews")
        val MAX_DISTANCE = doublePreferencesKey("filter_max_distance")
        val MAX_ETA = intPreferencesKey("filter_max_eta")
        val MAX_FEE = intPreferencesKey("filter_max_fee")
        val MAX_PRICE = intPreferencesKey("filter_max_price")
        val CUISINE = stringPreferencesKey("filter_cuisine")
        val TAG = stringPreferencesKey("filter_tag")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LAST_SYNCED_AT = longPreferencesKey("last_synced_at")
    }
}

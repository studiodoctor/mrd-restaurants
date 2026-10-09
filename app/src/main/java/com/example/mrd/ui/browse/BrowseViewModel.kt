package com.example.mrd.ui.browse

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.mrd.data.remote.NetworkSimulator
import com.example.mrd.data.remote.SimulatedNetwork
import com.example.mrd.domain.AdvancedFilters
import com.example.mrd.domain.FailureReason
import com.example.mrd.domain.FilterStore
import com.example.mrd.domain.RefreshResult
import com.example.mrd.domain.Restaurant
import com.example.mrd.domain.RestaurantRepository
import com.example.mrd.domain.SearchFilters
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** State of the background sync, independent of what the list is showing. */
sealed interface RefreshUi {
    data object Idle : RefreshUi
    data object Loading : RefreshUi
    data class Failed(val reason: FailureReason) : RefreshUi
}

data class BrowseUiState(
    val filters: SearchFilters = SearchFilters(),
    val refresh: RefreshUi = RefreshUi.Loading,
    /** Null until the first DB read lands, so the UI can show a skeleton instead of "empty". */
    val cachedCount: Int? = null,
    val lastSyncedAt: Long? = null,
    val networkMode: SimulatedNetwork = SimulatedNetwork.ONLINE,
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val repository: RestaurantRepository,
    private val filterStore: FilterStore,
    private val networkSimulator: NetworkSimulator,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // Filter inputs live in SavedStateHandle: they survive rotation *and* process death.
    // (DataStore, below, additionally carries them across cold starts.)
    /** Raw text-field value. Exposed unmapped so the TextField can read it without extra hops. */
    val query: StateFlow<String> = savedStateHandle.getStateFlow(KEY_QUERY, "")
    private val openOnly: StateFlow<Boolean> = savedStateHandle.getStateFlow(KEY_OPEN_ONLY, false)
    private val advanced: StateFlow<AdvancedFilters> = savedStateHandle.getStateFlow(KEY_ADVANCED, AdvancedFilters())
    private val filters: Flow<SearchFilters> = combine(query, openOnly, advanced) { q, o, a -> SearchFilters(q, o, a) }

    private val refreshState = MutableStateFlow<RefreshUi>(RefreshUi.Loading)
    private var refreshJob: Job? = null

    val uiState: StateFlow<BrowseUiState> = combine(
        filters,
        refreshState,
        repository.observeCount(),
        repository.lastSyncedAt,
        networkSimulator.mode,
    ) { filters, refresh, count, syncedAt, mode ->
        BrowseUiState(filters, refresh, count, syncedAt, mode)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), BrowseUiState())

    /** Feeds the "Popular right now" carousel. */
    val popular: StateFlow<List<Restaurant>> = repository.observePopular(POPULAR_COUNT)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    /**
     * The paged list. Typing is debounced (but clearing is instant) so we don't re-query the
     * database per keystroke; toggling "Open now" applies immediately. `cachedIn` keeps loaded
     * pages across configuration changes so rotation doesn't reload or lose scroll position.
     */
    val restaurants: Flow<PagingData<Restaurant>> = combine(
        query.debounce { if (it.isEmpty()) 0L else SEARCH_DEBOUNCE_MS }
            .map { it.trim() }
            .distinctUntilChanged(),
        openOnly,
        advanced,
    ) { q, o, a -> SearchFilters(q, o, a) }
        .distinctUntilChanged()
        .flatMapLatest { repository.pagedRestaurants(it) }
        .cachedIn(viewModelScope)

    init {
        restoreFilters()
        persistFilters()
        refresh()
    }

    fun onQueryChange(value: String) {
        savedStateHandle[KEY_QUERY] = value.take(MAX_QUERY_LENGTH)
    }

    fun onOpenOnlyChange(value: Boolean) {
        savedStateHandle[KEY_OPEN_ONLY] = value
    }

    fun onAdvancedFiltersChange(value: AdvancedFilters) {
        savedStateHandle[KEY_ADVANCED] = value
    }

    fun clearFilters() {
        savedStateHandle[KEY_QUERY] = ""
        savedStateHandle[KEY_OPEN_ONLY] = false
        savedStateHandle[KEY_ADVANCED] = AdvancedFilters()
    }

    fun onFavoriteClick(id: String, favorite: Boolean) {
        viewModelScope.launch { repository.setFavorite(id, favorite) }
    }

    fun refresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            refreshState.value = RefreshUi.Loading
            refreshState.value = when (val result = repository.refresh()) {
                is RefreshResult.Success -> RefreshUi.Idle
                is RefreshResult.Failure -> RefreshUi.Failed(result.reason)
            }
        }
    }

    /** Debug-only seam (see NetworkSimulator); the menu that calls this is hidden in release. */
    fun setNetworkMode(mode: SimulatedNetwork) = networkSimulator.set(mode)

    /** On a true cold start (nothing in SavedStateHandle) bring back the last saved inputs. */
    private fun restoreFilters() {
        viewModelScope.launch {
            if (savedStateHandle.get<Boolean>(KEY_RESTORED) == true) return@launch
            val saved = filterStore.load()
            // Don't clobber anything the user already typed while we were reading from disk.
            if (query.value.isEmpty() && !openOnly.value && advanced.value == AdvancedFilters()) {
                savedStateHandle[KEY_QUERY] = saved.query
                savedStateHandle[KEY_OPEN_ONLY] = saved.openOnly
                savedStateHandle[KEY_ADVANCED] = saved.advanced
            }
            savedStateHandle[KEY_RESTORED] = true
        }
    }

    private fun persistFilters() {
        viewModelScope.launch {
            filters.drop(1).debounce(PERSIST_DEBOUNCE_MS).collect { filterStore.save(it) }
        }
    }

    private companion object {
        const val POPULAR_COUNT = 5
        const val KEY_QUERY = "query"
        const val KEY_OPEN_ONLY = "open_only"
        const val KEY_ADVANCED = "advanced_filters"
        const val KEY_RESTORED = "filters_restored"
        const val SEARCH_DEBOUNCE_MS = 250L
        const val PERSIST_DEBOUNCE_MS = 400L
        const val STOP_TIMEOUT_MS = 5_000L
        const val MAX_QUERY_LENGTH = 100
    }
}

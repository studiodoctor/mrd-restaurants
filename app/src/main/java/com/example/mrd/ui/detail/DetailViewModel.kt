package com.example.mrd.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mrd.domain.FailureReason
import com.example.mrd.domain.MenuCategory
import com.example.mrd.domain.RefreshResult
import com.example.mrd.domain.Restaurant
import com.example.mrd.domain.RestaurantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface DetailUiState {
    data object Loading : DetailUiState
    data class Content(val restaurant: Restaurant) : DetailUiState
    data object NotFound : DetailUiState
    data class Error(val reason: FailureReason) : DetailUiState
}

/** The menu has its own lifecycle: the page is useful before (or without) it. */
sealed interface MenuUiState {
    data object Loading : MenuUiState
    data class Loaded(val categories: List<MenuCategory>) : MenuUiState

    /** The server answered, but there is no menu (yet). */
    data object Empty : MenuUiState
    data class Error(val reason: FailureReason) : MenuUiState
}

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: RestaurantRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // Type-safe nav stores route arguments under their property name.
    private val restaurantId: String = checkNotNull(savedStateHandle.get<String>(ARG_ID)) {
        "DetailViewModel requires a restaurant id"
    }

    /** Progress of a "fetch if needed" operation, shared by the restaurant and its menu. */
    private sealed interface Fetch {
        data object Checking : Fetch
        data object Fetching : Fetch
        data object Done : Fetch
        data class Failed(val reason: FailureReason) : Fetch
    }

    private val fetch = MutableStateFlow<Fetch>(Fetch.Checking)
    private val menuFetch = MutableStateFlow<Fetch>(Fetch.Fetching)

    val uiState: StateFlow<DetailUiState> = combine(
        repository.observeRestaurant(restaurantId),
        fetch,
    ) { restaurant, fetch ->
        when {
            restaurant != null -> DetailUiState.Content(restaurant)
            fetch is Fetch.Failed -> DetailUiState.Error(fetch.reason)
            fetch == Fetch.Done -> DetailUiState.NotFound
            else -> DetailUiState.Loading
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), DetailUiState.Loading)

    /**
     * Cache first: a menu seen before is shown immediately (and offline) while a refresh runs in
     * the background. A failed refresh only surfaces as an error if there is nothing cached.
     */
    val menuState: StateFlow<MenuUiState> = combine(
        repository.observeMenu(restaurantId),
        menuFetch,
    ) { categories, fetch ->
        when {
            categories.isNotEmpty() -> MenuUiState.Loaded(categories)
            fetch is Fetch.Failed -> MenuUiState.Error(fetch.reason)
            fetch == Fetch.Done -> MenuUiState.Empty
            else -> MenuUiState.Loading
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), MenuUiState.Loading)

    init {
        ensureLoaded()
        loadMenu()
    }

    fun retry() = ensureLoaded()

    fun retryMenu() = loadMenu()

    fun onFavoriteClick() {
        val current = (uiState.value as? DetailUiState.Content)?.restaurant ?: return
        viewModelScope.launch { repository.setFavorite(current.id, !current.isFavorite) }
    }

    /**
     * Normal navigation finds the restaurant in the cache immediately. A deep link on a fresh
     * install does not, so we fall back to one sync before declaring it "not found".
     */
    private fun ensureLoaded() {
        viewModelScope.launch {
            fetch.value = Fetch.Checking
            if (repository.observeRestaurant(restaurantId).first() != null) {
                fetch.value = Fetch.Done
                return@launch
            }
            fetch.value = Fetch.Fetching
            fetch.value = when (val result = repository.refresh()) {
                is RefreshResult.Success -> Fetch.Done
                is RefreshResult.Failure -> Fetch.Failed(result.reason)
            }
        }
    }

    private fun loadMenu() {
        viewModelScope.launch {
            menuFetch.value = Fetch.Fetching
            menuFetch.value = when (val result = repository.refreshMenu(restaurantId)) {
                is RefreshResult.Success -> Fetch.Done
                is RefreshResult.Failure -> Fetch.Failed(result.reason)
            }
        }
    }

    private companion object {
        const val ARG_ID = "id"
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

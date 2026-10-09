package com.example.mrd.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mrd.domain.Restaurant
import com.example.mrd.domain.RestaurantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface FavoritesUiState {
    data object Loading : FavoritesUiState
    data object Empty : FavoritesUiState
    data class Content(val restaurants: List<Restaurant>) : FavoritesUiState
}

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val repository: RestaurantRepository,
) : ViewModel() {

    val uiState: StateFlow<FavoritesUiState> = repository.observeFavorites()
        .map { if (it.isEmpty()) FavoritesUiState.Empty else FavoritesUiState.Content(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), FavoritesUiState.Loading)

    fun removeFavorite(id: String) {
        viewModelScope.launch { repository.setFavorite(id, false) }
    }
}

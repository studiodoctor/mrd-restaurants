package com.example.mrd.ui.favorites

import app.cash.turbine.test
import com.example.mrd.testutil.FakeRestaurantRepository
import com.example.mrd.testutil.MainDispatcherRule
import com.example.mrd.testutil.restaurant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class FavoritesViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val repository = FakeRestaurantRepository()

    @Test
    fun `no favourites shows the empty state`() = runTest {
        repository.restaurants.value = mapOf("r-1" to restaurant(id = "r-1"))
        FavoritesViewModel(repository).uiState.test {
            var state = awaitItem()
            if (state == FavoritesUiState.Loading) state = awaitItem()
            assertEquals(FavoritesUiState.Empty, state)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `only favourites are listed and removing one updates the list`() = runTest {
        repository.restaurants.value = mapOf(
            "r-1" to restaurant(id = "r-1", isFavorite = true),
            "r-2" to restaurant(id = "r-2", name = "Other"),
        )
        val vm = FavoritesViewModel(repository)
        vm.uiState.test {
            var state = awaitItem()
            if (state == FavoritesUiState.Loading) state = awaitItem()
            assertEquals(listOf("r-1"), (state as FavoritesUiState.Content).restaurants.map { it.id })

            vm.removeFavorite("r-1")
            var next = awaitItem()
            while (next !is FavoritesUiState.Empty) next = awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
    }
}

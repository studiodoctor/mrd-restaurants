package com.example.mrd.ui.detail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.example.mrd.domain.FailureReason
import com.example.mrd.domain.MenuCategory
import com.example.mrd.domain.MenuItem
import com.example.mrd.domain.RefreshResult
import com.example.mrd.testutil.FakeRestaurantRepository
import com.example.mrd.testutil.MainDispatcherRule
import com.example.mrd.testutil.restaurant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DetailViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val repository = FakeRestaurantRepository()

    private fun viewModel(id: String = "r-1") =
        DetailViewModel(repository, SavedStateHandle(mapOf("id" to id)))

    private suspend fun ReceiveTurbine<DetailUiState>.awaitNot(vararg unwanted: DetailUiState): DetailUiState {
        while (true) {
            val item = awaitItem()
            if (item !in unwanted) return item
        }
    }

    @Test
    fun `cached restaurant is shown without hitting the network`() = runTest {
        repository.restaurants.value = mapOf("r-1" to restaurant())

        viewModel().uiState.test {
            val state = awaitNot(DetailUiState.Loading)
            assertEquals(DetailUiState.Content(restaurant()), state)
            assertEquals(0, repository.refreshCalls)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `cold deep link fetches once and then shows the restaurant`() = runTest {
        repository.onRefresh = { repository.restaurants.value = mapOf("r-1" to restaurant()) }
        repository.nextRefresh = RefreshResult.Success(1)

        viewModel().uiState.test {
            val state = awaitNot(DetailUiState.Loading)
            assertTrue(state is DetailUiState.Content)
            assertEquals(1, repository.refreshCalls)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `unknown id after a successful sync is not found`() = runTest {
        repository.nextRefresh = RefreshResult.Success(1)

        viewModel("does-not-exist").uiState.test {
            assertEquals(DetailUiState.NotFound, awaitNot(DetailUiState.Loading))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `failed fetch surfaces an error and retry can recover`() = runTest {
        repository.nextRefresh = RefreshResult.Failure(FailureReason.NETWORK)
        val vm = viewModel()

        vm.uiState.test {
            assertEquals(DetailUiState.Error(FailureReason.NETWORK), awaitNot(DetailUiState.Loading))

            repository.onRefresh = { repository.restaurants.value = mapOf("r-1" to restaurant()) }
            repository.nextRefresh = RefreshResult.Success(1)
            vm.retry()

            val recovered = awaitNot(DetailUiState.Loading, DetailUiState.Error(FailureReason.NETWORK))
            assertTrue(recovered is DetailUiState.Content)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `favourite toggle flips the flag on the current restaurant`() = runTest {
        repository.restaurants.value = mapOf("r-1" to restaurant(isFavorite = false))
        val vm = viewModel()

        vm.uiState.test {
            awaitNot(DetailUiState.Loading)
            vm.onFavoriteClick()
            val updated = awaitNot(DetailUiState.Content(restaurant(isFavorite = false)))
            assertTrue((updated as DetailUiState.Content).restaurant.isFavorite)

            vm.onFavoriteClick()
            val reverted = awaitNot(updated)
            assertFalse((reverted as DetailUiState.Content).restaurant.isFavorite)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- menu ---------------------------------------------------------------------------------

    private val sampleMenu = listOf(
        MenuCategory("c1", "Mains", listOf(MenuItem("i1", "Burger", null, 9900, true, emptyList()))),
    )

    @Test
    fun `menu loads from the refresh and shows the categories`() = runTest {
        repository.restaurants.value = mapOf("r-1" to restaurant())
        repository.onMenuRefresh = { repository.menus.value = mapOf("r-1" to sampleMenu) }

        viewModel().menuState.test {
            val state = awaitItem().let { if (it == MenuUiState.Loading) awaitItem() else it }
            assertEquals(MenuUiState.Loaded(sampleMenu), state)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `successful refresh with no menu is empty`() = runTest {
        repository.nextMenuRefresh = RefreshResult.Success(0)

        viewModel().menuState.test {
            val state = awaitItem().let { if (it == MenuUiState.Loading) awaitItem() else it }
            assertEquals(MenuUiState.Empty, state)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `failed menu refresh is an error and retry can recover`() = runTest {
        repository.nextMenuRefresh = RefreshResult.Failure(FailureReason.SERVER)
        val vm = viewModel()

        vm.menuState.test {
            var state = awaitItem()
            while (state == MenuUiState.Loading) state = awaitItem()
            assertEquals(MenuUiState.Error(FailureReason.SERVER), state)

            repository.onMenuRefresh = { repository.menus.value = mapOf("r-1" to sampleMenu) }
            repository.nextMenuRefresh = RefreshResult.Success(1)
            vm.retryMenu()

            var recovered = awaitItem()
            while (recovered !is MenuUiState.Loaded) recovered = awaitItem()
            assertEquals(MenuUiState.Loaded(sampleMenu), recovered)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `cached menu stays visible when the refresh fails`() = runTest {
        repository.menus.value = mapOf("r-1" to sampleMenu)
        repository.nextMenuRefresh = RefreshResult.Failure(FailureReason.NETWORK)

        viewModel().menuState.test {
            var state = awaitItem()
            while (state == MenuUiState.Loading) state = awaitItem()
            assertEquals(MenuUiState.Loaded(sampleMenu), state)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }
}

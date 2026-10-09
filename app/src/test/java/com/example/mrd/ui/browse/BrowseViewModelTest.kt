package com.example.mrd.ui.browse

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.example.mrd.data.remote.NetworkSimulator
import com.example.mrd.domain.FailureReason
import com.example.mrd.domain.RefreshResult
import com.example.mrd.domain.SearchFilters
import com.example.mrd.testutil.FakeFilterStore
import com.example.mrd.testutil.FakeRestaurantRepository
import com.example.mrd.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BrowseViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val repository = FakeRestaurantRepository()
    private val filterStore = FakeFilterStore()

    private fun viewModel(handle: SavedStateHandle = SavedStateHandle()) =
        BrowseViewModel(repository, filterStore, NetworkSimulator(), handle)

    private suspend fun ReceiveTurbine<BrowseUiState>.awaitMatching(
        predicate: (BrowseUiState) -> Boolean,
    ): BrowseUiState {
        while (true) {
            val item = awaitItem()
            if (predicate(item)) return item
        }
    }

    @Test
    fun `failed refresh with a cache keeps the cache count and reports the failure`() = runTest {
        repository.count.value = 25
        repository.nextRefresh = RefreshResult.Failure(FailureReason.NETWORK)

        viewModel().uiState.test {
            val state = awaitMatching { it.refresh is RefreshUi.Failed }
            assertEquals(RefreshUi.Failed(FailureReason.NETWORK), state.refresh)
            assertEquals(25, state.cachedCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `failed refresh with an empty cache exposes zero cached rows`() = runTest {
        repository.count.value = 0
        repository.nextRefresh = RefreshResult.Failure(FailureReason.SERVER)

        viewModel().uiState.test {
            val state = awaitMatching { it.refresh is RefreshUi.Failed && it.cachedCount != null }
            assertEquals(0, state.cachedCount)
            assertEquals(RefreshUi.Failed(FailureReason.SERVER), state.refresh)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `retry after a failure recovers to idle`() = runTest {
        repository.count.value = 10
        repository.nextRefresh = RefreshResult.Failure(FailureReason.NETWORK)
        val vm = viewModel()

        vm.uiState.test {
            awaitMatching { it.refresh is RefreshUi.Failed }

            repository.nextRefresh = RefreshResult.Success(10)
            vm.refresh()

            awaitMatching { it.refresh == RefreshUi.Idle }
            assertEquals(2, repository.refreshCalls)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `last saved filters are restored on a cold start`() = runTest {
        filterStore.saved = SearchFilters(query = "pizza", openOnly = true)

        viewModel().uiState.test {
            val state = awaitMatching { it.filters == SearchFilters("pizza", true) }
            assertTrue(state.filters.isActive)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `restored saved state is not overwritten by persisted filters`() = runTest {
        filterStore.saved = SearchFilters(query = "stale", openOnly = false)
        val handle = SavedStateHandle(
            mapOf("query" to "fresh", "open_only" to true, "filters_restored" to true),
        )

        viewModel(handle).uiState.test {
            val state = awaitMatching { it.filters.query.isNotEmpty() }
            assertEquals(SearchFilters("fresh", true), state.filters)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearFilters resets both inputs`() = runTest {
        val vm = viewModel()
        vm.onQueryChange("sushi")
        vm.onOpenOnlyChange(true)

        vm.uiState.test {
            awaitMatching { it.filters == SearchFilters("sushi", true) }
            vm.clearFilters()
            awaitMatching { it.filters == SearchFilters() }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `filters are persisted after the user stops typing`() = runTest {
        val vm = viewModel()
        vm.onQueryChange("sushi")
        advanceTimeBy(500)
        runCurrent()

        assertEquals(SearchFilters("sushi", false), filterStore.saved)
    }

    @Test
    fun `typing is debounced so only the settled query hits the database`() = runTest {
        val vm = viewModel()
        backgroundScope.launch { vm.restaurants.collect { } }
        runCurrent()

        vm.onQueryChange("s")
        advanceTimeBy(100)
        vm.onQueryChange("su")
        advanceTimeBy(100)
        vm.onQueryChange("sus")
        advanceTimeBy(400)
        runCurrent()

        assertEquals(listOf("", "sus"), repository.requestedFilters.map { it.query })
    }

    @Test
    fun `clearing the search applies immediately without waiting for the debounce`() = runTest {
        val vm = viewModel()
        backgroundScope.launch { vm.restaurants.collect { } }
        vm.onQueryChange("sushi")
        advanceTimeBy(300)
        runCurrent()

        vm.onQueryChange("")
        runCurrent() // no time advance

        assertEquals("", repository.requestedFilters.last().query)
    }
}

package com.example.mrd.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdvancedFiltersTest {

    @Test
    fun `defaults are inactive`() {
        assertFalse(AdvancedFilters().isActive)
        assertEquals(0, AdvancedFilters().badgeCount)
        assertFalse(SearchFilters().isActive)
    }

    @Test
    fun `free delivery counts as an active filter`() {
        // 0 is a real value for the fee (free), unlike for the other filters where it means "any".
        assertEquals(1, AdvancedFilters(maxFeeCents = 0).activeCount)
    }

    @Test
    fun `sort alone is not a narrowing filter but does show on the badge`() {
        val sorted = AdvancedFilters(sort = SortOrder.NEAREST)
        assertEquals(0, sorted.activeCount)
        assertEquals(1, sorted.badgeCount)
        assertTrue(SearchFilters(advanced = sorted).isActive)
    }

    @Test
    fun `every narrowing filter is counted once`() {
        val all = AdvancedFilters(
            minRating = 4.0, minReviews = 100, maxDistanceKm = 3.0, maxEtaMinutes = 30,
            maxFeeCents = 0, maxPriceLevel = 2, cuisine = "Pizza", tag = "Halaal",
        )
        assertEquals(8, all.activeCount)
    }
}

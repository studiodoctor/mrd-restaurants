package com.example.mrd.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RestaurantFormattingTest {

    @Test
    fun `prices are formatted from integer cents`() {
        assertEquals("R0.00", formatRand(0))
        assertEquals("R0.05", formatRand(5))
        assertEquals("R15.00", formatRand(1500))
        assertEquals("R69.05", formatRand(6905))
        assertEquals("R1234.56", formatRand(123_456))
    }

    @Test
    fun `rating always shows one decimal regardless of device locale`() {
        assertEquals("4.6", formatRating(4.6))
        assertEquals("4.0", formatRating(4.0))
        assertEquals("3.3", formatRating(3.34))
    }

    @Test
    fun `minutes of day are formatted as 24 hour clock`() {
        assertEquals("00:00", formatMinutesOfDay(0))
        assertEquals("09:05", formatMinutesOfDay(545))
        assertEquals("22:30", formatMinutesOfDay(1350))
    }

    @Test
    fun `cuisine art picks a matching emoji and is stable`() {
        assertEquals("🍣", cuisineArtFor(listOf("Sushi", "Japanese"), "x").emoji)
        assertEquals(cuisineArtFor(listOf("Sushi"), "a"), cuisineArtFor(listOf("Sushi"), "b"))
    }

    @Test
    fun `cuisine art falls back for unknown or missing cuisines`() {
        val art = cuisineArtFor(emptyList(), "Mystery Place")
        assertTrue(art.emoji.isNotEmpty())
        assertEquals(art, cuisineArtFor(emptyList(), "Mystery Place"))
    }

    @Test
    fun `stock photo matches the cuisine and is stable per restaurant`() {
        val pizza = stockPhotoUrl(listOf("Pizza", "Italian"), "Napoli")
        assertTrue(pizza.startsWith("https://images.unsplash.com/photo-"))
        assertEquals(pizza, stockPhotoUrl(listOf("Pizza", "Italian"), "Napoli"))
        // A sushi restaurant never gets a pizza photo.
        assertTrue(stockPhotoUrl(listOf("Sushi"), "Napoli") != pizza)
    }

    @Test
    fun `unknown cuisines still get a real photo`() {
        assertTrue(stockPhotoUrl(listOf("Martian"), "X").startsWith("https://images.unsplash.com/photo-"))
        assertTrue(stockPhotoUrl(emptyList(), "X").startsWith("https://images.unsplash.com/photo-"))
    }
}

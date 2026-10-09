package com.example.mrd.data.mapper

import com.example.mrd.data.local.RestaurantEntity
import com.example.mrd.data.local.RestaurantWithFavorite
import com.example.mrd.data.remote.MenuResponseDto
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MappersTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private fun el(raw: String): JsonElement = json.parseToJsonElement(raw)

    private fun parse(vararg raw: String) = json.parseRestaurants(raw.map(::el), syncedAt = 1L)

    @Test
    fun `record without id or name is dropped`() {
        val result = parse(
            """{"id":"a","name":"Alpha"}""",
            """{"name":"No Id"}""",
            """{"id":"c"}""",
            """{"id":"  ","name":"Blank Id"}""",
        )
        assertEquals(listOf("a"), result.map { it.id })
    }

    @Test
    fun `one poisoned record does not fail the batch`() {
        val result = parse(
            """{"id":"a","name":"Alpha"}""",
            """{"id":"bad","name":"Bad","rating":"n/a"}""",
            """{"id":"c","name":"Charlie"}""",
        )
        assertEquals(listOf("a", "c"), result.map { it.id })
    }

    @Test
    fun `duplicate ids keep the first and positions stay contiguous`() {
        val result = parse(
            """{"id":"a","name":"First"}""",
            """{"id":"a","name":"Second"}""",
            """{"id":"b","name":"Third"}""",
        )
        assertEquals(listOf("First", "Third"), result.map { it.name })
        assertEquals(listOf(0, 1), result.map { it.position })
    }

    @Test
    fun `out of range optional values become unknown`() {
        val entity = parse(
            """{"id":"a","name":"A","rating":7.2,"delivery_fee_cents":-5,"eta_minutes":0}""",
        ).single()
        assertNull(entity.rating)
        assertNull(entity.deliveryFeeCents)
        assertNull(entity.etaMinutes)
    }

    @Test
    fun `missing optional values are preserved as null and missing open flag means closed`() {
        val entity = parse("""{"id":"a","name":"A"}""").single()
        assertNull(entity.rating)
        assertNull(entity.deliveryFeeCents)
        assertNull(entity.imageUrl)
        assertFalse(entity.isOpen)
        assertEquals("", entity.cuisines)
    }

    @Test
    fun `zero delivery fee is kept as free rather than treated as missing`() {
        val entity = parse("""{"id":"a","name":"A","delivery_fee_cents":0}""").single()
        assertEquals(0, entity.deliveryFeeCents)
    }

    @Test
    fun `cuisines are trimmed and blanks removed`() {
        val entity = parse("""{"id":"a","name":"A","cuisines":[" Sushi ","","Japanese"]}""").single()
        assertEquals("Sushi${LIST_SEPARATOR}Japanese", entity.cuisines)
    }

    @Test
    fun `entity maps to domain with cuisines split and favourite flag`() {
        val entity = RestaurantEntity(
            id = "a", name = "A", cuisines = "Sushi|Japanese", rating = 4.3, deliveryFeeCents = 0,
            etaMinutes = 25, isOpen = false, imageUrl = null, neighbourhood = null, position = 0, syncedAt = 1L,
        )
        val domain = RestaurantWithFavorite(entity, isFavorite = true).toDomain()
        assertEquals(listOf("Sushi", "Japanese"), domain.cuisines)
        assertTrue(domain.isFavorite)
        assertFalse(domain.isOpen)
    }

    @Test
    fun `escapeLike neutralises wildcards and the escape character`() {
        assertEquals("100\\%", "100%".escapeLike())
        assertEquals("a\\_b", "a_b".escapeLike())
        assertEquals("a\\\\b", "a\\b".escapeLike())
        assertEquals("plain", "plain".escapeLike())
    }

    // --- about fields -------------------------------------------------------------------------

    @Test
    fun `about fields are parsed and cleaned`() {
        val entity = parse(
            """{"id":"a","name":"A","description":"  Nice  ","address":"1 Main Rd","opens_at":"09:00",
               "closes_at":"22:30","price_level":3,"review_count":120,"min_order_cents":5000,
               "tags":[" Halaal ","","Top rated"]}""",
        ).single()
        assertEquals("Nice", entity.description)
        assertEquals(540, entity.opensAtMinutes)
        assertEquals(1350, entity.closesAtMinutes)
        assertEquals(3, entity.priceLevel)
        assertEquals(120, entity.reviewCount)
        assertEquals(5000, entity.minOrderCents)
        assertEquals("Halaal${LIST_SEPARATOR}Top rated", entity.tags)
    }

    @Test
    fun `invalid about values become unknown instead of failing the record`() {
        val entity = parse(
            """{"id":"a","name":"A","opens_at":"25:99","closes_at":"soon","price_level":9,"review_count":-4}""",
        ).single()
        assertNull(entity.opensAtMinutes)
        assertNull(entity.closesAtMinutes)
        assertNull(entity.priceLevel)
        assertNull(entity.reviewCount)
    }

    @Test
    fun `minutes of day parsing`() {
        assertEquals(0, "00:00".toMinutesOfDay())
        assertEquals(1439, "23:59".toMinutesOfDay())
        assertNull("24:00".toMinutesOfDay())
        assertNull("9:00pm".toMinutesOfDay())
        assertNull(null.toMinutesOfDay())
    }

    @Test
    fun `distance is kept when plausible and dropped otherwise`() {
        assertEquals(2.4, parse("""{"id":"a","name":"A","distance_km":2.4}""").single().distanceKm!!, 0.0)
        assertNull(parse("""{"id":"a","name":"A","distance_km":-1}""").single().distanceKm)
        assertNull(parse("""{"id":"a","name":"A"}""").single().distanceKm)
    }

    // --- menu ---------------------------------------------------------------------------------

    private fun menu(raw: String): MenuResponseDto = json.decodeFromString(MenuResponseDto.serializer(), raw)

    @Test
    fun `menu drops nameless items and empty categories but keeps price-less items`() {
        val rows = json.parseMenu(
            "r-1",
            menu(
                """{"categories":[
                  {"id":"c1","name":"Mains","items":[
                    {"id":"i1","name":"Burger","price_cents":9900},
                    {"id":"i2","price_cents":500},
                    {"id":"i3","name":"Mystery"}]},
                  {"id":"c2","name":"Empty","items":[]}]}""",
            ),
        )
        assertEquals(listOf("Burger", "Mystery"), rows.map { it.name })
        assertNull(rows.last().priceCents)
        val categories = rows.toMenuCategories()
        assertEquals(listOf("Mains"), categories.map { it.name })
    }

    @Test
    fun `menu keeps server order and first of duplicate item ids`() {
        val rows = json.parseMenu(
            "r-1",
            menu(
                """{"categories":[{"id":"c1","name":"A","items":[
                  {"id":"x","name":"First"},{"id":"x","name":"Second"},{"id":"y","name":"Third"}]}]}""",
            ),
        )
        assertEquals(listOf("First", "Third"), rows.map { it.name })
        assertEquals(listOf(0, 1), rows.map { it.position })
    }

    @Test
    fun `item options survive an encode and decode round trip`() {
        val rows = json.parseMenu(
            "r-1",
            menu(
                """{"categories":[{"id":"c1","name":"A","items":[{"id":"x","name":"Pizza","price_cents":100,
                  "options":[{"name":"Extra | cheese","price_cents":1500},{"name":"","price_cents":1}]}]}]}""",
            ),
        )
        val options = rows.single().toDomain().options
        assertEquals(1, options.size)
        assertEquals(1500, options.single().priceCents)
        assertTrue(options.single().name.startsWith("Extra"))
    }
}

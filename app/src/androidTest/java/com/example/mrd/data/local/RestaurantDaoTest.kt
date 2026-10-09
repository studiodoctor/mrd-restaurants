package com.example.mrd.data.local

import android.content.Context
import androidx.paging.PagingSource
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs the real SQL against an in-memory Room database. This is where the things unit tests
 * with fakes can't prove live: LIKE escaping, the favourites JOIN, and stale-row cleanup.
 */
@RunWith(AndroidJUnit4::class)
class RestaurantDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: RestaurantDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        dao = db.restaurantDao()
    }

    @After
    fun tearDown() = db.close()

    private fun entity(id: String, name: String, open: Boolean = true, position: Int = 0, syncedAt: Long = 1L) =
        RestaurantEntity(
            id = id, name = name, cuisines = "Grill", rating = 4.0, deliveryFeeCents = 0, etaMinutes = 20,
            isOpen = open, imageUrl = null, neighbourhood = null, position = position, syncedAt = syncedAt,
        )

    private suspend fun names(
        query: String = "",
        openOnly: Boolean = false,
        minRating: Double = 0.0,
        minReviews: Int = 0,
        maxDistanceKm: Double = 0.0,
        maxEtaMinutes: Int = 0,
        maxFeeCents: Int = -1,
        maxPriceLevel: Int = 0,
        cuisine: String = "",
        tag: String = "",
        sort: Int = 0,
    ): List<String> {
        val result = dao.pagingSource(
            query, openOnly, minRating, minReviews, maxDistanceKm, maxEtaMinutes, maxFeeCents,
            maxPriceLevel, cuisine, tag, sort,
        )
            .load(PagingSource.LoadParams.Refresh(key = null, loadSize = 100, placeholdersEnabled = false))
        return (result as PagingSource.LoadResult.Page).data.map { it.restaurant.name }
    }

    @Test
    fun searchIsCaseInsensitiveSubstringOnName() = runBlocking {
        dao.upsertAll(listOf(entity("1", "Cape Town Grill", position = 0), entity("2", "Sushi Bar", position = 1)))
        assertEquals(listOf("Cape Town Grill"), names("town"))
        assertEquals(listOf("Sushi Bar"), names("SUSHI"))
        assertEquals(2, names("").size)
    }

    @Test
    fun openOnlyHidesClosedRestaurants() = runBlocking {
        dao.upsertAll(
            listOf(
                entity("1", "Open Place", open = true, position = 0),
                entity("2", "Closed Place", open = false, position = 1),
            ),
        )
        assertEquals(listOf("Open Place"), names(openOnly = true))
        assertEquals(2, names(openOnly = false).size)
    }

    @Test
    fun likeWildcardsInInputAreMatchedLiterally() = runBlocking {
        dao.upsertAll(
            listOf(
                entity("1", "100% Vegan Kitchen", position = 0),
                entity("2", "Plain Cafe", position = 1),
                entity("3", "Under_Score Eats", position = 2),
            ),
        )
        // The repository escapes before calling the DAO; mirror that here.
        assertEquals(listOf("100% Vegan Kitchen"), names("100\\%"))
        assertEquals(listOf("Under_Score Eats"), names("r\\_s"))
        // Documents *why* the repository escapes: a raw "%" is a wildcard and matches every row.
        assertEquals(3, names("%").size)
    }

    @Test
    fun resultsFollowServerOrder() = runBlocking {
        dao.upsertAll(listOf(entity("b", "B", position = 1), entity("a", "A", position = 0)))
        assertEquals(listOf("A", "B"), names())
    }

    @Test
    fun favouriteSurvivesAListRefreshThatReplacesTheRow() = runBlocking {
        dao.upsertAll(listOf(entity("1", "Cape Town Grill")))
        dao.addFavorite(FavoriteEntity("1", createdAt = 1L))

        // A later sync REPLACEs the restaurant row.
        dao.upsertAll(listOf(entity("1", "Cape Town Grill (renamed)", syncedAt = 2L)))
        dao.deleteNotSyncedAt(2L)

        val row = dao.observeById("1").first()!!
        assertEquals("Cape Town Grill (renamed)", row.restaurant.name)
        assertTrue(row.isFavorite)
    }

    @Test
    fun removingAFavouriteClearsTheFlag() = runBlocking {
        dao.upsertAll(listOf(entity("1", "A")))
        dao.addFavorite(FavoriteEntity("1", 1L))
        dao.removeFavorite("1")
        assertFalse(dao.observeById("1").first()!!.isFavorite)
    }

    @Test
    fun staleRowsAreDeletedAfterASync() = runBlocking {
        dao.upsertAll(listOf(entity("1", "Keep", syncedAt = 1L), entity("2", "Gone", syncedAt = 1L)))
        dao.upsertAll(listOf(entity("1", "Keep", syncedAt = 2L)))
        dao.deleteNotSyncedAt(2L)

        assertEquals(1, dao.observeCount().first())
        assertEquals(null, dao.observeById("2").first())
    }

    @Test
    fun favouritesListContainsOnlyFavouritesNewestFirst() = runBlocking {
        dao.upsertAll(listOf(entity("1", "A", position = 0), entity("2", "B", position = 1), entity("3", "C", position = 2)))
        dao.addFavorite(FavoriteEntity("1", createdAt = 1L))
        dao.addFavorite(FavoriteEntity("3", createdAt = 2L))

        val favourites = dao.observeFavorites().first()
        assertEquals(listOf("C", "A"), favourites.map { it.restaurant.name })
        assertTrue(favourites.all { it.isFavorite })
    }

    @Test
    fun popularIsOpenWellRatedAndRankedByReviewCount() = runBlocking {
        dao.upsertAll(
            listOf(
                entity("1", "Quiet gem", position = 0).copy(rating = 4.8, reviewCount = 10),
                entity("2", "Crowd favourite", position = 1).copy(rating = 4.5, reviewCount = 900),
                entity("3", "Closed hit", open = false, position = 2).copy(rating = 4.9, reviewCount = 5000),
                entity("4", "Meh", position = 3).copy(rating = 3.9, reviewCount = 9999),
            ),
        )
        val popular = dao.observePopular(5).first()
        assertEquals(listOf("Crowd favourite", "Quiet gem"), popular.map { it.restaurant.name })
    }

    private fun rich(id: String, name: String, position: Int, block: RestaurantEntity.() -> RestaurantEntity) =
        entity(id, name, position = position).block()

    @Test
    fun advancedFiltersNarrowTheList() = runBlocking {
        dao.upsertAll(
            listOf(
                rich("1", "Near cheap", 0) { copy(rating = 4.6, reviewCount = 800, distanceKm = 1.2, etaMinutes = 18, deliveryFeeCents = 0, priceLevel = 1, cuisines = "Pizza|Italian", tags = "Halaal") },
                rich("2", "Far pricey", 1) { copy(rating = 3.8, reviewCount = 50, distanceKm = 9.0, etaMinutes = 50, deliveryFeeCents = 4500, priceLevel = 4, cuisines = "Sushi", tags = "") },
                rich("3", "Unknowns", 2) { copy(rating = null, reviewCount = null, distanceKm = null, etaMinutes = null, deliveryFeeCents = null, priceLevel = null) },
            ),
        )
        assertEquals(3, names().size)
        assertEquals(listOf("Near cheap"), names(minRating = 4.5))
        assertEquals(listOf("Near cheap"), names(minReviews = 500))
        assertEquals(listOf("Near cheap"), names(maxDistanceKm = 3.0))
        assertEquals(listOf("Near cheap"), names(maxEtaMinutes = 30))
        assertEquals(listOf("Near cheap"), names(maxFeeCents = 0))
        assertEquals(listOf("Near cheap"), names(maxPriceLevel = 2))
        assertEquals(listOf("Far pricey"), names(cuisine = "Sushi"))
        assertEquals(listOf("Near cheap"), names(cuisine = "Italian"))
        assertEquals(listOf("Near cheap"), names(tag = "Halaal"))
        // Unknown values never satisfy an active filter, but "Any" keeps them.
        assertFalse(names(maxDistanceKm = 100.0).contains("Unknowns"))
        assertTrue(names().contains("Unknowns"))
    }

    @Test
    fun sortingPutsUnknownValuesLast() = runBlocking {
        dao.upsertAll(
            listOf(
                rich("1", "Slow", 0) { copy(etaMinutes = 50, rating = 4.0) },
                rich("2", "Unknown", 1) { copy(etaMinutes = null, rating = null) },
                rich("3", "Quick", 2) { copy(etaMinutes = 15, rating = 4.9) },
            ),
        )
        assertEquals(listOf("Quick", "Slow", "Unknown"), names(sort = 2)) // FASTEST
        assertEquals(listOf("Quick", "Slow", "Unknown"), names(sort = 1)) // TOP_RATED
        assertEquals(listOf("Slow", "Unknown", "Quick"), names(sort = 0)) // RECOMMENDED = server order
    }
}

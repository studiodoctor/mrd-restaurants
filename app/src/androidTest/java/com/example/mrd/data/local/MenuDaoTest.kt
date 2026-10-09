package com.example.mrd.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MenuDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: MenuDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        dao = db.menuDao()
    }

    @After
    fun tearDown() = db.close()

    private fun item(restaurantId: String, id: String, position: Int) = MenuItemEntity(
        restaurantId = restaurantId, id = id, categoryId = "c1", categoryName = "Mains", name = "Dish $id",
        description = null, priceCents = 1000, available = true, options = "", position = position,
    )

    @Test
    fun menusAreScopedPerRestaurantAndOrderedByPosition() = runBlocking {
        dao.insertMenuItems(listOf(item("r-1", "b", 1), item("r-1", "a", 0), item("r-2", "a", 0)))
        assertEquals(listOf("a", "b"), dao.observeMenu("r-1").first().map { it.id })
        assertEquals(listOf("a"), dao.observeMenu("r-2").first().map { it.id })
    }

    @Test
    fun deleteOnlyRemovesOneRestaurantsMenu() = runBlocking {
        dao.insertMenuItems(listOf(item("r-1", "a", 0), item("r-2", "a", 0)))
        dao.deleteMenu("r-1")
        assertEquals(emptyList<String>(), dao.observeMenu("r-1").first().map { it.id })
        assertEquals(1, dao.observeMenu("r-2").first().size)
    }
}

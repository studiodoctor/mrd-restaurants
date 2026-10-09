package com.example.mrd.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Proves the v1 -> v2 AutoMigration keeps user data (favourites above all) and adds the new
 * columns / menu table. Needs `app/schemas/.../1.json` to be committed (see README).
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrate1To2_keepsRowsAndFavourites() {
        helper.createDatabase(DB_NAME, 1).apply {
            execSQL(
                """INSERT INTO restaurants (id, name, cuisines, rating, deliveryFeeCents, etaMinutes, isOpen,
                   imageUrl, neighbourhood, position, syncedAt)
                   VALUES ('r-1', 'Cape Town Grill', 'Grill', 4.5, 0, 30, 1, NULL, NULL, 0, 1)""",
            )
            execSQL("INSERT INTO favorites (restaurantId, createdAt) VALUES ('r-1', 5)")
            close()
        }

        val db = helper.runMigrationsAndValidate(DB_NAME, 2, true)

        db.query("SELECT name, tags, description FROM restaurants WHERE id = 'r-1'").use {
            assertTrue(it.moveToFirst())
            assertEquals("Cape Town Grill", it.getString(0))
            assertEquals("", it.getString(1))
            assertTrue(it.isNull(2))
        }
        db.query("SELECT COUNT(*) FROM favorites WHERE restaurantId = 'r-1'").use {
            it.moveToFirst()
            assertEquals(1, it.getInt(0))
        }
        db.query("SELECT COUNT(*) FROM menu_items").use {
            it.moveToFirst()
            assertEquals(0, it.getInt(0))
        }
    }

    @Test
    fun migrate2To3_addsDistanceAndKeepsFavourites() {
        helper.createDatabase(DB_NAME_V2, 2).apply {
            execSQL(
                """INSERT INTO restaurants (id, name, cuisines, rating, deliveryFeeCents, etaMinutes, isOpen,
                   imageUrl, neighbourhood, position, syncedAt, tags)
                   VALUES ('r-1', 'Cape Town Grill', 'Grill', 4.5, 0, 30, 1, NULL, NULL, 0, 1, '')""",
            )
            execSQL("INSERT INTO favorites (restaurantId, createdAt) VALUES ('r-1', 5)")
            close()
        }

        val db = helper.runMigrationsAndValidate(DB_NAME_V2, 3, true)

        db.query("SELECT name, distanceKm FROM restaurants WHERE id = 'r-1'").use {
            assertTrue(it.moveToFirst())
            assertEquals("Cape Town Grill", it.getString(0))
            assertTrue(it.isNull(1))
        }
        db.query("SELECT COUNT(*) FROM favorites").use {
            it.moveToFirst()
            assertEquals(1, it.getInt(0))
        }
    }

    private companion object {
        const val DB_NAME_V2 = "migration-test-v2"
        const val DB_NAME = "migration-test"
    }
}

package com.example.mrd.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Schema history
 *  v1  restaurants, favorites
 *  v2  restaurants gains the "about" columns (description, address, hours, price level, reviews,
 *      min order, tags) and a new menu_items table.
 *
 * NOTE: deliberately no `fallbackToDestructiveMigration()`. The restaurants table is a cache, but
 * `favorites` is user data, so every schema change ships a real migration. v1 -> v2 is generated
 * by Room (it only adds nullable/defaulted columns and a table), which keeps favourites and the
 * cached list. It needs the exported schema files in `app/schemas` — commit that folder.
 */
@Database(
    entities = [RestaurantEntity::class, FavoriteEntity::class, MenuItemEntity::class],
    version = 3,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3)],
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun restaurantDao(): RestaurantDao
    abstract fun menuDao(): MenuDao
}

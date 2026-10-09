package com.example.mrd.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MenuDao {

    @Query("SELECT * FROM menu_items WHERE restaurantId = :restaurantId ORDER BY position ASC")
    fun observeMenu(restaurantId: String): Flow<List<MenuItemEntity>>

    @Query("DELETE FROM menu_items WHERE restaurantId = :restaurantId")
    suspend fun deleteMenu(restaurantId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMenuItems(items: List<MenuItemEntity>)
}

package com.example.mrd.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RestaurantDao {

    /**
     * [query] must already be LIKE-escaped (see `escapeLike`); `\` is the escape character.
     * Room's paging integration turns this into a LIMIT/OFFSET source that re-queries when either
     * table changes, so favouriting a row updates the list without any manual plumbing.
     */
    @Query(
        """
        SELECT r.*, (f.restaurantId IS NOT NULL) AS isFavorite
        FROM restaurants r
        LEFT JOIN favorites f ON f.restaurantId = r.id
        WHERE (:query = '' OR r.name LIKE '%' || :query || '%' ESCAPE '\')
          AND (:openOnly = 0 OR r.isOpen = 1)
          AND (:minRating <= 0 OR r.rating >= :minRating)
          AND (:minReviews <= 0 OR r.reviewCount >= :minReviews)
          AND (:maxDistanceKm <= 0 OR r.distanceKm <= :maxDistanceKm)
          AND (:maxEtaMinutes <= 0 OR r.etaMinutes <= :maxEtaMinutes)
          AND (:maxFeeCents < 0 OR r.deliveryFeeCents <= :maxFeeCents)
          AND (:maxPriceLevel <= 0 OR r.priceLevel <= :maxPriceLevel)
          AND (:cuisine = '' OR ('|' || r.cuisines || '|') LIKE '%|' || :cuisine || '|%')
          AND (:tag = '' OR ('|' || r.tags || '|') LIKE '%|' || :tag || '|%')
        ORDER BY
          CASE WHEN :sort = 1 THEN COALESCE(r.rating, 0) END DESC,
          CASE WHEN :sort = 2 THEN COALESCE(r.etaMinutes, 100000) END ASC,
          CASE WHEN :sort = 3 THEN COALESCE(r.distanceKm, 100000) END ASC,
          CASE WHEN :sort = 4 THEN COALESCE(r.deliveryFeeCents, 100000) END ASC,
          CASE WHEN :sort = 5 THEN COALESCE(r.reviewCount, 0) END DESC,
          r.position ASC
        """,
    )
    fun pagingSource(
        query: String,
        openOnly: Boolean,
        minRating: Double,
        minReviews: Int,
        maxDistanceKm: Double,
        maxEtaMinutes: Int,
        maxFeeCents: Int,
        maxPriceLevel: Int,
        cuisine: String,
        tag: String,
        sort: Int,
    ): PagingSource<Int, RestaurantWithFavorite>

    @Query(
        """
        SELECT r.*, (f.restaurantId IS NOT NULL) AS isFavorite
        FROM restaurants r
        LEFT JOIN favorites f ON f.restaurantId = r.id
        WHERE r.id = :id
        """,
    )
    fun observeById(id: String): Flow<RestaurantWithFavorite?>

    /**
     * "Popular right now": open, well-rated restaurants ranked by how many people reviewed them
     * (rating breaks ties). Falls back gracefully to rating order when review counts are missing.
     */
    @Query(
        """
        SELECT r.*, (f.restaurantId IS NOT NULL) AS isFavorite
        FROM restaurants r
        LEFT JOIN favorites f ON f.restaurantId = r.id
        WHERE r.isOpen = 1 AND r.rating >= 4.3
        ORDER BY COALESCE(r.reviewCount, 0) DESC, r.rating DESC, r.position ASC
        LIMIT :limit
        """,
    )
    fun observePopular(limit: Int): Flow<List<RestaurantWithFavorite>>

    /** Favourited restaurants that are in the cache, most recently favourited first. */
    @Query(
        """
        SELECT r.*, 1 AS isFavorite
        FROM favorites f
        INNER JOIN restaurants r ON r.id = f.restaurantId
        ORDER BY f.createdAt DESC
        """,
    )
    fun observeFavorites(): Flow<List<RestaurantWithFavorite>>

    @Query("SELECT COUNT(*) FROM restaurants")
    fun observeCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(restaurants: List<RestaurantEntity>)

    /** Removes rows the latest sync did not touch, without needing a (999-variable-limited) NOT IN. */
    @Query("DELETE FROM restaurants WHERE syncedAt != :syncedAt")
    suspend fun deleteNotSyncedAt(syncedAt: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE restaurantId = :id")
    suspend fun removeFavorite(id: String)
}

package com.example.mrd.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import androidx.room.withTransaction
import com.example.mrd.core.IoDispatcher
import com.example.mrd.data.local.AppDatabase
import com.example.mrd.data.local.FavoriteEntity
import com.example.mrd.data.local.MenuDao
import com.example.mrd.data.local.RestaurantDao
import com.example.mrd.data.local.UserPreferences
import com.example.mrd.data.mapper.escapeLike
import com.example.mrd.data.mapper.parseMenu
import com.example.mrd.data.mapper.parseRestaurants
import com.example.mrd.data.mapper.toDomain
import com.example.mrd.data.mapper.toMenuCategories
import com.example.mrd.data.remote.RestaurantApi
import com.example.mrd.domain.FailureReason
import com.example.mrd.domain.MenuCategory
import com.example.mrd.domain.RefreshResult
import com.example.mrd.domain.Restaurant
import com.example.mrd.domain.RestaurantRepository
import com.example.mrd.domain.SearchFilters
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException

@Singleton
class RestaurantRepositoryImpl @Inject constructor(
    private val api: RestaurantApi,
    private val dao: RestaurantDao,
    private val menuDao: MenuDao,
    private val db: AppDatabase,
    private val json: Json,
    private val preferences: UserPreferences,
    @IoDispatcher private val io: CoroutineDispatcher,
) : RestaurantRepository {

    /** Collapses concurrent refreshes (pull-to-refresh + retry + detail deep link) into a queue. */
    private val refreshMutex = Mutex()

    override fun pagedRestaurants(filters: SearchFilters): Flow<PagingData<Restaurant>> {
        val query = filters.query.trim().escapeLike()
        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                initialLoadSize = PAGE_SIZE * 2,
                prefetchDistance = PAGE_SIZE / 2,
                // Placeholders would let the scrollbar reflect the full size, but need COUNT(*)
                // on every invalidation. Off keeps invalidations cheap as the table grows.
                enablePlaceholders = false,
            ),
            pagingSourceFactory = {
                val a = filters.advanced
                dao.pagingSource(
                    query = query,
                    openOnly = filters.openOnly,
                    minRating = a.minRating,
                    minReviews = a.minReviews,
                    maxDistanceKm = a.maxDistanceKm,
                    maxEtaMinutes = a.maxEtaMinutes,
                    maxFeeCents = a.maxFeeCents,
                    maxPriceLevel = a.maxPriceLevel,
                    cuisine = a.cuisine,
                    tag = a.tag,
                    sort = a.sort.ordinal,
                )
            },
        ).flow.map { page -> page.map { it.toDomain() } }
    }

    override fun observeRestaurant(id: String): Flow<Restaurant?> =
        dao.observeById(id).map { it?.toDomain() }

    override fun observePopular(limit: Int): Flow<List<Restaurant>> =
        dao.observePopular(limit).map { rows -> rows.map { it.toDomain() } }

    override fun observeFavorites(): Flow<List<Restaurant>> =
        dao.observeFavorites().map { rows -> rows.map { it.toDomain() } }

    override fun observeCount(): Flow<Int> = dao.observeCount()

    override val lastSyncedAt: Flow<Long?> = preferences.lastSyncedAt

    override suspend fun refresh(): RefreshResult = refreshMutex.withLock {
        withContext(io) {
            guarded {
                val response = api.getRestaurants()
                val syncedAt = System.currentTimeMillis()
                val entities = json.parseRestaurants(response.restaurants, syncedAt)

                // Never replace a good cache with nothing: an empty/garbled 200 is treated as a
                // failed sync so the user keeps what they had.
                if (entities.isEmpty()) {
                    RefreshResult.Failure(FailureReason.INVALID_DATA)
                } else {
                    db.withTransaction {
                        dao.upsertAll(entities)
                        dao.deleteNotSyncedAt(syncedAt)
                    }
                    preferences.setLastSyncedAt(syncedAt)
                    RefreshResult.Success(entities.size)
                }
            }
        }
    }

    override suspend fun setFavorite(id: String, favorite: Boolean) {
        withContext(io) {
            if (favorite) {
                dao.addFavorite(FavoriteEntity(restaurantId = id, createdAt = System.currentTimeMillis()))
            } else {
                dao.removeFavorite(id)
            }
        }
    }

    override fun observeMenu(restaurantId: String): Flow<List<MenuCategory>> =
        menuDao.observeMenu(restaurantId).map { it.toMenuCategories() }

    override suspend fun refreshMenu(restaurantId: String): RefreshResult = withContext(io) {
        guarded {
            val response = api.getMenu(restaurantId)
            val items = json.parseMenu(restaurantId, response)
            // Replace, don't merge: items removed upstream must disappear from the cache too.
            db.withTransaction {
                menuDao.deleteMenu(restaurantId)
                menuDao.insertMenuItems(items)
            }
            RefreshResult.Success(items.size)
        }
    }

    /** One place that turns expected failures into typed results (cancellation still propagates). */
    private suspend fun guarded(block: suspend () -> RefreshResult): RefreshResult =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            RefreshResult.Failure(FailureReason.SERVER)
        } catch (e: IOException) {
            RefreshResult.Failure(FailureReason.NETWORK)
        } catch (e: SerializationException) {
            RefreshResult.Failure(FailureReason.INVALID_DATA)
        } catch (e: IllegalArgumentException) {
            RefreshResult.Failure(FailureReason.INVALID_DATA)
        }

    private companion object {
        const val PAGE_SIZE = 30
    }
}

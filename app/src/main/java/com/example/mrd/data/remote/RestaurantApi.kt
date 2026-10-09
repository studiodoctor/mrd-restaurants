package com.example.mrd.data.remote

import retrofit2.http.GET
import retrofit2.http.Path

interface RestaurantApi {
    @GET("restaurants")
    suspend fun getRestaurants(): RestaurantsResponse

    @GET("restaurants/{id}/menu")
    suspend fun getMenu(@Path("id") restaurantId: String): MenuResponseDto
}

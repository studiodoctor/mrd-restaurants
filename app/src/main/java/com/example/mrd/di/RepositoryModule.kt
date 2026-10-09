package com.example.mrd.di

import com.example.mrd.data.RestaurantRepositoryImpl
import com.example.mrd.data.local.UserPreferences
import com.example.mrd.domain.FilterStore
import com.example.mrd.domain.RestaurantRepository
import com.example.mrd.domain.ThemeStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindRestaurantRepository(impl: RestaurantRepositoryImpl): RestaurantRepository

    @Binds
    @Singleton
    abstract fun bindFilterStore(impl: UserPreferences): FilterStore

    @Binds
    @Singleton
    abstract fun bindThemeStore(impl: UserPreferences): ThemeStore
}

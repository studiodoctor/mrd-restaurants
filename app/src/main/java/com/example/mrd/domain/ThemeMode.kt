package com.example.mrd.domain

import kotlinx.coroutines.flow.Flow

/** How the app picks its colour scheme. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Persists the user's theme choice across cold starts. */
interface ThemeStore {
    val themeMode: Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
}

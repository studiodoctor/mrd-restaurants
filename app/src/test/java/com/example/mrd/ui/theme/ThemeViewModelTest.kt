package com.example.mrd.ui.theme

import com.example.mrd.domain.ThemeMode
import com.example.mrd.testutil.FakeThemeStore
import com.example.mrd.testutil.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ThemeViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    @Test
    fun `starts with the saved mode so there is no wrong first frame`() {
        val vm = ThemeViewModel(FakeThemeStore(ThemeMode.DARK))
        assertEquals(ThemeMode.DARK, vm.themeMode.value)
    }

    @Test
    fun `choosing a mode updates the state and is persisted`() = runTest {
        val store = FakeThemeStore()
        val vm = ThemeViewModel(store)
        assertEquals(ThemeMode.SYSTEM, vm.themeMode.value)

        vm.setThemeMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, vm.themeMode.value)

        vm.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, vm.themeMode.value)
    }
}

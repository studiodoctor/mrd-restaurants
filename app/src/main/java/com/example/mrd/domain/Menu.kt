package com.example.mrd.domain

data class MenuCategory(
    val id: String,
    val name: String,
    val items: List<MenuItem>,
)

data class MenuItem(
    val id: String,
    val name: String,
    val description: String?,
    /** Null means the backend didn't send a price; the UI shows "Price on request". */
    val priceCents: Int?,
    val available: Boolean,
    val options: List<MenuOption>,
)

data class MenuOption(
    val name: String,
    val priceCents: Int,
)

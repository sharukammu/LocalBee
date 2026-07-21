package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

data class CategoryConfigItem(val name: String, val emoji: String)

class CategoryService {
    // This is the centralized configuration file/structure for all categories.
    // Dynamic updates here will instantly reflect across the Home screen and search modules.
    private val categories = listOf(
        CategoryConfigItem("Fruits", "🍎"),
        CategoryConfigItem("Vegetables", "🥬"),
        CategoryConfigItem("Food", "🍗"),
        CategoryConfigItem("Fish & Chicken", "🐟"),
        CategoryConfigItem("Grocery", "🛒"),
        CategoryConfigItem("Milk", "🥛"),
        CategoryConfigItem("Bakery", "🍞"),
        CategoryConfigItem("Flowers", "💐"),
        CategoryConfigItem("Gifts", "🎁"),
        CategoryConfigItem("Jewellery", "💍"),
        CategoryConfigItem("Kids", "👶"),
        CategoryConfigItem("Mobile", "📱")
    )

    fun fetchCategories(): Flow<List<CategoryConfigItem>> {
        return flowOf(categories)
    }

    fun getCategoryNames(): List<String> {
        return categories.map { it.name }
    }
}

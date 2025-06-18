package com.example.whatsappfilter.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FilterCategoryDao {
    @Query("SELECT * FROM filter_categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<FilterCategory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: FilterCategory)

    @Update
    suspend fun updateCategory(category: FilterCategory)

    @Delete
    suspend fun deleteCategory(category: FilterCategory)

    @Query("SELECT * FROM filter_categories WHERE isEnabled = 1")
    fun getEnabledCategories(): Flow<List<FilterCategory>>

    @Query("UPDATE filter_categories SET isEnabled = :isEnabled WHERE id = :categoryId")
    suspend fun setCategoryEnabled(categoryId: String, isEnabled: Boolean)

    @Query("SELECT * FROM filter_categories WHERE id = :categoryId")
    suspend fun getCategoryById(categoryId: String): FilterCategory?
} 
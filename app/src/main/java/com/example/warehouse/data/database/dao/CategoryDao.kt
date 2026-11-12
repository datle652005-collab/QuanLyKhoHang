package com.example.warehouse.data.database.dao

import androidx.room.*
import com.example.warehouse.data.database.entity.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY createdAt DESC, categoryId DESC")
    fun getAllCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE categoryId = :id LIMIT 1")
    suspend fun getCategoryById(id: Long): Category?

    @Query("SELECT * FROM categories WHERE categoryName LIKE '%' || :query || '%' ORDER BY createdAt DESC, categoryId DESC")
    fun searchCategories(query: String): Flow<List<Category>>

    @Query("SELECT COUNT(*) FROM categories WHERE LOWER(TRIM(categoryName)) = LOWER(TRIM(:name)) AND categoryId != :excludeId")
    suspend fun countCategoryByName(name: String, excludeId: Long = 0): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long

    @Update
    suspend fun updateCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoryCount(): Int
}

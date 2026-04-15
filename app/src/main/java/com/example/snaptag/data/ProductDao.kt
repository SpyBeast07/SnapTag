package com.example.snaptag.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Delete
    suspend fun deleteProduct(product: ProductEntity)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteById(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<ProductEntity>)

    @Query("DELETE FROM products")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM products")
    fun getTotalProductCount(): Flow<Int>

    @Query("SELECT SUM(stock) FROM products")
    fun getTotalStockUnits(): Flow<Int?>

    @Query("SELECT SUM(price * stock) FROM products")
    fun getTotalInventoryValue(): Flow<Double?>

    @Query("SELECT * FROM products ORDER BY stock DESC LIMIT 5")
    fun getTopProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE stock < 5")
    fun getLowStockProducts(): Flow<List<ProductEntity>>
}

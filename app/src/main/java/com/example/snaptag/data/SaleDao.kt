package com.example.snaptag.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {
    @Insert
    suspend fun insertSale(sale: SaleEntity): Long

    @Insert
    suspend fun insertSaleItems(items: List<SaleItemEntity>)

    @Transaction
    suspend fun recordSale(sale: SaleEntity, items: List<SaleItemEntity>) {
        val id = insertSale(sale)
        val itemsWithId = items.map { it.copy(saleId = id) }
        insertSaleItems(itemsWithId)
    }

    @Query("SELECT * FROM sales ORDER BY timestamp DESC")
    fun getAllSales(): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales WHERE timestamp BETWEEN :from AND :to ORDER BY timestamp DESC")
    fun getSalesBetweenDates(from: Long, to: Long): List<SaleEntity>

    @Query("SELECT SUM(totalAmount) FROM sales")
    fun getTotalRevenue(): Flow<Double?>

    @Query("SELECT COUNT(*) FROM sales")
    fun getTotalOrders(): Flow<Int>

    @Query("SELECT SUM(totalItems) FROM sales")
    fun getTotalItemsSold(): Flow<Int?>

    @Query("SELECT * FROM sales ORDER BY timestamp DESC LIMIT 10")
    fun getRecentSales(): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    suspend fun getSaleItems(saleId: Long): List<SaleItemEntity>
}

package com.example.snaptag.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProductRepository(
    private val productDao: ProductDao,
    private val saleDao: SaleDao
) {

    val allProducts: Flow<List<Product>> = productDao.getAllProducts().map { entities ->
        entities.map { it.toProduct() }
    }

    suspend fun recordSale(sale: SaleEntity, items: List<SaleItemEntity>) {
        saleDao.recordSale(sale, items)
    }

    suspend fun processSale(sale: SaleEntity, saleItems: List<SaleItemEntity>, cartItems: List<com.example.snaptag.viewmodel.CartItem>) {
        // 1. Update Stocks
        for (item in cartItems) {
            val product = productDao.getProductById(item.productId)
            if (product != null) {
                val updatedProduct = product.copy(stock = (product.stock - item.quantity).coerceAtLeast(0))
                productDao.updateProduct(updatedProduct)
            }
        }
        
        // 2. Record Sale (in a transaction)
        saleDao.recordSale(sale, saleItems)
    }

    val allSales: Flow<List<SaleEntity>> = saleDao.getAllSales()

    val totalRevenue: Flow<Double> = saleDao.getTotalRevenue().map { it ?: 0.0 }

    val totalOrders: Flow<Int> = saleDao.getTotalOrders()

    val totalItemsSold: Flow<Int> = saleDao.getTotalItemsSold().map { it ?: 0 }

    val recentSales: Flow<List<SaleEntity>> = saleDao.getRecentSales()

    suspend fun getSalesBetweenDates(from: Long, to: Long): List<SaleEntity> {
        return saleDao.getSalesBetweenDates(from, to)
    }

    suspend fun getSaleItems(saleId: Long): List<SaleItemEntity> {
        return saleDao.getSaleItems(saleId)
    }

    suspend fun insert(product: Product) {
        productDao.insertProduct(product.toEntity())
    }

    suspend fun update(product: Product) {
        productDao.updateProduct(product.toEntity())
    }

    suspend fun delete(product: Product) {
        productDao.deleteProduct(product.toEntity())
    }

    suspend fun deleteById(id: String) {
        productDao.deleteById(id)
    }

    suspend fun insertAll(products: List<Product>) {
        productDao.insertAll(products.map { it.toEntity() })
    }

    suspend fun deleteAll() {
        productDao.deleteAll()
    }

    val totalProductCount: Flow<Int> = productDao.getTotalProductCount()

    val totalStockUnits: Flow<Int> = productDao.getTotalStockUnits().map { it ?: 0 }

    val totalInventoryValue: Flow<Double> = productDao.getTotalInventoryValue().map { it ?: 0.0 }

    val topProducts: Flow<List<Product>> = productDao.getTopProducts().map { entities ->
        entities.map { it.toProduct() }
    }

    val lowStockProducts: Flow<List<Product>> = productDao.getLowStockProducts().map { entities ->
        entities.map { it.toProduct() }
    }

    suspend fun getProductById(id: String): Product? {
        return productDao.getProductById(id)?.toProduct()
    }
}

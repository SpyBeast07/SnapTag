package com.example.snaptag.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProductRepository(private val productDao: ProductDao) {

    val allProducts: Flow<List<Product>> = productDao.getAllProducts().map { entities ->
        entities.map { it.toProduct() }
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

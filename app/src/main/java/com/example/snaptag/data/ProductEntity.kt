package com.example.snaptag.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val name: String,
    val price: Double,
    val stock: Int,
    val barcode: String? = null
)

fun ProductEntity.toProduct() = Product(id, name, price, stock, barcode)
fun Product.toEntity() = ProductEntity(id, name, price, stock, barcode)

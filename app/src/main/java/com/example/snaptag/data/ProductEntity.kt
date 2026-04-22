package com.example.snaptag.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val name: String,
    val price: Double,
    val stock: Int,
    val barcode: String? = null,
    val gstPercent: Double = 0.0,
    val discountPercent: Double = 0.0
)

fun ProductEntity.toProduct() = Product(id, name, price, stock, barcode, gstPercent, discountPercent)
fun Product.toEntity() = ProductEntity(id, name, price, stock, barcode, gstPercent, discountPercent)

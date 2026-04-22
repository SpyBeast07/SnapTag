package com.example.snaptag.data

data class Product(
    val id: String,
    val name: String,
    val price: Double,
    val stock: Int,
    val barcode: String? = null,
    val gstPercent: Double = 0.0,
    val discountPercent: Double = 0.0
)

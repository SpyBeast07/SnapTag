package com.example.snaptag.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(tableName = "sales")
data class SaleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val totalItems: Int,
    val totalAmount: Double,
    val customerPhone: String? = null,
    val subtotal: Double = 0.0,
    val totalItemDiscounts: Double = 0.0,
    val billDiscountAmount: Double = 0.0,
    val billDiscountPercent: Double = 0.0,
    val totalTaxableValue: Double = 0.0,
    val totalGst: Double = 0.0
)

@Entity(
    tableName = "sale_items",
    foreignKeys = [
        ForeignKey(
            entity = SaleEntity::class,
            parentColumns = ["id"],
            childColumns = ["saleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["saleId"])]
)
data class SaleItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleId: Long,
    val productName: String,
    val quantity: Int,
    val price: Double,
    val gstPercentage: Double? = null,
    val discountPercent: Double = 0.0,
    val itemDiscountAmount: Double = 0.0,
    val billDiscountShare: Double = 0.0,
    val taxableValue: Double = 0.0,
    val gstAmount: Double = 0.0,
    val finalTotal: Double = 0.0
)

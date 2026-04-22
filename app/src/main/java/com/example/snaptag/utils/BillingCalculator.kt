package com.example.snaptag.utils

import com.example.snaptag.viewmodel.CartItem
import kotlin.math.round

data class CalculatedItem(
    val cartItem: CartItem,
    val itemTotal: Double,
    val itemDiscountAmount: Double,
    val afterItemDiscount: Double,
    val billDiscountShare: Double,
    val finalTaxableValue: Double,
    val gstAmount: Double,
    val finalItemTotal: Double
)

data class BillSummary(
    val items: List<CalculatedItem>,
    val subtotal: Double, // Sum of (Price * Qty)
    val totalItemDiscounts: Double,
    val billDiscountAmount: Double,
    val totalTaxableValue: Double,
    val totalGst: Double,
    val grandTotal: Double
)

object BillingCalculator {
    fun calculateBill(items: List<CartItem>, billDiscountPercent: Double): BillSummary {
        val rawSubtotal = items.sumOf { it.price * it.quantity }
        
        val itemsWithInitialTotals = items.map { item ->
            val itemTotal = item.price * item.quantity
            val itemDiscountAmount = itemTotal * item.discountPercent / 100.0
            val afterItemDiscount = itemTotal - itemDiscountAmount
            Triple(item, itemDiscountAmount, afterItemDiscount)
        }

        val totalAfterItemDiscounts = itemsWithInitialTotals.sumOf { it.third }
        val billDiscountAmount = totalAfterItemDiscounts * billDiscountPercent / 100.0

        val calculatedItems = itemsWithInitialTotals.map { (item, itemDiscountAmount, afterItemDiscount) ->
            val itemTotal = item.price * item.quantity
            
            // Step 5: Distribute Bill Discount
            val itemShare = if (totalAfterItemDiscounts > 0.0) afterItemDiscount / totalAfterItemDiscounts else 0.0
            val itemBillDiscount = billDiscountAmount * itemShare
            val finalTaxableValue = (afterItemDiscount - itemBillDiscount).coerceAtLeast(0.0)

            // Step 6: Apply GST
            val gstAmount = finalTaxableValue * item.gstPercent / 100.0

            // Step 7: Final Per Item
            val finalItemTotal = finalTaxableValue + gstAmount

            CalculatedItem(
                cartItem = item,
                itemTotal = roundTwoDecimals(itemTotal),
                itemDiscountAmount = roundTwoDecimals(itemDiscountAmount),
                afterItemDiscount = roundTwoDecimals(afterItemDiscount),
                billDiscountShare = roundTwoDecimals(itemBillDiscount),
                finalTaxableValue = roundTwoDecimals(finalTaxableValue),
                gstAmount = roundTwoDecimals(gstAmount),
                finalItemTotal = roundTwoDecimals(finalItemTotal)
            )
        }

        return BillSummary(
            items = calculatedItems,
            subtotal = roundTwoDecimals(rawSubtotal),
            totalItemDiscounts = roundTwoDecimals(calculatedItems.sumOf { it.itemDiscountAmount }),
            billDiscountAmount = roundTwoDecimals(billDiscountAmount),
            totalTaxableValue = roundTwoDecimals(calculatedItems.sumOf { it.finalTaxableValue }),
            totalGst = roundTwoDecimals(calculatedItems.sumOf { it.gstAmount }),
            grandTotal = roundTwoDecimals(calculatedItems.sumOf { it.finalItemTotal })
        )
    }

    private fun roundTwoDecimals(value: Double): Double {
        return round(value * 100) / 100.0
    }
}

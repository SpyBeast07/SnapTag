package com.example.snaptag.ui.components

import android.graphics.Rect

data class OCRPriceElement(
    val text: String,
    val boundingBox: Rect?
)

object PriceDetector {

    /**
     * Smart price detection logic.
     * 1. Extracts numeric elements.
     * 2. Prefers numbers with decimals.
     * 3. Handles large numbers.
     * 4. Handles split prices based on visual height (Big digits = whole, Small digits = decimal).
     */
    fun detectPrice(elements: List<OCRPriceElement>): String? {
        val cleanElements = elements.map { 
            it.copy(text = normalize(it.text)) 
        }.filter { it.text.isNotEmpty() }

        // 1. Try to find an element that already looks like a decimal price
        for (element in cleanElements) {
            if (element.text.contains(".") && isValidPrice(element.text)) {
                return element.text
            }
        }

        // 2. Smart Split Detection: Use visual height to distinguish whole vs decimal
        // We look for any two elements where one is significantly taller than the other
        for (i in cleanElements.indices) {
            for (j in cleanElements.indices) {
                if (i == j) continue

                val partA = cleanElements[i] // Potential whole number
                val partB = cleanElements[j] // Potential decimal part

                val heightA = partA.boundingBox?.height() ?: 0
                val heightB = partB.boundingBox?.height() ?: 0

                // Heuristic: Whole number is taller, and decimal part is usually 2 digits
                if (heightA > heightB * 1.2 && partB.text.length == 2 && !partA.text.contains(".")) {
                    val combined = "${partA.text}.${partB.text}"
                    if (isValidPrice(combined)) return combined
                }
            }
        }

        // 3. Try to find a large whole number
        for (element in cleanElements) {
            val value = element.text.toDoubleOrNull() ?: 0.0
            if (value >= 100.0 && isValidPrice(element.text)) {
                return element.text
            }
        }

        // 4. Fallback: any valid numeric value
        for (element in cleanElements) {
            if (isValidPrice(element.text)) {
                return element.text
            }
        }

        return null
    }

    private fun normalize(text: String): String {
        // Keep digits and at most one dot
        val onlyNumeric = text.replace(Regex("[^0-9.]"), "")
        
        val dotIndex = onlyNumeric.indexOf('.')
        return if (dotIndex != -1) {
            val whole = onlyNumeric.substring(0, dotIndex)
            val decimal = onlyNumeric.substring(dotIndex + 1).replace(".", "").take(2)
            if (decimal.isEmpty()) whole else "$whole.$decimal"
        } else {
            onlyNumeric
        }
    }

    private fun isValidPrice(priceStr: String): Boolean {
        if (priceStr.length > 7) return false
        val value = priceStr.toDoubleOrNull() ?: return false
        return value in 10.0..10000.0
    }
}

package com.example.snaptag.ui.components

import android.graphics.Rect

data class OCRPriceElement(
    val text: String,
    val boundingBox: Rect? = null
)

data class ScoredPrice(val price: String, val score: Int)

object PriceDetector {

    private val frequencyMap = mutableMapOf<String, Int>()

    fun clearStability() {
        frequencyMap.clear()
    }

    /**
     * Smart price detection logic with multi-stage filtering, scoring, and group dominance.
     */
    fun detectPrices(elements: List<OCRPriceElement>, fullText: String = ""): List<ScoredPrice> {
        val candidates = mutableMapOf<String, Int>()

        // 1. Broad Pattern Search on Full Text
        val broadPattern = Regex("[0-9]{1,3}(?:[.,][0-9]{3})*[.,][0-9]{2}|[0-9]{1,6}")
        broadPattern.findAll(fullText).forEach { match ->
            val norm = normalize(match.value)
            if (isValidPrice(norm)) {
                val score = calculateScore(norm, elements, fullText)
                candidates[norm] = (candidates[norm] ?: 0) + score
            }
        }

        // Pre-normalize elements for structural analysis
        val cleanElements = elements.map { 
            it.copy(text = normalize(it.text)) 
        }.filter { it.text.isNotEmpty() }

        // 2. Elements that are already decimal prices
        for (element in cleanElements) {
            if (element.text.contains(".") && isValidPrice(element.text)) {
                val score = calculateScore(element.text, elements, fullText)
                candidates[element.text] = (candidates[element.text] ?: 0) + score
            }
        }

        // 3. Smart Split Detection
        for (i in cleanElements.indices) {
            for (j in cleanElements.indices) {
                if (i == j) continue
                val partA = cleanElements[i]
                val partB = cleanElements[j]
                val rectA = partA.boundingBox ?: continue
                val rectB = partB.boundingBox ?: continue

                if (partB.text.length != 2) continue
                
                val heightA = rectA.height()
                val heightB = rectB.height()
                val isTaller = heightA > heightB * 1.15
                
                val horizontalGap = rectB.left - rectA.right
                val verticalOverlap = Math.min(rectA.bottom, rectB.bottom) - Math.max(rectA.top, rectB.top)
                val isNear = horizontalGap in -(rectA.width() / 2)..rectA.width()
                val isAligned = verticalOverlap > 0 || (rectB.top >= rectA.top && rectB.bottom <= rectA.bottom)

                if ((isTaller || (isNear && isAligned)) && !partA.text.contains(".")) {
                    val combined = "${partA.text}.${partB.text}"
                    if (isValidPrice(combined)) {
                        val score = calculateScore(combined, elements, fullText)
                        candidates[combined] = (candidates[combined] ?: 0) + score
                    }
                }
            }
        }

        // 4. Fallback: Individual numeric values
        for (element in cleanElements) {
            if (isValidPrice(element.text)) {
                val score = calculateScore(element.text, elements, fullText)
                candidates[element.text] = (candidates[element.text] ?: 0) + score
            }
        }

        val initialResults = candidates.map { ScoredPrice(it.key, it.value) }
        
        // 5. Group Dominance Logic: Boost largest cluster
        val maxVal = initialResults.maxOfOrNull { it.price.toDoubleOrNull() ?: 0.0 } ?: 0.0
        return initialResults.map { scored ->
            val v = scored.price.toDoubleOrNull() ?: 0.0
            if (v > maxVal * 0.5 && maxVal > 0) {
                scored.copy(score = scored.score + 1)
            } else {
                scored
            }
        }.sortedByDescending { it.score }
    }

    private fun calculateScore(price: String, elements: List<OCRPriceElement>, fullText: String): Int {
        var score = 0
        val value = price.toDoubleOrNull() ?: 0.0

        // Positive
        if (price.contains(".")) score += 3
        if (value in 10.0..20000.0) score += 2
        if (value > 100.0) score += 1

        // Penalty for tiny decimals (e.g., .9 or 1.2)
        if (price.contains(".")) {
            val parts = price.split("\\.".toRegex())
            if (parts.size == 2 && parts[1].length < 2) {
                score -= 2
            }
        }

        // Context boost (MRP fix)
        if (fullText.contains("MRP") && fullText.contains(price)) {
            score += 5
        } else {
            // Check proximity in elements
            val priceElement = elements.find { normalize(it.text) == price || it.text.contains(price) }
            if (priceElement != null) {
                val box = priceElement.boundingBox
                if (box != null) {
                    val nearbyText = elements.filter { el ->
                        val elBox = el.boundingBox ?: return@filter false
                        val dx = Math.abs(elBox.centerX() - box.centerX())
                        val dy = Math.abs(elBox.centerY() - box.centerY())
                        dx < box.width() * 3 && dy < box.height() * 2
                    }.joinToString(" ") { it.text }
                    
                    if (
                        nearbyText.contains("₹") || 
                        nearbyText.contains("MRP", true) || 
                        nearbyText.contains("Rs", true) || 
                        nearbyText.contains("INR", true) ||
                        nearbyText.contains("Price", true)
                    ) {
                        score += 5
                    }
                }
            }
        }

        // Negative
        if (price.startsWith("0") && price.length > 3) score -= 2
        if (price.length > 6) score -= 2

        return score
    }

    private fun normalize(text: String): String {
        val cleaned = text
            .replace("O", "0")
            .replace("o", "0")
            .replace(",", ".")
            .replace(Regex("[^0-9.]"), "")

        val parts = cleaned.split("\\.".toRegex())
        return when {
            parts.size > 2 -> parts[0] + "." + parts.drop(1).joinToString("").take(2)
            parts.size == 2 -> parts[0] + "." + parts[1].take(2)
            else -> cleaned
        }
    }

    private fun isValidPrice(priceStr: String): Boolean {
        if (priceStr.isEmpty() || priceStr.length > 8) return false
        val value = priceStr.toDoubleOrNull() ?: return false
        return value in 5.0..50000.0
    }

    /**
     * Updates stability with decay/reset logic for responsiveness and memory safety.
     */
    fun updateStability(prices: List<ScoredPrice>): List<ScoredPrice> {
        val current = prices.map { it.price }.toSet()

        // increment seen
        prices.forEach {
            frequencyMap[it.price] = (frequencyMap[it.price] ?: 0) + 1
        }

        // decay unseen
        val keys = frequencyMap.keys.toList()
        for (key in keys) {
            if (key !in current) {
                frequencyMap[key] = (frequencyMap[key] ?: 1) - 1
                if (frequencyMap[key]!! <= 0) {
                    frequencyMap.remove(key)
                }
            }
        }

        return prices.filter { (frequencyMap[it.price] ?: 0) >= 2 }
    }

    fun getBestPrice(prices: List<ScoredPrice>): String? {
        return prices.maxByOrNull { it.score }?.price
    }
}

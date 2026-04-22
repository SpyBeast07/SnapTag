package com.example.snaptag.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.snaptag.data.SaleEntity
import com.example.snaptag.viewmodel.CartItem
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfGenerator {
    fun generateBillPdf(
        context: Context,
        shopName: String,
        address: String,
        phone: String,
        email: String,
        gst: String,
        footerNote: String,
        billSummary: BillSummary,
        customerPhone: String? = null,
        isGstEnabled: Boolean = true
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint()
        val titlePaint = Paint()

        val billId = System.currentTimeMillis().toString()
        val dateTime = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())

        var y = 40f

        // Header
        titlePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        titlePaint.textSize = 20f
        titlePaint.textAlign = Paint.Align.CENTER
        canvas.drawText(shopName, 297f, y, titlePaint)
        y += 25f

        paint.textSize = 10f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(address, 297f, y, paint)
        y += 15f
        canvas.drawText("Phone: $phone", 297f, y, paint)
        y += 12f
        if (email.isNotEmpty()) {
            canvas.drawText("Email: $email", 297f, y, paint)
            y += 12f
        }
        if (gst.isNotEmpty()) {
            canvas.drawText("GSTIN: $gst", 297f, y, paint)
            y += 12f
        }

        y += 20f
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 10f
        canvas.drawText("Bill ID: $billId", 40f, y, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Date: $dateTime", 555f, y, paint)
        y += 15f
        if (!customerPhone.isNullOrBlank()) {
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("Customer: $customerPhone", 40f, y, paint)
            y += 15f
        }

        y += 10f
        // Table Header
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 9f
        
        val colItem = 40f
        val colQty = 200f
        val colPrice = 240f
        val colDisc = 300f
        val colTaxable = 370f
        val colGst = 440f
        val colTotal = 510f

        canvas.drawText("Item", colItem, y, paint)
        canvas.drawText("Qty", colQty, y, paint)
        canvas.drawText("Price", colPrice, y, paint)
        canvas.drawText("Disc", colDisc, y, paint)
        canvas.drawText("Taxable", colTaxable, y, paint)
        canvas.drawText("GST", colGst, y, paint)
        canvas.drawText("Total", colTotal, y, paint)
        
        y += 5f
        canvas.drawLine(40f, y, 555f, y, paint)
        y += 15f

        // Table Items
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        billSummary.items.forEach { item ->
            val cartItem = item.cartItem
            canvas.drawText(if (cartItem.name.length > 25) cartItem.name.substring(0, 22) + "..." else cartItem.name, colItem, y, paint)
            canvas.drawText(cartItem.quantity.toString(), colQty, y, paint)
            canvas.drawText(String.format(Locale.getDefault(), "%.2f", cartItem.price), colPrice, y, paint)
            
            val totalDisc = item.itemDiscountAmount + item.billDiscountShare
            canvas.drawText(String.format(Locale.getDefault(), "%.2f", totalDisc), colDisc, y, paint)
            canvas.drawText(String.format(Locale.getDefault(), "%.2f", item.finalTaxableValue), colTaxable, y, paint)
            canvas.drawText(String.format(Locale.getDefault(), "%.2f (%.0f%%)", item.gstAmount, cartItem.gstPercent), colGst, y, paint)
            canvas.drawText(String.format(Locale.getDefault(), "%.2f", item.finalItemTotal), colTotal, y, paint)
            
            y += 15f
            
            // Basic page break check
            if (y > 750f) {
                // Not handling multiple pages for now as per simple project scope, but added check
                return@forEach
            }
        }

        y += 5f
        canvas.drawLine(40f, y, 555f, y, paint)
        y += 20f

        // Summary
        val summaryX = 370f
        val valueX = 555f
        paint.textAlign = Paint.Align.LEFT
        
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Items Total:", summaryX, y, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("₹${String.format(Locale.getDefault(), "%.2f", billSummary.subtotal)}", valueX, y, paint)
        y += 15f

        if (billSummary.totalItemDiscounts > 0) {
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("Item Discounts:", summaryX, y, paint)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("-₹${String.format(Locale.getDefault(), "%.2f", billSummary.totalItemDiscounts)}", valueX, y, paint)
            y += 15f
        }

        if (billSummary.billDiscountAmount > 0) {
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("Bill Discount:", summaryX, y, paint)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("-₹${String.format(Locale.getDefault(), "%.2f", billSummary.billDiscountAmount)}", valueX, y, paint)
            y += 15f
        }

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("Taxable Value:", summaryX, y, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("₹${String.format(Locale.getDefault(), "%.2f", billSummary.totalTaxableValue)}", valueX, y, paint)
        y += 15f

        if (isGstEnabled && billSummary.totalGst > 0) {
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("Total GST:", summaryX, y, paint)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("+₹${String.format(Locale.getDefault(), "%.2f", billSummary.totalGst)}", valueX, y, paint)
            y += 15f
        }

        y += 5f
        paint.textSize = 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("Grand Total:", summaryX, y, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("₹${String.format(Locale.getDefault(), "%.2f", billSummary.grandTotal)}", valueX, y, paint)
        y += 40f

        // Footer
        paint.textSize = 10f
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        canvas.drawText(footerNote, 297f, y, paint)

        pdfDocument.finishPage(page)

        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val fileName = if (!customerPhone.isNullOrBlank()) {
            "Bill_${customerPhone.filter { it.isDigit() }}_${sdf.format(Date())}.pdf"
        } else {
            "Bill_${sdf.format(Date())}.pdf"
        }

        val cacheDir = File(context.getExternalFilesDir(null), "SnapTag/bills")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        val cacheFile = File(cacheDir, fileName)

        try {
            pdfDocument.writeTo(FileOutputStream(cacheFile))
            saveToPublicStorage(context, cacheFile, fileName, "bills")
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            pdfDocument.close()
        }

        return cacheFile
    }

    fun generateSalesReportPdf(
        context: Context,
        fromDate: Long,
        toDate: Long,
        sales: List<SaleEntity>,
        totalRevenue: Double,
        totalItemsSold: Int
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint()
        val titlePaint = Paint()

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dateTimeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val generatedOn = dateTimeFormat.format(Date())

        var y = 40f

        // Header
        titlePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        titlePaint.textSize = 20f
        titlePaint.textAlign = Paint.Align.CENTER
        canvas.drawText("SnapTag Sales Report", 297f, y, titlePaint)
        y += 25f

        paint.textSize = 12f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("Generated on: $generatedOn", 297f, y, paint)
        y += 15f
        canvas.drawText("From: ${dateFormat.format(Date(fromDate))} To: ${dateFormat.format(Date(toDate))}", 297f, y, paint)
        y += 30f

        // Table Header
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("Order ID", 40f, y, paint)
        canvas.drawText("Date & Time", 120f, y, paint)
        canvas.drawText("Items", 350f, y, paint)
        canvas.drawText("Amount", 450f, y, paint)
        y += 10f
        canvas.drawLine(40f, y, 555f, y, paint)
        y += 20f

        // Table Items
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        sales.forEach { sale ->
            // Use a smaller font for the table if needed
            paint.textSize = 10f
            val dateTimeStr = dateTimeFormat.format(Date(sale.timestamp))
            
            canvas.drawText("#${sale.id}", 40f, y, paint)
            canvas.drawText(dateTimeStr, 110f, y, paint)
            canvas.drawText(sale.totalItems.toString(), 350f, y, paint)
            canvas.drawText("₹${String.format(Locale.getDefault(), "%.2f", sale.totalAmount)}", 450f, y, paint)
            y += 20f
            
            // Basic page break check
            if (y > 780f) {
                // In a real app we'd start a new page, but for now we'll just stop
                // or the user could be warned if they have 1000s of sales.
                return@forEach 
            }
        }

        y += 10f
        canvas.drawLine(40f, y, 555f, y, paint)
        y += 25f

        // Summary
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("Total Orders: ${sales.size}", 40f, y, paint)
        y += 20f
        canvas.drawText("Total Items Sold: $totalItemsSold", 40f, y, paint)
        y += 25f
        paint.textSize = 14f
        canvas.drawText("Grand Total Revenue: ₹${String.format(Locale.getDefault(), "%.2f", totalRevenue)}", 40f, y, paint)
        y += 40f

        paint.textSize = 12f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("-- End of Report --", 297f, y, paint)

        pdfDocument.finishPage(page)

        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val fileName = "SalesReport_${sdf.format(Date())}.pdf"

        val cacheDir = File(context.getExternalFilesDir(null), "SnapTag/sales")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        val cacheFile = File(cacheDir, fileName)

        try {
            pdfDocument.writeTo(FileOutputStream(cacheFile))
            saveToPublicStorage(context, cacheFile, fileName, "sales")
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            pdfDocument.close()
        }

        return cacheFile
    }

    private fun saveToPublicStorage(context: Context, sourceFile: File, fileName: String, subFolder: String) {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/SnapTag/$subFolder")
            }
        }

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val uri = resolver.insert(collection, contentValues)
        uri?.let {
            resolver.openOutputStream(it)?.use { outputStream ->
                sourceFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }
    }
}

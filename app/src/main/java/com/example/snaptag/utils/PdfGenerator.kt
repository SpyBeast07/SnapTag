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
        cartItems: List<CartItem>,
        totalAmount: Double,
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

        paint.textSize = 12f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(address, 297f, y, paint)
        y += 15f
        canvas.drawText("Phone: $phone", 297f, y, paint)
        y += 15f
        if (email.isNotEmpty()) {
            canvas.drawText("Email: $email", 297f, y, paint)
            y += 15f
        }
        if (gst.isNotEmpty()) {
            canvas.drawText("GST: $gst", 297f, y, paint)
            y += 15f
        }

        y += 20f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("Bill ID: $billId", 40f, y, paint)
        canvas.drawText("Date: $dateTime", 350f, y, paint)
        y += 30f

        // Table Header
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Item Name", 40f, y, paint)
        canvas.drawText("Qty", 250f, y, paint)
        canvas.drawText("Price", 310f, y, paint)
        canvas.drawText("GST %", 380f, y, paint)
        canvas.drawText("GST ₹", 440f, y, paint)
        canvas.drawText("Total", 510f, y, paint)
        y += 10f
        canvas.drawLine(40f, y, 555f, y, paint)
        y += 20f

        // Table Items
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        cartItems.forEach { item ->
            canvas.drawText(item.name, 40f, y, paint)
            canvas.drawText(item.quantity.toString(), 250f, y, paint)
            canvas.drawText(String.format(Locale.getDefault(), "%.2f", item.price), 310f, y, paint)
            
            val gstPercent = if (isGstEnabled) (item.gstPercentage ?: 0.0) else 0.0
            val gstValue = if (isGstEnabled && item.gstPercentage != null) {
                (item.price * item.quantity * item.gstPercentage / 100.0)
            } else {
                0.0
            }
            val itemTotal = (item.price * item.quantity) + gstValue

            canvas.drawText(String.format(Locale.getDefault(), "%.1f%%", gstPercent), 380f, y, paint)
            canvas.drawText(String.format(Locale.getDefault(), "%.2f", gstValue), 440f, y, paint)
            canvas.drawText(String.format(Locale.getDefault(), "%.2f", itemTotal), 510f, y, paint)
            y += 20f
        }

        y += 10f
        canvas.drawLine(40f, y, 555f, y, paint)
        y += 25f

        // Summary
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Total Items: ${cartItems.sumOf { it.quantity }}", 40f, y, paint)
        paint.textSize = 14f
        canvas.drawText("Total Amount: ₹${String.format(Locale.getDefault(), "%.2f", totalAmount)}", 380f, y, paint)
        y += 40f

        // Footer
        paint.textSize = 12f
        paint.textAlign = Paint.Align.CENTER
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

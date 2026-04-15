package com.example.snaptag.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
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
        customerPhone: String? = null
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
        canvas.drawText("Qty", 300f, y, paint)
        canvas.drawText("Price", 380f, y, paint)
        canvas.drawText("Total", 480f, y, paint)
        y += 10f
        canvas.drawLine(40f, y, 555f, y, paint)
        y += 20f

        // Table Items
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        cartItems.forEach { item ->
            canvas.drawText(item.name, 40f, y, paint)
            canvas.drawText(item.quantity.toString(), 300f, y, paint)
            canvas.drawText(String.format(Locale.getDefault(), "%.2f", item.price), 380f, y, paint)
            canvas.drawText(String.format(Locale.getDefault(), "%.2f", item.price * item.quantity), 480f, y, paint)
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

        // 1. Save to app-specific directory for sharing (FileProvider)
        val cacheDir = File(context.getExternalFilesDir(null), "SnapTag")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        val cacheFile = File(cacheDir, fileName)

        try {
            pdfDocument.writeTo(FileOutputStream(cacheFile))
            
            // 2. Save to Public Downloads folder (Scoped Storage / MediaStore)
            saveToPublicStorage(context, cacheFile, fileName)

        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            pdfDocument.close()
        }

        return cacheFile
    }

    private fun saveToPublicStorage(context: Context, sourceFile: File, fileName: String) {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/SnapTag")
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

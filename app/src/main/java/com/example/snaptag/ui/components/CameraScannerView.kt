package com.example.snaptag.ui.components

import android.annotation.SuppressLint
import android.util.Log
import android.util.Size
import android.view.MotionEvent
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executors

@OptIn(androidx.annotation.OptIn::class)
@androidx.camera.core.ExperimentalGetImage
@Composable
fun CameraScannerView(
    onPriceConfirmed: (String) -> Unit,
    onBarcodeConfirmed: (String) -> Unit,
    onDismiss: () -> Unit,
    isBarcodeOnly: Boolean = false
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    
    val textRecognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    val barcodeScanner = remember { 
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_CODE_93,
                Barcode.FORMAT_CODABAR,
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_ITF,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_PDF417,
                Barcode.FORMAT_AZTEC,
                Barcode.FORMAT_DATA_MATRIX
            )
            .build()
        BarcodeScanning.getClient(options)
    }

    var stablePrices by remember { mutableStateOf<List<ScoredPrice>>(emptyList()) }
    var uiPrices by remember { mutableStateOf<List<ScoredPrice>>(emptyList()) }
    var isPaused by remember { mutableStateOf(false) }
    var frameCounter by remember { mutableIntStateOf(0) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var lastUpdateTime by remember { mutableLongStateOf(0L) }

    // UI update throttling (~500ms) with list pruning
    LaunchedEffect(stablePrices) {
        val now = System.currentTimeMillis()
        if (now - lastUpdateTime < 500) return@LaunchedEffect

        lastUpdateTime = now

        val currentSet = stablePrices.map { it.price }.toSet()
        // Pruning: Only keep prices that are still present in the stablePrices list
        val currentUiPrices = uiPrices.filter { it.price in currentSet }.toMutableList()
        var changed = uiPrices.size != currentUiPrices.size

        stablePrices.forEach { stable ->
            val existingIndex = currentUiPrices.indexOfFirst { it.price == stable.price }
            if (existingIndex != -1) {
                // Update score if it changed
                if (currentUiPrices[existingIndex].score != stable.score) {
                    currentUiPrices[existingIndex] = stable
                    changed = true
                }
            } else {
                // Add new price if not already in the list
                currentUiPrices.add(stable)
                changed = true
            }
        }

        if (changed) {
            uiPrices = currentUiPrices.sortedByDescending { it.score }
        }
    }

    // Clear stability when entering/exiting
    DisposableEffect(Unit) {
        PriceDetector.clearStability()
        onDispose {
            PriceDetector.clearStability()
        }
    }

    // Full-screen Box to consume clicks and prevent them from leaking to the screen behind
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { /* Consume clicks */ }
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Camera Preview Section
            Box(modifier = Modifier.weight(1.2f)) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }

                        // Tap-to-Focus Implementation
                        @SuppressLint("ClickableViewAccessibility")
                        previewView.setOnTouchListener { view, event ->
                            if (event.action == MotionEvent.ACTION_UP) {
                                view.performClick()
                                val factory = previewView.meteringPointFactory
                                val point = factory.createPoint(event.x, event.y)
                                val action = FocusMeteringAction.Builder(point).build()
                                camera?.cameraControl?.startFocusAndMetering(action)
                            }
                            true
                        }

                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setTargetResolution(Size(1280, 720))
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                            imageAnalysis.setAnalyzer(executor) { imageProxy ->
                                frameCounter++
                                if (isPaused || (!isBarcodeOnly && frameCounter % 3 != 0)) {
                                    imageProxy.close()
                                    return@setAnalyzer
                                }

                                val mediaImage = imageProxy.image
                                if (mediaImage != null) {
                                    val rotation = imageProxy.imageInfo.rotationDegrees
                                    val image = InputImage.fromMediaImage(mediaImage, rotation)
                                    
                                    // 1. Barcode Detection
                                    barcodeScanner.process(image)
                                        .addOnSuccessListener { barcodes ->
                                            if (barcodes.isNotEmpty()) {
                                                val barcodeValue = barcodes[0].rawValue ?: barcodes[0].displayValue
                                                if (barcodeValue != null) {
                                                    Log.d("CameraScanner", "Barcode detected: $barcodeValue")
                                                    onBarcodeConfirmed(barcodeValue)
                                                    isPaused = true
                                                }
                                            }
                                            
                                            // 2. Price OCR
                                            if (!isBarcodeOnly && !isPaused) {
                                                textRecognizer.process(image)
                                                    .addOnSuccessListener { visionText ->
                                                        val elements = visionText.textBlocks.flatMap { block ->
                                                            block.lines.flatMap { line ->
                                                                line.elements.map { element ->
                                                                    OCRPriceElement(
                                                                        text = element.text,
                                                                        boundingBox = element.boundingBox
                                                                    )
                                                                }
                                                            }
                                                        }

                                                        val currentPrices = PriceDetector.detectPrices(elements, visionText.text)
                                                        stablePrices = PriceDetector.updateStability(currentPrices)
                                                    }
                                                    .addOnFailureListener { e ->
                                                        Log.e("CameraScanner", "OCR Failed", e)
                                                    }
                                                    .addOnCompleteListener { imageProxy.close() }
                                            } else {
                                                imageProxy.close()
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("CameraScanner", "Barcode Detection Failed", e)
                                            imageProxy.close()
                                        }
                                } else {
                                    imageProxy.close()
                                }
                            }

                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                            try {
                                cameraProvider.unbindAll()
                                camera = cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageAnalysis
                                )
                            } catch (e: Exception) {
                                Log.e("CameraScannerView", "Binding failed", e)
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Center Guide Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                        .height(120.dp)
                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .border(2.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    )
                }

                // Close Button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            // Prices List Section
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Detected Prices",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (uiPrices.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (isBarcodeOnly) "Point camera at a barcode" else "Point camera at a price tag",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        val recommended = PriceDetector.getBestPrice(uiPrices)
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(uiPrices, key = { it.price }) { scoredPrice ->
                                val isRecommended = scoredPrice.price == recommended
                                PriceItem(
                                    price = scoredPrice,
                                    isRecommended = isRecommended,
                                    onClick = {
                                        isPaused = true
                                        PriceDetector.clearStability()
                                        onPriceConfirmed(scoredPrice.price)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PriceItem(price: ScoredPrice, isRecommended: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isRecommended) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "₹${price.price}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isRecommended) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isRecommended) {
                    Text(
                        text = "Best Match (Score: ${price.score})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                text = "Select",
                style = MaterialTheme.typography.labelLarge,
                color = if (isRecommended) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
        }
    }
}

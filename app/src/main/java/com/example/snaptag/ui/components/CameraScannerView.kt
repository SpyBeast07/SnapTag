package com.example.snaptag.ui.components

import android.util.Log
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executors

@OptIn(ExperimentalGetImage::class)
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
        // Exclude QR code as requested
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

    var scannedResult by remember { mutableStateOf<String?>(null) }
    var resultType by remember { mutableStateOf<ScannerResultType?>(null) }
    var isPaused by remember { mutableStateOf(false) }

    // Stability detection for OCR
    var lastDetectedOcr by remember { mutableStateOf("") }
    var stableCountOcr by remember { mutableStateOf(0) }
    var currentLiveOcr by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
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
                        if (isPaused) {
                            imageProxy.close()
                            return@setAnalyzer
                        }

                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                            
                            // 1. Try Barcode first
                            barcodeScanner.process(image)
                                .addOnSuccessListener { barcodes ->
                                    if (barcodes.isNotEmpty()) {
                                        val barcodeValue = barcodes[0].rawValue ?: barcodes[0].displayValue
                                        if (barcodeValue != null) {
                                            scannedResult = barcodeValue
                                            resultType = ScannerResultType.BARCODE
                                            isPaused = true
                                            imageProxy.close()
                                            return@addOnSuccessListener
                                        }
                                    }
                                    
                                    // 2. If no barcode, try OCR (unless barcode only)
                                    if (!isBarcodeOnly) {
                                        textRecognizer.process(image)
                                            .addOnSuccessListener { visionText ->
                                                val priceElements = visionText.textBlocks.flatMap { block ->
                                                    block.lines.flatMap { line ->
                                                        line.elements.map { element ->
                                                            OCRPriceElement(
                                                                text = element.text,
                                                                boundingBox = element.boundingBox
                                                            )
                                                        }
                                                    }
                                                }

                                                val detected = PriceDetector.detectPrice(priceElements)
                                                if (detected != null) {
                                                    currentLiveOcr = detected
                                                    if (detected == lastDetectedOcr) {
                                                        stableCountOcr++
                                                    } else {
                                                        stableCountOcr = 1
                                                        lastDetectedOcr = detected
                                                    }

                                                    if (stableCountOcr >= 3) {
                                                        scannedResult = detected
                                                        resultType = ScannerResultType.PRICE
                                                        isPaused = true
                                                    }
                                                }
                                            }
                                            .addOnCompleteListener { imageProxy.close() }
                                    } else {
                                        imageProxy.close()
                                    }
                                }
                                .addOnFailureListener {
                                    imageProxy.close()
                                }
                        } else {
                            imageProxy.close()
                        }
                    }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        Log.e("CameraScannerView", "Use case binding failed", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Dim background when paused
        if (isPaused) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))
        }

        // Overlay UI (Bottom)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isPaused && scannedResult != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(8.dp),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            if (resultType == ScannerResultType.PRICE) "Price Detected" else "Barcode Scanned",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = if (resultType == ScannerResultType.PRICE) "₹$scannedResult" else scannedResult!!,
                            style = if (resultType == ScannerResultType.PRICE) MaterialTheme.typography.displayMedium else MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                            FilledIconButton(
                                onClick = {
                                    isPaused = false
                                    scannedResult = null
                                    resultType = null
                                    stableCountOcr = 0
                                    lastDetectedOcr = ""
                                },
                                modifier = Modifier.size(56.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Retry", modifier = Modifier.size(32.dp))
                            }
                            FilledIconButton(
                                onClick = {
                                    if (resultType == ScannerResultType.PRICE) {
                                        onPriceConfirmed(scannedResult!!)
                                    } else {
                                        onBarcodeConfirmed(scannedResult!!)
                                    }
                                },
                                modifier = Modifier.size(56.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(Icons.Default.Check, contentDescription = "Confirm", modifier = Modifier.size(32.dp))
                            }
                        }
                    }
                }
            } else {
                Surface(
                    onClick = {
                        if (currentLiveOcr.isNotEmpty()) {
                            scannedResult = currentLiveOcr
                            resultType = ScannerResultType.PRICE
                            isPaused = true
                        }
                    },
                    enabled = currentLiveOcr.isNotEmpty(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                    shape = MaterialTheme.shapes.extraLarge,
                    tonalElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (currentLiveOcr.isNotEmpty()) {
                            Text(
                                text = "Detecting Price: ₹$currentLiveOcr",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Tap to capture now",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        } else {
                            Text(
                                text = if (isBarcodeOnly) "Scanning for Barcodes..." else "Scanning for Barcodes or Prices...",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black.copy(alpha = 0.5f),
                    contentColor = Color.White
                )
            ) {
                Text("Cancel")
            }
        }
    }
}

enum class ScannerResultType {
    PRICE, BARCODE
}

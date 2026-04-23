package com.example.snaptag.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.snaptag.data.Product

@Composable
fun ProductDialog(
    product: Product? = null,
    initialName: String = "",
    initialPrice: String = "",
    initialBarcode: String = "",
    existingProducts: List<Product> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (name: String, price: Double, stock: Int, barcode: String?, gst: Double, discount: Double) -> Unit,
    onUpdateExisting: ((Product, Int) -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onScanRequest: () -> Unit = {}
) {
    var name by remember { mutableStateOf(product?.name ?: initialName) }
    var price by remember { mutableStateOf(product?.price?.toString() ?: initialPrice) }
    var stock by remember { mutableStateOf(product?.stock?.toString() ?: "1") }
    var barcode by remember { mutableStateOf(product?.barcode ?: initialBarcode) }
    var gst by remember { mutableStateOf(product?.gstPercent?.toString() ?: "") }
    var discount by remember { mutableStateOf(product?.discountPercent?.toString() ?: "") }

    var nameError by remember { mutableStateOf<String?>(null) }
    var priceError by remember { mutableStateOf<String?>(null) }
    var stockError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(initialBarcode) {
        if (initialBarcode.isNotEmpty()) {
            barcode = initialBarcode
        }
    }

    LaunchedEffect(product) {
        if (product != null && initialBarcode.isEmpty()) {
            barcode = product.barcode ?: ""
            gst = product.gstPercent.toString()
            discount = product.discountPercent.toString()
        }
    }

    var showDuplicateDialog by remember { mutableStateOf<Product?>(null) }

    if (showDuplicateDialog != null) {
        val existing = showDuplicateDialog!!
        AlertDialog(
            onDismissRequest = { showDuplicateDialog = null },
            title = { Text("Product Already Exists") },
            text = { Text("A product with name '${existing.name}' or barcode '${existing.barcode}' already exists. Would you like to add ${stock.toIntOrNull() ?: 0} units to the existing stock?") },
            confirmButton = {
                Button(
                    onClick = {
                        val s = stock.toIntOrNull() ?: 0
                        onUpdateExisting?.invoke(existing, s)
                        showDuplicateDialog = null
                        onDismiss()
                    }
                ) {
                    Text("Add to Existing")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDuplicateDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (product == null) "Add New Product" else "Edit Product") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(
                    value = name,
                    onValueChange = { 
                        name = it
                        if (it.isNotBlank()) nameError = null
                    },
                    label = { Text("Product Name") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    singleLine = true,
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it) } }
                )
                TextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = { Text("Barcode") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = onScanRequest) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Barcode")
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    singleLine = true
                )
                TextField(
                    value = price,
                    onValueChange = { 
                        price = it
                        if (it.toDoubleOrNull() != null && it.toDouble() >= 0) priceError = null
                    },
                    label = { Text("Price (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = priceError != null,
                    supportingText = priceError?.let { { Text(it) } }
                )
                TextField(
                    value = gst,
                    onValueChange = { gst = it },
                    label = { Text("GST (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                TextField(
                    value = discount,
                    onValueChange = { discount = it },
                    label = { Text("Discount (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                TextField(
                    value = stock,
                    onValueChange = { 
                        stock = it
                        if (it.toIntOrNull() != null && it.toInt() >= 0) stockError = null
                    },
                    label = { Text("Stock") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = stockError != null,
                    supportingText = stockError?.let { { Text(it) } }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = price.toDoubleOrNull()
                    val s = stock.toIntOrNull()
                    val g = gst.toDoubleOrNull() ?: 0.0
                    val d = discount.toDoubleOrNull() ?: 0.0
                    
                    var isValid = true
                    if (name.isBlank()) {
                        nameError = "Name is required"
                        isValid = false
                    }
                    if (p == null || p < 0) {
                        priceError = "Invalid price"
                        isValid = false
                    }
                    if (s == null || s < 0) {
                        stockError = "Invalid stock"
                        isValid = false
                    }

                    if (isValid) {
                        val duplicate = if (product == null) {
                            existingProducts.find { 
                                it.name.trim().equals(name.trim(), ignoreCase = true) || 
                                (it.barcode != null && barcode.isNotBlank() && it.barcode == barcode.trim())
                            }
                        } else null
                        
                        if (duplicate != null) {
                            showDuplicateDialog = duplicate
                        } else {
                            onSave(name, p!!, s!!, if (barcode.isBlank()) null else barcode.trim(), g, d)
                        }
                    }
                }
            ) {
                Text(if (product == null) "Save Product" else "Update Product")
            }
        },
        dismissButton = {
            Row {
                if (product != null && onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

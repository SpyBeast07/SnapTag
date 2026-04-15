package com.example.snaptag.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
    onSave: (name: String, price: Double, stock: Int, barcode: String?) -> Unit,
    onUpdateExisting: ((Product, Int) -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(product?.name ?: initialName) }
    var price by remember { mutableStateOf(product?.price?.toString() ?: initialPrice) }
    var stock by remember { mutableStateOf(product?.stock?.toString() ?: "1") }
    var barcode by remember { mutableStateOf(product?.barcode ?: initialBarcode) }
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
                    onValueChange = { name = it },
                    label = { Text("Product Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                TextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = { Text("Barcode") },
                    modifier = Modifier.fillMaxWidth()
                )
                TextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Price (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                TextField(
                    value = stock,
                    onValueChange = { stock = it },
                    label = { Text("Stock") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = price.toDoubleOrNull() ?: 0.0
                    val s = stock.toIntOrNull() ?: 0
                    if (name.isNotBlank()) {
                        val duplicate = if (product == null) {
                            existingProducts.find { 
                                it.name.trim().equals(name.trim(), ignoreCase = true) || 
                                (it.barcode != null && barcode.isNotBlank() && it.barcode == barcode.trim())
                            }
                        } else null
                        
                        if (duplicate != null) {
                            showDuplicateDialog = duplicate
                        } else {
                            onSave(name, p, s, if (barcode.isBlank()) null else barcode.trim())
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

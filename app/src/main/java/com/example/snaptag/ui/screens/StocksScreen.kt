package com.example.snaptag.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelStoreOwner
import com.example.snaptag.data.Product
import com.example.snaptag.ui.components.*
import com.example.snaptag.viewmodel.ProductViewModel
import com.example.snaptag.viewmodel.ProductViewModelFactory
import java.util.Locale

@Composable
fun StocksScreen(
    viewModelStoreOwner: ViewModelStoreOwner,
    viewModelFactory: ProductViewModelFactory
) {
    val viewModel: ProductViewModel = viewModel(
        viewModelStoreOwner = viewModelStoreOwner,
        factory = viewModelFactory
    )
    val products by viewModel.products.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    
    var showScanner by remember { mutableStateOf(false) }
    var isSearchScanning by remember { mutableStateOf(false) }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var isAddingProduct by remember { mutableStateOf(false) }
    var scannedPrice by remember { mutableStateOf("") }
    var scannedName by remember { mutableStateOf("") }
    var scannedBarcode by remember { mutableStateOf("") }
    
    var showConfirmIncrementDialog by remember { mutableStateOf<Product?>(null) }

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showScanner = true
        }
    }

    val filteredProducts = products.filter {
        it.name.contains(searchQuery, ignoreCase = true) || 
        (it.barcode?.contains(searchQuery, ignoreCase = true) ?: false)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = { TopBar("SnapTag - Stocks") },
            floatingActionButton = {
                FloatingActionButton(onClick = {
                    val permissionCheckResult = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    if (permissionCheckResult == PackageManager.PERMISSION_GRANTED) {
                        showScanner = true
                    } else {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Product")
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
            ) {
                SearchBar(
                    modifier = Modifier.padding(top = 8.dp),
                    query = searchQuery,
                    onQueryChange = { viewModel.updateSearchQuery(it) },
                    onScanClick = {
                        val permissionCheckResult = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                        if (permissionCheckResult == PackageManager.PERMISSION_GRANTED) {
                            isSearchScanning = true
                            showScanner = true
                        } else {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    placeholder = "Search by name or barcode"
                )

                Text(
                    text = "Total Items: ${filteredProducts.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredProducts) { product ->
                        ProductItem(
                            product = product,
                            onClick = { selectedProduct = product },
                            onIncrement = {
                                viewModel.updateProduct(product.copy(stock = product.stock + 1))
                            },
                            onDecrement = {
                                if (product.stock > 0) {
                                    viewModel.updateProduct(product.copy(stock = product.stock - 1))
                                }
                            }
                        )
                    }
                }
            }
        }

        if (showScanner) {
            CameraScannerView(
                isBarcodeOnly = isSearchScanning,
                onPriceConfirmed = { price ->
                    scannedPrice = price
                    scannedName = ""
                    scannedBarcode = ""
                    showScanner = false
                    isSearchScanning = false
                    isAddingProduct = true
                },
                onBarcodeConfirmed = { barcode ->
                    showScanner = false
                    if (isSearchScanning) {
                        viewModel.updateSearchQuery(barcode)
                        isSearchScanning = false
                    } else {
                        val existing = products.find { it.barcode == barcode || it.name.equals(barcode, ignoreCase = true) }
                        if (existing != null) {
                            // Ask to increment instead of auto-incrementing
                            showConfirmIncrementDialog = existing
                        } else {
                            // Also prepare for adding a new product
                            scannedName = ""
                            scannedPrice = ""
                            scannedBarcode = barcode
                            isAddingProduct = true
                        }
                    }
                },
                onDismiss = { 
                    showScanner = false
                    isSearchScanning = false
                }
            )
        }
    }

    // Confirm Increment Dialog
    if (showConfirmIncrementDialog != null) {
        val existing = showConfirmIncrementDialog!!
        AlertDialog(
            onDismissRequest = { showConfirmIncrementDialog = null },
            title = { Text("Product Found") },
            text = { Text("Found '${existing.name}'. Would you like to increment its stock by 1?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateProduct(existing.copy(stock = existing.stock + 1))
                        showConfirmIncrementDialog = null
                        Toast.makeText(context, "Stock updated", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Increment Stock")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmIncrementDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Product Dialog
    if (isAddingProduct) {
        ProductDialog(
            initialName = scannedName,
            initialPrice = scannedPrice,
            initialBarcode = scannedBarcode,
            existingProducts = products,
            onDismiss = { isAddingProduct = false },
            onSave = { name, price, stock, barcode ->
                viewModel.addProduct(name, price, stock, barcode)
                isAddingProduct = false
            },
            onUpdateExisting = { existing, addedStock ->
                viewModel.updateProduct(existing.copy(stock = existing.stock + addedStock))
                isAddingProduct = false
            }
        )
    }

    // Edit Product Dialog
    selectedProduct?.let { product ->
        ProductDialog(
            product = product,
            existingProducts = products,
            onDismiss = { selectedProduct = null },
            onSave = { name, price, stock, barcode ->
                viewModel.updateProduct(product.copy(name = name, price = price, stock = stock, barcode = barcode))
                selectedProduct = null
            },
            onUpdateExisting = { existing, addedStock ->
                viewModel.updateProduct(existing.copy(stock = existing.stock + addedStock))
                selectedProduct = null
            },
            onDelete = {
                viewModel.deleteProduct(product.id)
                selectedProduct = null
            }
        )
    }
}

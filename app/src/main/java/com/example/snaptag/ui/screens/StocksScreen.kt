package com.example.snaptag.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.snaptag.data.Product
import com.example.snaptag.ui.components.*
import com.example.snaptag.viewmodel.ProductViewModel
import com.example.snaptag.viewmodel.ProductViewModelFactory

@Composable
fun StocksScreen(viewModelFactory: ProductViewModelFactory) {
    val viewModel: ProductViewModel = viewModel(factory = viewModelFactory)
    val products by viewModel.products.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    
    var showScanner by remember { mutableStateOf(false) }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var isAddingProduct by remember { mutableStateOf(false) }
    var scannedPrice by remember { mutableStateOf("") }
    
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showScanner = true
        }
    }

    val filteredProducts = products.filter {
        it.name.contains(searchQuery, ignoreCase = true)
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
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    placeholder = { Text("Search products") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredProducts) { product ->
                        ProductItem(
                            product = product,
                            onClick = { selectedProduct = product }
                        )
                    }
                }
            }
        }

        if (showScanner) {
            CameraScannerView(
                onPriceConfirmed = { price ->
                    scannedPrice = price
                    showScanner = false
                    isAddingProduct = true
                },
                onDismiss = { showScanner = false }
            )
        }
    }

    // Add Product Dialog
    if (isAddingProduct) {
        ProductDialog(
            initialPrice = scannedPrice,
            onDismiss = { isAddingProduct = false },
            onSave = { name, price, stock ->
                viewModel.addProduct(name, price, stock)
                isAddingProduct = false
            }
        )
    }

    // Edit Product Dialog
    selectedProduct?.let { product ->
        ProductDialog(
            product = product,
            onDismiss = { selectedProduct = null },
            onSave = { name, price, stock ->
                viewModel.updateProduct(product.copy(name = name, price = price, stock = stock))
                selectedProduct = null
            },
            onDelete = {
                viewModel.deleteProduct(product.id)
                selectedProduct = null
            }
        )
    }
}

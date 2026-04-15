package com.example.snaptag.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import com.example.snaptag.viewmodel.ProductViewModel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.snaptag.ui.components.CameraScannerView
import com.example.snaptag.ui.components.TopBar
import com.example.snaptag.ui.components.SearchBar
import com.example.snaptag.viewmodel.BillingViewModel
import com.example.snaptag.viewmodel.CartItem
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingScreen(viewModel: BillingViewModel, productViewModel: ProductViewModel) {
    val cartItems by viewModel.cartItems.collectAsState()
    val products by viewModel.products.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val totalItems by viewModel.totalItems.collectAsState()
    val totalAmount by viewModel.totalAmount.collectAsState()
    val paymentQrUri by productViewModel.paymentQrUri.collectAsState()

    var showScanner by remember { mutableStateOf(false) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) showScanner = true
    }

    val filteredProducts = if (searchQuery.isBlank()) products else products.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
        (it.barcode?.contains(searchQuery, ignoreCase = true) ?: false)
    }

    Scaffold(
        topBar = {
            TopBar(
                title = "SnapTag - Billing"
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                SearchBar(
                    modifier = Modifier.padding(top = 8.dp),
                    query = searchQuery,
                    onQueryChange = { viewModel.updateSearchQuery(it) },
                    onScanClick = {
                        val permissionCheckResult = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                        if (permissionCheckResult == PackageManager.PERMISSION_GRANTED) {
                            showScanner = true
                        } else {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    placeholder = "Search product or scan"
                )

                if (searchQuery.isNotEmpty()) {
                    Popup(
                        alignment = Alignment.TopStart,
                        offset = androidx.compose.ui.unit.IntOffset(0, 200),
                        properties = PopupProperties(focusable = false)
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .heightIn(max = 300.dp)
                                .padding(horizontal = 8.dp),
                            elevation = CardDefaults.cardElevation(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            if (filteredProducts.isEmpty()) {
                                Text(
                                    "No products found",
                                    modifier = Modifier.padding(16.dp),
                                    color = Color.Gray
                                )
                            } else {
                                LazyColumn {
                                    items(filteredProducts) { product ->
                                        val cartItem = cartItems.find { it.productId == product.id }
                                        ListItem(
                                            headlineContent = { Text(product.name) },
                                            supportingContent = { Text("₹${product.price} | Stock: ${product.stock}") },
                                            trailingContent = {
                                                IconButton(onClick = {
                                                    if ((cartItem?.quantity ?: 0) < product.stock) {
                                                        viewModel.addToCart(product)
                                                        viewModel.updateSearchQuery("")
                                                    } else {
                                                        Toast.makeText(context, "Out of Stock", Toast.LENGTH_SHORT).show()
                                                    }
                                                }) {
                                                    Icon(Icons.Default.Add, contentDescription = "Add")
                                                }
                                            },
                                            modifier = Modifier.clickable {
                                                if ((cartItem?.quantity ?: 0) < product.stock) {
                                                    viewModel.addToCart(product)
                                                    viewModel.updateSearchQuery("")
                                                } else {
                                                    Toast.makeText(context, "Out of Stock", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        )
                                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Cart Items", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (cartItems.isEmpty()) {
                    item {
                        Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Cart is empty", color = Color.Gray)
                        }
                    }
                } else {
                    items(cartItems) { item ->
                        val product = products.find { it.id == item.productId }
                        val context = LocalContext.current
                        CartItemRow(
                            item = item,
                            onIncrement = {
                                if (product != null && item.quantity < product.stock) {
                                    viewModel.incrementQuantity(item.productId)
                                } else {
                                    Toast.makeText(context, "Out of Stock", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onDecrement = { viewModel.decrementQuantity(item.productId) },
                            isAtMaxStock = product != null && item.quantity >= product.stock
                        )
                    }
                }
            }

            // Billing Summary
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("Total Items:", style = MaterialTheme.typography.titleMedium)
                        Text("$totalItems", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("Subtotal:", style = MaterialTheme.typography.titleMedium)
                        Text("₹${String.format(Locale.getDefault(), "%.2f", totalAmount)}",
                            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Button(
                onClick = { showBottomSheet = true },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Explore Products List")
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.clearCart() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    enabled = cartItems.isNotEmpty()
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Clear Cart")
                }

                Button(
                    onClick = { if (cartItems.isNotEmpty()) showPaymentDialog = true },
                    modifier = Modifier.weight(1f),
                    enabled = cartItems.isNotEmpty()
                ) {
                    Text("Generate Bill")
                }
            }
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Explore Products", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { showBottomSheet = false }) {
                        Text("Done", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(filteredProducts) { product ->
                        val cartItem = cartItems.find { it.productId == product.id }
                        ProductExploreItem(
                            product = product,
                            cartQuantity = cartItem?.quantity ?: 0,
                            onIncrement = {
                                if ((cartItem?.quantity ?: 0) < product.stock) {
                                    viewModel.addToCart(product)
                                } else {
                                    Toast.makeText(context, "Out of Stock", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onDecrement = {
                                if (cartItem != null) {
                                    viewModel.decrementQuantity(product.id)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showScanner) {
        CameraScannerView(
            isBarcodeOnly = true,
            onBarcodeConfirmed = { barcode ->
                val product = products.find { it.barcode == barcode }
                if (product != null) {
                    viewModel.addToCart(product)
                    Toast.makeText(context, "Added: ${product.name}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Product not found", Toast.LENGTH_SHORT).show()
                }
                showScanner = false
            },
            onDismiss = { showScanner = false },
            onPriceConfirmed = {}
        )
    }

    if (showPaymentDialog) {
        AlertDialog(
            onDismissRequest = { showPaymentDialog = false },
            title = { Text("Payment", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (paymentQrUri != null) {
                            AsyncImage(
                                model = paymentQrUri,
                                contentDescription = "Payment QR",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(48.dp))
                                Text("No QR Code Set", style = MaterialTheme.typography.bodySmall)
                                Text("(Go to Settings to add one)", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("Total Amount", style = MaterialTheme.typography.bodyMedium)
                    Text("₹${String.format(Locale.getDefault(), "%.2f", totalAmount)}", 
                        style = MaterialTheme.typography.headlineMedium, 
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.generateBill {
                            Toast.makeText(context, "Bill Paid Successfully", Toast.LENGTH_LONG).show()
                            showPaymentDialog = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Mark as Paid")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPaymentDialog = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel / Unpaid")
                }
            }
        )
    }
}

@Composable
fun ProductExploreItem(
    product: com.example.snaptag.data.Product,
    cartQuantity: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (cartQuantity > 0) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, fontWeight = FontWeight.SemiBold)
                Text("₹${product.price} | Stock: ${product.stock}", style = MaterialTheme.typography.bodySmall)
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (cartQuantity > 0) {
                    IconButton(onClick = onDecrement, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Remove, contentDescription = null)
                    }
                    Text("$cartQuantity", modifier = Modifier.padding(horizontal = 8.dp), fontWeight = FontWeight.Bold)
                }
                IconButton(
                    onClick = onIncrement,
                    modifier = Modifier.size(32.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = if (cartQuantity >= product.stock) Color.Gray else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                }
            }
        }
    }
}

@Composable
fun CartItemRow(
    item: CartItem,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    isAtMaxStock: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, fontWeight = FontWeight.SemiBold)
                Text("₹${item.price} x ${item.quantity}", style = MaterialTheme.typography.bodySmall)
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("₹${String.format(Locale.getDefault(), "%.2f", item.price * item.quantity)}", 
                    fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp))
                
                IconButton(onClick = onDecrement, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Remove, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                }
                Text("${item.quantity}", modifier = Modifier.padding(horizontal = 4.dp), fontWeight = FontWeight.Bold)
                IconButton(
                    onClick = onIncrement,
                    modifier = Modifier.size(32.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = if (isAtMaxStock) Color.Gray else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                }
            }
        }
    }
}

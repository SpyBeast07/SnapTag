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
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.snaptag.utils.PdfGenerator
import java.io.File
import com.example.snaptag.viewmodel.ProductViewModel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.snaptag.utils.HapticManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.snaptag.ui.components.CameraScannerView
import com.example.snaptag.ui.components.TopBar
import com.example.snaptag.ui.components.SearchBar
import com.example.snaptag.viewmodel.BillingViewModel
import com.example.snaptag.viewmodel.CartItem
import com.example.snaptag.utils.BillSummary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingScreen(
    viewModel: BillingViewModel,
    productViewModel: ProductViewModel
) {
    val cartItems by viewModel.cartItems.collectAsState()
    val products by viewModel.products.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val totalItems by viewModel.totalItems.collectAsState()
    val billSummary by viewModel.billSummary.collectAsState()
    val isGstEnabled by viewModel.isGstEnabled.collectAsState()
    val billDiscountPercent by viewModel.billDiscountPercent.collectAsState()
    
    val paymentQrUri by productViewModel.paymentQrUri.collectAsState()
    val shopName by productViewModel.shopName.collectAsState()
    val shopAddress by productViewModel.shopAddress.collectAsState()
    val shopPhone by productViewModel.shopPhone.collectAsState()
    val shopEmail by productViewModel.shopEmail.collectAsState()
    val shopGst by productViewModel.shopGst.collectAsState()
    val footerNote by productViewModel.footerNote.collectAsState()

    var showScanner by remember { mutableStateOf(false) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var customerPhone by remember { mutableStateOf("") }
    var billDiscountInput by remember { mutableStateOf("") }
    
    var lastPaidSummary by remember { mutableStateOf<BillSummary?>(null) }
    var lastPaidGstEnabled by remember { mutableStateOf(true) }

    val context = LocalContext.current
    
    LaunchedEffect(billDiscountInput) {
        viewModel.updateBillDiscount(billDiscountInput.toDoubleOrNull() ?: 0.0)
    }
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
                        HapticManager.light(context)
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
                                                        HapticManager.medium(context)
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
                                                    HapticManager.medium(context)
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
                if (billSummary.items.isEmpty()) {
                    item {
                        Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Cart is empty", color = Color.Gray)
                        }
                    }
                } else {
                    items(billSummary.items) { item ->
                        val product = products.find { it.id == item.cartItem.productId }
                        val context = LocalContext.current
                        CartItemRow(
                            calculatedItem = item,
                            onIncrement = {
                                if (product != null && item.cartItem.quantity < product.stock) {
                                    viewModel.incrementQuantity(item.cartItem.productId)
                                } else {
                                    Toast.makeText(context, "Out of Stock", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onDecrement = { viewModel.decrementQuantity(item.cartItem.productId) },
                            isAtMaxStock = product != null && item.cartItem.quantity >= product.stock
                        )
                    }
                }
            }

            // Billing Summary
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Text("GST", style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.width(8.dp))
                            Switch(
                                checked = isGstEnabled,
                                onCheckedChange = { viewModel.toggleGst(it) },
                                modifier = Modifier.scale(0.8f)
                            )
                        }

                        OutlinedTextField(
                            value = billDiscountInput,
                            onValueChange = { billDiscountInput = it },
                            label = { Text("Disc %", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f).height(50.dp),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                            ),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp)

                    SummaryRow("Subtotal:", billSummary.subtotal, style = MaterialTheme.typography.bodySmall)
                    if (billSummary.totalItemDiscounts > 0 || billSummary.billDiscountAmount > 0) {
                        val totalD = billSummary.totalItemDiscounts + billSummary.billDiscountAmount
                        SummaryRow("Total Disc:", -totalD, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Grand Total:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("₹${String.format(Locale.getDefault(), "%.2f", billSummary.grandTotal)}",
                            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Button(
                onClick = { 
                    HapticManager.light(context)
                    showBottomSheet = true 
                },
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
                    onClick = { 
                        HapticManager.strong(context)
                        viewModel.clearCart() 
                        billDiscountInput = ""
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    enabled = cartItems.isNotEmpty()
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Clear Cart")
                }

                Button(
                    onClick = { 
                        if (cartItems.isNotEmpty()) {
                            HapticManager.light(context)
                            showPaymentDialog = true 
                        }
                    },
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
        Box(modifier = Modifier.fillMaxSize()) {
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
                    Text("₹${String.format(Locale.getDefault(), "%.2f", billSummary.grandTotal)}", 
                        style = MaterialTheme.typography.headlineMedium, 
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = customerPhone,
                        onValueChange = { customerPhone = it },
                        label = { Text("Customer Phone (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone
                        ),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (cartItems.isNotEmpty()) {
                            HapticManager.strong(context)
                            val summaryToSave = billSummary
                            val gstEnabledToSave = isGstEnabled
                            viewModel.generateBill(customerPhone.ifBlank { null }) { sale ->
                                lastPaidSummary = summaryToSave
                                lastPaidGstEnabled = gstEnabledToSave
                                billDiscountInput = ""
                                Toast.makeText(context, "Bill Paid Successfully", Toast.LENGTH_LONG).show()
                                showPaymentDialog = false
                                showShareDialog = true
                            }
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

    if (showShareDialog) {
        AlertDialog(
            onDismissRequest = { 
                showShareDialog = false
                customerPhone = ""
            },
            title = { Text("Share Bill") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Customer: ${customerPhone.ifBlank { "No number entered" }}")
                    Text("Bill is ready to be shared or saved.")
                }
            },
            confirmButton = {
                Button(onClick = {
                    HapticManager.medium(context)
                    lastPaidSummary?.let { summary ->
                        val finalFile = PdfGenerator.generateBillPdf(
                            context = context,
                            shopName = shopName,
                            address = shopAddress,
                            phone = shopPhone,
                            email = shopEmail,
                            gst = shopGst,
                            footerNote = footerNote,
                            billSummary = summary,
                            customerPhone = customerPhone.ifBlank { null },
                            isGstEnabled = lastPaidGstEnabled
                        )

                        finalFile?.let { file ->
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )

                            val digits = customerPhone.filter { it.isDigit() }
                            val cleanPhone = when {
                                digits.length == 10 -> "91$digits"
                                digits.length == 11 && digits.startsWith("0") -> "91${digits.substring(1)}"
                                digits.length >= 12 -> digits
                                else -> digits
                            }

                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                if (cleanPhone.isNotEmpty()) {
                                    putExtra("jid", "${cleanPhone}@s.whatsapp.net")
                                }
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            
                            intent.`package` = "com.whatsapp"
                            
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                intent.`package` = null
                                context.startActivity(Intent.createChooser(intent, "Share Bill"))
                            }
                        }
                    }
                    customerPhone = ""
                    showShareDialog = false
                }) {
                    Text("Send via WhatsApp")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    HapticManager.light(context)
                    lastPaidSummary?.let { summary ->
                        PdfGenerator.generateBillPdf(
                            context = context,
                            shopName = shopName,
                            address = shopAddress,
                            phone = shopPhone,
                            email = shopEmail,
                            gst = shopGst,
                            footerNote = footerNote,
                            billSummary = summary,
                            customerPhone = customerPhone.ifBlank { null },
                            isGstEnabled = lastPaidGstEnabled
                        )
                    }
                    customerPhone = ""
                    showShareDialog = false 
                }) {
                    Text("Save & Close")
                }
            }
        )
    }
}

@Composable
fun SummaryRow(label: String, amount: Double, color: Color = Color.Unspecified, style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
        Text(label, style = style)
        Text("${if (amount < 0) "-" else ""}₹${String.format(Locale.getDefault(), "%.2f", Math.abs(amount))}", 
            style = style, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun CartItemRow(
    calculatedItem: com.example.snaptag.utils.CalculatedItem,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    isAtMaxStock: Boolean
) {
    val context = LocalContext.current
    val item = calculatedItem.cartItem
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.name, fontWeight = FontWeight.SemiBold)
                    Text("₹${item.price} x ${item.quantity}", style = MaterialTheme.typography.bodySmall)
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("₹${String.format(Locale.getDefault(), "%.2f", calculatedItem.finalItemTotal)}", 
                        fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp), color = MaterialTheme.colorScheme.primary)
                    
                    IconButton(onClick = {
                        HapticManager.light(context)
                        onDecrement()
                    }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Remove, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    }
                    Text("${item.quantity}", modifier = Modifier.padding(horizontal = 4.dp), fontWeight = FontWeight.Bold)
                    IconButton(
                        onClick = {
                            if (!isAtMaxStock) {
                                HapticManager.light(context)
                            }
                            onIncrement()
                        },
                        modifier = Modifier.size(32.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = if (isAtMaxStock) Color.Gray else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                    }
                }
            }
            
            // Per-item breakdown
            if (calculatedItem.itemDiscountAmount > 0 || calculatedItem.billDiscountShare > 0 || calculatedItem.gstAmount > 0) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        if (calculatedItem.itemDiscountAmount > 0) {
                            Text("Item Disc: -₹${calculatedItem.itemDiscountAmount} (${item.discountPercent}%)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                        if (calculatedItem.billDiscountShare > 0) {
                            Text("Bill Disc Share: -₹${calculatedItem.billDiscountShare}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Taxable: ₹${calculatedItem.finalTaxableValue}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        if (calculatedItem.gstAmount > 0) {
                            Text("GST: +₹${calculatedItem.gstAmount} (${item.gstPercent}%)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductExploreItem(
    product: com.example.snaptag.data.Product,
    cartQuantity: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    val context = LocalContext.current
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
                Text("₹${product.price} | GST: ${product.gstPercent}% | Disc: ${product.discountPercent}%", style = MaterialTheme.typography.bodySmall)
                Text("Stock: ${product.stock}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (cartQuantity > 0) {
                    IconButton(onClick = {
                        HapticManager.light(context)
                        onDecrement()
                    }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Remove, contentDescription = null)
                    }
                    Text("$cartQuantity", modifier = Modifier.padding(horizontal = 8.dp), fontWeight = FontWeight.Bold)
                }
                IconButton(
                    onClick = {
                        if (cartQuantity < product.stock) {
                            HapticManager.light(context)
                        }
                        onIncrement()
                    },
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

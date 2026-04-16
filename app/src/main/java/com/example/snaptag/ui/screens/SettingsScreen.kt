package com.example.snaptag.ui.screens

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants
import com.example.snaptag.data.Product
import com.example.snaptag.ui.components.TopBar
import com.example.snaptag.viewmodel.ProductViewModel
import com.example.snaptag.viewmodel.ProductViewModelFactory
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun SettingsScreen(
    viewModelStoreOwner: ViewModelStoreOwner,
    viewModelFactory: ProductViewModelFactory,
    onNavigateToAbout: () -> Unit
) {
    val viewModel: ProductViewModel = viewModel(
        viewModelStoreOwner = viewModelStoreOwner,
        factory = viewModelFactory
    )
    val products by viewModel.products.collectAsState()
    val paymentQrUri by viewModel.paymentQrUri.collectAsState()
    val shopName by viewModel.shopName.collectAsState()
    val shopAddress by viewModel.shopAddress.collectAsState()
    val shopPhone by viewModel.shopPhone.collectAsState()
    val shopEmail by viewModel.shopEmail.collectAsState()
    val shopGst by viewModel.shopGst.collectAsState()
    val footerNote by viewModel.footerNote.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()

    val view = LocalView.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showClearDialog by remember { mutableStateOf(false) }
    var showReceiptDialog by remember { mutableStateOf(false) }
    var showAppearanceDialog by remember { mutableStateOf(false) }

    val qrPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let {
                // Persist permission if needed or copy to internal storage
                // For simplicity, we just save the URI string
                context.contentResolver.takePersistableUriPermission(it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                viewModel.savePaymentQrUri(it.toString())
            }
        }
    )

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
        onResult = { uri ->
            uri?.let {
                exportDataToUri(context, it, products, scope)
            }
        }
    )

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                importData(context, it, viewModel, scope)
            }
        }
    )

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Reset All Data") },
            text = { Text("Are you sure you want to delete all products? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        viewModel.clearAllData()
                        showClearDialog = false
                        Toast.makeText(context, "All data cleared", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    showClearDialog = false 
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAppearanceDialog) {
        AlertDialog(
            onDismissRequest = { showAppearanceDialog = false },
            title = { Text("Choose Appearance") },
            text = {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                viewModel.updateThemeMode("system")
                                showAppearanceDialog = false
                            }
                            .padding(vertical = 8.dp)
                    ) {
                        RadioButton(selected = themeMode == "system", onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            viewModel.updateThemeMode("system")
                            showAppearanceDialog = false
                        })
                        Text("System Default", modifier = Modifier.padding(start = 8.dp))
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                viewModel.updateThemeMode("light")
                                showAppearanceDialog = false
                            }
                            .padding(vertical = 8.dp)
                    ) {
                        RadioButton(selected = themeMode == "light", onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            viewModel.updateThemeMode("light")
                            showAppearanceDialog = false
                        })
                        Text("Light", modifier = Modifier.padding(start = 8.dp))
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                viewModel.updateThemeMode("dark")
                                showAppearanceDialog = false
                            }
                            .padding(vertical = 8.dp)
                    ) {
                        RadioButton(selected = themeMode == "dark", onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            viewModel.updateThemeMode("dark")
                            showAppearanceDialog = false
                        })
                        Text("Dark", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAppearanceDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showReceiptDialog) {
        var tempName by remember { mutableStateOf(shopName) }
        var tempAddress by remember { mutableStateOf(shopAddress) }
        var tempPhone by remember { mutableStateOf(shopPhone) }
        var tempEmail by remember { mutableStateOf(shopEmail) }
        var tempGst by remember { mutableStateOf(shopGst) }
        var tempFooter by remember { mutableStateOf(footerNote) }

        AlertDialog(
            onDismissRequest = { showReceiptDialog = false },
            title = { Text("Receipt Details") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = tempName, onValueChange = { tempName = it }, label = { Text("Shop Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = tempAddress, onValueChange = { tempAddress = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = tempPhone, onValueChange = { tempPhone = it }, label = { Text("Phone Number") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = tempEmail, onValueChange = { tempEmail = it }, label = { Text("Email (Optional)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = tempGst, onValueChange = { tempGst = it }, label = { Text("GST Number (Optional)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = tempFooter, onValueChange = { tempFooter = it }, label = { Text("Footer Note") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    viewModel.updateReceiptDetails(tempName, tempAddress, tempPhone, tempEmail, tempGst, tempFooter)
                    showReceiptDialog = false
                    Toast.makeText(context, "Receipt details updated", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    showReceiptDialog = false 
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = { TopBar("SnapTag - Settings") }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                SettingsHeader("Receipt Settings")
                SettingsItem(
                    icon = Icons.Default.Receipt,
                    title = "Receipt Details",
                    subtitle = "Shop name, address, phone, GST, etc.",
                    onClick = { showReceiptDialog = true }
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                            .clickable { qrPickerLauncher.launch("image/*") },
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
                                Text("Tap to add QR", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    TextButton(onClick = { qrPickerLauncher.launch("image/*") }) {
                        Text(if (paymentQrUri == null) "Select Payment QR" else "Change Payment QR")
                    }
                }
            }

            item {
                SettingsHeader("Data & Maintenance")
                SettingsItem(
                    icon = Icons.Default.Backup,
                    title = "Backup Data",
                    subtitle = "Export products to a JSON file",
                    onClick = {
                        val fileName = "SnapTag_Backup_${System.currentTimeMillis()}.json"
                        createDocumentLauncher.launch(fileName)
                    }
                )
                SettingsItem(
                    icon = Icons.Default.Restore,
                    title = "Restore Data",
                    subtitle = "Import products from a JSON file",
                    onClick = {
                        importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                    }
                )
                SettingsItem(
                    icon = Icons.Default.DeleteForever,
                    title = "Clear Data",
                    subtitle = "Reset all inventory (Careful!)",
                    onClick = { showClearDialog = true }
                )
            }

            item {
                SettingsHeader("General")
                SettingsItem(
                    icon = Icons.Default.Palette,
                    title = "Appearance",
                    subtitle = when(themeMode) {
                        "light" -> "Light"
                        "dark" -> "Dark"
                        else -> "System Default"
                    },
                    onClick = { showAppearanceDialog = true }
                )
                SettingsItem(
                    icon = Icons.Default.Language,
                    title = "Language",
                    subtitle = "English (US)",
                    onClick = {}
                )
            }

            item {
                SettingsHeader("Support")
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "About SnapTag",
                    subtitle = "Logo, Description, Privacy & Terms",
                    onClick = onNavigateToAbout
                )
            }
        }
    }
}

private fun exportDataToUri(context: Context, uri: Uri, products: List<Product>, scope: CoroutineScope) {
    scope.launch(Dispatchers.IO) {
        try {
            val gson = Gson()
            val jsonString = gson.toJson(products)

            context.contentResolver.openOutputStream(uri)?.use {
                it.write(jsonString.toByteArray())
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Backup saved successfully", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

private fun importData(context: Context, uri: Uri, viewModel: ProductViewModel, scope: CoroutineScope) {
    scope.launch(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val jsonString = inputStream?.bufferedReader()?.use { it.readText() }

            if (jsonString != null) {
                val gson = Gson()
                val type = object : TypeToken<List<Product>>() {}.type
                val products: List<Product> = gson.fromJson(jsonString, type)

                withContext(Dispatchers.Main) {
                    viewModel.importProducts(products)
                    Toast.makeText(context, "Imported ${products.size} products", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
fun SettingsHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val view = LocalView.current
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, contentDescription = null) },
        modifier = Modifier.clickable { 
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            onClick() 
        }
    )
}

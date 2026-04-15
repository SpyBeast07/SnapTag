package com.example.snaptag.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
fun SettingsScreen(viewModelFactory: ProductViewModelFactory) {
    val viewModel: ProductViewModel = viewModel(factory = viewModelFactory)
    val products by viewModel.products.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showClearDialog by remember { mutableStateOf(false) }

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
                TextButton(onClick = { showClearDialog = false }) {
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
                SettingsHeader("General")
                SettingsItem(
                    icon = Icons.Default.Language,
                    title = "Language",
                    subtitle = "English (US)",
                    onClick = {}
                )
            }

            item {
                SettingsHeader("Account")
                SettingsItem(
                    icon = Icons.Default.Person,
                    title = "Profile",
                    subtitle = "Edit your personal info",
                    onClick = {}
                )
                SettingsItem(
                    icon = Icons.Default.Security,
                    title = "Privacy & Security",
                    subtitle = "Password and data usage",
                    onClick = {}
                )
            }

            item {
                SettingsHeader("Data & Maintenance")
                SettingsItem(
                    icon = Icons.Default.Backup,
                    title = "Backup Data",
                    subtitle = "Export products to Downloads as JSON",
                    onClick = {
                        exportData(context, products)
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
                SettingsHeader("App Info")
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "Version",
                    subtitle = "1.0.0 (Stable)",
                    onClick = {}
                )
            }
        }
    }
}

private fun exportData(context: Context, products: List<Product>) {
    try {
        val gson = Gson()
        val jsonString = gson.toJson(products)
        val fileName = "SnapTag_Backup_${System.currentTimeMillis()}.json"

        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, fileName)

        FileOutputStream(file).use {
            it.write(jsonString.toByteArray())
        }

        Toast.makeText(context, "Backup saved to Downloads", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
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
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, contentDescription = null) },
        modifier = Modifier.clickable { onClick() }
    )
}

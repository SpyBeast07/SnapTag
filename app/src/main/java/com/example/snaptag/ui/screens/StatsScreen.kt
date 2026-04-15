package com.example.snaptag.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.snaptag.data.Product
import com.example.snaptag.viewmodel.StatsViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(viewModel: StatsViewModel) {
    val totalProducts by viewModel.totalProducts.collectAsState()
    val totalUnits by viewModel.totalUnits.collectAsState()
    val totalValue by viewModel.totalValue.collectAsState()
    val topProducts by viewModel.topProducts.collectAsState()
    val lowStockProducts by viewModel.lowStockProducts.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SnapTag - Stats", fontWeight = FontWeight.Bold) }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        title = "Total Products",
                        value = totalProducts.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Total Units",
                        value = totalUnits.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                StatCard(
                    title = "Total Inventory Value",
                    value = "₹${String.format(Locale.getDefault(), "%.2f", totalValue)}",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item { SectionHeader(title = "Top 5 Products") }

            if (topProducts.isEmpty()) {
                item { Text("No data available", color = Color.Gray) }
            } else {
                items(topProducts) { product ->
                    ProductStatItem(product = product)
                }
            }

            item { SectionHeader(title = "Low Stock Alerts") }

            if (lowStockProducts.isEmpty()) {
                item { Text("All items well stocked!", color = Color.Gray) }
            } else {
                items(lowStockProducts) { product ->
                    ProductStatItem(product = product, isLowStock = true)
                }
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun ProductStatItem(product: Product, isLowStock: Boolean = false) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isLowStock) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = product.name, fontWeight = FontWeight.SemiBold)
                Text(text = "₹${product.price}", style = MaterialTheme.typography.bodySmall)
            }
            Text(
                text = "Stock: ${product.stock}",
                fontWeight = FontWeight.Bold,
                color = if (isLowStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

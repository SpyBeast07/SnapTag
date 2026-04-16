package com.example.snaptag.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.snaptag.data.SaleEntity
import com.example.snaptag.utils.PdfGenerator
import com.example.snaptag.viewmodel.StatsViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesScreen(viewModel: StatsViewModel) {
    val totalRevenue by viewModel.totalRevenue.collectAsState()
    val totalOrders by viewModel.totalOrders.collectAsState()
    val totalItemsSold by viewModel.totalItemsSold.collectAsState()
    val allSales by viewModel.allSales.collectAsState()

    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sales Analytics", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = {
                        if (allSales.isNotEmpty()) {
                            val firstSale = allSales.last().timestamp
                            val lastSale = allSales.first().timestamp
                            val file = PdfGenerator.generateSalesReportPdf(
                                context,
                                firstSale,
                                lastSale,
                                allSales,
                                totalRevenue,
                                totalItemsSold
                            )
                            if (file != null) {
                                Toast.makeText(context, "Report exported to Downloads/SnapTag/sales", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(context, "No sales to export", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Export Report")
                    }
                }
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
                StatCard(
                    title = "Total Revenue",
                    value = "₹${String.format(Locale.getDefault(), "%.2f", totalRevenue)}",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        title = "Total Orders",
                        value = totalOrders.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Items Sold",
                        value = totalItemsSold.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item { SectionHeader(title = "Sale History") }

            if (allSales.isEmpty()) {
                item { Text("No sales recorded yet", color = Color.Gray) }
            } else {
                items(allSales) { sale ->
                    SaleListItem(sale = sale)
                }
            }
        }
    }
}

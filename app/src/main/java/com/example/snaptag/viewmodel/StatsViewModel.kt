package com.example.snaptag.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.snaptag.data.Product
import com.example.snaptag.data.ProductRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class StatsViewModel(private val repository: ProductRepository) : ViewModel() {

    val totalProducts: StateFlow<Int> = repository.totalProductCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalUnits: StateFlow<Int> = repository.totalStockUnits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalValue: StateFlow<Double> = repository.totalInventoryValue
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val topProducts: StateFlow<List<Product>> = repository.topProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lowStockProducts: StateFlow<List<Product>> = repository.lowStockProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

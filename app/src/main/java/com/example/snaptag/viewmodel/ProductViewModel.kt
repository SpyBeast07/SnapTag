package com.example.snaptag.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.snaptag.data.Product
import com.example.snaptag.data.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class ProductViewModel(private val repository: ProductRepository, application: Application) : AndroidViewModel(application) {
    private val sharedPrefs: SharedPreferences = application.getSharedPreferences("SnapTagPrefs", Context.MODE_PRIVATE)

    private val _paymentQrUri = MutableStateFlow(sharedPrefs.getString("payment_qr_uri", null))
    val paymentQrUri: StateFlow<String?> = _paymentQrUri.asStateFlow()

    fun savePaymentQrUri(uri: String?) {
        sharedPrefs.edit().putString("payment_qr_uri", uri).apply()
        _paymentQrUri.value = uri
    }
    val products: StateFlow<List<Product>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addProduct(name: String, price: Double, stock: Int, barcode: String? = null) {
        viewModelScope.launch {
            val newProduct = Product(
                id = UUID.randomUUID().toString(),
                name = name,
                price = price,
                stock = stock,
                barcode = barcode
            )
            repository.insert(newProduct)
        }
    }

    fun updateProduct(updatedProduct: Product) {
        viewModelScope.launch {
            repository.update(updatedProduct)
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            repository.deleteById(productId)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.deleteAll()
        }
    }

    fun importProducts(products: List<Product>) {
        viewModelScope.launch {
            repository.insertAll(products)
        }
    }
}

class ProductViewModelFactory(
    private val repository: ProductRepository,
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProductViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProductViewModel(repository, application) as T
        }
        if (modelClass.isAssignableFrom(StatsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StatsViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(BillingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BillingViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

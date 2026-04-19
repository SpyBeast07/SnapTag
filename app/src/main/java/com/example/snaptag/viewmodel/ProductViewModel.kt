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

    private val _shopName = MutableStateFlow(sharedPrefs.getString("shop_name", "My Shop") ?: "My Shop")
    val shopName: StateFlow<String> = _shopName.asStateFlow()

    private val _shopAddress = MutableStateFlow(sharedPrefs.getString("shop_address", "") ?: "")
    val shopAddress: StateFlow<String> = _shopAddress.asStateFlow()

    private val _shopPhone = MutableStateFlow(sharedPrefs.getString("shop_phone", "") ?: "")
    val shopPhone: StateFlow<String> = _shopPhone.asStateFlow()

    private val _shopEmail = MutableStateFlow(sharedPrefs.getString("shop_email", "") ?: "")
    val shopEmail: StateFlow<String> = _shopEmail.asStateFlow()

    private val _shopGst = MutableStateFlow(sharedPrefs.getString("shop_gst", "") ?: "")
    val shopGst: StateFlow<String> = _shopGst.asStateFlow()

    private val _footerNote = MutableStateFlow(sharedPrefs.getString("footer_note", "Thank you for shopping with us!") ?: "Thank you for shopping with us!")
    val footerNote: StateFlow<String> = _footerNote.asStateFlow()

    private val _themeMode = MutableStateFlow(sharedPrefs.getString("theme_mode", "system") ?: "system")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    fun updateThemeMode(mode: String) {
        sharedPrefs.edit().putString("theme_mode", mode).apply()
        _themeMode.value = mode
    }

    fun updateReceiptDetails(
        name: String,
        address: String,
        phone: String,
        email: String,
        gst: String,
        footer: String
    ) {
        sharedPrefs.edit().apply {
            putString("shop_name", name)
            putString("shop_address", address)
            putString("shop_phone", phone)
            putString("shop_email", email)
            putString("shop_gst", gst)
            putString("footer_note", footer)
        }.apply()
        _shopName.value = name
        _shopAddress.value = address
        _shopPhone.value = phone
        _shopEmail.value = email
        _shopGst.value = gst
        _footerNote.value = footer
    }

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

    fun addProduct(name: String, price: Double, stock: Int, barcode: String? = null, gst: Double? = null) {
        viewModelScope.launch {
            val newProduct = Product(
                id = UUID.randomUUID().toString(),
                name = name,
                price = price,
                stock = stock,
                barcode = barcode,
                gstPercentage = gst
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

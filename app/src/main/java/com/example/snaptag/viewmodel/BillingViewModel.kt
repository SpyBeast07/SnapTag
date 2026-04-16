package com.example.snaptag.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.snaptag.data.Product
import com.example.snaptag.data.ProductRepository
import com.example.snaptag.data.SaleEntity
import com.example.snaptag.data.SaleItemEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CartItem(
    val productId: String,
    val name: String,
    val price: Double,
    val quantity: Int
)

class BillingViewModel(private val repository: ProductRepository) : ViewModel() {

    val products: StateFlow<List<Product>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val totalItems: StateFlow<Int> = _cartItems
        .map { it.sumOf { item -> item.quantity } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalAmount: StateFlow<Double> = _cartItems
        .map { it.sumOf { item -> item.price * item.quantity } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addToCart(product: Product) {
        val currentList = _cartItems.value.toMutableList()
        val existingItemIndex = currentList.indexOfFirst { it.productId == product.id }

        if (existingItemIndex != -1) {
            val existingItem = currentList[existingItemIndex]
            if (existingItem.quantity < product.stock) {
                currentList[existingItemIndex] = existingItem.copy(quantity = existingItem.quantity + 1)
                _cartItems.value = currentList
            }
        } else if (product.stock > 0) {
            currentList.add(CartItem(product.id, product.name, product.price, 1))
            _cartItems.value = currentList
        }
    }

    fun incrementQuantity(productId: String) {
        val currentList = _cartItems.value.toMutableList()
        val index = currentList.indexOfFirst { it.productId == productId }
        if (index != -1) {
            val product = products.value.find { it.id == productId }
            if (product != null && currentList[index].quantity < product.stock) {
                currentList[index] = currentList[index].copy(quantity = currentList[index].quantity + 1)
                _cartItems.value = currentList
            }
        }
    }

    fun decrementQuantity(productId: String) {
        val currentList = _cartItems.value.toMutableList()
        val index = currentList.indexOfFirst { it.productId == productId }
        if (index != -1) {
            val currentItem = currentList[index]
            if (currentItem.quantity > 1) {
                currentList[index] = currentItem.copy(quantity = currentItem.quantity - 1)
            } else {
                currentList.removeAt(index)
            }
            _cartItems.value = currentList
        }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
    }

    fun generateBill(customerPhone: String? = null, onSuccess: (SaleEntity) -> Unit) {
        viewModelScope.launch {
            val items = _cartItems.value
            if (items.isEmpty()) return@launch

            // 1. Snapshot cart data to avoid race conditions
            val billItems = items.toList()
            val totalItemsCount = billItems.sumOf { it.quantity }
            val totalBillAmount = billItems.sumOf { it.price * it.quantity }
            
            // 2. Clear cart immediately to prevent double clicks/submissions
            _cartItems.value = emptyList()

            // 3. Update stock and record sale in a single transaction-like flow via repository
            // Create the sale record
            val sale = SaleEntity(
                timestamp = System.currentTimeMillis(),
                totalItems = totalItemsCount,
                totalAmount = totalBillAmount,
                customerPhone = if (customerPhone.isNullOrBlank()) null else customerPhone
            )
            val saleItems = billItems.map {
                SaleItemEntity(
                    saleId = 0, // Will be set by DAO
                    productName = it.name,
                    quantity = it.quantity,
                    price = it.price
                )
            }

            // Perform DB updates
            repository.processSale(sale, saleItems, billItems)

            onSuccess(sale)
        }
    }
}

class BillingViewModelFactory(private val repository: ProductRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BillingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BillingViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

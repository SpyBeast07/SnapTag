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
    val quantity: Int,
    val gstPercentage: Double? = null
)

class BillingViewModel(private val repository: ProductRepository) : ViewModel() {

    val products: StateFlow<List<Product>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isGstEnabled = MutableStateFlow(true)
    val isGstEnabled: StateFlow<Boolean> = _isGstEnabled.asStateFlow()

    private val _discountValue = MutableStateFlow(0.0)
    val discountValue: StateFlow<Double> = _discountValue.asStateFlow()

    private val _isDiscountPercentage = MutableStateFlow(true)
    val isDiscountPercentage: StateFlow<Boolean> = _isDiscountPercentage.asStateFlow()

    fun updateDiscount(value: Double) {
        _discountValue.value = value
    }

    fun toggleDiscountType(isPercentage: Boolean) {
        _isDiscountPercentage.value = isPercentage
    }

    fun toggleGst(enabled: Boolean) {
        _isGstEnabled.value = enabled
    }

    val totalItems: StateFlow<Int> = _cartItems
        .map { it.sumOf { item -> item.quantity } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val subtotalAmount: StateFlow<Double> = _cartItems
        .map { it.sumOf { item -> item.price * item.quantity } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val discountAmount: StateFlow<Double> = combine(subtotalAmount, _discountValue, _isDiscountPercentage) { subtotal, discount, isPercentage ->
        if (isPercentage) {
            (subtotal * discount / 100.0)
        } else {
            discount
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val discountedSubtotal: StateFlow<Double> = combine(subtotalAmount, discountAmount) { subtotal, discount ->
        (subtotal - discount).coerceAtLeast(0.0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalGstAmount: StateFlow<Double> = combine(_cartItems, discountedSubtotal, subtotalAmount, _isGstEnabled) { items, discSubtotal, subtotal, gstEnabled ->
        if (!gstEnabled || subtotal == 0.0) 0.0
        else {
            val discountFactor = discSubtotal / subtotal
            items.sumOf { item ->
                val basePrice = item.price * item.quantity
                if (item.gstPercentage != null) {
                    val discountedBasePrice = basePrice * discountFactor
                    (discountedBasePrice * item.gstPercentage / 100.0)
                } else {
                    0.0
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalAmount: StateFlow<Double> = combine(discountedSubtotal, totalGstAmount) { subtotal, gst ->
        subtotal + gst
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

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
            currentList.add(CartItem(product.id, product.name, product.price, 1, product.gstPercentage))
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
        _discountValue.value = 0.0
    }

    fun generateBill(customerPhone: String? = null, finalTotal: Double, onSuccess: (SaleEntity) -> Unit) {
        viewModelScope.launch {
            val items = _cartItems.value
            if (items.isEmpty()) return@launch

            // 1. Snapshot cart data to avoid race conditions
            val billItems = items.toList()
            val totalItemsCount = billItems.sumOf { it.quantity }
            
            // 2. Clear cart immediately to prevent double clicks/submissions
            _cartItems.value = emptyList()
            _discountValue.value = 0.0

            // 3. Update stock and record sale in a single transaction-like flow via repository
            // Create the sale record
            val sale = SaleEntity(
                timestamp = System.currentTimeMillis(),
                totalItems = totalItemsCount,
                totalAmount = finalTotal,
                customerPhone = if (customerPhone.isNullOrBlank()) null else customerPhone
            )
            val saleItems = billItems.map {
                SaleItemEntity(
                    saleId = 0, // Will be set by DAO
                    productName = it.name,
                    quantity = it.quantity,
                    price = it.price,
                    gstPercentage = it.gstPercentage
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

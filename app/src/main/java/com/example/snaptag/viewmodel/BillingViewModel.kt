package com.example.snaptag.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.snaptag.data.Product
import com.example.snaptag.data.ProductRepository
import com.example.snaptag.data.SaleEntity
import com.example.snaptag.data.SaleItemEntity
import com.example.snaptag.utils.BillSummary
import com.example.snaptag.utils.BillingCalculator
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CartItem(
    val productId: String,
    val name: String,
    val price: Double,
    val quantity: Int,
    val gstPercent: Double = 0.0,
    val discountPercent: Double = 0.0
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

    private val _billDiscountPercent = MutableStateFlow(0.0)
    val billDiscountPercent: StateFlow<Double> = _billDiscountPercent.asStateFlow()

    fun updateBillDiscount(percent: Double) {
        _billDiscountPercent.value = percent
    }

    fun toggleGst(enabled: Boolean) {
        _isGstEnabled.value = enabled
    }

    val billSummary: StateFlow<BillSummary> = combine(_cartItems, _billDiscountPercent, _isGstEnabled) { items, discount, gstEnabled ->
        // If GST is disabled, we effectively treat all items as 0% GST for the calculation
        val itemsForCalc = if (gstEnabled) items else items.map { it.copy(gstPercent = 0.0) }
        BillingCalculator.calculateBill(itemsForCalc, discount)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 
        BillSummary(emptyList(), 0.0, 0.0, 0.0, 0.0, 0.0, 0.0))

    val totalItems: StateFlow<Int> = _cartItems
        .map { it.sumOf { item -> item.quantity } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

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
            currentList.add(CartItem(
                productId = product.id,
                name = product.name,
                price = product.price,
                quantity = 1,
                gstPercent = product.gstPercent,
                discountPercent = product.discountPercent
            ))
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
        _billDiscountPercent.value = 0.0
    }

    fun generateBill(customerPhone: String? = null, onSuccess: (SaleEntity) -> Unit) {
        viewModelScope.launch {
            val summary = billSummary.value
            if (summary.items.isEmpty()) return@launch

            val totalItemsCount = summary.items.sumOf { it.cartItem.quantity }
            
            _cartItems.value = emptyList()
            _billDiscountPercent.value = 0.0

            val sale = SaleEntity(
                timestamp = System.currentTimeMillis(),
                totalItems = totalItemsCount,
                totalAmount = summary.grandTotal,
                customerPhone = if (customerPhone.isNullOrBlank()) null else customerPhone,
                subtotal = summary.subtotal,
                totalItemDiscounts = summary.totalItemDiscounts,
                billDiscountAmount = summary.billDiscountAmount,
                billDiscountPercent = _billDiscountPercent.value,
                totalTaxableValue = summary.totalTaxableValue,
                totalGst = summary.totalGst
            )
            
            val saleItems = summary.items.map { calcItem ->
                SaleItemEntity(
                    saleId = 0,
                    productName = calcItem.cartItem.name,
                    quantity = calcItem.cartItem.quantity,
                    price = calcItem.cartItem.price,
                    gstPercentage = calcItem.cartItem.gstPercent,
                    discountPercent = calcItem.cartItem.discountPercent,
                    itemDiscountAmount = calcItem.itemDiscountAmount,
                    billDiscountShare = calcItem.billDiscountShare,
                    taxableValue = calcItem.finalTaxableValue,
                    gstAmount = calcItem.gstAmount,
                    finalTotal = calcItem.finalItemTotal
                )
            }

            repository.processSale(sale, saleItems, summary.items.map { it.cartItem })
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

package com.example.warehouse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.warehouse.data.database.entity.Category
import com.example.warehouse.data.database.relation.ProductWithCategoryAndSupplier
import com.example.warehouse.data.repository.WarehouseRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.ceil

data class InventoryAlertsUiState(
    val products: List<ProductWithCategoryAndSupplier> = emptyList(),
    val allProducts: List<ProductWithCategoryAndSupplier> = emptyList(),
    val filteredProducts: List<ProductWithCategoryAndSupplier> = emptyList(),
    val categories: List<Category> = emptyList(),
    val lowStockCount: Int = 0,
    val nearOutOfStockCount: Int = 0,
    val outOfStockCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)

class InventoryAlertsViewModel(private val repository: WarehouseRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    val selectedCategoryId = _selectedCategoryId.asStateFlow()

    private val _selectedAlertType = MutableStateFlow("ALL") // "ALL", "LOW", "NEAR", "OUT"
    val selectedAlertType = _selectedAlertType.asStateFlow()

    private val _scrollToProductId = MutableStateFlow<Long?>(null)
    val scrollToProductId = _scrollToProductId.asStateFlow()

    fun setScrollToProductId(id: Long?) {
        _scrollToProductId.value = id
    }

    private val _allProducts = repository.getActiveProductsWithDetails()
    private val _allCategories = repository.getAllCategories()

    val uiState: StateFlow<InventoryAlertsUiState> = combine(
        _allProducts,
        _allCategories,
        _searchQuery,
        _selectedCategoryId,
        _selectedAlertType
    ) { products, categories, query, categoryId, alertType ->
        
        // Filter alerts (products that have stock low, critical, or out of stock)
        val alertProducts = products.filter { item ->
            val qty = item.quantityInStock
            val min = item.product.minStock
            qty == 0 || qty <= min
        }

        // Calculate counts based on defined conditions:
        // 1. Tồn kho thấp: qty <= minStock && qty > 0
        // 2. Tồn kho gần hết: qty <= ceil(minStock * 0.5) && qty > 0
        // 3. Đã hết hàng: qty == 0
        val lowStockCount = alertProducts.count { item ->
            val qty = item.quantityInStock
            val min = item.product.minStock
            qty > 0 && qty <= min
        }
        val nearOutOfStockCount = alertProducts.count { item ->
            val qty = item.quantityInStock
            val min = item.product.minStock
            qty > 0 && qty <= ceil(min * 0.5f).toInt()
        }
        val outOfStockCount = alertProducts.count { item ->
            item.quantityInStock == 0
        }

        // Filter list based on selected filter options
        val filtered = alertProducts.filter { item ->
            // Search text filter
            val matchesQuery = query.isEmpty() || 
                    item.product.productName.contains(query, ignoreCase = true) || 
                    item.product.productCode.contains(query, ignoreCase = true)
            
            // Category filter
            val matchesCategory = categoryId == null || categoryId == 0L || item.product.categoryId == categoryId

            // Alert Type filter
            val qty = item.quantityInStock
            val min = item.product.minStock
            val matchesAlert = when (alertType) {
                "LOW" -> qty > 0 && qty <= min
                "NEAR" -> qty > 0 && qty <= ceil(min * 0.5f).toInt()
                "OUT" -> qty == 0
                else -> true // "ALL"
            }

            matchesQuery && matchesCategory && matchesAlert
        }

        InventoryAlertsUiState(
            products = alertProducts,
            allProducts = products,
            filteredProducts = filtered,
            categories = categories,
            lowStockCount = lowStockCount,
            nearOutOfStockCount = nearOutOfStockCount,
            outOfStockCount = outOfStockCount,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = InventoryAlertsUiState(isLoading = true)
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(id: Long?) {
        _selectedCategoryId.value = id
    }

    fun setSelectedAlertType(type: String) {
        _selectedAlertType.value = type
    }

    // Update min stock
    suspend fun updateMinStock(productId: Long, newMinStock: Int, userRole: String): Result<Unit> {
        // Server side (Backend check) simulation returning 403 Forbidden
        if (!com.example.warehouse.utils.PermissionManager.canUpdateMinStock(userRole)) {
            return Result.failure(Exception("403 Forbidden: Bạn không có quyền thực hiện thao tác này"))
        }
        if (newMinStock < 0) {
            return Result.failure(Exception("Số lượng tồn kho tối thiểu không được là số âm"))
        }

        return try {
            val product = repository.getProductById(productId)
            if (product != null) {
                val updatedProduct = product.copy(
                    minStock = newMinStock,
                    updatedAt = System.currentTimeMillis()
                )
                repository.updateProduct(updatedProduct)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Không tìm thấy sản phẩm"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

package com.example.warehouse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.warehouse.data.database.entity.Category
import com.example.warehouse.data.database.entity.Product
import com.example.warehouse.data.database.entity.Supplier
import com.example.warehouse.data.database.relation.ProductWithCategoryAndSupplier
import com.example.warehouse.data.repository.WarehouseRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProductViewModel(private val repository: WarehouseRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    val selectedCategoryId: StateFlow<Long?> = _selectedCategoryId.asStateFlow()

    private val _statusFilter = MutableStateFlow("ALL") // "ALL", "ACTIVE", "INACTIVE"
    val statusFilter: StateFlow<String> = _statusFilter.asStateFlow()

    val categories: StateFlow<List<Category>> = repository.getAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val suppliers: StateFlow<List<Supplier>> = repository.getAllSuppliers()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val products: StateFlow<List<ProductWithCategoryAndSupplier>> = combine(
        repository.getAllProductsWithDetails(),
        _searchQuery,
        _selectedCategoryId,
        _statusFilter
    ) { allProducts, query, catId, status ->
        allProducts.filter { item ->
            val p = item.product

            // Match status: "ALL", "ACTIVE", "INACTIVE"
            val matchesStatus = when (status) {
                "ACTIVE" -> p.status.equals("ACTIVE", ignoreCase = true)
                "INACTIVE" -> p.status.equals("INACTIVE", ignoreCase = true)
                else -> true
            }

            // Match category
            val matchesCategory = (catId == null || p.categoryId == catId)

            // Match search query
            val matchesQuery = if (query.isBlank()) {
                true
            } else {
                val q = query.trim()
                p.productName.contains(q, ignoreCase = true) || p.productCode.contains(q, ignoreCase = true)
            }

            matchesStatus && matchesCategory && matchesQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setStatusFilter(status: String) {
        _statusFilter.value = status
    }

    private val _scannedProduct = MutableStateFlow<ProductWithCategoryAndSupplier?>(null)
    val scannedProduct: StateFlow<ProductWithCategoryAndSupplier?> = _scannedProduct.asStateFlow()

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun selectCategoryFilter(categoryId: Long?) {
        _selectedCategoryId.value = categoryId
    }

    fun lookupBarcode(code: String) {
        viewModelScope.launch {
            val result = repository.getProductByBarcodeOrCode(code.trim())
            _scannedProduct.value = result
            if (result != null) {
                _searchQuery.value = result.product.productCode
            }
        }
    }

    fun clearScannedProduct() {
        _scannedProduct.value = null
    }

    suspend fun addCategoryQuick(name: String): Category {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) throw IllegalArgumentException("Tên danh mục không được để trống")
        val existing = categories.value.find { it.name.trim().equals(trimmed, ignoreCase = true) }
        if (existing != null) return existing
        val id = repository.insertCategory(Category(categoryName = trimmed))
        return Category(categoryId = id, categoryName = trimmed)
    }

    suspend fun addSupplierQuick(name: String): Supplier {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) throw IllegalArgumentException("Tên nhà cung cấp không được để trống")
        val existing = suppliers.value.find { it.name.trim().equals(trimmed, ignoreCase = true) }
        if (existing != null) return existing
        val id = repository.insertSupplier(Supplier(supplierName = trimmed))
        return Supplier(supplierId = id, supplierName = trimmed)
    }

    fun saveProductWithCategory(
        product: Product,
        categoryNameInput: String,
        selectedCatId: Long?,
        isEditing: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val catInput = categoryNameInput.trim()
                
                val finalCatId: Long? = if (selectedCatId != null && selectedCatId > 0) {
                    val selectedCat = categories.value.find { it.id == selectedCatId }
                    if (selectedCat != null && selectedCat.name.equals(catInput, ignoreCase = true)) {
                        selectedCatId
                    } else if (catInput.isNotBlank()) {
                        val cat = addCategoryQuick(catInput)
                        cat.id
                    } else {
                        selectedCatId
                    }
                } else if (catInput.isNotBlank()) {
                    val cat = addCategoryQuick(catInput)
                    cat.id
                } else {
                    null
                }

                // 1. Validate Category mandatory
                if (finalCatId == null || finalCatId <= 0) {
                    onError("Bắt buộc phải chọn hoặc nhập Danh mục trước khi lưu sản phẩm.")
                    return@launch
                }

                // 2. Validate min_stock >= 0
                if (product.minStock < 0) {
                    onError("Số lượng tồn tối thiểu (Tồn kho an toàn) phải lớn hơn hoặc bằng 0.")
                    return@launch
                }

                val currentProductId = if (isEditing) product.productId else 0

                // 3. Validate Unique productCode
                if (repository.isProductCodeDuplicate(product.productCode, currentProductId)) {
                    onError("Mã sản phẩm '${product.productCode}' đã tồn tại trong hệ thống.")
                    return@launch
                }

                // 4. Validate Unique productName
                if (repository.isProductNameDuplicate(product.productName, currentProductId)) {
                    onError("Tên sản phẩm '${product.productName}' đã tồn tại trong hệ thống.")
                    return@launch
                }

                val finalProduct = product.copy(
                    categoryId = finalCatId
                )

                if (isEditing) {
                    repository.updateProduct(finalProduct)
                } else {
                    // Atomic transaction creating product + inventory (quantity = 0)
                    repository.createProductAtomic(finalProduct)
                }

                onSuccess()
            } catch (e: Exception) {
                onError("Đã xảy ra lỗi khi lưu sản phẩm: ${e.localizedMessage}")
            }
        }
    }

    fun deleteProduct(
        product: Product,
        actingUserRole: String? = null,
        onResult: (isHardDeleted: Boolean, message: String) -> Unit
    ) {
        if (!com.example.warehouse.utils.PermissionManager.canDeleteProduct(actingUserRole)) {
            onResult(false, "Bạn không có quyền thực hiện thao tác này")
            return
        }
        viewModelScope.launch {
            try {
                val (isHardDeleted, message) = repository.deleteProductSmart(product)
                onResult(isHardDeleted, message)
            } catch (e: Exception) {
                onResult(false, "Không thể xử lý yêu cầu xóa: ${e.localizedMessage}")
            }
        }
    }

    suspend fun getProductWithDetailsById(id: Long): ProductWithCategoryAndSupplier? {
        return repository.getProductWithDetailsById(id)
    }
}

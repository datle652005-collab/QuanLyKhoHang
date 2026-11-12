package com.example.warehouse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.warehouse.data.database.entity.Supplier
import com.example.warehouse.data.database.entity.User
import com.example.warehouse.data.database.relation.ImportReceiptWithDetails
import com.example.warehouse.data.database.relation.ProductWithCategoryAndSupplier
import com.example.warehouse.data.repository.WarehouseRepository
import com.example.warehouse.utils.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ImportCartItem(
    val product: ProductWithCategoryAndSupplier,
    val quantity: Int,
    val price: Double
)

class ImportViewModel(private val repository: WarehouseRepository) : ViewModel() {

    // Raw Receipts & Master Data
    val importReceipts: StateFlow<List<ImportReceiptWithDetails>> = repository.getAllImportReceipts()
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

    val activeSuppliers: StateFlow<List<Supplier>> = repository.getActiveSuppliers()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val users: StateFlow<List<User>> = repository.getAllUsers()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Form State (Create Receipt)
    private val _cartItems = MutableStateFlow<List<ImportCartItem>>(emptyList())
    val cartItems: StateFlow<List<ImportCartItem>> = _cartItems.asStateFlow()

    private val _selectedSupplier = MutableStateFlow<Supplier?>(null)
    val selectedSupplier: StateFlow<Supplier?> = _selectedSupplier.asStateFlow()

    private val _supplierNameText = MutableStateFlow("")
    val supplierNameText: StateFlow<String> = _supplierNameText.asStateFlow()

    private val _importDate = MutableStateFlow(System.currentTimeMillis())
    val importDate: StateFlow<Long> = _importDate.asStateFlow()

    private val _note = MutableStateFlow("")
    val note: StateFlow<String> = _note.asStateFlow()

    // Filter States (List View)
    val startDateFilter = MutableStateFlow<Long?>(null)
    val endDateFilter = MutableStateFlow<Long?>(null)
    val supplierFilter = MutableStateFlow<Long?>(null)
    val userFilter = MutableStateFlow<Long?>(null)
    val statusFilter = MutableStateFlow<String>("ALL") // "ALL", "Đã lưu", "Đã hủy"

    private data class ImportFilterState(
        val start: Long? = null,
        val end: Long? = null,
        val supplierId: Long? = null,
        val userId: Long? = null,
        val status: String = "ALL"
    )

    private val filterState = combine(
        startDateFilter,
        endDateFilter,
        supplierFilter,
        userFilter,
        statusFilter
    ) { start, end, supplierId, userId, status ->
        ImportFilterState(start, end, supplierId, userId, status)
    }

    // Filtered Receipt List based on importDate (NOT createdAt)
    val filteredReceipts: StateFlow<List<ImportReceiptWithDetails>> = combine(
        importReceipts,
        filterState
    ) { receipts, filter ->
        receipts.filter { item ->
            val receipt = item.receipt

            // Date Range Filter based on importDate
            val matchesStart = filter.start == null || receipt.importDate >= DateUtils.getStartOfDay(filter.start)
            val matchesEnd = filter.end == null || receipt.importDate <= DateUtils.getEndOfDay(filter.end)

            // Supplier Filter
            val matchesSupplier = filter.supplierId == null || receipt.supplierId == filter.supplierId

            // User/Creator Filter
            val matchesUser = filter.userId == null || receipt.userId == filter.userId

            // Status Filter
            val matchesStatus = when (filter.status) {
                "Đã lưu" -> receipt.status.equals("Đã lưu", ignoreCase = true) || receipt.status.equals("Completed", ignoreCase = true)
                "Đã hủy" -> receipt.status.equals("Đã hủy", ignoreCase = true) || receipt.status.equals("Đã xóa", ignoreCase = true)
                else -> true
            }

            matchesStart && matchesEnd && matchesSupplier && matchesUser && matchesStatus
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setSupplierNameText(name: String) {
        _supplierNameText.value = name
    }

    fun selectSupplier(supplier: Supplier?) {
        _selectedSupplier.value = supplier
    }

    fun setImportDate(dateMillis: Long) {
        _importDate.value = dateMillis
    }

    fun setNote(newNote: String) {
        _note.value = newNote
    }

    // Add product to cart with deduplication logic
    fun addProductToCart(productWithDetails: ProductWithCategoryAndSupplier, quantity: Int = 1) {
        // Enforce active status
        if (!productWithDetails.product.status.equals("ACTIVE", ignoreCase = true)) {
            return
        }

        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.product.id == productWithDetails.product.id }
        if (index >= 0) {
            val existing = current[index]
            val updatedQty = existing.quantity + quantity
            current[index] = existing.copy(quantity = updatedQty)
        } else {
            current.add(
                ImportCartItem(
                    product = productWithDetails,
                    quantity = quantity.coerceAtLeast(1),
                    price = productWithDetails.product.importPrice
                )
            )
        }
        _cartItems.value = current
    }

    fun updateCartItem(productId: Long, newQuantity: Int, newPrice: Double) {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.product.id == productId }
        if (index >= 0) {
            current[index] = current[index].copy(
                quantity = newQuantity.coerceAtLeast(0),
                price = newPrice.coerceAtLeast(0.0)
            )
            _cartItems.value = current
        }
    }

    fun removeCartItem(productId: Long) {
        _cartItems.value = _cartItems.value.filterNot { it.product.product.id == productId }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        _selectedSupplier.value = null
        _supplierNameText.value = ""
        _importDate.value = System.currentTimeMillis()
        _note.value = ""
    }

    fun createReceipt(userId: Long?, onComplete: (Boolean, String) -> Unit) {
        val supplier = _selectedSupplier.value
        val supName = _supplierNameText.value.trim()

        if (supplier == null && supName.isBlank()) {
            onComplete(false, "Bắt buộc phải chọn hoặc nhập nhà cung cấp!")
            return
        }

        if (_cartItems.value.isEmpty()) {
            onComplete(false, "Vui lòng chọn ít nhất 1 sản phẩm nhập kho")
            return
        }

        val now = System.currentTimeMillis()
        if (_importDate.value > now + 60_000L) {
            onComplete(false, "Không thể chọn ngày nhập kho trong tương lai")
            return
        }

        for (item in _cartItems.value) {
            if (item.quantity <= 0) {
                onComplete(false, "Sản phẩm '${item.product.product.productName}' phải có số lượng > 0")
                return
            }
            if (item.price < 0.0) {
                onComplete(false, "Sản phẩm '${item.product.product.productName}' phải có đơn giá >= 0")
                return
            }
        }

        viewModelScope.launch {
            try {
                val details = _cartItems.value.map {
                    Pair(it.product.product, Pair(it.quantity, it.price))
                }
                val receiptId = repository.createImportReceipt(
                    code = null,
                    supplierId = supplier?.id,
                    supplierNameText = supName,
                    userId = userId,
                    importDate = _importDate.value,
                    note = _note.value.trim(),
                    rawItems = details
                )
                clearCart()
                onComplete(true, "Tạo phiếu nhập kho thành công!")
            } catch (e: Exception) {
                onComplete(false, e.message ?: "Lỗi tạo phiếu nhập kho")
            }
        }
    }

    fun cancelReceipt(receiptId: Long, currentUser: User, deleteReason: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                repository.cancelImportReceipt(receiptId, currentUser, deleteReason)
                onComplete(true, "Hủy phiếu nhập kho thành công!")
            } catch (e: Exception) {
                onComplete(false, e.message ?: "Không thể hủy phiếu nhập kho")
            }
        }
    }

    fun updateImportDate(receiptId: Long, newImportDate: Long, currentUser: User, onComplete: (Boolean, String) -> Unit) {
        if (newImportDate > System.currentTimeMillis() + 60_000L) {
            onComplete(false, "Ngày nhập kho không được chọn trong tương lai")
            return
        }

        viewModelScope.launch {
            try {
                repository.updateImportReceiptDate(receiptId, newImportDate, currentUser)
                onComplete(true, "Cập nhật ngày nhập kho thành công!")
            } catch (e: Exception) {
                onComplete(false, e.message ?: "Lỗi cập nhật ngày nhập kho")
            }
        }
    }

    suspend fun getImportReceiptById(id: Long): ImportReceiptWithDetails? {
        return repository.getImportReceiptById(id)
    }

    fun clearFilters() {
        startDateFilter.value = null
        endDateFilter.value = null
        supplierFilter.value = null
        userFilter.value = null
        statusFilter.value = "ALL"
    }

    fun updateReceiptFull(
        receiptId: Long,
        supplierId: Long?,
        supplierNameText: String?,
        importDate: Long,
        note: String,
        rawItems: List<ImportCartItem>,
        currentUser: User,
        onComplete: (Boolean, String) -> Unit
    ) {
        if (supplierId == null && (supplierNameText == null || supplierNameText.trim().isBlank())) {
            onComplete(false, "Bắt buộc phải chọn hoặc nhập nhà cung cấp!")
            return
        }

        viewModelScope.launch {
            try {
                val details = rawItems.map {
                    Pair(it.product.product, Pair(it.quantity, it.price))
                }
                repository.updateImportReceiptFull(
                    receiptId = receiptId,
                    supplierId = supplierId,
                    supplierNameText = supplierNameText,
                    importDate = importDate,
                    note = note,
                    rawItems = details,
                    user = currentUser
                )
                onComplete(true, "Cập nhật phiếu nhập kho thành công!")
            } catch (e: Exception) {
                onComplete(false, e.message ?: "Lỗi cập nhật phiếu nhập kho")
            }
        }
    }



    fun loadReceiptForEdit(receiptId: Long, onLoaded: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val receiptWithDetails = repository.getImportReceiptById(receiptId)
                if (receiptWithDetails != null) {
                    val r = receiptWithDetails.receipt
                    _selectedSupplier.value = if (r.supplierId != null) repository.getSupplierById(r.supplierId) else null
                    _supplierNameText.value = r.supplierNameText ?: ""
                    _importDate.value = r.importDate
                    _note.value = r.note
                    
                    val items = receiptWithDetails.details.mapNotNull { d ->
                        val fullProduct = repository.getProductWithDetailsById(d.product.productId)
                        if (fullProduct != null) {
                            ImportCartItem(
                                product = fullProduct,
                                quantity = d.detail.quantity,
                                price = d.detail.unitPrice
                            )
                        } else null
                    }
                    _cartItems.value = items
                    onLoaded(true)
                } else {
                    onLoaded(false)
                }
            } catch (e: Exception) {
                onLoaded(false)
            }
        }
    }
}


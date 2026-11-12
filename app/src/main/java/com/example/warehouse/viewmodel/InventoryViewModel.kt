package com.example.warehouse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.warehouse.data.database.entity.Category
import com.example.warehouse.data.database.entity.StockCheckDetail
import com.example.warehouse.data.database.entity.User
import com.example.warehouse.data.database.relation.InventoryCheckWithDetails
import com.example.warehouse.data.database.relation.ProductWithCategoryAndSupplier
import com.example.warehouse.data.repository.WarehouseRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

enum class StockCheckScope {
    ALL,
    CATEGORY,
    MANUAL
}

class InventoryViewModel(private val repository: WarehouseRepository) : ViewModel() {

    // --- All Stock Checks from DB ---
    val allInventoryChecks: StateFlow<List<InventoryCheckWithDetails>> = repository.getAllInventoryChecks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // --- Filters for Stock Check List ---
    private val _statusFilter = MutableStateFlow("ALL") // "ALL", "Đang tiến hành", "Đang chờ xét duyệt", "Đã hoàn thành", "Đã hủy"
    val statusFilter: StateFlow<String> = _statusFilter.asStateFlow()

    private val _dateRangeFilter = MutableStateFlow<Pair<Long?, Long?>>(Pair(null, null)) // startDate, endDate
    val dateRangeFilter: StateFlow<Pair<Long?, Long?>> = _dateRangeFilter.asStateFlow()

    private val _creatorFilter = MutableStateFlow<Long?>(null) // userId
    val creatorFilter: StateFlow<Long?> = _creatorFilter.asStateFlow()

    val filteredChecks: StateFlow<List<InventoryCheckWithDetails>> = combine(
        allInventoryChecks,
        _statusFilter,
        _dateRangeFilter,
        _creatorFilter
    ) { checks, status, dateRange, creatorId ->
        checks.filter { item ->
            // Status match
            val matchesStatus = if (status == "ALL") true else item.check.status == status

            // Date match
            val (start, end) = dateRange
            val matchesStart = if (start != null) item.check.checkDate >= start else true
            val matchesEnd = if (end != null) item.check.checkDate <= (end + 86399999L) else true

            // Creator match
            val matchesCreator = if (creatorId != null) item.check.userId == creatorId else true

            matchesStatus && matchesStart && matchesEnd && matchesCreator
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setStatusFilter(status: String) {
        _statusFilter.value = status
    }

    fun setDateRangeFilter(start: Long?, end: Long?) {
        _dateRangeFilter.value = Pair(start, end)
    }

    fun setCreatorFilter(userId: Long?) {
        _creatorFilter.value = userId
    }

    fun clearFilters() {
        _statusFilter.value = "ALL"
        _dateRangeFilter.value = Pair(null, null)
        _creatorFilter.value = null
    }

    // --- Product & Category Data for Creation ---
    val categories: StateFlow<List<Category>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeProducts: StateFlow<List<ProductWithCategoryAndSupplier>> = repository.getAllProductsWithDetails()
        .map { list -> list.filter { it.product.status.equals("ACTIVE", ignoreCase = true) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val users: StateFlow<List<User>> = repository.getAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Create Form State ---
    private val _createTitle = MutableStateFlow("")
    val createTitle: StateFlow<String> = _createTitle.asStateFlow()

    private val _createNote = MutableStateFlow("")
    val createNote: StateFlow<String> = _createNote.asStateFlow()

    private val _createCheckDate = MutableStateFlow(System.currentTimeMillis())
    val createCheckDate: StateFlow<Long> = _createCheckDate.asStateFlow()

    private val _createScope = MutableStateFlow(StockCheckScope.ALL)
    val createScope: StateFlow<StockCheckScope> = _createScope.asStateFlow()

    private val _selectedCategoryIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedCategoryIds: StateFlow<Set<Long>> = _selectedCategoryIds.asStateFlow()

    private val _selectedProductIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedProductIds: StateFlow<Set<Long>> = _selectedProductIds.asStateFlow()

    fun setCreateTitle(title: String) { _createTitle.value = title }
    fun setCreateNote(note: String) { _createNote.value = note }
    
    fun setCreateCheckDate(dateMillis: Long) {
        val todayEnd = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        // Do not allow future dates
        if (dateMillis <= todayEnd) {
            _createCheckDate.value = dateMillis
        }
    }

    fun setCreateScope(scope: StockCheckScope) { _createScope.value = scope }

    fun toggleCategorySelection(catId: Long) {
        val current = _selectedCategoryIds.value.toMutableSet()
        if (current.contains(catId)) current.remove(catId) else current.add(catId)
        _selectedCategoryIds.value = current
    }

    fun selectAllCategories() {
        _selectedCategoryIds.value = categories.value.map { it.categoryId }.toSet()
    }

    fun clearCategorySelection() {
        _selectedCategoryIds.value = emptySet()
    }

    fun toggleProductSelection(prodId: Long) {
        val current = _selectedProductIds.value.toMutableSet()
        if (current.contains(prodId)) current.remove(prodId) else current.add(prodId)
        _selectedProductIds.value = current
    }

    fun addAllProductsToManualScope() {
        _selectedProductIds.value = activeProducts.value.map { it.product.id }.toSet()
    }

    fun clearManualScopeProducts() {
        _selectedProductIds.value = emptySet()
    }

    fun resetCreateForm() {
        _createTitle.value = ""
        _createNote.value = ""
        _createCheckDate.value = System.currentTimeMillis()
        _createScope.value = StockCheckScope.ALL
        _selectedCategoryIds.value = emptySet()
        _selectedProductIds.value = emptySet()
    }

    // --- Resolve Product IDs based on Scope ---
    fun resolveProductIdsForScope(): List<Long> {
        val allProds = activeProducts.value
        return when (_createScope.value) {
            StockCheckScope.ALL -> allProds.map { it.product.id }
            StockCheckScope.CATEGORY -> {
                val cats = _selectedCategoryIds.value
                allProds.filter { cats.contains(it.product.categoryId) }.map { it.product.id }
            }
            StockCheckScope.MANUAL -> {
                _selectedProductIds.value.toList()
            }
        }
    }

    fun createStockCheck(
        userId: Long?,
        actingUserRole: String? = null,
        onComplete: (Boolean, String, Long?) -> Unit
    ) {
        if (!com.example.warehouse.utils.PermissionManager.canManageStockCheckSession(actingUserRole)) {
            onComplete(false, "Bạn không có quyền thực hiện thao tác này", null)
            return
        }
        val title = _createTitle.value.trim()
        if (title.isEmpty()) {
            onComplete(false, "Vui lòng nhập tiêu đề kiểm kê", null)
            return
        }

        val targetProductIds = resolveProductIdsForScope()
        if (targetProductIds.isEmpty()) {
            onComplete(false, "Không có sản phẩm nào trong phạm vi kiểm kê đã chọn", null)
            return
        }

        viewModelScope.launch {
            try {
                val checkId = repository.createStockCheck(
                    title = title,
                    note = _createNote.value.trim(),
                    checkDate = _createCheckDate.value,
                    userId = userId,
                    productIds = targetProductIds
                )
                resetCreateForm()
                onComplete(true, "Tạo kiểm kê thành công", checkId)
            } catch (e: Exception) {
                onComplete(false, "Lỗi: ${e.message}", null)
            }
        }
    }

    // --- Detail & Counting Actions ---
    suspend fun getInventoryCheckById(id: Long): InventoryCheckWithDetails? {
        return repository.getInventoryCheckById(id)
    }

    fun updateStockCheckHeaderAndProducts(
        checkId: Long,
        title: String,
        note: String,
        checkDate: Long,
        productIds: List<Long>,
        actingUserRole: String? = null,
        onResult: (Boolean, String) -> Unit
    ) {
        if (!com.example.warehouse.utils.PermissionManager.canManageStockCheckSession(actingUserRole)) {
            onResult(false, "Bạn không có quyền thực hiện thao tác này")
            return
        }
        if (title.trim().isEmpty()) {
            onResult(false, "Tiêu đề không được để trống")
            return
        }
        if (productIds.isEmpty()) {
            onResult(false, "Phạm vi phải có ít nhất 1 sản phẩm")
            return
        }

        viewModelScope.launch {
            try {
                repository.updateStockCheckHeaderAndProducts(
                    checkId = checkId,
                    title = title.trim(),
                    note = note.trim(),
                    checkDate = checkDate,
                    productIds = productIds
                )
                onResult(true, "Cập nhật thông tin kiểm kê thành công")
            } catch (e: Exception) {
                onResult(false, "Lỗi: ${e.message}")
            }
        }
    }

    fun saveActualQuantities(
        checkId: Long,
        updatedDetails: List<StockCheckDetail>,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.saveActualQuantities(checkId, updatedDetails)
                onResult(true, "Lưu tạm kết quả kiểm kê thành công")
            } catch (e: Exception) {
                onResult(false, "Lỗi: ${e.message}")
            }
        }
    }

    fun submitForApproval(
        checkId: Long,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.submitForApproval(checkId)
                onResult(true, "Đã gửi kiểm kê để xét duyệt")
            } catch (e: Exception) {
                onResult(false, e.message ?: "Lỗi khi gửi xét duyệt")
            }
        }
    }

    fun returnToDraft(
        checkId: Long,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.returnToDraft(checkId)
                onResult(true, "Đã trả kiểm kê về trạng thái 'Đang tiến hành'")
            } catch (e: Exception) {
                onResult(false, e.message ?: "Lỗi khi trả kiểm kê")
            }
        }
    }

    fun rejectStockCheck(
        checkId: Long,
        reason: String,
        actingUserRole: String? = null,
        onResult: (Boolean, String) -> Unit
    ) {
        if (reason.trim().isEmpty()) {
            onResult(false, "Vui lòng nhập lý do từ chối!")
            return
        }
        if (actingUserRole != null && !com.example.warehouse.utils.PermissionManager.canApproveStockCheck(actingUserRole)) {
            onResult(false, "Bạn không có quyền thực hiện thao tác này")
            return
        }
        viewModelScope.launch {
            try {
                repository.rejectStockCheck(checkId, reason.trim())
                onResult(true, "Đã từ chối kiểm kê và yêu cầu kiểm kê lại!")
            } catch (e: Exception) {
                onResult(false, e.message ?: "Lỗi khi từ chối kiểm kê")
            }
        }
    }

    fun approveStockCheck(
        checkId: Long,
        actingUserRole: String? = null,
        onResult: (Boolean, String) -> Unit
    ) {
        if (!com.example.warehouse.utils.PermissionManager.canApproveStockCheck(actingUserRole)) {
            onResult(false, "Bạn không có quyền thực hiện thao tác này")
            return
        }
        viewModelScope.launch {
            try {
                repository.approveStockCheck(checkId)
                onResult(true, "Duyệt thành công! Tồn kho thực tế đã được cập nhật.")
            } catch (e: Exception) {
                onResult(false, "Lỗi khi duyệt kiểm kê: ${e.message}")
            }
        }
    }

    fun cancelStockCheck(
        checkId: Long,
        deleteReason: String,
        actingUserRole: String? = null,
        onResult: (Boolean, String) -> Unit
    ) {
        if (actingUserRole != null && !com.example.warehouse.utils.PermissionManager.canManageStockCheckSession(actingUserRole)) {
            onResult(false, "Bạn không có quyền thực hiện thao tác này")
            return
        }
        viewModelScope.launch {
            try {
                repository.cancelStockCheck(checkId, deleteReason)
                onResult(true, "Đã hủy kiểm kê")
            } catch (e: Exception) {
                onResult(false, "Lỗi khi hủy: ${e.message}")
            }
        }
    }

    fun deleteStockCheck(
        checkId: Long,
        actingUserRole: String? = null,
        onResult: (Boolean, String) -> Unit
    ) {
        if (!com.example.warehouse.utils.PermissionManager.canManageStockCheckSession(actingUserRole)) {
            onResult(false, "Bạn không có quyền thực hiện thao tác này")
            return
        }
        viewModelScope.launch {
            try {
                repository.deleteStockCheck(checkId)
                onResult(true, "Đã xóa kiểm kê")
            } catch (e: Exception) {
                onResult(false, "Lỗi khi xóa: ${e.message}")
            }
        }
    }

    // --- Warning check helper for Import/Export ---
    suspend fun getActiveChecksForProducts(productIds: List<Long>): List<InventoryCheckWithDetails> {
        return repository.getActiveChecksForProducts(productIds)
    }
}

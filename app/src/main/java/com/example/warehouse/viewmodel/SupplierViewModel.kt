package com.example.warehouse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.warehouse.data.database.entity.Supplier
import com.example.warehouse.data.repository.WarehouseRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SupplierViewModel(private val repository: WarehouseRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val suppliers: StateFlow<List<Supplier>> = searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                repository.getAllSuppliers()
            } else {
                repository.searchSuppliers(query.trim())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun addSupplier(
        supplier: Supplier,
        onSuccess: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val name = supplier.name.trim()
            if (name.isBlank()) {
                onError("Vui lòng nhập tên nhà cung cấp!")
                return@launch
            }
            val count = repository.countSupplierByName(name)
            if (count > 0) {
                onError("Tên nhà cung cấp '$name' đã tồn tại!")
                return@launch
            }
            val generatedCode = if (supplier.supplierCode.isBlank()) {
                "NCC" + System.currentTimeMillis().toString().takeLast(6)
            } else {
                supplier.supplierCode.trim()
            }
            val newSupplier = supplier.copy(
                supplierName = name,
                supplierCode = generatedCode,
                contactPerson = supplier.contactPerson.trim(),
                phone = supplier.phone.trim(),
                email = supplier.email.trim(),
                taxCode = supplier.taxCode.trim(),
                address = supplier.address.trim(),
                note = supplier.note.trim(),
                status = "ACTIVE",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            val id = repository.insertSupplier(newSupplier)
            if (id > 0) {
                onSuccess("Thêm nhà cung cấp '$name' thành công!")
            } else {
                onError("Thêm nhà cung cấp thất bại!")
            }
        }
    }

    fun updateSupplier(
        supplier: Supplier,
        actingUserRole: String? = null,
        onSuccess: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (!com.example.warehouse.utils.PermissionManager.canEditOrDeleteSupplier(actingUserRole)) {
            onError("Bạn không có quyền thực hiện thao tác này")
            return
        }
        viewModelScope.launch {
            val name = supplier.name.trim()
            if (name.isBlank()) {
                onError("Vui lòng nhập tên nhà cung cấp!")
                return@launch
            }
            val count = repository.countSupplierByName(name, excludeId = supplier.supplierId)
            if (count > 0) {
                onError("Tên nhà cung cấp '$name' đã tồn tại!")
                return@launch
            }
            val generatedCode = if (supplier.supplierCode.isBlank()) {
                "NCC" + System.currentTimeMillis().toString().takeLast(6)
            } else {
                supplier.supplierCode.trim()
            }
            val updated = supplier.copy(
                supplierName = name,
                supplierCode = generatedCode,
                contactPerson = supplier.contactPerson.trim(),
                phone = supplier.phone.trim(),
                email = supplier.email.trim(),
                taxCode = supplier.taxCode.trim(),
                address = supplier.address.trim(),
                note = supplier.note.trim(),
                updatedAt = System.currentTimeMillis()
            )
            repository.updateSupplier(updated)
            onSuccess("Cập nhật nhà cung cấp '$name' thành công!")
        }
    }

    fun deleteSupplier(
        supplier: Supplier,
        actingUserRole: String? = null,
        onSuccess: (String) -> Unit = {},
        onDeactivated: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (!com.example.warehouse.utils.PermissionManager.canEditOrDeleteSupplier(actingUserRole)) {
            onError("Bạn không có quyền thực hiện thao tác này")
            return
        }
        viewModelScope.launch {
            try {
                val importCount = repository.getImportReceiptCountBySupplier(supplier.supplierId)
                if (importCount > 0) {
                    val deactivated = supplier.copy(status = "INACTIVE", updatedAt = System.currentTimeMillis())
                    repository.updateSupplier(deactivated)
                    onDeactivated("Nhà cung cấp '${supplier.name}' đã có $importCount phiếu nhập kho nên không thể xóa, đã chuyển về trạng thái Ngừng hoạt động!")
                } else {
                    repository.deleteSupplier(supplier)
                    onSuccess("Xóa nhà cung cấp '${supplier.name}' thành công!")
                }
            } catch (e: Exception) {
                onError("Không thể thực hiện thao tác xóa nhà cung cấp '${supplier.name}'!")
            }
        }
    }
}

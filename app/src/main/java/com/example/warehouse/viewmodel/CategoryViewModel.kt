package com.example.warehouse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.warehouse.data.database.entity.Category
import com.example.warehouse.data.repository.WarehouseRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CategoryViewModel(private val repository: WarehouseRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val categories: StateFlow<List<Category>> = searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                repository.getAllCategories()
            } else {
                repository.searchCategories(query.trim())
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

    fun addCategory(
        name: String,
        description: String,
        actingUserRole: String? = null,
        onSuccess: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (!com.example.warehouse.utils.PermissionManager.canManageCategory(actingUserRole)) {
            onError("Bạn không có quyền thực hiện thao tác này")
            return
        }
        viewModelScope.launch {
            val trimmedName = name.trim()
            if (trimmedName.isBlank()) {
                onError("Vui lòng nhập tên danh mục!")
                return@launch
            }
            val count = repository.countCategoryByName(trimmedName)
            if (count > 0) {
                onError("Tên danh mục '$trimmedName' đã tồn tại!")
                return@launch
            }
            val id = repository.insertCategory(
                Category(
                    categoryName = trimmedName,
                    description = description.trim(),
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )
            if (id > 0) {
                onSuccess("Thêm danh mục '$trimmedName' thành công!")
            } else {
                onError("Thêm danh mục thất bại!")
            }
        }
    }

    fun updateCategory(
        category: Category,
        newName: String,
        newDescription: String,
        actingUserRole: String? = null,
        onSuccess: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (!com.example.warehouse.utils.PermissionManager.canManageCategory(actingUserRole)) {
            onError("Bạn không có quyền thực hiện thao tác này")
            return
        }
        viewModelScope.launch {
            val trimmedName = newName.trim()
            if (trimmedName.isBlank()) {
                onError("Vui lòng nhập tên danh mục!")
                return@launch
            }
            val count = repository.countCategoryByName(trimmedName, excludeId = category.categoryId)
            if (count > 0) {
                onError("Tên danh mục '$trimmedName' đã tồn tại!")
                return@launch
            }
            repository.updateCategory(
                category.copy(
                    categoryName = trimmedName,
                    description = newDescription.trim(),
                    updatedAt = System.currentTimeMillis()
                )
            )
            onSuccess("Cập nhật danh mục '$trimmedName' thành công!")
        }
    }

    fun deleteCategory(
        category: Category,
        actingUserRole: String? = null,
        onSuccess: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (!com.example.warehouse.utils.PermissionManager.canManageCategory(actingUserRole)) {
            onError("Bạn không có quyền thực hiện thao tác này")
            return
        }
        viewModelScope.launch {
            try {
                val activeProductCount = repository.getActiveProductCountByCategory(category.categoryId)
                if (activeProductCount > 0) {
                    onError("Danh mục '${category.categoryName}' đang có $activeProductCount sản phẩm, không thể xóa!")
                    return@launch
                }
                repository.deleteCategory(category)
                onSuccess("Xóa danh mục '${category.categoryName}' thành công!")
            } catch (e: Exception) {
                onError("Không thể xóa danh mục '${category.categoryName}'!")
            }
        }
    }
}

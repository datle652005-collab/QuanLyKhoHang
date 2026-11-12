package com.example.warehouse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.warehouse.data.database.entity.StockHistory
import com.example.warehouse.data.database.relation.ProductWithCategoryAndSupplier
import com.example.warehouse.data.repository.WarehouseRepository
import com.example.warehouse.utils.DateUtils
import kotlinx.coroutines.flow.*

data class DashboardUiState(
    val totalProducts: Int = 0,
    val totalStockQty: Int = 0,
    val todayImportsCount: Int = 0,
    val todayExportsCount: Int = 0,
    val lowStockCount: Int = 0,
    val lowStockProducts: List<ProductWithCategoryAndSupplier> = emptyList(),
    val recentStockHistory: List<StockHistory> = emptyList()
)

class DashboardViewModel(private val repository: WarehouseRepository) : ViewModel() {

    private val startOfDay = DateUtils.getStartOfDay()

    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<DashboardUiState> = combine(
        repository.getProductCount(),
        repository.getTotalStockQuantity(),
        repository.getTodayImportCount(startOfDay),
        repository.getTodayExportCount(startOfDay),
        repository.getLowStockCount(),
        repository.getLowStockProducts(),
        repository.getAllStockHistory().map { it.take(5) }
    ) { args: Array<Any> ->
        DashboardUiState(
            totalProducts = args[0] as Int,
            totalStockQty = args[1] as Int,
            todayImportsCount = args[2] as Int,
            todayExportsCount = args[3] as Int,
            lowStockCount = args[4] as Int,
            lowStockProducts = args[5] as List<ProductWithCategoryAndSupplier>,
            recentStockHistory = args[6] as List<StockHistory>
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )
}

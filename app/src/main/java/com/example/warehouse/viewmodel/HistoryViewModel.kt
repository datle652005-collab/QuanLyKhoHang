package com.example.warehouse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.warehouse.data.database.entity.Product
import com.example.warehouse.data.database.entity.StockHistory
import com.example.warehouse.data.repository.WarehouseRepository
import kotlinx.coroutines.flow.*

class HistoryViewModel(private val repository: WarehouseRepository) : ViewModel() {

    private val _selectedProductId = MutableStateFlow<Long?>(null)
    val selectedProductId: StateFlow<Long?> = _selectedProductId.asStateFlow()

    private val _actionFilter = MutableStateFlow<String?>(null)
    val actionFilter: StateFlow<String?> = _actionFilter.asStateFlow()

    val products: StateFlow<List<Product>> = repository.getAllProductsWithDetails()
        .map { list -> list.map { it.product } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val stockHistory: StateFlow<List<StockHistory>> = combine(
        _selectedProductId,
        _actionFilter
    ) { productId, action ->
        Pair(productId, action)
    }.flatMapLatest { (productId, action) ->
        if (productId != null) {
            repository.getStockHistoryByProduct(productId)
        } else {
            repository.getAllStockHistory()
        }.map { list ->
            if (action != null) list.filter { it.action == action } else list
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun selectProductFilter(productId: Long?) {
        _selectedProductId.value = productId
    }

    fun selectActionFilter(action: String?) {
        _actionFilter.value = action
    }
}

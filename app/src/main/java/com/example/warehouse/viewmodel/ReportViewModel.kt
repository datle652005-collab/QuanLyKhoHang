package com.example.warehouse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.warehouse.data.database.dao.CategoryStockStat
import com.example.warehouse.data.database.entity.Category
import com.example.warehouse.data.database.relation.ProductWithCategoryAndSupplier
import com.example.warehouse.data.repository.WarehouseRepository
import com.example.warehouse.utils.DateUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.util.Calendar
import com.example.warehouse.data.database.dao.TopProductStat

enum class DateRangeType(val label: String) {
    TODAY("Hôm nay"),
    WEEK("7 ngày qua"),
    MONTH("Tháng này"),
    CUSTOM("Tùy chỉnh")
}

data class CombinedTimelineStat(
    val dateStr: String,
    val importQty: Int,
    val exportQty: Int
)

data class ReportUiState(
    // 2.1 Tổng tồn kho
    val totalStockQuantity: Int = 0,
    val todayImportQuantity: Int = 0,
    val todayExportQuantity: Int = 0,
    val categoryStockStats: List<CategoryStockStat> = emptyList(),

    // 2.2 Thống kê nhập/xuất theo thời gian
    val dateRangeType: DateRangeType = DateRangeType.MONTH,
    val customStartDate: Long = System.currentTimeMillis(),
    val customEndDate: Long = System.currentTimeMillis(),
    val effectiveStartTime: Long = 0L,
    val effectiveEndTime: Long = 0L,
    val totalImportQuantity: Int = 0,
    val totalExportQuantity: Int = 0,
    val timelineStats: List<CombinedTimelineStat> = emptyList(),

    // 2.3 Top sản phẩm nhập / xuất
    val topImportedProducts: List<TopProductStat> = emptyList(),
    val topExportedProducts: List<TopProductStat> = emptyList(),

    // 2.4 Cảnh báo thiếu hàng
    val selectedCategoryId: Long? = null,
    val lowStockProducts: List<ProductWithCategoryAndSupplier> = emptyList(),
    val categoriesList: List<Category> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
class ReportViewModel(private val repository: WarehouseRepository) : ViewModel() {

    private val _dateRangeType = MutableStateFlow(DateRangeType.MONTH)
    private val _customStartDate = MutableStateFlow(getStartOfMonth())
    private val _customEndDate = MutableStateFlow(getEndOfDay())

    private val _selectedCategoryId = MutableStateFlow<Long?>(null)

    // Calculate start & end millis based on selected DateRangeType
    private val dateRangeParams: Flow<Pair<Long, Long>> = combine(
        _dateRangeType,
        _customStartDate,
        _customEndDate
    ) { type, customStart, customEnd ->
        calculateMillisRange(type, customStart, customEnd)
    }

    private val startOfDay = DateUtils.getStartOfDay()

    // 2.1 Overall & Category Stock Stats
    private val totalStockQtyFlow = repository.getTotalStockQuantity()
    private val todayImportFlow = repository.getTodayImportCount(startOfDay)
    private val todayExportFlow = repository.getTodayExportCount(startOfDay)
    private val categoryStatsFlow = repository.getCategoryStockStats()

    // 2.2 Time-based Import/Export Stats (DB level calculation)
    private val timeStatsFlow = dateRangeParams.flatMapLatest { (startTime, endTime) ->
        combine(
            repository.getTotalImportQuantityByDateRange(startTime, endTime),
            repository.getDailyImportStats(startTime, endTime),
            repository.getTotalExportQuantityByDateRange(startTime, endTime),
            repository.getDailyExportStats(startTime, endTime)
        ) { totalImport, dailyImports, totalExport, dailyExports ->
            val map = mutableMapOf<String, Pair<Int, Int>>() // dateStr -> Pair(import, export)

            dailyImports.forEach {
                val current = map[it.dateStr] ?: Pair(0, 0)
                map[it.dateStr] = Pair(it.totalQty, current.second)
            }

            dailyExports.forEach {
                val current = map[it.dateStr] ?: Pair(0, 0)
                map[it.dateStr] = Pair(current.first, it.totalQty)
            }

            val timeline = map.entries
                .map { CombinedTimelineStat(it.key, it.value.first, it.value.second) }
                .sortedBy { it.dateStr }

            Triple(totalImport, totalExport, timeline)
        }
    }

    // Top Products
    private val topProductsFlow = dateRangeParams.flatMapLatest { (startTime, endTime) ->
        combine(
            repository.getTopImportedProducts(startTime, endTime, 10),
            repository.getTopExportedProducts(startTime, endTime, 10)
        ) { topImp, topExp ->
            Pair(topImp, topExp)
        }
    }

    // 2.3 Low Stock Products
    private val lowStockFlow = _selectedCategoryId.flatMapLatest { catId ->
        repository.getLowStockProductsFiltered(catId)
    }

    private val categoriesFlow = repository.getAllCategories()

    val reportState: StateFlow<ReportUiState> = combine(
        combine(totalStockQtyFlow, todayImportFlow, todayExportFlow, categoryStatsFlow) { qty, tImp, tExp, stats ->
            Quad(qty, tImp, tExp, stats)
        },
        timeStatsFlow,
        topProductsFlow,
        combine(_dateRangeType, _customStartDate, _customEndDate, dateRangeParams) { type, cStart, cEnd, range ->
            Quad(type, cStart, cEnd, range)
        },
        combine(_selectedCategoryId, lowStockFlow, categoriesFlow) { catId, lowList, cats ->
            Triple(catId, lowList, cats)
        }
    ) { p1, p2, pTop, p3, p4 ->
        val (totalQty, todayImp, todayExp, categoryStats) = p1
        val (totalImport, totalExport, timeline) = p2
        val (topImp, topExp) = pTop
        val (dateType, customStart, customEnd, range) = p3
        val (selectedCatId, lowStockList, cats) = p4

        ReportUiState(
            totalStockQuantity = totalQty,
            todayImportQuantity = todayImp,
            todayExportQuantity = todayExp,
            categoryStockStats = categoryStats,
            dateRangeType = dateType,
            customStartDate = customStart,
            customEndDate = customEnd,
            effectiveStartTime = range.first,
            effectiveEndTime = range.second,
            totalImportQuantity = totalImport,
            totalExportQuantity = totalExport,
            timelineStats = timeline,
            topImportedProducts = topImp,
            topExportedProducts = topExp,
            selectedCategoryId = selectedCatId,
            lowStockProducts = lowStockList,
            categoriesList = cats
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReportUiState()
    )

    fun setDateRangeType(type: DateRangeType) {
        _dateRangeType.value = type
    }

    fun setCustomDateRange(startMillis: Long, endMillis: Long) {
        _customStartDate.value = getStartOfDayMillis(startMillis)
        _customEndDate.value = getEndOfDayMillis(endMillis)
        _dateRangeType.value = DateRangeType.CUSTOM
    }

    fun setSelectedCategoryFilter(categoryId: Long?) {
        _selectedCategoryId.value = if (categoryId == 0L) null else categoryId
    }

    companion object {
        private fun getStartOfDay(): Long = getStartOfDayMillis(System.currentTimeMillis())
        private fun getEndOfDay(): Long = getEndOfDayMillis(System.currentTimeMillis())

        private fun getStartOfMonth(): Long {
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

        private fun getStartOfDayMillis(timeMillis: Long): Long {
            val cal = Calendar.getInstance()
            cal.timeInMillis = timeMillis
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

        private fun getEndOfDayMillis(timeMillis: Long): Long {
            val cal = Calendar.getInstance()
            cal.timeInMillis = timeMillis
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            return cal.timeInMillis
        }

        private fun calculateMillisRange(
            type: DateRangeType,
            customStart: Long,
            customEnd: Long
        ): Pair<Long, Long> {
            val now = System.currentTimeMillis()
            val endOfDay = getEndOfDayMillis(now)

            return when (type) {
                DateRangeType.TODAY -> Pair(getStartOfDayMillis(now), endOfDay)
                DateRangeType.WEEK -> Pair(getStartOfDayMillis(now - 6 * 24 * 60 * 60 * 1000L), endOfDay)
                DateRangeType.MONTH -> Pair(getStartOfMonth(), endOfDay)
                DateRangeType.CUSTOM -> Pair(getStartOfDayMillis(customStart), getEndOfDayMillis(customEnd))
            }
        }
    }
}

private data class Quad<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

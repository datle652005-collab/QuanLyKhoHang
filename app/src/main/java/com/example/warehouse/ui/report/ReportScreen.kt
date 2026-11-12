package com.example.warehouse.ui.report

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.warehouse.data.database.dao.CategoryStockStat
import com.example.warehouse.data.database.dao.TopProductStat
import com.example.warehouse.data.database.entity.Category
import com.example.warehouse.data.database.relation.ProductWithCategoryAndSupplier
import com.example.warehouse.ui.theme.PrimaryBlue
import com.example.warehouse.ui.theme.PrimaryBlueLight
import com.example.warehouse.utils.DateUtils
import com.example.warehouse.viewmodel.CombinedTimelineStat
import com.example.warehouse.viewmodel.DateRangeType
import com.example.warehouse.viewmodel.ReportViewModel
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    viewModel: ReportViewModel,
    userRole: String? = null,
    onNavigateToProductDetail: (Long) -> Unit
) {
    val state by viewModel.reportState.collectAsState()

    var showCustomDatePickerDialog by remember { mutableStateOf(false) }

    val numberFormatter = remember { NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")) }

    fun formatNumber(value: Int): String {
        return try {
            numberFormatter.format(value)
        } catch (e: Exception) {
            "$value"
        }
    }

    if (!com.example.warehouse.utils.PermissionManager.canViewFullReportModule(userRole)) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Bạn không có quyền truy cập module này.",
                color = Color.Gray,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {

        // ═══════════════════════════════════════
        // 1. TỔNG QUAN TỒN KHO HÔM NAY
        // ═══════════════════════════════════════
        item {
            com.example.warehouse.ui.component.TodayStockOverviewCard(
                totalStockQty = state.totalStockQuantity,
                todayImportQty = state.todayImportQuantity,
                todayExportQty = state.todayExportQuantity
            )
        }

        // ═══════════════════════════════════════
        // 2. TỒN KHO THEO DANH MỤC (Capped 5 items with scroll)
        // ═══════════════════════════════════════
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Tồn kho theo danh mục",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (state.categoryStockStats.isEmpty()) {
                        Text(
                            text = "Chưa có dữ liệu danh mục",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            state.categoryStockStats.forEachIndexed { index, catStat ->
                                CategoryStockRow(
                                    catStat = catStat,
                                    totalStockAll = state.totalStockQuantity,
                                    formatNumber = ::formatNumber
                                )
                                if (index < state.categoryStockStats.size - 1) {
                                    HorizontalDivider(
                                        color = Color(0xFFF1F5F9),
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ═══════════════════════════════════════
        // 3. THỐNG KÊ NHẬP/XUẤT THEO THỜI GIAN
        // ═══════════════════════════════════════
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Thống kê Nhập / Xuất theo thời gian",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Filter khoảng thời gian
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(DateRangeType.values()) { type ->
                            val isSelected = state.dateRangeType == type
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (type == DateRangeType.CUSTOM) {
                                        showCustomDatePickerDialog = true
                                    } else {
                                        viewModel.setDateRangeType(type)
                                    }
                                },
                                label = { Text(type.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryBlue,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    if (state.dateRangeType == DateRangeType.CUSTOM) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFEFF6FF))
                                .clickable { showCustomDatePickerDialog = true }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val startStr = DateUtils.formatDateOnly(state.effectiveStartTime)
                            val endStr = DateUtils.formatDateOnly(state.effectiveEndTime)
                            Text(
                                text = "Từ ngày: $startStr  →  Đến ngày: $endStr",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = PrimaryBlue
                            )
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = "Chọn ngày",
                                tint = PrimaryBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Thẻ tổng quan nhập / xuất trong khoảng thời gian
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Nhập kho
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.South,
                                        contentDescription = null,
                                        tint = PrimaryBlue,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Đã nhập",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = PrimaryBlue,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = formatNumber(state.totalImportQuantity),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PrimaryBlue
                                )
                            }
                        }

                        // Xuất kho
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.North,
                                        contentDescription = null,
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Đã xuất",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFEF4444),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = formatNumber(state.totalExportQuantity),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFEF4444)
                                )
                            }
                        }

                        // Chênh lệch
                        val netDiff = state.totalImportQuantity - state.totalExportQuantity
                        val diffColor = if (netDiff >= 0) Color(0xFF10B981) else Color(0xFFF59E0B)
                        val diffBg = if (netDiff >= 0) Color(0xFFECFDF5) else Color(0xFFFFFBEB)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = diffBg),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Chênh lệch",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = diffColor,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = (if (netDiff > 0) "+" else "") + formatNumber(netDiff),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = diffColor
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Bảng chi tiết nhập vs xuất
                    Text(
                        text = "Chi tiết nhập vs xuất theo mốc thời gian",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (state.timelineStats.isEmpty()) {
                        Text(
                            text = "Không có giao dịch nhập/xuất nào trong khoảng thời gian này.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        // Table Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF1F5F9), shape = RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Ngày",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1.2f)
                            )
                            Text(
                                text = "Nhập",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "Xuất",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF4444),
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "Tăng/Giảm",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        val maxQtyInTimeline = state.timelineStats.maxOf { maxOf(it.importQty, it.exportQty) }.coerceAtLeast(1)

                        state.timelineStats.forEach { stat ->
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stat.dateStr,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1.2f)
                                    )
                                    Text(
                                        text = formatNumber(stat.importQty),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryBlue,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = formatNumber(stat.exportQty),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFEF4444),
                                        modifier = Modifier.weight(1f)
                                    )
                                    val diff = stat.importQty - stat.exportQty
                                    Text(
                                        text = (if (diff > 0) "+" else "") + formatNumber(diff),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (diff >= 0) Color(0xFF10B981) else Color(0xFFF59E0B),
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp)
                                        .padding(bottom = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val importRatio = stat.importQty.toFloat() / maxQtyInTimeline
                                    val exportRatio = stat.exportQty.toFloat() / maxQtyInTimeline

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(4.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFE2E8F0))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(importRatio)
                                                .background(PrimaryBlue)
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(4.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFE2E8F0))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(exportRatio)
                                                .background(Color(0xFFEF4444))
                                        )
                                    }
                                }

                                HorizontalDivider(color = Color(0xFFF1F5F9))
                            }
                        }
                    }
                }
            }
        }

        // ═══════════════════════════════════════
        // 4. TOP SẢN PHẨM NHẬP NHIỀU VÀ XUẤT NHIỀU
        // ═══════════════════════════════════════
        item {
            TopProductsCard(
                topImported = state.topImportedProducts,
                topExported = state.topExportedProducts,
                formatNumber = ::formatNumber
            )
        }

        // ═══════════════════════════════════════
        // 5. Sản phẩm SẮP HẾT / HẾT HÀNG
        // ═══════════════════════════════════════
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Sản phẩm sắp hết / Hết hàng",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }

                        Surface(
                            color = Color(0xFFFEF2F2),
                            shape = CircleShape
                        ) {
                            Text(
                                text = "${state.lowStockProducts.size} sản phẩm",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF4444),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Lọc theo Danh mục (Combobox)
                    var categoryDropdownExpanded by remember { mutableStateOf(false) }

                    ExposedDropdownMenuBox(
                        expanded = categoryDropdownExpanded,
                        onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val selectedCatText = if (state.selectedCategoryId == null) {
                            "Tất cả danh mục"
                        } else {
                            state.categoriesList.find { it.categoryId == state.selectedCategoryId }?.categoryName ?: "Tất cả danh mục"
                        }

                        OutlinedTextField(
                            value = selectedCatText,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Xem theo danh mục") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded)
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryBlue,
                                unfocusedBorderColor = Color(0xFFCBD5E1),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = categoryDropdownExpanded,
                            onDismissRequest = { categoryDropdownExpanded = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Tất cả danh mục",
                                        fontWeight = if (state.selectedCategoryId == null) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    viewModel.setSelectedCategoryFilter(null)
                                    categoryDropdownExpanded = false
                                }
                            )
                            state.categoriesList.forEach { cat ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = cat.categoryName,
                                            fontWeight = if (state.selectedCategoryId == cat.categoryId) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        viewModel.setSelectedCategoryFilter(cat.categoryId)
                                        categoryDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (state.lowStockProducts.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Không có sản phẩm nào chạm ngưỡng thiếu hàng!",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF334155)
                            )
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            state.lowStockProducts.forEach { item ->
                                LowStockItemCard(
                                    item = item,
                                    onClick = { onNavigateToProductDetail(item.product.productId) },
                                    formatNumber = ::formatNumber
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Custom Date Range Picker Dialog
    if (showCustomDatePickerDialog) {
        CustomDateRangePickerDialog(
            initialStart = state.customStartDate,
            initialEnd = state.customEndDate,
            onDismiss = { showCustomDatePickerDialog = false },
            onConfirm = { startMillis, endMillis ->
                viewModel.setCustomDateRange(startMillis, endMillis)
                showCustomDatePickerDialog = false
            }
        )
    }
}

@Composable
private fun TopProductsCard(
    topImported: List<TopProductStat>,
    topExported: List<TopProductStat>,
    formatNumber: (Int) -> String
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Nhập nhiều, 1: Xuất nhiều

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Top sản phẩm nhập & xuất nhiều",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Selector tabs between Top Nhập nhiều & Top Xuất nhiều
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF1F5F9))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Tab 0: Top Nhập nhiều
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selectedTab == 0) PrimaryBlue else Color.Transparent)
                        .clickable { selectedTab = 0 }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.South,
                            contentDescription = null,
                            tint = if (selectedTab == 0) Color.White else Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Top Nhập nhiều",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == 0) Color.White else Color(0xFF64748B)
                        )
                    }
                }

                // Tab 1: Top Xuất nhiều
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selectedTab == 1) Color(0xFFEF4444) else Color.Transparent)
                        .clickable { selectedTab = 1 }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.North,
                            contentDescription = null,
                            tint = if (selectedTab == 1) Color.White else Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Top Xuất nhiều",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == 1) Color.White else Color(0xFF64748B)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val currentList = if (selectedTab == 0) topImported else topExported

            if (currentList.isEmpty()) {
                Text(
                    text = if (selectedTab == 0) "Chưa có dữ liệu nhập hàng trong khoảng thời gian này." else "Chưa có dữ liệu xuất hàng trong khoảng thời gian này.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF1F5F9), shape = RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mã",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155),
                        modifier = Modifier.weight(0.9f)
                    )
                    Text(
                        text = "Tên",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155),
                        modifier = Modifier.weight(2f)
                    )
                    Text(
                        text = "SL",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedTab == 0) PrimaryBlue else Color(0xFFEF4444),
                        modifier = Modifier.weight(0.8f),
                        textAlign = TextAlign.End
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                currentList.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.productCode,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B),
                            modifier = Modifier.weight(0.9f)
                        )
                        Text(
                            text = item.productName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            modifier = Modifier.weight(2f)
                        )
                        Text(
                            text = formatNumber(item.totalQty),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (selectedTab == 0) PrimaryBlue else Color(0xFFEF4444),
                            modifier = Modifier.weight(0.8f),
                            textAlign = TextAlign.End
                        )
                    }
                    if (index < currentList.size - 1) {
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryStockRow(
    catStat: CategoryStockStat,
    totalStockAll: Int,
    formatNumber: (Int) -> String
) {
    val ratio = if (totalStockAll > 0) catStat.totalQuantity.toFloat() / totalStockAll else 0f

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = catStat.categoryName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${catStat.productCount} sản phẩm thuộc danh mục",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )
            }

            Text(
                text = formatNumber(catStat.totalQuantity),
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = PrimaryBlue
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Progress bar proportion
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(Color(0xFFE2E8F0))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(ratio.coerceIn(0f, 1f))
                    .background(PrimaryBlue)
            )
        }
    }
}

@Composable
private fun LowStockItemCard(
    item: ProductWithCategoryAndSupplier,
    onClick: () -> Unit,
    formatNumber: (Int) -> String
) {
    val currentQty = item.quantityInStock
    val minStock = item.product.minStock
    val isOutOfStock = currentQty <= 0

    val badgeBg = if (isOutOfStock) Color(0xFFFEE2E2) else Color(0xFFFEF3C7)
    val badgeTextColor = if (isOutOfStock) Color(0xFFDC2626) else Color(0xFFD97706)
    val badgeText = if (isOutOfStock) "HẾT HÀNG (0)" else "SẮP HẾT (${formatNumber(currentQty)}/${formatNumber(minStock)})"

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isOutOfStock) Color(0xFFFECACA) else Color(0xFFFDE68A)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.product.productName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Mã: ${item.product.productCode} • Danh mục: ${item.category?.categoryName ?: "Chưa phân loại"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Tồn hiện tại: ${formatNumber(currentQty)}  |  Tối thiểu: ${formatNumber(minStock)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF334155)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                color = badgeBg,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = badgeTextColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomDateRangePickerDialog(
    initialStart: Long,
    initialEnd: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long, Long) -> Unit
) {
    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialStart,
        initialSelectedEndDateMillis = initialEnd
    )

    DatePickerDialog(
        colors = DatePickerDefaults.colors(containerColor = Color.White),
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val start = dateRangePickerState.selectedStartDateMillis ?: initialStart
                    val end = dateRangePickerState.selectedEndDateMillis ?: start
                    onConfirm(start, end)
                }
            ) {
                Text("Áp dụng", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    ) {
        DateRangePicker(
            state = dateRangePickerState,
            title = { Text("Chọn khoảng thời gian", modifier = Modifier.padding(16.dp)) },
            headline = {
                val start = dateRangePickerState.selectedStartDateMillis
                val end = dateRangePickerState.selectedEndDateMillis
                val startStr = if (start != null) DateUtils.formatDateOnly(start) else "Từ ngày"
                val endStr = if (end != null) DateUtils.formatDateOnly(end) else "Đến ngày"
                Text("$startStr - $endStr", modifier = Modifier.padding(horizontal = 16.dp))
            },
            showModeToggle = false,
            modifier = Modifier.weight(1f)
        )
    }
}

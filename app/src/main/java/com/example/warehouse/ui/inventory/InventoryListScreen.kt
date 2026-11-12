package com.example.warehouse.ui.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import com.example.warehouse.ui.component.WarehouseEmptyView
import com.example.warehouse.ui.theme.PrimaryBlue
import com.example.warehouse.utils.DateUtils
import com.example.warehouse.viewmodel.InventoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryListScreen(
    viewModel: InventoryViewModel,
    onNavigateToCreate: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    successMessage: String? = null,
    onClearSuccessMessage: () -> Unit = {},
    userRole: String? = null
) {
    val checks by viewModel.filteredChecks.collectAsState()
    val statusFilter by viewModel.statusFilter.collectAsState()
    val dateRangeFilter by viewModel.dateRangeFilter.collectAsState()
    val creatorFilter by viewModel.creatorFilter.collectAsState()
    val users by viewModel.users.collectAsState()

    var showFilterSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(successMessage) {
        if (!successMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(successMessage)
            onClearSuccessMessage()
        }
    }

    val statusList = listOf(
        Pair("ALL", "Tất cả"),
        Pair("Đang tiến hành", "Đang tiến hành"),
        Pair("Yêu cầu kiểm kê lại", "Yêu cầu kiểm kê lại"),
        Pair("Đang chờ xét duyệt", "Chờ xét duyệt"),
        Pair("Đã hoàn thành", "Đã hoàn thành"),
        Pair("Đã hủy", "Đã hủy")
    )

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            if (com.example.warehouse.utils.PermissionManager.canManageStockCheckSession(userRole)) {
                FloatingActionButton(
                    onClick = onNavigateToCreate,
                    containerColor = PrimaryBlue,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("create_inventory_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tạo phiếu kiểm kê")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp)
        ) {
            var statusDropdownExpanded by remember { mutableStateOf(false) }

            // Status Filter Combobox
            ExposedDropdownMenuBox(
                expanded = statusDropdownExpanded,
                onExpandedChange = { statusDropdownExpanded = !statusDropdownExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                val currentStatusLabel = statusList.find { it.first == statusFilter }?.second ?: "Tất cả"
                OutlinedTextField(
                    value = currentStatusLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Trạng thái kiểm kê") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusDropdownExpanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = statusDropdownExpanded,
                    onDismissRequest = { statusDropdownExpanded = false }
                ) {
                    statusList.forEach { (key, label) ->
                        DropdownMenuItem(
                            text = { Text(label, fontWeight = if (statusFilter == key) FontWeight.Bold else FontWeight.Normal) },
                            onClick = {
                                viewModel.setStatusFilter(key)
                                statusDropdownExpanded = false
                            },
                            leadingIcon = {
                                if (statusFilter == key) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = PrimaryBlue)
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (checks.isEmpty()) {
                WarehouseEmptyView(
                    title = "Chưa có kiểm kê nào",
                    message = "Bấm nút '+' để bắt đầu tạo kiểm kê kho mới",
                    icon = Icons.Default.FactCheck,
                    onActionClick = onNavigateToCreate,
                    actionTitle = "Tạo kiểm kê"
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp, top = 4.dp)
                ) {
                    items(checks, key = { it.check.id }) { item ->
                        val check = item.check
                        val totalItems = item.details.size
                        val countedItems = item.details.count { it.detail.actualQuantity != null }

                        val totalDiscrepancy = item.details.sumOf { d ->
                            val diff = d.detail.difference ?: 0
                            kotlin.math.abs(diff)
                        }

                        Card(
                            onClick = { onNavigateToDetail(check.id) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = check.title.ifEmpty { check.code },
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.weight(1f)
                                    )

                                    // Status Badge
                                    val (badgeBg, badgeFg) = when (check.status) {
                                        "Đang tiến hành" -> Pair(Color(0xFFDBEAFE), Color(0xFF1D4ED8))
                                        "Yêu cầu kiểm kê lại" -> Pair(Color(0xFFFEE2E2), Color(0xFFDC2626))
                                        "Đang chờ xét duyệt" -> Pair(Color(0xFFFEF3C7), Color(0xFFD97706))
                                        "Đã hoàn thành" -> Pair(Color(0xFFDCFCE7), Color(0xFF15803D))
                                        "Đã hủy" -> Pair(Color(0xFFF3F4F6), Color(0xFF4B5563))
                                        else -> Pair(Color(0xFFF1F5F9), Color(0xFF64748B))
                                    }

                                    Surface(color = badgeBg, shape = RoundedCornerShape(12.dp)) {
                                        Text(
                                            text = check.status,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = badgeFg,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "Ngày: ${DateUtils.formatDate(check.checkDate)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                    item.user?.let { u ->
                                        Text(
                                            "Tạo bởi: ${u.fullName}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray
                                        )
                                    }
                                }

                                if (check.note.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        "Ghi chú: ${check.note}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Progress Indicator
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Tiến độ: $countedItems/$totalItems sản phẩm",
                                        style = MaterialTheme.typography.bodyMedium
                                    )

                                    Text(
                                        text = if (totalDiscrepancy > 0) "Lệch $totalDiscrepancy SP" else if (countedItems == totalItems) "Khớp 100%" else "Đang đếm...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (totalDiscrepancy > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { if (totalItems > 0) countedItems.toFloat() / totalItems else 0f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = if (countedItems == totalItems) Color(0xFF16A34A) else PrimaryBlue
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

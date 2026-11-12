package com.example.warehouse.ui.export

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.warehouse.ui.component.ExportReceiptCard
import com.example.warehouse.ui.component.WarehouseEmptyView
import com.example.warehouse.utils.DateUtils
import com.example.warehouse.viewmodel.ExportViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportListScreen(
    viewModel: ExportViewModel,
    onNavigateToCreate: () -> Unit,
    onNavigateToDetail: (Long) -> Unit
) {
    val receipts by viewModel.exportReceipts.collectAsState()

    // Filter states
    var filterStatus by remember { mutableStateOf("Đã lưu") } // Default is "Đã lưu", options: "Đã lưu", "Đã hủy", "Tất cả"
    var filterRecipient by remember { mutableStateOf("") }
    var filterCreator by remember { mutableStateOf("") }
    var filterStartDate by remember { mutableStateOf<Long?>(null) }
    var filterEndDate by remember { mutableStateOf<Long?>(null) }

    var showFilterSection by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    // Apply filtering in memory reactively
    val filteredReceipts = remember(receipts, filterStatus, filterRecipient, filterCreator, filterStartDate, filterEndDate) {
        receipts.filter { receiptWithDetails ->
            val receipt = receiptWithDetails.receipt
            val user = receiptWithDetails.user

            // 1. Filter status
            val matchesStatus = when (filterStatus) {
                "Tất cả" -> true
                "Đã lưu" -> receipt.status == "Đã lưu"
                "Đã hủy" -> receipt.status == "Đã hủy" || receipt.status == "Đã xóa"
                else -> true
            }

            // 2. Filter recipient
            val matchesRecipient = if (filterRecipient.isNotBlank()) {
                receipt.recipient.contains(filterRecipient, ignoreCase = true)
            } else true

            // 3. Filter creator
            val matchesCreator = if (filterCreator.isNotBlank()) {
                user?.fullName?.contains(filterCreator, ignoreCase = true) ?: false
            } else true

            // 4. Filter date range (based on export_date, not created_at)
            val matchesStartDate = if (filterStartDate != null) {
                val cal = java.util.Calendar.getInstance()
                cal.timeInMillis = filterStartDate!!
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                receipt.exportDate >= cal.timeInMillis
            } else true

            val matchesEndDate = if (filterEndDate != null) {
                val cal = java.util.Calendar.getInstance()
                cal.timeInMillis = filterEndDate!!
                cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
                cal.set(java.util.Calendar.MINUTE, 59)
                cal.set(java.util.Calendar.SECOND, 59)
                cal.set(java.util.Calendar.MILLISECOND, 999)
                receipt.exportDate <= cal.timeInMillis
            } else true

            matchesStatus && matchesRecipient && matchesCreator && matchesStartDate && matchesEndDate
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreate,
                containerColor = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.testTag("create_export_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tạo phiếu xuất kho")
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
            // Header with filter toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Lịch sử xuất kho",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = { showFilterSection = !showFilterSection },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (showFilterSection) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                    ),
                    modifier = Modifier.testTag("filter_toggle_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Bộ lọc tìm kiếm",
                        tint = if (showFilterSection) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }
            }

            if (showFilterSection) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "BỘ LỌC TÌM KIẾM",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Status Filter Chips
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Trạng thái:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF334155)
                            )
                            listOf("Đã lưu", "Đã hủy", "Tất cả").forEach { status ->
                                val selected = filterStatus == status
                                FilterChip(
                                    selected = selected,
                                    onClick = { filterStatus = status },
                                    label = { Text(status, style = MaterialTheme.typography.bodySmall) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Text fields row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = filterRecipient,
                                onValueChange = { filterRecipient = it },
                                placeholder = { Text("Người nhận...") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("filter_recipient_input")
                            )

                            OutlinedTextField(
                                value = filterCreator,
                                onValueChange = { filterCreator = it },
                                placeholder = { Text("Người lập...") },
                                leadingIcon = { Icon(Icons.Default.AssignmentInd, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("filter_creator_input")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Date Range Selectors
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Start Date Button
                            Surface(
                                color = Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showStartDatePicker = true }
                                    .padding(vertical = 10.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (filterStartDate != null) DateUtils.formatDateOnly(filterStartDate!!) else "Từ ngày",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // End Date Button
                            Surface(
                                color = Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showEndDatePicker = true }
                                    .padding(vertical = 10.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (filterEndDate != null) DateUtils.formatDateOnly(filterEndDate!!) else "Đến ngày",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // Clear Filters Icon Button
                            if (filterStartDate != null || filterEndDate != null || filterRecipient.isNotEmpty() || filterCreator.isNotEmpty() || filterStatus != "Đã lưu") {
                                IconButton(
                                    onClick = {
                                        filterStartDate = null
                                        filterEndDate = null
                                        filterRecipient = ""
                                        filterCreator = ""
                                        filterStatus = "Đã lưu"
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Clear, contentDescription = "Xóa lọc", tint = Color.Red)
                                }
                            }
                        }
                    }
                }
            }

            if (filteredReceipts.isEmpty()) {
                WarehouseEmptyView(
                    title = "Không tìm thấy phiếu xuất",
                    message = "Thử điều chỉnh lại bộ lọc tìm kiếm",
                    icon = Icons.Default.Outbox,
                    onActionClick = {
                        filterStatus = "Tất cả"
                        filterRecipient = ""
                        filterCreator = ""
                        filterStartDate = null
                        filterEndDate = null
                    },
                    actionTitle = "Đặt lại bộ lọc"
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredReceipts, key = { it.receipt.id }) { receiptWithDetails ->
                        ExportReceiptCard(
                            receiptWithDetails = receiptWithDetails,
                            onClick = { onNavigateToDetail(receiptWithDetails.receipt.id) }
                        )
                    }
                }
            }
        }
    }

    // Start Date Picker Dialog
    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = filterStartDate)
        DatePickerDialog(
        colors = DatePickerDefaults.colors(containerColor = Color.White),
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    filterStartDate = datePickerState.selectedDateMillis
                    showStartDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text("Hủy")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // End Date Picker Dialog
    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = filterEndDate)
        DatePickerDialog(
        colors = DatePickerDefaults.colors(containerColor = Color.White),
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    filterEndDate = datePickerState.selectedDateMillis
                    showEndDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text("Hủy")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

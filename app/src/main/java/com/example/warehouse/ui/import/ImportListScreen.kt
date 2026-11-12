package com.example.warehouse.ui.import

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoveToInbox
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.warehouse.ui.component.ImportReceiptCard
import com.example.warehouse.ui.component.WarehouseEmptyView
import com.example.warehouse.utils.DateUtils
import com.example.warehouse.viewmodel.ImportViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportListScreen(
    viewModel: ImportViewModel,
    onNavigateToCreate: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    successMessage: String? = null,
    onClearSuccessMessage: () -> Unit = {}
) {
    val receipts by viewModel.filteredReceipts.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()
    val users by viewModel.users.collectAsState()

    val startDateFilter by viewModel.startDateFilter.collectAsState()
    val endDateFilter by viewModel.endDateFilter.collectAsState()
    val selectedSupplierFilter by viewModel.supplierFilter.collectAsState()
    val selectedUserFilter by viewModel.userFilter.collectAsState()
    val selectedStatusFilter by viewModel.statusFilter.collectAsState()

    var showFilterSheet by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    var activeSnackbarMessage by remember { mutableStateOf<String?>(null) }
    var snackbarJob by remember { mutableStateOf<Job?>(null) }
    val coroutineScope = rememberCoroutineScope()

    fun showSingleSnackbar(message: String) {
        if (activeSnackbarMessage == message && snackbarHostState.currentSnackbarData != null) {
            return
        }
        snackbarJob?.cancel()
        snackbarJob = coroutineScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            activeSnackbarMessage = message
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            activeSnackbarMessage = null
        }
    }

    LaunchedEffect(successMessage) {
        successMessage?.let { msg ->
            showSingleSnackbar(msg)
            onClearSuccessMessage()
        }
    }

    val hasActiveFilters = startDateFilter != null || endDateFilter != null ||
            selectedSupplierFilter != null || selectedUserFilter != null || selectedStatusFilter != "ALL"

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = Color(0xFF1E293B),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreate,
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("create_import_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tạo phiếu nhập kho")
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
            // Status Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = selectedStatusFilter == "ALL",
                    onClick = { viewModel.statusFilter.value = "ALL" },
                    label = { Text("Tất cả") }
                )
                FilterChip(
                    selected = selectedStatusFilter == "Đã lưu",
                    onClick = { viewModel.statusFilter.value = "Đã lưu" },
                    label = { Text("Đã lưu") }
                )
                FilterChip(
                    selected = selectedStatusFilter == "Đã hủy",
                    onClick = { viewModel.statusFilter.value = "Đã hủy" },
                    label = { Text("Đã hủy") }
                )

                IconButton(
                    onClick = { showFilterSheet = true },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (hasActiveFilters) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Icon(
                        Icons.Default.FilterList,
                        contentDescription = "Lọc nâng cao",
                        tint = if (hasActiveFilters) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (hasActiveFilters) {
                    IconButton(onClick = { viewModel.clearFilters() }) {
                        Icon(Icons.Default.Clear, contentDescription = "Xóa bộ lọc", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // Summary indicator if filters are active
            if (hasActiveFilters) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Đang lọc theo ngày nhập kho / NC/ Trạng thái (${receipts.size} kết quả)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        TextButton(
                            onClick = { viewModel.clearFilters() },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Bỏ lọc", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (receipts.isEmpty()) {
                WarehouseEmptyView(
                    title = if (hasActiveFilters) "Không tìm thấy phiếu nhập phù hợp" else "Chưa có phiếu nhập kho",
                    message = if (hasActiveFilters) "Thử thay đổi điều kiện lọc hoặc bấm Bỏ lọc" else "Bấm '+' để tạo phiếu nhập kho đầu tiên",
                    icon = Icons.Default.MoveToInbox,
                    onActionClick = if (hasActiveFilters) { { viewModel.clearFilters() } } else onNavigateToCreate,
                    actionTitle = if (hasActiveFilters) "Bỏ bộ lọc" else "Tạo phiếu nhập"
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(receipts, key = { it.receipt.id }) { receiptWithDetails ->
                        ImportReceiptCard(
                            receiptWithDetails = receiptWithDetails,
                            onClick = { onNavigateToDetail(receiptWithDetails.receipt.id) }
                        )
                    }
                }
            }
        }
    }

    // Filter Bottom Sheet
    if (showFilterSheet) {
        ModalBottomSheet(onDismissRequest = { showFilterSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Lọc phiếu nhập kho", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    TextButton(onClick = {
                        viewModel.clearFilters()
                        showFilterSheet = false
                    }) {
                        Text("Đặt lại")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Filter by import_date (Start Date & End Date)
                Text("Theo ngày nhập kho (import_date):", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { showStartDatePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(startDateFilter?.let { DateUtils.formatDateOnly(it) } ?: "Từ ngày")
                    }

                    OutlinedButton(
                        onClick = { showEndDatePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(endDateFilter?.let { DateUtils.formatDateOnly(it) } ?: "Đến ngày")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Supplier Filter Dropdown
                Text("Nhà cung cấp:", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(6.dp))
                var supplierDropdownExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = supplierDropdownExpanded,
                    onExpandedChange = { supplierDropdownExpanded = !supplierDropdownExpanded }
                ) {
                    val currentSupName = suppliers.find { it.id == selectedSupplierFilter }?.name ?: "Tất cả nhà cung cấp"
                    OutlinedTextField(
                        value = currentSupName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = supplierDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = supplierDropdownExpanded,
                        onDismissRequest = { supplierDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Tất cả nhà cung cấp") },
                            onClick = {
                                viewModel.supplierFilter.value = null
                                supplierDropdownExpanded = false
                            }
                        )
                        suppliers.forEach { sup ->
                            DropdownMenuItem(
                                text = { Text(sup.name) },
                                onClick = {
                                    viewModel.supplierFilter.value = sup.id
                                    supplierDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Creator Filter Dropdown
                Text("Người thực hiện / Lập phiếu:", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(6.dp))
                var userDropdownExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = userDropdownExpanded,
                    onExpandedChange = { userDropdownExpanded = !userDropdownExpanded }
                ) {
                    val currentUserName = users.find { it.id == selectedUserFilter }?.fullName ?: "Tất cả người thực hiện"
                    OutlinedTextField(
                        value = currentUserName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = userDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = userDropdownExpanded,
                        onDismissRequest = { userDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Tất cả người thực hiện") },
                            onClick = {
                                viewModel.userFilter.value = null
                                userDropdownExpanded = false
                            }
                        )
                        users.forEach { u ->
                            DropdownMenuItem(
                                text = { Text(u.fullName) },
                                onClick = {
                                    viewModel.userFilter.value = u.id
                                    userDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { showFilterSheet = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("ÁP DỤNG BỘ LỌC")
                }
            }
        }
    }

    // Start Date Picker
    if (showStartDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = startDateFilter ?: System.currentTimeMillis())
        DatePickerDialog(
        colors = DatePickerDefaults.colors(containerColor = Color.White),
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.startDateFilter.value = pickerState.selectedDateMillis
                    showStartDatePicker = false
                }) {
                    Text("Chọn")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.startDateFilter.value = null
                    showStartDatePicker = false
                }) {
                    Text("Xóa lọc ngày")
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    // End Date Picker
    if (showEndDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = endDateFilter ?: System.currentTimeMillis())
        DatePickerDialog(
        colors = DatePickerDefaults.colors(containerColor = Color.White),
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.endDateFilter.value = pickerState.selectedDateMillis
                    showEndDatePicker = false
                }) {
                    Text("Chọn")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.endDateFilter.value = null
                    showEndDatePicker = false
                }) {
                    Text("Xóa lọc ngày")
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

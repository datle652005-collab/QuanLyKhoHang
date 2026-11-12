package com.example.warehouse.ui.import

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.warehouse.data.database.relation.ImportReceiptWithDetails
import com.example.warehouse.ui.theme.PrimaryBlue
import com.example.warehouse.utils.CurrencyFormatter
import com.example.warehouse.utils.DateUtils
import com.example.warehouse.viewmodel.AuthViewModel
import com.example.warehouse.viewmodel.ImportViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportDetailScreen(
    receiptId: Long,
    viewModel: ImportViewModel,
    authViewModel: AuthViewModel,
    onNavigateToEdit: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateBackWithMsg: ((String) -> Unit)? = null,
    successMessage: String? = null,
    onClearSuccessMessage: () -> Unit = {}
) {
    var receiptWithDetails by remember { mutableStateOf<ImportReceiptWithDetails?>(null) }
    var refreshTrigger by remember { mutableStateOf(0) }

    val currentUser by authViewModel.currentUser.collectAsState()

    var showCancelConfirmDialog by remember { mutableStateOf(false) }
    var isUnderstandChecked by remember { mutableStateOf(false) }
    var deleteReason by remember { mutableStateOf("") }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var activeSnackbarMessage by remember { mutableStateOf<String?>(null) }
    var snackbarJob by remember { mutableStateOf<Job?>(null) }

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

    LaunchedEffect(receiptId, refreshTrigger) {
        receiptWithDetails = viewModel.getImportReceiptById(receiptId)
    }

    LaunchedEffect(successMessage) {
        successMessage?.let { msg ->
            showSingleSnackbar(msg)
            refreshTrigger++
            onClearSuccessMessage()
        }
    }

    val item = receiptWithDetails
    if (item == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val receipt = item.receipt
    val isCancelled = receipt.status.equals("Đã hủy", ignoreCase = true)
    val totalAmount = if (receipt.totalAmount > 0) receipt.totalAmount else item.details.sumOf { it.detail.quantity * it.detail.unitPrice }

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
        bottomBar = {
            if (!isCancelled) {
                Surface(
                    tonalElevation = 8.dp,
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (com.example.warehouse.utils.PermissionManager.canEditOrDeleteImportDetail(currentUser?.role)) {
                            OutlinedButton(
                                onClick = { onNavigateToEdit(receiptId) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sửa phiếu")
                            }
                        }

                        if (com.example.warehouse.utils.PermissionManager.canCancelImportReceipt(currentUser?.role)) {
                            Button(
                                onClick = {
                                    isUnderstandChecked = false
                                    deleteReason = ""
                                    showCancelConfirmDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("cancel_import_receipt_btn")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Hủy phiếu", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            if (isCancelled) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFFCA5A5))),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "PHIẾU NÀY ĐÃ BỊ HỦY",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF991B1B)
                                )
                            }
                            if (receipt.deleteReason != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Lý do hủy: ${receipt.deleteReason}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF7F1D1D)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (isCancelled) Color(0xFFFEF2F2) else Color(0xFFF0F9FF)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(if (isCancelled) Color(0xFFFCA5A5) else Color(0xFFBAE6FD))),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = PrimaryBlue,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = receipt.code,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }

                            // Status Badge (Hide if "Đã lưu" or "Completed")
                            val statusText = when {
                                isCancelled -> "ĐÃ HỦY"
                                receipt.status.equals("Đã chỉnh sửa", ignoreCase = true) -> "ĐÃ CHỈNH SỬA"
                                else -> ""
                            }
                            if (statusText.isNotEmpty()) {
                                Surface(
                                    color = if (isCancelled) Color(0xFFFEE2E2) else Color(0xFFFEF3C7),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = statusText,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCancelled) Color(0xFFDC2626) else Color(0xFFB45309),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Dates display
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Ngày nhập kho: ",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                DateUtils.formatDateOnly(receipt.importDate),
                                style = MaterialTheme.typography.bodyMedium,
                                color = PrimaryBlue,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            "Thời điểm tạo phiếu: ${DateUtils.formatDateTime(receipt.createdAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        item.supplier?.let { sup ->
                            Text("Nhà cung cấp: ${sup.name}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                        item.user?.let { u ->
                            Text("Người lập phiếu: ${u.fullName}", style = MaterialTheme.typography.bodyMedium)
                        }

                        if (receipt.note.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Ghi chú:\n${receipt.note}",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Tổng giá trị nhập:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                CurrencyFormatter.formatVnd(totalAmount),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isCancelled) Color.Gray else PrimaryBlue
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text("Danh sách sản phẩm (${item.details.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(item.details) { detailWithProduct ->
                val p = detailWithProduct.product
                val d = detailWithProduct.detail

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(p.productName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text("Mã SP: ${p.productCode}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Số lượng: ${d.quantity} • Đơn giá: ${CurrencyFormatter.formatVnd(d.unitPrice)}", style = MaterialTheme.typography.bodySmall)
                        }
                        Text(
                            text = CurrencyFormatter.formatVnd(d.subtotal),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isCancelled) Color.Gray else PrimaryBlue
                        )
                    }
                }
            }
        }
    }

    // Cancel/Delete Receipt Confirmation Dialog
    if (showCancelConfirmDialog) {
        val user = currentUser
        AlertDialog(
            onDismissRequest = { showCancelConfirmDialog = false },
            title = { Text("Xác nhận hủy phiếu nhập") },
            text = {
                Column {
                    Text(
                        "Bạn có chắc chắn muốn hủy phiếu nhập kho '${receipt.code}' không?\n\n" +
                                "Hủy giao dịch này sẽ tự động cập nhập lại tồn kho. Thao tác này không thể hoàn tác.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = deleteReason,
                        onValueChange = { deleteReason = it },
                        label = { Text("Lý do hủy phiếu (Bắt buộc)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("delete_reason_input")
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isUnderstandChecked = !isUnderstandChecked }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = isUnderstandChecked,
                            onCheckedChange = { isUnderstandChecked = it },
                            modifier = Modifier.testTag("understand_checkbox")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tôi đã hiểu", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    enabled = isUnderstandChecked && deleteReason.isNotBlank(),
                    onClick = {
                        showCancelConfirmDialog = false
                        if (user == null) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Không thể xác định người dùng hiện tại.")
                            }
                            return@Button
                        }
                        viewModel.cancelReceipt(
                            receiptId = receipt.id,
                            currentUser = user,
                            deleteReason = deleteReason.trim(),
                            onComplete = { success, msg ->
                                if (success) {
                                    refreshTrigger++
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Hủy phiếu nhập kho thành công!")
                                    }
                                } else {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(msg)
                                    }
                                }
                            }
                        )
                    },
                    modifier = Modifier.testTag("confirm_delete_receipt_btn")
                ) {
                    Text("ĐỒNG Ý HỦY")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCancelConfirmDialog = false }) {
                    Text("ĐÓNG")
                }
            }
        )
    }

    // Edit Import Date Dialog
    if (showDatePickerDialog) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = receipt.importDate,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis <= System.currentTimeMillis()
                }
            }
        )

        DatePickerDialog(
        colors = DatePickerDefaults.colors(containerColor = Color.White),
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedDate = datePickerState.selectedDateMillis
                        showDatePickerDialog = false
                        val user = currentUser
                        if (selectedDate != null && user != null) {
                            viewModel.updateImportDate(
                                receiptId = receipt.id,
                                newImportDate = selectedDate,
                                currentUser = user,
                                onComplete = { success, msg ->
                                    if (success) {
                                        refreshTrigger++
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Cập nhật ngày nhập kho thành công!")
                                        }
                                    } else {
                                        errorMessage = msg
                                    }
                                }
                            )
                        }
                    }
                ) {
                    Text("LƯU NGÀY")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text("HỦY")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Error Dialog (Shows detailed error, e.g. insufficient stock for cancellation or lack of permission)
    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("THÔNG BÁO LỖI", fontWeight = FontWeight.Bold, color = PrimaryBlue) },
            text = { Text(errorMessage ?: "") },
            confirmButton = {
                Button(onClick = { errorMessage = null }) {
                    Text("HIỂU RỒI")
                }
            }
        )
    }
}

package com.example.warehouse.ui.export

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.warehouse.data.database.relation.ExportReceiptWithDetails
import com.example.warehouse.ui.theme.PrimaryBlue
import com.example.warehouse.ui.component.BarcodeScannerSheet
import com.example.warehouse.utils.CurrencyFormatter
import com.example.warehouse.utils.DateUtils
import com.example.warehouse.viewmodel.ExportViewModel
import com.example.warehouse.viewmodel.ProductViewModel
import kotlinx.coroutines.launch

data class EditDetailItem(
    val productId: Long,
    val productName: String,
    val productCode: String,
    var quantity: Int,
    val price: Double,
    val stock: Int // available stock = current stock + what was already assigned to this receipt
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportDetailScreen(
    receiptId: Long,
    viewModel: ExportViewModel,
    productViewModel: ProductViewModel,
    userRole: String,
    userFullName: String,
    onNavigateBack: () -> Unit
) {
    var receiptWithDetails by remember { mutableStateOf<ExportReceiptWithDetails?>(null) }
    var isEditMode by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showProductPickerSheet by remember { mutableStateOf(false) }
    var showScannerSheet by remember { mutableStateOf(false) }

    val availableProducts by productViewModel.products.collectAsState()

    // Edit states
    var editRecipient by remember { mutableStateOf("") }
    var editNote by remember { mutableStateOf("") }
    var editExportDate by remember { mutableStateOf(System.currentTimeMillis()) }
    val editDetailsList = remember { mutableStateListOf<EditDetailItem>() }

    // Delete states
    var deleteReason by remember { mutableStateOf("") }
    var isConfirmedCheckbox by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    androidx.activity.compose.BackHandler(enabled = isEditMode) {
        isEditMode = false
    }

    LaunchedEffect(receiptId) {
        receiptWithDetails = viewModel.getExportReceiptById(receiptId)
    }

    val item = receiptWithDetails
    if (item == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val receipt = item.receipt
    val isManager = userRole == "ADMIN"
    val isDeleted = receipt.status == "Đã xóa" || receipt.status == "Đã hủy"

    // Populate edit fields when edit mode is toggled on or products load
    LaunchedEffect(isEditMode, availableProducts) {
        if (isEditMode && availableProducts.isNotEmpty() && editDetailsList.isEmpty()) {
            editRecipient = receipt.recipient
            editNote = receipt.note
            editExportDate = receipt.exportDate
            editDetailsList.clear()
            item.details.forEach {
                val matchingProduct = availableProducts.find { p -> p.product.productId == it.detail.productId }
                val currentStock = matchingProduct?.quantityInStock ?: 0
                editDetailsList.add(
                    EditDetailItem(
                        productId = it.detail.productId,
                        productName = it.product.productName,
                        productCode = it.product.productCode,
                        quantity = it.detail.quantity,
                        price = it.detail.price,
                        stock = (currentStock + it.detail.quantity)
                    )
                )
            }
        }
    }

    LaunchedEffect(isEditMode) {
        if (!isEditMode) {
            editDetailsList.clear()
        }
    }


    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            if (isEditMode) {
                // Bottom Bar for EDIT mode
                val totalCost = editDetailsList.sumOf { it.quantity * it.price }
                Surface(
                    tonalElevation = 8.dp,
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Tổng tiền xuất kho:", style = MaterialTheme.typography.titleMedium, color = Color(0xFF0F172A))
                            Text(
                                text = CurrencyFormatter.formatVnd(totalCost),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { isEditMode = false },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                            ) {
                                Text("HỦY", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    if (editDetailsList.isEmpty()) {
                                        scope.launch { snackbarHostState.showSnackbar("Danh sách sản phẩm xuất không được để trống!") }
                                        return@Button
                                    }
                                    if (editDetailsList.any { it.quantity <= 0 }) {
                                        scope.launch { snackbarHostState.showSnackbar("Vui lòng nhập số lượng hợp lệ (> 0) cho tất cả sản phẩm!") }
                                        return@Button
                                    }

                                    viewModel.updateReceipt(
                                        receiptId = receiptId,
                                        exportDate = editExportDate,
                                        recipient = editRecipient.trim(),
                                        note = editNote.trim(),
                                        details = editDetailsList.map { Pair(it.productId, it.quantity) },
                                        userFullName = userFullName,
                                        actingUserRole = userRole,
                                        onComplete = { success, msg ->
                                            if (success) {
                                                isEditMode = false
                                                scope.launch {
                                                    receiptWithDetails = viewModel.getExportReceiptById(receiptId)
                                                    snackbarHostState.showSnackbar("Cập nhật thành công!")
                                                }
                                            } else {
                                                scope.launch { snackbarHostState.showSnackbar(msg) }
                                            }
                                        }
                                    )
                                },
                                enabled = editDetailsList.isNotEmpty() && editDetailsList.all { it.quantity > 0 && it.quantity <= it.stock },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(52.dp)
                                    .testTag("save_export_receipt_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("CẬP NHẬT", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = Color.White)
                            }
                        }
                    }
                }
            } else if (isManager && !isDeleted) {
                // Bottom Bar for VIEW mode
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
                        OutlinedButton(
                            onClick = { isEditMode = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("edit_export_receipt_btn")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sửa phiếu")
                        }

                        Button(
                            onClick = { showDeleteDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("delete_export_receipt_btn")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("HỦY PHIẾU", fontWeight = FontWeight.Bold)
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
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isDeleted) {
                item {
                    // Delete warning card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                        border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
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
                }
            }

            if (!isEditMode) {
                // VIEW MODE content
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (isDeleted) Color(0xFFFEF2F2) else Color(0xFFF0F9FF)),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(if (isDeleted) Color(0xFFFCA5A5) else Color(0xFFBAE6FD))),
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
                                    isDeleted -> "ĐÃ HỦY"
                                    receipt.status.equals("Đã chỉnh sửa", ignoreCase = true) -> "ĐÃ CHỈNH SỬA"
                                    else -> ""
                                }
                                if (statusText.isNotEmpty()) {
                                    Surface(
                                        color = if (isDeleted) Color(0xFFFEE2E2) else Color(0xFFFEF3C7),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = statusText,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDeleted) Color(0xFFDC2626) else Color(0xFFB45309),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Dates display
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Ngày xuất kho: ",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    DateUtils.formatDateOnly(receipt.exportDate),
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

                            if (receipt.recipient.isNotBlank()) {
                                Text("Người nhận hàng: ${receipt.recipient}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
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
                                Text("Tổng giá trị xuất:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    text = CurrencyFormatter.formatVnd(receipt.totalAmount),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDeleted) Color.Gray else PrimaryBlue
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Danh sách sản phẩm (${item.details.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                                Text("Số lượng: ${d.quantity} • Đơn giá: ${CurrencyFormatter.formatVnd(d.price)}", style = MaterialTheme.typography.bodySmall)
                            }
                            Text(
                                text = CurrencyFormatter.formatVnd(d.quantity * d.price),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isDeleted) Color.Gray else PrimaryBlue
                            )
                        }
                    }
                }
            } else {
                // EDIT MODE content
                item {
                    // Recipient Input
                    OutlinedTextField(
                        value = editRecipient,
                        onValueChange = { editRecipient = it },
                        label = { Text("Người nhận hàng (Tùy chọn)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_export_recipient_input")
                    )
                }

                item {
                    // Export Date Selector
                    OutlinedTextField(
                        value = DateUtils.formatDateOnly(editExportDate),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Ngày xuất kho") },
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Default.CalendarToday, contentDescription = "Chọn ngày xuất kho")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true }
                    )
                }

                item {
                    // Note Input
                    OutlinedTextField(
                        value = editNote,
                        onValueChange = { editNote = it },
                        label = { Text("Ghi chú phiếu xuất") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Sản phẩm xuất (${editDetailsList.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { showProductPickerSheet = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("add_product_to_edit_export_btn")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Thêm SP")
                            }

                            IconButton(
                                onClick = { showScannerSheet = true },
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                            ) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = "Quét vạch/QR", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                    }
                }

                items(editDetailsList) { detailItem ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(detailItem.productName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "Mã SP: ${detailItem.productCode} • Tồn khả dụng: ${detailItem.stock}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { editDetailsList.remove(detailItem) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Xóa dòng", tint = PrimaryBlue)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    OutlinedTextField(
                                        value = if (detailItem.quantity == 0) "" else detailItem.quantity.toString(),
                                        onValueChange = { input ->
                                            val qty = input.toIntOrNull() ?: 0
                                            val idx = editDetailsList.indexOf(detailItem)
                                            if (idx >= 0) {
                                                editDetailsList[idx] = detailItem.copy(quantity = qty)
                                            }
                                        },
                                        label = { Text("Số lượng") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        isError = detailItem.quantity <= 0 || detailItem.quantity > detailItem.stock,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    if (detailItem.quantity > detailItem.stock) {
                                        Text(
                                            text = "Tối đa ${detailItem.stock}",
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = CurrencyFormatter.formatVnd(detailItem.price),
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Đơn giá xuất") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1.2f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Date Picker Dialog (Restricts future dates)
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = editExportDate,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis <= System.currentTimeMillis()
                }
            }
        )
        DatePickerDialog(
        colors = DatePickerDefaults.colors(containerColor = Color.White),
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selected ->
                        editExportDate = selected
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Hủy")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Delete confirmation dialog with Reason and Checkbox
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Hủy phiếu xuất kho", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Hành động này sẽ hủy phiếu xuất kho, chuyển trạng thái sang 'Đã hủy' và hoàn trả toàn bộ số lượng xuất vào kho hàng.")
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
                        modifier = Modifier.clickable { isConfirmedCheckbox = !isConfirmedCheckbox }
                    ) {
                        Checkbox(
                            checked = isConfirmedCheckbox,
                            onCheckedChange = { isConfirmedCheckbox = it },
                            modifier = Modifier.testTag("confirm_delete_checkbox")
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tôi xác nhận hoàn trả toàn bộ số lượng xuất của phiếu này vào kho hàng.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteReceipt(
                            receiptId = receiptId,
                            deleteReason = deleteReason.trim(),
                            actingUserRole = userRole,
                            onComplete = { success, msg ->
                                if (success) {
                                    showDeleteDialog = false
                                    scope.launch {
                                        receiptWithDetails = viewModel.getExportReceiptById(receiptId)
                                        snackbarHostState.showSnackbar(msg)
                                    }
                                } else {
                                    scope.launch { snackbarHostState.showSnackbar(msg) }
                                }
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    enabled = deleteReason.isNotBlank() && isConfirmedCheckbox,
                    modifier = Modifier.testTag("confirm_delete_btn")
                ) {
                    Text("Xác nhận hủy")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    // Product Picker in Edit Mode
    if (showProductPickerSheet) {
        ModalBottomSheet(onDismissRequest = { showProductPickerSheet = false }, containerColor = Color.White) {
            var searchQuery by remember { mutableStateOf("") }
            val filteredProducts = availableProducts.filter {
                (it.product.productName.contains(searchQuery, ignoreCase = true) ||
                 it.product.productCode.contains(searchQuery, ignoreCase = true)) &&
                 it.product.status.equals("ACTIVE", ignoreCase = true)
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Text("Thêm sản phẩm xuất kho", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Tìm theo tên hoặc mã SP...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxHeight(0.6f)
                ) {
                    items(filteredProducts) { item ->
                        val product = item.product
                        val isAlreadyAdded = editDetailsList.any { it.productId == product.productId }

                        Card(
                            onClick = {
                                if (isAlreadyAdded) {
                                    scope.launch { snackbarHostState.showSnackbar("Sản phẩm đã có sẵn trong danh sách chỉnh sửa!") }
                                    return@Card
                                }

                                if (product.exportPrice <= 0.0) {
                                    scope.launch { snackbarHostState.showSnackbar("Sản phẩm chưa được cấu hình giá xuất bán ở module Sản phẩm!") }
                                    return@Card
                                }

                                if (item.quantityInStock <= 0) {
                                    scope.launch { snackbarHostState.showSnackbar("Sản phẩm đã hết hàng trong kho!") }
                                    return@Card
                                }

                                editDetailsList.add(
                                    EditDetailItem(
                                        productId = product.productId,
                                        productName = product.productName,
                                        productCode = product.productCode,
                                        quantity = 1,
                                        price = product.exportPrice,
                                        stock = item.quantityInStock
                                    )
                                )
                                showProductPickerSheet = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(product.productName, fontWeight = FontWeight.Bold)
                                    Text("Mã SP: ${product.productCode} • Tồn kho: ${item.quantityInStock}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                }
                                Text(CurrencyFormatter.formatVnd(product.exportPrice), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }
            }
        }
    }

    // Barcode Scanner Sheet in Edit Mode
    if (showScannerSheet) {
        BarcodeScannerSheet(
            onCodeScanned = { barcode ->
                val match = availableProducts.find {
                    it.product.productCode.equals(barcode, ignoreCase = true) &&
                    it.product.status.equals("ACTIVE", ignoreCase = true)
                }
                if (match != null) {
                    val isAlreadyAdded = editDetailsList.any { it.productId == match.product.productId }
                    if (isAlreadyAdded) {
                        scope.launch { snackbarHostState.showSnackbar("Sản phẩm đã có trong danh sách!") }
                    } else if (match.product.exportPrice <= 0.0) {
                        scope.launch { snackbarHostState.showSnackbar("Sản phẩm chưa được cấu hình giá xuất bán!") }
                    } else if (match.quantityInStock <= 0) {
                        scope.launch { snackbarHostState.showSnackbar("Sản phẩm đã hết hàng trong kho!") }
                    } else {
                        editDetailsList.add(
                            EditDetailItem(
                                productId = match.product.productId,
                                productName = match.product.productName,
                                productCode = match.product.productCode,
                                quantity = 1,
                                price = match.product.exportPrice,
                                stock = match.quantityInStock
                            )
                        )
                        scope.launch { snackbarHostState.showSnackbar("Đã thêm '${match.product.productName}'") }
                    }
                } else {
                    scope.launch { snackbarHostState.showSnackbar("Không tìm thấy sản phẩm hoạt động có mã '$barcode'") }
                }
                showScannerSheet = false
            },
            onDismiss = { showScannerSheet = false }
        )
    }
}

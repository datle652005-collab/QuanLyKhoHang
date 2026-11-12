package com.example.warehouse.ui.export

import androidx.compose.material.icons.filled.CalendarToday
import com.example.warehouse.utils.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.warehouse.ui.component.BarcodeScannerSheet
import com.example.warehouse.ui.theme.PrimaryBlue
import com.example.warehouse.utils.CurrencyFormatter
import com.example.warehouse.viewmodel.ExportViewModel
import com.example.warehouse.viewmodel.ProductViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportCreateScreen(
    exportViewModel: ExportViewModel,
    productViewModel: ProductViewModel,
    inventoryViewModel: com.example.warehouse.viewmodel.InventoryViewModel? = null,
    currentUserId: Long?,
    userFullName: String,
    onNavigateBack: () -> Unit
) {
    val cartItems by exportViewModel.cartItems.collectAsState()
    val note by exportViewModel.note.collectAsState()
    val recipient by exportViewModel.recipient.collectAsState()
    val exportDate by exportViewModel.exportDate.collectAsState()
    val availableProducts by productViewModel.products.collectAsState()

    val activeChecks by (inventoryViewModel?.allInventoryChecks?.collectAsState() ?: remember { mutableStateOf(emptyList()) })

    val activeCheckProductMap = remember(activeChecks) {
        val map = mutableMapOf<Long, String>()
        activeChecks.filter { it.check.status in listOf("Đang tiến hành", "Yêu cầu kiểm kê lại", "Đang chờ xét duyệt") }.forEach { checkWithDetails ->
            checkWithDetails.details.forEach { d ->
                map[d.detail.productId] = checkWithDetails.check.title.ifEmpty { checkWithDetails.check.code }
            }
        }
        map
    }

    var showProductPickerSheet by remember { mutableStateOf(false) }
    var showScannerSheet by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val totalCost = cartItems.sumOf { it.quantity * it.price }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            Surface(tonalElevation = 8.dp, color = Color.White, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tổng tiền xuất:", style = MaterialTheme.typography.titleMedium, color = Color(0xFF0F172A))
                        Text(
                            text = CurrencyFormatter.formatVnd(totalCost),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            exportViewModel.createReceipt(
                                userId = currentUserId,
                                createdBy = userFullName,
                                onComplete = { success, msg ->
                                    if (success) {
                                        onNavigateBack()
                                    } else {
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    }
                                }
                            )
                        },
                        enabled = cartItems.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("submit_export_receipt_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("HOÀN TẤT XUẤT KHO", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = Color.White)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Recipient Input
            OutlinedTextField(
                value = recipient,
                onValueChange = { exportViewModel.setRecipient(it) },
                label = { Text("Người nhận hàng (Tùy chọn)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("export_recipient_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Export Date Selector (Defaults to today, allows past, disallows future)
            OutlinedTextField(
                value = DateUtils.formatDateOnly(exportDate),
                onValueChange = {},
                readOnly = true,
                label = { Text("Ngày xuất kho (Mặc định: Hôm nay)") },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Chọn ngày xuất kho")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Note Input
            OutlinedTextField(
                value = note,
                onValueChange = { exportViewModel.setNote(it) },
                label = { Text("Ghi chú phiếu xuất kho") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Add Product Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showProductPickerSheet = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("add_product_to_export_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Chọn sản phẩm xuất")
                }

                IconButton(
                    onClick = { showScannerSheet = true },
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Quét vạch", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Danh sách sản phẩm xuất (${cartItems.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            // Cart Items
            if (cartItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Chưa chọn sản phẩm nào để xuất kho", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(cartItems, key = { it.product.product.id }) { item ->
                        val p = item.product.product
                        val activeCheckTitle = activeCheckProductMap[p.id]

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                if (activeCheckTitle != null) {
                                    Surface(
                                        color = Color(0xFFFEF3C7),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Sản phẩm đang trong đợt kiểm kê '$activeCheckTitle', số liệu kiểm kê có thể bị ảnh hưởng khi xuất kho song song.",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFFB45309)
                                            )
                                        }
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(p.productName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                        Text("Mã SP: ${p.productCode} • Khả dụng: ${item.product.quantityInStock}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                    }
                                    IconButton(onClick = { exportViewModel.removeCartItem(p.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = MaterialTheme.colorScheme.error)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        OutlinedTextField(
                                            value = if (item.quantity == 0) "" else item.quantity.toString(),
                                            onValueChange = { input ->
                                                val qty = input.toIntOrNull() ?: 0
                                                exportViewModel.updateCartItem(p.id, qty, item.price)
                                            },
                                            label = { Text("Số lượng") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            isError = item.quantity <= 0 || item.quantity > item.product.quantityInStock,
                                            modifier = Modifier.width(130.dp)
                                        )
                                        if (item.quantity > item.product.quantityInStock) {
                                            Text(
                                                text = "Tối đa ${item.product.quantityInStock}",
                                                color = MaterialTheme.colorScheme.error,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }

                                    // Display unit price as read-only text/label
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Đơn giá xuất", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                                        Text(
                                            text = CurrencyFormatter.formatVnd(item.price),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = "Thành tiền: ${CurrencyFormatter.formatVnd(item.quantity * item.price)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Medium,
                                            color = PrimaryBlue
                                        )
                                    }
                                }
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
            initialSelectedDateMillis = exportDate,
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
                        exportViewModel.setExportDate(selected)
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

    if (showProductPickerSheet) {
        ModalBottomSheet(onDismissRequest = { showProductPickerSheet = false }, containerColor = Color.White) {
            var searchQuery by remember { mutableStateOf("") }
            val filteredProducts = availableProducts.filter {
                (it.product.productName.contains(searchQuery, ignoreCase = true) ||
                 it.product.productCode.contains(searchQuery, ignoreCase = true)) &&
                 it.product.status.equals("ACTIVE", ignoreCase = true)
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Text("Chọn sản phẩm xuất kho", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar in Picker Sheet
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
                        Card(
                            onClick = {
                                val err = exportViewModel.addProductToCart(item)
                                if (err != null) {
                                    scope.launch { snackbarHostState.showSnackbar(err) }
                                } else {
                                    showProductPickerSheet = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.product.productName, fontWeight = FontWeight.Bold)
                                    Text("Mã SP: ${item.product.productCode} • Khả dụng: ${item.quantityInStock}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                }
                                Text(CurrencyFormatter.formatVnd(item.product.exportPrice), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showScannerSheet) {
        BarcodeScannerSheet(
            onCodeScanned = { barcode ->
                val match = availableProducts.find { it.product.productCode == barcode && it.product.status.equals("ACTIVE", ignoreCase = true) }
                if (match != null) {
                    val err = exportViewModel.addProductToCart(match)
                    if (err != null) {
                        scope.launch { snackbarHostState.showSnackbar(err) }
                    }
                } else {
                    scope.launch { snackbarHostState.showSnackbar("Không tìm thấy sản phẩm ACTIVE có mã vạch này!") }
                }
                showScannerSheet = false
            },
            onDismiss = { showScannerSheet = false }
        )
    }
}

package com.example.warehouse.ui.import

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Menu
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
import androidx.compose.ui.unit.sp
import com.example.warehouse.ui.component.BarcodeScannerSheet
import com.example.warehouse.ui.theme.PrimaryBlue
import com.example.warehouse.utils.CurrencyFormatter
import com.example.warehouse.utils.DateUtils
import com.example.warehouse.viewmodel.ImportViewModel
import com.example.warehouse.viewmodel.ProductViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportCreateScreen(
    importViewModel: ImportViewModel,
    productViewModel: ProductViewModel,
    inventoryViewModel: com.example.warehouse.viewmodel.InventoryViewModel? = null,
    currentUserId: Long?,
    userFullName: String,
    onNavigateBack: () -> Unit,
    onSuccess: (String) -> Unit = {}
) {
    val activeSuppliers by importViewModel.activeSuppliers.collectAsState()
    val suppliers by importViewModel.suppliers.collectAsState()
    val selectedSupplier by importViewModel.selectedSupplier.collectAsState()
    val cartItems by importViewModel.cartItems.collectAsState()
    val importDate by importViewModel.importDate.collectAsState()
    val note by importViewModel.note.collectAsState()

    val supplierNameText by importViewModel.supplierNameText.collectAsState()

    val availableProducts by productViewModel.products.collectAsState()

    val activeChecks by (inventoryViewModel?.allInventoryChecks?.collectAsState() ?: remember { mutableStateOf(emptyList()) })

    // Map of productId -> title of active stock check
    val activeCheckProductMap = remember(activeChecks) {
        val map = mutableMapOf<Long, String>()
        activeChecks.filter { it.check.status in listOf("Đang tiến hành", "Yêu cầu kiểm kê lại", "Đang chờ xét duyệt") }.forEach { checkWithDetails ->
            checkWithDetails.details.forEach { d ->
                map[d.detail.productId] = checkWithDetails.check.title.ifEmpty { checkWithDetails.check.code }
            }
        }
        map
    }
    // Filter only ACTIVE products for import
    val activeProducts = remember(availableProducts) {
        availableProducts.filter { it.product.status.equals("ACTIVE", ignoreCase = true) }
    }

    var showSupplierDialog by remember { mutableStateOf(false) }
    var showProductPickerSheet by remember { mutableStateOf(false) }
    var showScannerSheet by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        importViewModel.clearCart()
        importViewModel.selectSupplier(null)
        importViewModel.setSupplierNameText("")
        importViewModel.setNote("")
        importViewModel.setImportDate(System.currentTimeMillis())
    }

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
                        Text("Tổng tiền nhập kho:", style = MaterialTheme.typography.titleMedium, color = Color(0xFF0F172A))
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
                            if (cartItems.isEmpty()) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Vui lòng thêm ít nhất 1 sản phẩm!")
                                }
                                return@Button
                            }

                            importViewModel.createReceipt(
                                userId = currentUserId,
                                onComplete = { success, msg ->
                                    if (success) {
                                        onSuccess(msg)
                                    } else {
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(msg)
                                        }
                                    }
                                }
                            )
                        },
                        enabled = cartItems.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("submit_import_receipt_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("LƯU PHIẾU NHẬP KHO", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = Color.White)
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
            // Select Supplier input (Optional, text with dialog button)
            OutlinedTextField(
                value = supplierNameText,
                onValueChange = {
                    importViewModel.setSupplierNameText(it)
                    if (selectedSupplier != null && it != selectedSupplier?.name) {
                        importViewModel.selectSupplier(null) // clear selected if user edits text
                    }
                },
                label = { Text("Nhà cung cấp *") },
                placeholder = { Text("Nhập tên hoặc chọn từ danh sách") },
                trailingIcon = {
                    IconButton(onClick = { showSupplierDialog = true }) {
                        Icon(Icons.Default.Menu, contentDescription = "Chọn nhà cung cấp")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("supplier_text_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Import Date Selector (Defaults to today, allows past, disallows future)
            OutlinedTextField(
                value = DateUtils.formatDateOnly(importDate),
                onValueChange = {},
                readOnly = true,
                label = { Text("Ngày nhập kho (Mặc định: Hôm nay)") },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Chọn ngày nhập kho")
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
                onValueChange = { importViewModel.setNote(it) },
                label = { Text("Ghi chú phiếu nhập") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Add Product Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showProductPickerSheet = true },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("add_product_to_import_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Thêm sản phẩm")
                }

                IconButton(
                    onClick = { showScannerSheet = true },
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Quét vạch/QR", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Danh sách sản phẩm nhập (${cartItems.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            // Cart Items List
            if (cartItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Chưa chọn sản phẩm nào để nhập kho", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
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
                                                text = "Sản phẩm đang trong đợt kiểm kê '$activeCheckTitle', số liệu kiểm kê có thể bị ảnh hưởng khi nhập kho song song.",
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
                                        Text(
                                            "Mã SP: ${p.productCode} • Tồn hiện tại: ${item.product.quantityInStock}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = { importViewModel.removeCartItem(p.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Xóa sản phẩm", tint = MaterialTheme.colorScheme.error)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = if (item.quantity == 0) "" else item.quantity.toString(),
                                        onValueChange = { input ->
                                            val qty = input.toIntOrNull() ?: 0
                                            importViewModel.updateCartItem(p.id, qty, item.price)
                                        },
                                        label = { Text("Số lượng (> 0)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        isError = item.quantity <= 0,
                                        modifier = Modifier.weight(1f)
                                    )

                                    OutlinedTextField(
                                        value = if (item.price == 0.0) "" else item.price.toLong().toString(),
                                        onValueChange = { input ->
                                            val price = input.toDoubleOrNull() ?: 0.0
                                            importViewModel.updateCartItem(p.id, item.quantity, price)
                                        },
                                        label = { Text("Đơn giá nhập") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        isError = item.price < 0,
                                        modifier = Modifier.weight(1.2f)
                                    )
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
            initialSelectedDateMillis = importDate,
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
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selected ->
                            importViewModel.setImportDate(selected)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Chọn")
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

    // Product Picker Bottom Sheet (Active status only)
    if (showProductPickerSheet) {
        var productSearchQuery by remember { mutableStateOf("") }
        val filteredActiveProducts = remember(activeProducts, productSearchQuery) {
            if (productSearchQuery.isBlank()) activeProducts
            else activeProducts.filter {
                it.product.productName.contains(productSearchQuery, ignoreCase = true) ||
                it.product.productCode.contains(productSearchQuery, ignoreCase = true)
            }
        }

        ModalBottomSheet(onDismissRequest = { showProductPickerSheet = false }, containerColor = Color.White) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Chọn sản phẩm nhập kho (Chỉ SP đang kinh doanh)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = productSearchQuery,
                    onValueChange = { productSearchQuery = it },
                    placeholder = { Text("Tìm theo mã hoặc tên sản phẩm...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (filteredActiveProducts.isEmpty()) {
                    Text("Không tìm thấy sản phẩm nào.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(filteredActiveProducts) { item ->
                            Card(
                                onClick = {
                                    importViewModel.addProductToCart(item)
                                    showProductPickerSheet = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.product.productName, fontWeight = FontWeight.Bold)
                                        Text("Mã SP: ${item.product.productCode} • Tồn: ${item.quantityInStock}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Text(
                                        CurrencyFormatter.formatVnd(item.product.importPrice),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Barcode / QR Scanner
    if (showScannerSheet) {
        BarcodeScannerSheet(
            onCodeScanned = { barcode ->
                val match = activeProducts.find { it.product.productCode.equals(barcode, ignoreCase = true) }
                if (match != null) {
                    importViewModel.addProductToCart(match)
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Đã thêm '${match.product.productName}' vào phiếu")
                    }
                } else {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Không tìm thấy sản phẩm ACTIVE có mã '$barcode'")
                    }
                }
                showScannerSheet = false
            },
            onDismiss = { showScannerSheet = false }
        )
    }

    if (showSupplierDialog) {
        var newSupInput by remember { mutableStateOf("") }
        var searchSupQuery by remember { mutableStateOf("") }
        var isCreating by remember { mutableStateOf(false) }

        val filteredSuppliers = remember(activeSuppliers, searchSupQuery) {
            if (searchSupQuery.isBlank()) activeSuppliers
            else activeSuppliers.filter { it.name.contains(searchSupQuery, ignoreCase = true) }
        }

        androidx.compose.ui.window.Dialog(onDismissRequest = { showSupplierDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "Chọn Nhà Cung Cấp",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        color = Color(0xFFF0F9FF),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Tạo nhanh nhà cung cấp mới:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = newSupInput,
                                    onValueChange = { newSupInput = it },
                                    placeholder = { Text("Tên NCC mới...") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (newSupInput.isNotBlank() && !isCreating) {
                                            isCreating = true
                                            coroutineScope.launch {
                                                try {
                                                    val sup = productViewModel.addSupplierQuick(newSupInput)
                                                    importViewModel.selectSupplier(sup)
                                                    importViewModel.setSupplierNameText(sup.name)
                                                    showSupplierDialog = false
                                                } finally {
                                                    isCreating = false
                                                }
                                            }
                                        }
                                    },
                                    enabled = newSupInput.isNotBlank() && !isCreating
                                ) {
                                    Text("Tạo")
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = searchSupQuery,
                        onValueChange = { searchSupQuery = it },
                        placeholder = { Text("Tìm nhà cung cấp...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Box(modifier = Modifier.fillMaxWidth().heightIn(max = 250.dp)) {
                        LazyColumn {
                            items(filteredSuppliers) { sup ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            importViewModel.selectSupplier(sup)
                                            importViewModel.setSupplierNameText(sup.name)
                                            showSupplierDialog = false
                                        }
                                        .padding(12.dp),
                                    color = Color.Transparent
                                ) {
                                    Text(sup.name)
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showSupplierDialog = false }) { Text("Đóng") }
                    }
                }
            }
        }
    }
}

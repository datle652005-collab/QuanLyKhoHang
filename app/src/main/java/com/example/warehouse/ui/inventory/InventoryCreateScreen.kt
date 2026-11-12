package com.example.warehouse.ui.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.warehouse.ui.component.BarcodeScannerSheet
import com.example.warehouse.ui.theme.PrimaryBlue
import com.example.warehouse.utils.DateUtils
import com.example.warehouse.viewmodel.InventoryViewModel
import com.example.warehouse.viewmodel.StockCheckScope
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryCreateScreen(
    inventoryViewModel: InventoryViewModel,
    currentUserId: Long?,
    userFullName: String,
    userRole: String? = null,
    onNavigateToDetail: (Long, String) -> Unit = { _, _ -> },
    onNavigateBack: () -> Unit,
    onSuccess: (String) -> Unit = {}
) {
    val title by inventoryViewModel.createTitle.collectAsState()
    val note by inventoryViewModel.createNote.collectAsState()
    val checkDate by inventoryViewModel.createCheckDate.collectAsState()
    val scopeType by inventoryViewModel.createScope.collectAsState()
    val categories by inventoryViewModel.categories.collectAsState()
    val selectedCategoryIds by inventoryViewModel.selectedCategoryIds.collectAsState()
    val activeProducts by inventoryViewModel.activeProducts.collectAsState()
    val selectedProductIds by inventoryViewModel.selectedProductIds.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var categorySearchQuery by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showScannerSheet by remember { mutableStateOf(false) }
    var scopeExpanded by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val resolvedProductIds = remember(scopeType, selectedCategoryIds, selectedProductIds, activeProducts) {
        inventoryViewModel.resolveProductIdsForScope()
    }

    val filteredCategories = remember(categories, categorySearchQuery) {
        if (categorySearchQuery.isBlank()) categories
        else {
            val q = categorySearchQuery.trim().lowercase()
            categories.filter { it.categoryName.lowercase().contains(q) }
        }
    }

    val filteredManualProducts = remember(activeProducts, searchQuery) {
        if (searchQuery.isBlank()) activeProducts
        else {
            val q = searchQuery.trim().lowercase()
            activeProducts.filter {
                it.product.productName.lowercase().contains(q) ||
                it.product.productCode.lowercase().contains(q)
            }
        }
    }

    LaunchedEffect(Unit) {
        if (title.isEmpty()) {
            val todayStr = DateUtils.formatDate(System.currentTimeMillis())
            inventoryViewModel.setCreateTitle("Kiểm kê kho ngày $todayStr")
        }
    }

    val isTitleBlank = title.isBlank()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            Surface(tonalElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = {
                            if (isTitleBlank) {
                                scope.launch { snackbarHostState.showSnackbar("Vui lòng nhập tiêu đề kiểm kê!") }
                                return@Button
                            }
                            if (resolvedProductIds.isEmpty()) {
                                scope.launch { snackbarHostState.showSnackbar("Vui lòng chọn sản phẩm trong phạm vi kiểm kê!") }
                                return@Button
                            }
                            inventoryViewModel.createStockCheck(
                                userId = currentUserId,
                                actingUserRole = userRole,
                                onComplete = { success, msg, newCheckId ->
                                    if (success) {
                                        val succMsg = "Tạo đợt kiểm kê thành công!"
                                        if (newCheckId != null) {
                                            onNavigateToDetail(newCheckId, succMsg)
                                        } else {
                                            onSuccess(succMsg)
                                            onNavigateBack()
                                        }
                                    } else {
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    }
                                }
                            )
                        },
                        enabled = !isTitleBlank && resolvedProductIds.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("submit_inventory_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "TẠO KIỂM KÊ (${resolvedProductIds.size} SẢN PHẨM)",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
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
            // Thông tin kiểm kê Card - Chỉnh sát lên với Tạo phiếu kho
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Thông tin kiểm kê",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = title,
                        onValueChange = { inventoryViewModel.setCreateTitle(it) },
                        label = { Text("Tiêu đề kiểm kê *") },
                        placeholder = { Text("Nhập tiêu đề kiểm kê...") },
                        singleLine = true,
                        isError = isTitleBlank,
                        supportingText = {
                            if (isTitleBlank) {
                                Text(
                                    text = "Bắt buộc phải nhập tiêu đề kiểm kê",
                                    color = Color(0xFFEF4444)
                                )
                            }
                        },
                        trailingIcon = {
                            if (isTitleBlank) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = "Bắt buộc nhập tiêu đề",
                                    tint = Color(0xFFEF4444)
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = note,
                        onValueChange = { inventoryViewModel.setCreateNote(it) },
                        label = { Text("Ghi chú (tùy chọn)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Date Picker Trigger
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true }
                            .border(1.dp, Color.LightGray, RoundedCornerShape(4.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Ngày bắt đầu kiểm kê", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(
                                DateUtils.formatDate(checkDate),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = PrimaryBlue)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Phạm vi kiểm kê dạng Combobox
                    ExposedDropdownMenuBox(
                        expanded = scopeExpanded,
                        onExpandedChange = { scopeExpanded = !scopeExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = when (scopeType) {
                                StockCheckScope.ALL -> "Toàn bộ kho"
                                StockCheckScope.CATEGORY -> "Theo danh mục"
                                StockCheckScope.MANUAL -> "Thủ công (Chọn sản phẩm)"
                            },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Phạm vi kiểm kê *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = scopeExpanded) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = scopeExpanded,
                            onDismissRequest = { scopeExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("Toàn bộ kho", fontWeight = FontWeight.Bold)
                                        Text("Kiểm kê tất cả ${activeProducts.size} sản phẩm", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                },
                                onClick = {
                                    inventoryViewModel.setCreateScope(StockCheckScope.ALL)
                                    scopeExpanded = false
                                },
                                leadingIcon = {
                                    if (scopeType == StockCheckScope.ALL) Icon(Icons.Default.Check, contentDescription = null, tint = PrimaryBlue)
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("Theo danh mục", fontWeight = FontWeight.Bold)
                                        Text("Chọn kiểm kê theo 1 hoặc nhiều danh mục", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                },
                                onClick = {
                                    inventoryViewModel.setCreateScope(StockCheckScope.CATEGORY)
                                    scopeExpanded = false
                                },
                                leadingIcon = {
                                    if (scopeType == StockCheckScope.CATEGORY) Icon(Icons.Default.Check, contentDescription = null, tint = PrimaryBlue)
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("Thủ công (Chọn sản phẩm)", fontWeight = FontWeight.Bold)
                                        Text("Chọn từng sản phẩm cụ thể để kiểm kê", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                },
                                onClick = {
                                    inventoryViewModel.setCreateScope(StockCheckScope.MANUAL)
                                    scopeExpanded = false
                                },
                                leadingIcon = {
                                    if (scopeType == StockCheckScope.MANUAL) Icon(Icons.Default.Check, contentDescription = null, tint = PrimaryBlue)
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Details section for chosen scope
            when (scopeType) {
                StockCheckScope.ALL -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Inventory, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Kiểm kê toàn bộ kho",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    "Kiểm kê sẽ bao gồm tất cả ${activeProducts.size} sản phẩm đang kinh doanh trong kho.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                StockCheckScope.CATEGORY -> {
                    // Category Search & Summary Bar
                    OutlinedTextField(
                        value = categorySearchQuery,
                        onValueChange = { categorySearchQuery = it },
                        placeholder = { Text("Tìm kiếm danh mục...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (categorySearchQuery.isNotEmpty()) {
                                IconButton(onClick = { categorySearchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = null)
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Đã chọn: ${selectedCategoryIds.size}/${categories.size} danh mục",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row {
                            TextButton(onClick = { inventoryViewModel.selectAllCategories() }) {
                                Text("Chọn tất cả")
                            }
                            TextButton(onClick = { inventoryViewModel.clearCategorySelection() }) {
                                Text("Bỏ chọn")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (filteredCategories.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Không tìm thấy danh mục phù hợp", color = Color.Gray)
                                }
                            }
                        } else {
                            items(filteredCategories, key = { it.categoryId }) { cat ->
                                val isSelected = selectedCategoryIds.contains(cat.categoryId)
                                val catProductCount = activeProducts.count { it.product.categoryId == cat.categoryId }

                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) Color(0xFFDBEAFE) else Color.White
                                    ),
                                    onClick = { inventoryViewModel.toggleCategorySelection(cat.categoryId) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(
                                                checked = isSelected,
                                                onCheckedChange = { inventoryViewModel.toggleCategorySelection(cat.categoryId) }
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                cat.categoryName,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                        }
                                        Text(
                                            "$catProductCount sản phẩm",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                StockCheckScope.MANUAL -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Đã chọn: ${selectedProductIds.size}/${activeProducts.size} sản phẩm",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row {
                            TextButton(onClick = { inventoryViewModel.addAllProductsToManualScope() }) {
                                Text("Chọn tất cả")
                            }
                            TextButton(onClick = { inventoryViewModel.clearManualScopeProducts() }) {
                                Text("Bỏ chọn")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Search and Scanner bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Tìm tên hoặc mã SP...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = null)
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(
                            onClick = { showScannerSheet = true },
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(PrimaryBlue)
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = "Quét mã QR", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(filteredManualProducts, key = { it.product.id }) { item ->
                            val p = item.product
                            val isSelected = selectedProductIds.contains(p.id)

                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) Color(0xFFDBEAFE).copy(alpha = 0.5f) else Color.White
                                ),
                                onClick = { inventoryViewModel.toggleProductSelection(p.id) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { inventoryViewModel.toggleProductSelection(p.id) }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(p.productName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                        Text("Mã: ${p.productCode} • Tồn: ${item.quantityInStock}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Date Picker Dialog (Blocks future dates)
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = checkDate
        )

        DatePickerDialog(
        colors = DatePickerDefaults.colors(containerColor = Color.White),
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selected ->
                        val todayEnd = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 59)
                            set(Calendar.MILLISECOND, 999)
                        }.timeInMillis

                        if (selected > todayEnd) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Không được chọn ngày trong tương lai!")
                            }
                        } else {
                            inventoryViewModel.setCreateCheckDate(selected)
                            showDatePicker = false
                        }
                    }
                }) {
                    Text("XÁC NHẬN", fontWeight = FontWeight.Bold, color = PrimaryBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("HỦY")
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                title = { Text("Chọn ngày bắt đầu kiểm kê", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold) }
            )
        }
    }

    // Barcode / QR Scanner Sheet
    if (showScannerSheet) {
        BarcodeScannerSheet(
            onCodeScanned = { barcode ->
                showScannerSheet = false
                val match = activeProducts.find { it.product.productCode.equals(barcode.trim(), ignoreCase = true) }
                if (match != null) {
                    inventoryViewModel.toggleProductSelection(match.product.id)
                    scope.launch {
                        snackbarHostState.showSnackbar("Đã chọn sản phẩm: ${match.product.productName}")
                    }
                } else {
                    scope.launch {
                        snackbarHostState.showSnackbar("Không tìm thấy sản phẩm có mã barcode: $barcode")
                    }
                }
            },
            onDismiss = { showScannerSheet = false }
        )
    }
}

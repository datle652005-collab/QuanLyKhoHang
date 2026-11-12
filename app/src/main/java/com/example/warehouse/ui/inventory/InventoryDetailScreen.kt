package com.example.warehouse.ui.inventory

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.warehouse.data.database.entity.StockCheckDetail
import com.example.warehouse.data.database.relation.InventoryCheckWithDetails
import com.example.warehouse.data.database.relation.InventoryDetailWithProduct
import com.example.warehouse.ui.component.BarcodeScannerSheet
import com.example.warehouse.ui.theme.PrimaryBlue
import com.example.warehouse.utils.DateUtils
import com.example.warehouse.viewmodel.InventoryViewModel
import com.example.warehouse.viewmodel.StockCheckScope
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryDetailScreen(
    checkId: Long,
    viewModel: InventoryViewModel,
    userRole: String = "STAFF", // "ADMIN", "MANAGER", "STAFF"
    onNavigateBack: () -> Unit,
    onNavigateBackWithMsg: (String) -> Unit = {},
    successMessage: String? = null,
    onClearSuccessMessage: () -> Unit = {}
) {
    val isManager = userRole.uppercase() in listOf("ADMIN", "MANAGER", "QUẢN LÝ KHO")

    var checkWithDetails by remember { mutableStateOf<InventoryCheckWithDetails?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val categories by viewModel.categories.collectAsState()
    val activeProducts by viewModel.activeProducts.collectAsState()
    val categoryMap = remember(categories) { categories.associate { it.categoryId to it.categoryName } }

    // Draft local edits for actual quantities & reasons
    val localActuals = remember { mutableStateMapOf<Long, String>() } // detailId -> actualQuantity string
    val localReasons = remember { mutableStateMapOf<Long, String>() } // detailId -> reason string

    var showEditSheet by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showApproveConfirmDialog by remember { mutableStateOf(false) }
    var showSubmitConfirmDialog by remember { mutableStateOf(false) }
    var showRejectDialog by remember { mutableStateOf(false) }
    var rejectReason by remember { mutableStateOf("") }
    var cancelReason by remember { mutableStateOf("") }
    var showScannerSheet by remember { mutableStateOf(false) }

    // State for Quantity Edit Dialog when clicking a product item
    var selectedDetailForQtyEdit by remember { mutableStateOf<InventoryDetailWithProduct?>(null) }
    var dialogTempQty by remember { mutableStateOf("0") }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(successMessage) {
        if (!successMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(successMessage)
            onClearSuccessMessage()
        }
    }

    val refreshData: () -> Unit = {
        scope.launch {
            val fetched = viewModel.getInventoryCheckById(checkId)
            checkWithDetails = fetched
            fetched?.details?.forEach { item ->
                localActuals[item.detail.detailId] = item.detail.actualQuantity?.toString() ?: ""
                localReasons[item.detail.detailId] = item.detail.reason
            }
        }
    }

    LaunchedEffect(checkId) {
        refreshData()
    }

    val item = checkWithDetails
    if (item == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val check = item.check
    val details = item.details

    // Filter items by search query
    val filteredDetails = remember(details, searchQuery) {
        if (searchQuery.isBlank()) details
        else {
            val q = searchQuery.trim().lowercase()
            details.filter {
                it.product.productName.lowercase().contains(q) ||
                it.product.productCode.lowercase().contains(q)
            }
        }
    }

    // Progress statistics
    val totalCount = details.size
    val countedCount = details.count { localActuals[it.detail.detailId]?.toIntOrNull() != null }
    val isAllCounted = countedCount == totalCount && totalCount > 0
    val uncountedCount = totalCount - countedCount

    // Discrepancy stats
    val totalDiscrepancyItems = details.count {
        val actual = localActuals[it.detail.detailId]?.toIntOrNull()
        actual != null && actual != it.detail.systemQuantity
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            // Action Bar depending on Status & Role
            Surface(tonalElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    when (check.status) {
                        "Đang tiến hành", "Yêu cầu kiểm kê lại" -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Save Draft Button
                                OutlinedButton(
                                    onClick = {
                                        val updatedList = details.map { d ->
                                            val actualStr = localActuals[d.detail.detailId]?.trim() ?: ""
                                            val actualInt = actualStr.toIntOrNull()
                                            val reasonStr = localReasons[d.detail.detailId]?.trim() ?: ""
                                            val diff = actualInt?.minus(d.detail.systemQuantity)

                                            d.detail.copy(
                                                actualQuantity = actualInt,
                                                difference = diff,
                                                reason = reasonStr
                                            )
                                        }

                                        viewModel.saveActualQuantities(checkId, updatedList) { success, msg ->
                                            scope.launch { snackbarHostState.showSnackbar(msg) }
                                            if (success) refreshData()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("LƯU TẠM")
                                }

                                // Submit for Approval
                                Button(
                                    onClick = {
                                        if (!isAllCounted) {
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Còn $uncountedCount sản phẩm chưa đếm số lượng!")
                                            }
                                            return@Button
                                        }
                                        showSubmitConfirmDialog = true
                                    },
                                    enabled = isAllCounted,
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                    modifier = Modifier.weight(1.5f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("GỬI XÉT DUYỆT", fontWeight = FontWeight.Bold)
                                }
                            }

                            if (!isAllCounted) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "⚠️ Cần đếm đủ $totalCount/$totalCount sản phẩm mới được gửi xét duyệt (còn thiếu $uncountedCount sản phẩm)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PrimaryBlue,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            }
                        }

                        "Đang chờ xét duyệt" -> {
                            if (com.example.warehouse.utils.PermissionManager.canApproveStockCheck(userRole)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            rejectReason = ""
                                            showRejectDialog = true
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                                        border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFFDC2626))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("TỪ CHỐI", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                                    }

                                    Button(
                                        onClick = { showApproveConfirmDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                        modifier = Modifier.weight(1.5f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("DUYỆT HOÀN TẤT", fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                Text(
                                    text = "Đang chờ Quản lý kho phê duyệt...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD97706),
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            }
                        }

                        "Đã hoàn thành" -> {
                            Surface(
                                color = Color(0xFFDCFCE7),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Kiểm kê đã hoàn thành. Tồn kho thực tế đã được tự động cập nhật.",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF15803D)
                                    )
                                }
                            }
                        }

                        "Đã hủy" -> {
                            Surface(
                                color = Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Cancel, contentDescription = null, tint = Color(0xFF64748B))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Kiểm kê này đã bị hủy.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF475569)
                                    )
                                }
                            }
                        }
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
            // Check Header Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
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
                            text = check.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )

                        // Status Badge
                        val (statusBg, statusFg) = when (check.status) {
                            "Đang tiến hành" -> Pair(Color(0xFFDBEAFE), Color(0xFF1D4ED8))
                            "Yêu cầu kiểm kê lại" -> Pair(Color(0xFFFEE2E2), Color(0xFFDC2626))
                            "Đang chờ xét duyệt" -> Pair(Color(0xFFFEF3C7), Color(0xFFD97706))
                            "Đã hoàn thành" -> Pair(Color(0xFFDCFCE7), Color(0xFF15803D))
                            "Đã hủy" -> Pair(Color(0xFFF3F4F6), Color(0xFF4B5563))
                            else -> Pair(Color(0xFFF1F5F9), Color(0xFF64748B))
                        }

                        Surface(color = statusBg, shape = RoundedCornerShape(16.dp)) {
                            Text(
                                text = check.status,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = statusFg,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Ngày bắt đầu: ${DateUtils.formatDate(check.checkDate)}", style = MaterialTheme.typography.bodySmall)
                    item.user?.let { u ->
                        Text("Người tạo: ${u.fullName}", style = MaterialTheme.typography.bodySmall)
                    }

                    if (check.status == "Đã hủy") {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFFCA5A5))),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "PHIẾU NÀY ĐÃ BỊ HỦY",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF991B1B)
                                    )
                                }
                                if (check.deleteReason != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Lý do hủy: ${check.deleteReason}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF7F1D1D)
                                    )
                                }
                            }
                        }
                    }

                    // Không có ghi chú thì KHÔNG hiển thị
                    if (check.note.isNotBlank()) {
                        Text(
                            "Ghi chú: ${check.note}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Progress indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Tiến độ đếm: $countedCount / $totalCount sản phẩm",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (totalDiscrepancyItems > 0) {
                            Text(
                                "Chênh lệch: $totalDiscrepancyItems SP",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { if (totalCount > 0) countedCount.toFloat() / totalCount else 0f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (isAllCounted) Color(0xFF16A34A) else PrimaryBlue
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Management Action Buttons (Nút Chỉnh sửa chuyển thành Icon Bút chì)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (check.status in listOf("Đang tiến hành", "Yêu cầu kiểm kê lại") && com.example.warehouse.utils.PermissionManager.canManageStockCheckSession(userRole)) {
                            IconButton(onClick = { showEditSheet = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Chỉnh sửa kiểm kê", tint = PrimaryBlue)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        if (check.status in listOf("Đang tiến hành", "Yêu cầu kiểm kê lại", "Đang chờ xét duyệt") && com.example.warehouse.utils.PermissionManager.canManageStockCheckSession(userRole)) {
                            IconButton(onClick = {
                                cancelReason = ""
                                showDeleteConfirmDialog = true
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Hủy kiểm kê", tint = PrimaryBlue)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar & QR Scanner
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Tìm sản phẩm...") },
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
                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Quét QR", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Detail Items List
            val canEditActuals = check.status in listOf("Đang tiến hành", "Yêu cầu kiểm kê lại")

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(filteredDetails, key = { it.detail.detailId }) { detailWithProd ->
                    val p = detailWithProd.product
                    val d = detailWithProd.detail

                    val categoryName = categoryMap[p.categoryId] ?: "Chưa phân loại"
                    val minStock = p.minStock

                    val currentActualStr = localActuals[d.detailId] ?: "0"
                    val currentActualInt = currentActualStr.toIntOrNull() ?: 0

                    val diff = currentActualInt - d.systemQuantity

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (canEditActuals) {
                                    Modifier.clickable {
                                        selectedDetailForQtyEdit = detailWithProd
                                        dialogTempQty = localActuals[d.detailId]?.ifBlank { "0" } ?: "0"
                                    }
                                } else Modifier
                            )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Dòng trên: Tên sản phẩm              0 (số đếm thực tế)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = p.productName,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "$currentActualInt",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryBlue
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Dòng dưới: Danh mục | Tồn an toàn: minStock
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$categoryName | Tồn an toàn: $minStock",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )

                                if (localActuals.containsKey(d.detailId) && localActuals[d.detailId]?.isNotBlank() == true) {
                                    Surface(
                                        color = when {
                                            diff > 0 -> Color(0xFFDCFCE7)
                                            diff < 0 -> Color(0xFFFEE2E2)
                                            else -> Color(0xFFF1F5F9)
                                        },
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = when {
                                                diff > 0 -> "Thừa +$diff"
                                                diff < 0 -> "Thiếu $diff"
                                                else -> "Khớp 100%"
                                            },
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = when {
                                                diff > 0 -> Color(0xFF15803D)
                                                diff < 0 -> Color(0xFFB91C1C)
                                                else -> Color(0xFF475569)
                                            },
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
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

    // Bảng nhỏ nhập số lượng khi click vào sản phẩm
    val currentSelectedDetail = selectedDetailForQtyEdit
    if (currentSelectedDetail != null) {
        val p = currentSelectedDetail.product
        val d = currentSelectedDetail.detail

        AlertDialog(
            onDismissRequest = { selectedDetailForQtyEdit = null },
            title = { Text("Vui lòng nhập số lượng", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        p.productName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                val curr = dialogTempQty.toIntOrNull() ?: 0
                                if (curr > 0) dialogTempQty = (curr - 1).toString()
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    Color(0xFFF1F5F9),
                                    RoundedCornerShape(8.dp)
                                )
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Giảm", tint = PrimaryBlue)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        OutlinedTextField(
                            value = dialogTempQty,
                            onValueChange = { newValue ->
                                if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                    dialogTempQty = newValue
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                fontSize = MaterialTheme.typography.titleLarge.fontSize
                            ),
                            modifier = Modifier.width(100.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        IconButton(
                            onClick = {
                                val curr = dialogTempQty.toIntOrNull() ?: 0
                                dialogTempQty = (curr + 1).toString()
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    Color(0xFFF1F5F9),
                                    RoundedCornerShape(8.dp)
                                )
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Tăng", tint = PrimaryBlue)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalQty = dialogTempQty.ifBlank { "0" }
                        localActuals[d.detailId] = finalQty
                        selectedDetailForQtyEdit = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("XÁC NHẬN")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedDetailForQtyEdit = null }) {
                    Text("HỦY")
                }
            }
        )
    }

    // QR Code Scanner Sheet
    if (showScannerSheet) {
        BarcodeScannerSheet(
            onCodeScanned = { barcode ->
                showScannerSheet = false
                val match = details.find { it.product.productCode.equals(barcode.trim(), ignoreCase = true) }
                if (match != null) {
                    searchQuery = match.product.productCode
                    scope.launch {
                        snackbarHostState.showSnackbar("Đã tìm thấy sản phẩm: ${match.product.productName}")
                    }
                } else {
                    scope.launch {
                        snackbarHostState.showSnackbar("Sản phẩm có mã $barcode không có trong kiểm kê này!")
                    }
                }
            },
            onDismiss = { showScannerSheet = false }
        )
    }

    // Trang Chỉnh sửa kiểm kê cố định (Dialog Full Screen) không bị lỗi bàn phím
    if (showEditSheet && checkWithDetails != null) {
        EditInventoryDialog(
            initialTitle = check.title,
            initialNote = check.note,
            initialCheckDate = check.checkDate,
            initialSelectedProductIds = details.map { it.product.productId }.toSet(),
            viewModel = viewModel,
            onDismiss = { showEditSheet = false },
            onSave = { newTitle, newNote, newDate, newProductIds ->
                viewModel.updateStockCheckHeaderAndProducts(
                    checkId = checkId,
                    title = newTitle,
                    note = newNote,
                    checkDate = newDate,
                    productIds = newProductIds,
                    actingUserRole = userRole
                ) { ok, msg ->
                    scope.launch { snackbarHostState.showSnackbar(msg) }
                    if (ok) {
                        showEditSheet = false
                        refreshData()
                    }
                }
            }
        )
    }

    // Confirm Submit for Approval Dialog
    if (showSubmitConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showSubmitConfirmDialog = false },
            title = { Text("Xác nhận gửi xét duyệt", fontWeight = FontWeight.Bold, color = PrimaryBlue) },
            text = { Text("Bạn có chắc chắn muốn gửi đợt kiểm kê này để Quản lý xét duyệt không?") },
            confirmButton = {
                Button(
                    onClick = {
                        showSubmitConfirmDialog = false
                        val updatedList = details.map { d ->
                            val actualInt = localActuals[d.detail.detailId]?.trim()?.toIntOrNull()
                            val reasonStr = localReasons[d.detail.detailId]?.trim() ?: ""
                            d.detail.copy(
                                actualQuantity = actualInt,
                                difference = actualInt?.minus(d.detail.systemQuantity),
                                reason = reasonStr
                            )
                        }

                        viewModel.saveActualQuantities(checkId, updatedList) { saveOk, saveMsg ->
                            if (saveOk) {
                                viewModel.submitForApproval(checkId) { subOk, subMsg ->
                                    scope.launch {
                                        snackbarHostState.showSnackbar(if (subOk) "Đã gửi đợt kiểm kê để xét duyệt thành công!" else subMsg)
                                    }
                                    refreshData()
                                }
                            } else {
                                scope.launch { snackbarHostState.showSnackbar(saveMsg) }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("GỬI XÉT DUYỆT", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSubmitConfirmDialog = false }) { Text("HỦY") }
            }
        )
    }

    // Reject Dialog with required reason input
    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = { Text("Từ chối đợt kiểm kê", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626)) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Nhập lý do từ chối để chuyển đợt kiểm kê về trạng thái 'Yêu cầu kiểm kê lại':",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = rejectReason,
                        onValueChange = { rejectReason = it },
                        label = { Text("Lý do từ chối *") },
                        placeholder = { Text("Nhập lý do từ chối...") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val reason = rejectReason.trim()
                        if (reason.isBlank()) return@Button
                        showRejectDialog = false
                        viewModel.rejectStockCheck(checkId, reason, actingUserRole = userRole) { ok, msg ->
                            scope.launch {
                                snackbarHostState.showSnackbar(if (ok) "Đã từ chối kiểm kê và yêu cầu kiểm kê lại thành công!" else msg)
                            }
                            if (ok) refreshData()
                        }
                    },
                    enabled = rejectReason.trim().isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("XÁC NHẬN TỪ CHỐI", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialog = false }) { Text("HỦY") }
            }
        )
    }

    // Confirm Delete Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Xác nhận hủy kiểm kê", fontWeight = FontWeight.Bold, color = PrimaryBlue) },
            text = {
                Column {
                    Text("Bạn có chắc chắn muốn hủy đợt kiểm kê này? Trạng thái sẽ được chuyển sang 'Đã hủy' và không thể tiếp tục thực hiện.")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = cancelReason,
                        onValueChange = { cancelReason = it },
                        label = { Text("Lý do hủy kiểm kê (Bắt buộc)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("delete_reason_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.cancelStockCheck(checkId, cancelReason.trim(), actingUserRole = userRole) { ok, msg ->
                            if (ok) {
                                onNavigateBackWithMsg("Đã hủy đợt kiểm kê thành công!")
                            } else {
                                scope.launch { snackbarHostState.showSnackbar(msg) }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    enabled = cancelReason.isNotBlank()
                ) {
                    Text("HỦY KIỂM KÊ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("ĐÓNG") }
            }
        )
    }

    // Confirm Approve Dialog
    if (showApproveConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showApproveConfirmDialog = false },
            title = { Text("Xác nhận phê duyệt kiểm kê", fontWeight = FontWeight.Bold, color = PrimaryBlue) },
            text = { Text("Hệ thống sẽ CẬP NHẬT TỒN KHO THỰC TẾ cho tất cả các sản phẩm trong kiểm kê này. Thao tác này là không thể hoàn tác. Bạn có chắc chắn muốn duyệt?") },
            confirmButton = {
                Button(
                    onClick = {
                        showApproveConfirmDialog = false
                        viewModel.approveStockCheck(checkId, actingUserRole = userRole) { ok, msg ->
                            scope.launch { snackbarHostState.showSnackbar(msg) }
                            refreshData()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                ) {
                    Text("XÁC NHẬN DUYỆT")
                }
            },
            dismissButton = {
                TextButton(onClick = { showApproveConfirmDialog = false }) { Text("HỦY") }
            }
        )
    }
}

// Fixed Full-Screen Dialog for "Chỉnh sửa kiểm kê" (Khắc phục hoàn toàn lỗi do bàn phím hiển thị)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditInventoryDialog(
    initialTitle: String,
    initialNote: String,
    initialCheckDate: Long,
    initialSelectedProductIds: Set<Long>,
    viewModel: InventoryViewModel,
    onDismiss: () -> Unit,
    onSave: (title: String, note: String, date: Long, productIds: List<Long>) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var note by remember { mutableStateOf(initialNote) }
    var checkDate by remember { mutableStateOf(initialCheckDate) }
    var scopeType by remember { mutableStateOf(StockCheckScope.MANUAL) }

    val categories by viewModel.categories.collectAsState()
    val activeProducts by viewModel.activeProducts.collectAsState()

    var selectedCategoryIds by remember { mutableStateOf(emptySet<Long>()) }
    var selectedProductIds by remember { mutableStateOf(initialSelectedProductIds) }

    var searchQuery by remember { mutableStateOf("") }
    var categorySearchQuery by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showScannerSheet by remember { mutableStateOf(false) }
    var scopeExpanded by remember { mutableStateOf(false) }

    val resolvedProductIds = remember(scopeType, selectedCategoryIds, selectedProductIds, activeProducts) {
        when (scopeType) {
            StockCheckScope.ALL -> activeProducts.map { it.product.id }
            StockCheckScope.CATEGORY -> {
                activeProducts.filter { selectedCategoryIds.contains(it.product.categoryId) }.map { it.product.id }
            }
            StockCheckScope.MANUAL -> selectedProductIds.toList()
        }
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

    val isTitleBlank = title.isBlank()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars),
            color = Color.White
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Chỉnh sửa kiểm kê", fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Đóng")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                    )
                },
                bottomBar = {
                    Surface(tonalElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.padding(16.dp)) {
                            Button(
                                onClick = {
                                    if (isTitleBlank) return@Button
                                    if (resolvedProductIds.isEmpty()) return@Button
                                    onSave(title.trim(), note.trim(), checkDate, resolvedProductIds)
                                },
                                enabled = !isTitleBlank && resolvedProductIds.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("LƯU THAY ĐỔI (${resolvedProductIds.size} SẢN PHẨM)", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Card Thông tin kiểm kê
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                label = { Text("Tiêu đề kiểm kê *") },
                                placeholder = { Text("Nhập tiêu đề kiểm kê...") },
                                singleLine = true,
                                isError = isTitleBlank,
                                supportingText = {
                                    if (isTitleBlank) {
                                        Text("Bắt buộc phải nhập tiêu đề kiểm kê", color = PrimaryBlue)
                                    }
                                },
                                trailingIcon = {
                                    if (isTitleBlank) {
                                        Icon(Icons.Default.Error, contentDescription = null, tint = PrimaryBlue)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = note,
                                onValueChange = { note = it },
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
                                    Text(DateUtils.formatDate(checkDate), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                }
                                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = PrimaryBlue)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Phạm vi kiểm kê Combobox
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
                                            scopeType = StockCheckScope.ALL
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
                                            scopeType = StockCheckScope.CATEGORY
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
                                            scopeType = StockCheckScope.MANUAL
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

                    // Details view according to selected scope
                    Box(modifier = Modifier.weight(1f)) {
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
                                            Text("Kiểm kê toàn bộ kho", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                            Text("Bao gồm tất cả ${activeProducts.size} sản phẩm trong kho.", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }

                            StockCheckScope.CATEGORY -> {
                                Column {
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

                                    Spacer(modifier = Modifier.height(6.dp))

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
                                            TextButton(onClick = { selectedCategoryIds = categories.map { it.categoryId }.toSet() }) {
                                                Text("Chọn tất cả")
                                            }
                                            TextButton(onClick = { selectedCategoryIds = emptySet() }) {
                                                Text("Bỏ chọn")
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        items(filteredCategories, key = { it.categoryId }) { cat ->
                                            val isSelected = selectedCategoryIds.contains(cat.categoryId)
                                            val catProductCount = activeProducts.count { it.product.categoryId == cat.categoryId }

                                            Card(
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isSelected) Color(0xFFDBEAFE) else Color.White
                                                ),
                                                onClick = {
                                                    selectedCategoryIds = if (isSelected) selectedCategoryIds - cat.categoryId else selectedCategoryIds + cat.categoryId
                                                },
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
                                                            onCheckedChange = {
                                                                selectedCategoryIds = if (isSelected) selectedCategoryIds - cat.categoryId else selectedCategoryIds + cat.categoryId
                                                            }
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(cat.categoryName, fontWeight = FontWeight.Bold)
                                                    }
                                                    Text("$catProductCount SP", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            StockCheckScope.MANUAL -> {
                                Column {
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
                                            TextButton(onClick = { selectedProductIds = activeProducts.map { it.product.id }.toSet() }) {
                                                Text("Chọn tất cả")
                                            }
                                            TextButton(onClick = { selectedProductIds = emptySet() }) {
                                                Text("Bỏ chọn")
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

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
                                            Icon(Icons.Default.QrCodeScanner, contentDescription = "Quét QR", tint = Color.White)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        items(filteredManualProducts, key = { it.product.id }) { item ->
                                            val p = item.product
                                            val isSelected = selectedProductIds.contains(p.id)

                                            Card(
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isSelected) Color(0xFFDBEAFE).copy(alpha = 0.5f) else Color.White
                                                ),
                                                onClick = {
                                                    selectedProductIds = if (isSelected) selectedProductIds - p.id else selectedProductIds + p.id
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Checkbox(
                                                        checked = isSelected,
                                                        onCheckedChange = {
                                                            selectedProductIds = if (isSelected) selectedProductIds - p.id else selectedProductIds + p.id
                                                        }
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

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // Date Picker Dialog in Modal
        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = checkDate)
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

                            if (selected <= todayEnd) {
                                checkDate = selected
                                showDatePicker = false
                            }
                        }
                    }) {
                        Text("XÁC NHẬN", fontWeight = FontWeight.Bold, color = PrimaryBlue)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("HỦY") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        // Barcode Scanner in Modal
        if (showScannerSheet) {
            BarcodeScannerSheet(
                onCodeScanned = { barcode ->
                    showScannerSheet = false
                    val match = activeProducts.find { it.product.productCode.equals(barcode.trim(), ignoreCase = true) }
                    if (match != null) {
                        selectedProductIds = selectedProductIds + match.product.id
                    }
                },
                onDismiss = { showScannerSheet = false }
            )
        }
    }
}

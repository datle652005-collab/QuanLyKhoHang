package com.example.warehouse.ui.product

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.warehouse.data.database.relation.ProductWithCategoryAndSupplier
import com.example.warehouse.ui.component.ConfirmDeleteDialog
import com.example.warehouse.ui.component.StockHistoryItemCard
import com.example.warehouse.ui.theme.PrimaryBlue
import com.example.warehouse.ui.theme.PrimaryBlueContainer
import com.example.warehouse.ui.theme.PrimaryBlueDark
import com.example.warehouse.ui.theme.PrimaryBlueLight
import com.example.warehouse.ui.theme.StockDanger
import com.example.warehouse.ui.theme.StockNormal
import com.example.warehouse.utils.CurrencyFormatter
import com.example.warehouse.viewmodel.HistoryViewModel
import com.example.warehouse.viewmodel.ProductViewModel

import androidx.compose.ui.graphics.asImageBitmap
import com.example.warehouse.util.BarcodeGenerator

@Composable
fun ProductDetailScreen(
    productId: Long,
    productViewModel: ProductViewModel,
    historyViewModel: HistoryViewModel,
    onNavigateToEdit: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateBackWithMsg: ((String) -> Unit)? = null,
    successMessage: String? = null,
    onClearSuccessMessage: () -> Unit = {},
    userRole: String? = null,
    onNavigateToReceiptDetail: (String, Long) -> Unit = { _, _ -> }
) {
    var productWithDetails by remember { mutableStateOf<ProductWithCategoryAndSupplier?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var infoDialogMessage by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    var activeSnackbarMessage by remember { mutableStateOf<String?>(null) }
    var snackbarJob by remember { mutableStateOf<Job?>(null) }

    val historyList by historyViewModel.stockHistory.collectAsState()
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

    fun refreshData() {
        coroutineScope.launch {
            productWithDetails = productViewModel.getProductWithDetailsById(productId)
        }
    }

    LaunchedEffect(successMessage) {
        successMessage?.let { msg ->
            showSingleSnackbar(msg)
            refreshData()
            onClearSuccessMessage()
        }
    }

    LaunchedEffect(productId) {
        productWithDetails = productViewModel.getProductWithDetailsById(productId)
        historyViewModel.selectProductFilter(productId)
    }

    val item = productWithDetails
    if (item == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PrimaryBlue)
        }
        return
    }

    val p = item.product
    val stockQty = item.quantityInStock

    val (stockStatusText, stockStatusBg, stockStatusTextCol) = when {
        stockQty <= 0 -> Triple("Hết hàng", Color(0xFFFEE2E2), Color(0xFFDC2626))
        stockQty <= p.minStock -> Triple("Sắp hết", Color(0xFFFEF3C7), Color(0xFFD97706))
        else -> Triple("Đủ hàng", Color(0xFFDCFCE7), Color(0xFF16A34A))
    }

    val qrBitmap = remember(p.productCode) { BarcodeGenerator.generateQRCode(p.productCode, 400, 400) }

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
            Surface(
                tonalElevation = 8.dp,
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (com.example.warehouse.utils.PermissionManager.canDeleteProduct(userRole)) {
                        OutlinedButton(
                            onClick = { showDeleteConfirm = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("delete_product_btn")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Xóa SP", fontWeight = FontWeight.Bold)
                        }
                    }
                    Button(
                        onClick = { onNavigateToEdit(productId) },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("edit_product_btn")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Chỉnh sửa", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                // Image Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(PrimaryBlueLight),
                    contentAlignment = Alignment.Center
                ) {
                    if (p.image.isNotEmpty()) {
                        AsyncImage(
                            model = p.image,
                            contentDescription = p.productName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = PrimaryBlue
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Titles & Badges
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = PrimaryBlueContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = p.productCode,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlueDark,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    item.category?.let { cat ->
                        Surface(
                            color = Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = cat.name,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF475569),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Status Badge
                    Surface(
                        color = if (p.status == "ACTIVE") Color(0xFFDCFCE7) else Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (p.status == "ACTIVE") "Đang kinh doanh" else "Ngừng kinh doanh",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (p.status == "ACTIVE") Color(0xFF15803D) else Color(0xFF64748B),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = p.productName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Stock Info Card with 3 Visual States
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Số lượng tồn kho",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.Gray
                            )
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "$stockQty",
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = stockStatusTextCol
                                )
                            }
                            Text(
                                text = "Mức tồn tối thiểu (An toàn): ${p.minStock}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }

                        Surface(
                            color = stockStatusBg,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = stockStatusText.uppercase(),
                                color = stockStatusTextCol,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Prices Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Giá xuất kho (Bán)", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                            Text(CurrencyFormatter.formatVnd(p.exportPrice), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = PrimaryBlueDark)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // QR Code Card (ZXing)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Mã QR Code Sản Phẩm (ZXing)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        qrBitmap?.let { bmp ->
                            androidx.compose.foundation.Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Mã QR",
                                modifier = Modifier.size(160.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Mã QR: ${p.productCode}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                    }
                }

                if (p.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Mô tả sản phẩm", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(p.description, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF334155))
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text("Lịch sử nhập / xuất / kiểm kê", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                Spacer(modifier = Modifier.height(8.dp))

                if (historyList.isEmpty()) {
                    Text(
                        text = "Chưa có lịch sử giao dịch nào",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            historyList.forEach { history ->
                                StockHistoryItemCard(
                                    history = history,
                                    useTypeInsteadOfCode = true,
                                    onClick = {
                                        if (history.referenceId != null) {
                                            onNavigateToReceiptDetail(history.action, history.referenceId)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        ConfirmDeleteDialog(
            title = "Xóa sản phẩm",
            message = "Bạn có chắc chắn muốn xóa sản phẩm '${p.productName}'?",
            onConfirm = {
                showDeleteConfirm = false
                productViewModel.deleteProduct(p, userRole) { isHardDeleted, message ->
                    if (isHardDeleted) {
                        onNavigateBackWithMsg?.invoke("Xóa sản phẩm thành công") ?: onNavigateBack()
                    } else {
                        infoDialogMessage = message
                        refreshData()
                    }
                }
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }

    if (infoDialogMessage != null) {
        AlertDialog(
            onDismissRequest = { infoDialogMessage = null },
            title = { Text("Thông báo xóa sản phẩm", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)) },
            text = { Text(infoDialogMessage ?: "", color = Color(0xFF334155)) },
            confirmButton = {
                TextButton(onClick = { infoDialogMessage = null }) {
                    Text("Đã hiểu", color = PrimaryBlue, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

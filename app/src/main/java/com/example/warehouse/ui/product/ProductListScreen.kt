package com.example.warehouse.ui.product

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.warehouse.ui.component.*
import com.example.warehouse.ui.theme.PrimaryBlue
import com.example.warehouse.ui.theme.PrimaryBlueContainer
import com.example.warehouse.ui.theme.PrimaryBlueLight
import com.example.warehouse.viewmodel.ProductViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    viewModel: ProductViewModel,
    onNavigateToAddProduct: () -> Unit,
    onNavigateToProductDetail: (Long) -> Unit,
    successMessage: String? = null,
    onClearSuccessMessage: () -> Unit = {}
) {
    val products by viewModel.products.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCatId by viewModel.selectedCategoryId.collectAsState()
    val statusFilter by viewModel.statusFilter.collectAsState()

    var showScannerSheet by remember { mutableStateOf(false) }
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

    val lowStockCount = products.count { it.quantityInStock <= it.product.minStock }

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
                onClick = onNavigateToAddProduct,
                containerColor = PrimaryBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("fab_add_product")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Thêm sản phẩm")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp)
        ) {

            // Search bar with barcode scanner action
            WarehouseSearchBar(
                query = searchQuery,
                onQueryChange = { viewModel.onSearchQueryChange(it) },
                placeholder = "Tìm kiếm sản phẩm, mã SP (QR)...",
                onScanClick = { showScannerSheet = true }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Summary metrics bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = PrimaryBlueLight,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Tổng số: ${products.size} sản phẩm",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                    }
                }

                if (lowStockCount > 0) {
                    Surface(
                        color = Color(0xFFFEE2E2),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Cảnh báo tồn: $lowStockCount",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2 Comboboxes on 1 line: Trạng thái hoạt động & Tất cả danh mục
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Combobox 1: Trạng thái hoạt động
                var statusExpanded by remember { mutableStateOf(false) }
                val statusText = when (statusFilter) {
                    "ACTIVE" -> "Đang kinh doanh"
                    "INACTIVE" -> "Ngừng kinh doanh"
                    else -> "Trạng thái hoạt động"
                }

                Box(modifier = Modifier.weight(1f)) {
                    OutlinedCard(
                        onClick = { statusExpanded = true },
                        colors = CardDefaults.outlinedCardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, if (statusFilter != "ALL") PrimaryBlue else Color(0xFFCBD5E1)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("combobox_status")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = statusText,
                                fontSize = 13.sp,
                                fontWeight = if (statusFilter != "ALL") FontWeight.Bold else FontWeight.Medium,
                                color = if (statusFilter != "ALL") PrimaryBlue else Color(0xFF1E293B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Trạng thái hoạt động",
                                tint = if (statusFilter != "ALL") PrimaryBlue else Color(0xFF64748B)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = statusExpanded,
                        onDismissRequest = { statusExpanded = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Tất cả trạng thái",
                                    fontWeight = if (statusFilter == "ALL") FontWeight.Bold else FontWeight.Normal,
                                    color = if (statusFilter == "ALL") PrimaryBlue else Color(0xFF1E293B)
                                )
                            },
                            onClick = {
                                viewModel.setStatusFilter("ALL")
                                statusExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Đang kinh doanh",
                                    fontWeight = if (statusFilter == "ACTIVE") FontWeight.Bold else FontWeight.Normal,
                                    color = if (statusFilter == "ACTIVE") PrimaryBlue else Color(0xFF1E293B)
                                )
                            },
                            onClick = {
                                viewModel.setStatusFilter("ACTIVE")
                                statusExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Ngừng kinh doanh",
                                    fontWeight = if (statusFilter == "INACTIVE") FontWeight.Bold else FontWeight.Normal,
                                    color = if (statusFilter == "INACTIVE") PrimaryBlue else Color(0xFF1E293B)
                                )
                            },
                            onClick = {
                                viewModel.setStatusFilter("INACTIVE")
                                statusExpanded = false
                            }
                        )
                    }
                }

                // Combobox 2: Tất cả danh mục
                var categoryExpanded by remember { mutableStateOf(false) }
                val categoryText = categories.find { it.id == selectedCatId }?.name ?: "Tất cả danh mục"

                Box(modifier = Modifier.weight(1f)) {
                    OutlinedCard(
                        onClick = { categoryExpanded = true },
                        colors = CardDefaults.outlinedCardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, if (selectedCatId != null) PrimaryBlue else Color(0xFFCBD5E1)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("combobox_category")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = categoryText,
                                fontSize = 13.sp,
                                fontWeight = if (selectedCatId != null) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedCatId != null) PrimaryBlue else Color(0xFF1E293B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Tất cả danh mục",
                                tint = if (selectedCatId != null) PrimaryBlue else Color(0xFF64748B)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Tất cả danh mục",
                                    fontWeight = if (selectedCatId == null) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedCatId == null) PrimaryBlue else Color(0xFF1E293B)
                                )
                            },
                            onClick = {
                                viewModel.selectCategoryFilter(null)
                                categoryExpanded = false
                            }
                        )
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = cat.name,
                                        fontWeight = if (selectedCatId == cat.id) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selectedCatId == cat.id) PrimaryBlue else Color(0xFF1E293B)
                                    )
                                },
                                onClick = {
                                    viewModel.selectCategoryFilter(cat.id)
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            if (products.isEmpty()) {
                WarehouseEmptyView(
                    title = "Chưa có sản phẩm nào",
                    message = if (searchQuery.isNotEmpty()) "Không có sản phẩm khớp với '$searchQuery'" else "Nhấn + ở góc trên để thêm sản phẩm đầu tiên",
                    icon = Icons.Default.Inventory2,
                    onActionClick = onNavigateToAddProduct,
                    actionTitle = "Thêm sản phẩm"
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(products, key = { it.product.id }) { item ->
                        ProductItemCard(
                            item = item,
                            onClick = { onNavigateToProductDetail(item.product.id) }
                        )
                    }
                }
            }
        }
    }

    if (showScannerSheet) {
        BarcodeScannerSheet(
            onCodeScanned = { barcode ->
                viewModel.lookupBarcode(barcode)
                showScannerSheet = false
            },
            onDismiss = { showScannerSheet = false }
        )
    }
}


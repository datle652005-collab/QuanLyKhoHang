@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.warehouse.ui.inventory

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.warehouse.data.database.relation.ProductWithCategoryAndSupplier
import com.example.warehouse.ui.component.BarcodeScannerSheet
import com.example.warehouse.ui.theme.*
import com.example.warehouse.viewmodel.InventoryAlertsViewModel
import kotlinx.coroutines.launch
import kotlin.math.ceil

@Composable
fun InventoryAlertsScreen(
    viewModel: InventoryAlertsViewModel,
    userRole: String,
    onNavigateToReset: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()
    val selectedAlertType by viewModel.selectedAlertType.collectAsState()
    val scrollToProductId by viewModel.scrollToProductId.collectAsState()
    val listState = rememberLazyListState()

    var showCategoryMenu by remember { mutableStateOf(false) }
    var showAlertFilterMenu by remember { mutableStateOf(false) }

    LaunchedEffect(scrollToProductId, state.filteredProducts) {
        if (scrollToProductId != null && state.filteredProducts.isNotEmpty()) {
            val index = state.filteredProducts.indexOfFirst { it.product.productId == scrollToProductId }
            if (index >= 0) {
                listState.animateScrollToItem(index)
                // Clear the scroll target after scrolling
                viewModel.setScrollToProductId(null)
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = LightBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- 1. Three Top Statistics Cards ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Out of stock (Đã hết hàng)
                StatCard(
                    title = "Đã hết hàng",
                    count = state.outOfStockCount,
                    bgColor = Color(0xFFFEF2F2),
                    borderColor = Color(0xFFFCA5A5),
                    textColor = Color(0xFF991B1B),
                    icon = Icons.Default.Cancel,
                    iconColor = DangerRed,
                    isSelected = selectedAlertType == "OUT",
                    onClick = {
                        viewModel.setSelectedAlertType(if (selectedAlertType == "OUT") "ALL" else "OUT")
                    },
                    modifier = Modifier.weight(1f)
                )

                // Critical Stock (Gần hết)
                StatCard(
                    title = "Gần hết hàng",
                    count = state.nearOutOfStockCount,
                    bgColor = Color(0xFFFFFBEB),
                    borderColor = Color(0xFFFDE68A),
                    textColor = Color(0xFF92400E),
                    icon = Icons.Default.Warning,
                    iconColor = StockWarning,
                    isSelected = selectedAlertType == "NEAR",
                    onClick = {
                        viewModel.setSelectedAlertType(if (selectedAlertType == "NEAR") "ALL" else "NEAR")
                    },
                    modifier = Modifier.weight(1f)
                )

                // Low Stock (Tồn thấp)
                StatCard(
                    title = "Tồn kho thấp",
                    count = state.lowStockCount,
                    bgColor = Color(0xFFEFF6FF),
                    borderColor = Color(0xFFBFDBFE),
                    textColor = Color(0xFF1E40AF),
                    icon = Icons.Default.TrendingDown,
                    iconColor = PrimaryBlue,
                    isSelected = selectedAlertType == "LOW",
                    onClick = {
                        viewModel.setSelectedAlertType(if (selectedAlertType == "LOW") "ALL" else "LOW")
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // --- 2. Search & Filters Card ---
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Search text field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Tìm theo tên hoặc mã sản phẩm...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("alerts_search_input")
                    )

                    // 2 Filter Comboboxes (Category and Alert Type)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Category Dropdown Filter
                        Box(modifier = Modifier.weight(1f)) {
                            val selectedCatName = state.categories.find { it.categoryId == selectedCategoryId }?.categoryName ?: "Tất cả danh mục"
                            OutlinedCard(
                                onClick = { showCategoryMenu = true },
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.outlinedCardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = selectedCatName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF334155),
                                        maxLines = 1
                                    )
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Gray)
                                }
                            }

                            DropdownMenu(
                                expanded = showCategoryMenu,
                                onDismissRequest = { showCategoryMenu = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Tất cả danh mục") },
                                    onClick = {
                                        viewModel.setSelectedCategory(null)
                                        showCategoryMenu = false
                                    }
                                )
                                state.categories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat.categoryName) },
                                        onClick = {
                                            viewModel.setSelectedCategory(cat.categoryId)
                                            showCategoryMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        // Alert Type Dropdown Filter
                        Box(modifier = Modifier.weight(1f)) {
                            val alertTypeText = when (selectedAlertType) {
                                "LOW" -> "Tồn kho thấp"
                                "NEAR" -> "Gần hết hàng"
                                "OUT" -> "Đã hết hàng"
                                else -> "Tất cả cảnh báo"
                            }
                            OutlinedCard(
                                onClick = { showAlertFilterMenu = true },
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.outlinedCardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = alertTypeText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF334155),
                                        maxLines = 1
                                    )
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Gray)
                                }
                            }

                            DropdownMenu(
                                expanded = showAlertFilterMenu,
                                onDismissRequest = { showAlertFilterMenu = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Tất cả cảnh báo") },
                                    onClick = {
                                        viewModel.setSelectedAlertType("ALL")
                                        showAlertFilterMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Tồn kho thấp") },
                                    onClick = {
                                        viewModel.setSelectedAlertType("LOW")
                                        showAlertFilterMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Gần hết hàng") },
                                    onClick = {
                                        viewModel.setSelectedAlertType("NEAR")
                                        showAlertFilterMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Đã hết hàng") },
                                    onClick = {
                                        viewModel.setSelectedAlertType("OUT")
                                        showAlertFilterMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // --- 3. Product List ---
            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            } else if (state.filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckBox,
                            contentDescription = null,
                            tint = Color(0xFFCBD5E1),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Không tìm thấy sản phẩm cảnh báo nào",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("alerts_list"),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(state.filteredProducts, key = { it.product.productId }) { item ->
                        AlertProductCard(item = item)
                    }
                }
            }
        }
    }
}

@Composable
fun ResetMinStockScreen(
    viewModel: InventoryAlertsViewModel,
    userRole: String,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()

    var showCategoryMenu by remember { mutableStateOf(false) }
    var showScannerSheet by remember { mutableStateOf(false) }
    var selectedProductForReset by remember { mutableStateOf<ProductWithCategoryAndSupplier?>(null) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = LightBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- Search & Category Filters ---
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Search Input with QR/Scanner icon
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Tìm theo tên, mã sản phẩm...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryBlue,
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("reset_search_input")
                        )

                        // QR/Barcode Scanner Action Button
                        IconButton(
                            onClick = { showScannerSheet = true },
                            modifier = Modifier
                                .size(52.dp)
                                .background(PrimaryBlueLight, RoundedCornerShape(8.dp))
                                .testTag("reset_scan_qr_btn")
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan QR", tint = PrimaryBlue)
                        }
                    }

                    // Combobox Category
                    Box(modifier = Modifier.fillMaxWidth()) {
                        val selectedCatName = state.categories.find { it.categoryId == selectedCategoryId }?.categoryName ?: "Tất cả danh mục"
                        OutlinedCard(
                            onClick = { showCategoryMenu = true },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.outlinedCardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedCatName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF334155),
                                    maxLines = 1
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Gray)
                            }
                        }

                        DropdownMenu(
                            expanded = showCategoryMenu,
                            onDismissRequest = { showCategoryMenu = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Tất cả danh mục") },
                                onClick = {
                                    viewModel.setSelectedCategory(null)
                                    showCategoryMenu = false
                                }
                            )
                            state.categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.categoryName) },
                                    onClick = {
                                        viewModel.setSelectedCategory(cat.categoryId)
                                        showCategoryMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // --- Product list to reset ---
            // Display matching products (can be all active products matching search)
            val searchLower = searchQuery.trim().lowercase()
            val resetProducts = state.allProducts.filter { item ->
                val matchesQuery = searchQuery.isEmpty() ||
                        item.product.productName.lowercase().contains(searchLower) ||
                        item.product.productCode.lowercase().contains(searchLower)
                val matchesCategory = selectedCategoryId == null || selectedCategoryId == 0L || item.product.categoryId == selectedCategoryId
                matchesQuery && matchesCategory
            }

            if (resetProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Không tìm thấy sản phẩm nào khớp bộ lọc",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("reset_products_list"),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(resetProducts, key = { it.product.productId }) { item ->
                        ResetProductItemCard(
                            item = item,
                            onClick = { selectedProductForReset = item }
                        )
                    }
                }
            }
        }
    }

    // Camera sheet scanner
    if (showScannerSheet) {
        BarcodeScannerSheet(
            onCodeScanned = { code ->
                viewModel.setSearchQuery(code)
                Toast.makeText(context, "Đã tìm thấy mã: $code", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showScannerSheet = false }
        )
    }

    // Show change threshold dialog
    if (selectedProductForReset != null) {
        ResetMinStockDialog(
            item = selectedProductForReset!!,
            userRole = userRole,
            onDismiss = { selectedProductForReset = null },
            onSave = { newThreshold ->
                scope.launch {
                    val result = viewModel.updateMinStock(
                        productId = selectedProductForReset!!.product.productId,
                        newMinStock = newThreshold,
                        userRole = userRole
                    )
                    selectedProductForReset = null
                    result.onSuccess {
                        scope.launch {
                            snackbarHostState.showSnackbar("Cập nhật ngưỡng tồn kho tối thiểu thành công!")
                        }
                    }.onFailure { err ->
                        scope.launch {
                            snackbarHostState.showSnackbar(err.message ?: "Lỗi khi cập nhật")
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun StatCard(
    title: String,
    count: Int,
    bgColor: Color,
    borderColor: Color,
    textColor: Color,
    icon: ImageVector,
    iconColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(108.dp)
            .animateContentSize(),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) bgColor else Color.White,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) iconColor else borderColor
        ),
        shadowElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(iconColor, RoundedCornerShape(3.dp))
                    )
                }
            }
            Column {
                Text(
                    text = count.toString(),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun AlertProductCard(item: ProductWithCategoryAndSupplier) {
    val qty = item.quantityInStock
    val min = item.product.minStock

    // Determine state
    val (statusLabel, badgeBg, badgeText) = when {
        qty == 0 -> Triple("Đã hết hàng", Color(0xFFFEF2F2), Color(0xFFDC2626))
        qty <= ceil(min * 0.5f).toInt() -> Triple("Gần hết", Color(0xFFFFFBEB), Color(0xFFD97706))
        else -> Triple("Tồn thấp", Color(0xFFEFF6FF), Color(0xFF2563EB))
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.product.productName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Mã: ${item.product.productCode}",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // Status badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statusLabel,
                        color = badgeText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Category, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    Text(
                        text = item.category?.categoryName ?: "Chưa phân loại",
                        fontSize = 13.sp,
                        color = Color(0xFF475569),
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "Tồn hiện tại", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        Text(
                            text = "$qty sản phẩm",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (qty == 0) DangerRed else Color(0xFF0F172A)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "Mức tối thiểu", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        Text(
                            text = "$min sản phẩm",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = Color(0xFF475569)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ResetProductItemCard(
    item: ProductWithCategoryAndSupplier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.product.productName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.product.productCode,
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(text = "•", color = Color(0xFFCBD5E1))
                    Text(
                        text = item.category?.categoryName ?: "Chưa phân loại",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Tối thiểu",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = item.product.minStock.toString(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = PrimaryBlue
                    )
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit min stock",
                        tint = Color.LightGray,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ResetMinStockDialog(
    item: ProductWithCategoryAndSupplier,
    userRole: String,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var minStockInput by remember { mutableStateOf(item.product.minStock.toString()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val currentVal = minStockInput.toIntOrNull() ?: 0

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(26.dp)
                )
                Text(
                    text = "Đặt lại tồn kho tối thiểu",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Info block
                Surface(
                    color = PrimaryBlueLight,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = item.product.productName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = PrimaryBlueDark
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Mã: ${item.product.productCode}  •  Danh mục: ${item.category?.categoryName ?: "Chưa phân loại"}",
                            fontSize = 11.sp,
                            color = Color(0xFF475569)
                        )
                    }
                }

                Text(
                    text = "Nhập số lượng tồn tối thiểu mới:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF334155)
                )

                // Stepper Row: [-]   Input   [+]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Minus Button
                    IconButton(
                        onClick = {
                            val nextVal = (currentVal - 1).coerceAtLeast(0)
                            minStockInput = nextVal.toString()
                            errorMessage = null
                        },
                        enabled = currentVal > 0,
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                if (currentVal > 0) PrimaryBlueLight else Color(0xFFF1F5F9),
                                RoundedCornerShape(8.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Decrease",
                            tint = if (currentVal > 0) PrimaryBlue else Color.Gray
                        )
                    }

                    // Input Field Box
                    OutlinedTextField(
                        value = minStockInput,
                        onValueChange = { input ->
                            if (input.isEmpty()) {
                                minStockInput = ""
                                errorMessage = "Vui lòng nhập một số hợp lệ"
                            } else {
                                val parsed = input.toIntOrNull()
                                if (parsed != null) {
                                    if (parsed < 0) {
                                        errorMessage = "Số lượng không được âm"
                                    } else {
                                        minStockInput = parsed.toString()
                                        errorMessage = null
                                    }
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier
                            .width(120.dp)
                            .padding(horizontal = 12.dp)
                            .testTag("reset_min_stock_input_field")
                    )

                    // Plus Button
                    IconButton(
                        onClick = {
                            val nextVal = currentVal + 1
                            minStockInput = nextVal.toString()
                            errorMessage = null
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .background(PrimaryBlueLight, RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Increase",
                            tint = PrimaryBlue
                        )
                    }
                }

                // Error notice if any
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = DangerRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalVal = minStockInput.toIntOrNull()
                    if (finalVal == null || finalVal < 0) {
                        errorMessage = "Vui lòng nhập một số nguyên không âm"
                    } else {
                        onSave(finalVal)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryBlue,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("save_reset_min_stock_btn")
            ) {
                Text("Lưu", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF64748B))
            ) {
                Text("Hủy", fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

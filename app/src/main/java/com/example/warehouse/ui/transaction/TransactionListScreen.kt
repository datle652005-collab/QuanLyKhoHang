package com.example.warehouse.ui.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.North
import androidx.compose.material.icons.filled.South
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.warehouse.ui.component.ExportReceiptCard
import com.example.warehouse.ui.component.ImportReceiptCard
import com.example.warehouse.ui.component.WarehouseEmptyView
import com.example.warehouse.ui.component.WarehouseTopBar
import com.example.warehouse.viewmodel.ExportViewModel
import com.example.warehouse.viewmodel.ImportViewModel

import com.example.warehouse.data.database.relation.ExportReceiptWithDetails
import com.example.warehouse.data.database.relation.ImportReceiptWithDetails
import com.example.warehouse.ui.theme.PrimaryBlue
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private sealed class TransactionListItem {
    abstract val timestamp: Long
    abstract val key: String

    data class Import(val item: ImportReceiptWithDetails) : TransactionListItem() {
        override val timestamp: Long get() = item.receipt.createdAt.takeIf { it > 0 } ?: item.receipt.importDate
        override val key: String get() = "imp_${item.receipt.id}"
    }

    data class Export(val item: ExportReceiptWithDetails) : TransactionListItem() {
        override val timestamp: Long get() = item.receipt.createdAt.takeIf { it > 0 } ?: item.receipt.exportDate
        override val key: String get() = "exp_${item.receipt.id}"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    importViewModel: ImportViewModel,
    exportViewModel: ExportViewModel,
    onNavigateToImportCreate: () -> Unit,
    onNavigateToExportCreate: () -> Unit,
    onNavigateToImportDetail: (Long) -> Unit,
    onNavigateToExportDetail: (Long) -> Unit,
    successMessage: String? = null,
    onClearSuccessMessage: () -> Unit = {}
) {
    val importReceipts by importViewModel.importReceipts.collectAsState()
    val exportReceipts by exportViewModel.exportReceipts.collectAsState()

    var showSelectTransactionSheet by remember { mutableStateOf(false) }
    var filterType by remember { mutableStateOf("ALL") } // ALL, IMPORT, EXPORT
    var filterMenuExpanded by remember { mutableStateOf(false) }

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

    val transactionItems = remember(importReceipts, exportReceipts, filterType) {
        val list = mutableListOf<TransactionListItem>()
        if (filterType == "ALL" || filterType == "IMPORT") {
            list.addAll(importReceipts.map { TransactionListItem.Import(it) })
        }
        if (filterType == "ALL" || filterType == "EXPORT") {
            list.addAll(exportReceipts.map { TransactionListItem.Export(it) })
        }
        list.sortedByDescending { it.timestamp }
    }

    val hasTransactions = transactionItems.isNotEmpty()

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
                onClick = { showSelectTransactionSheet = true },
                containerColor = PrimaryBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("add_transaction_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Thêm giao dịch")
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

            // Filter Button matching ok3.png ("Bộ lọc v")
            Box {
                OutlinedButton(
                    onClick = { filterMenuExpanded = true },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (filterType) {
                            "IMPORT" -> "Bộ lọc: Nhập kho"
                            "EXPORT" -> "Bộ lọc: Xuất kho"
                            else -> "Bộ lọc"
                        },
                        fontWeight = FontWeight.Medium
                    )
                }

                DropdownMenu(
                    expanded = filterMenuExpanded,
                    onDismissRequest = { filterMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Tất cả giao dịch") },
                        onClick = {
                            filterType = "ALL"
                            filterMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Chỉ phiếu nhập kho") },
                        onClick = {
                            filterType = "IMPORT"
                            filterMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Chỉ phiếu xuất kho") },
                        onClick = {
                            filterType = "EXPORT"
                            filterMenuExpanded = false
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!hasTransactions) {
                // Empty view matching ok3.png
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Chưa có giao dịch nào.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Nhấn + để thêm giao dịch kho.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(transactionItems, key = { it.key }) { listItem ->
                        when (listItem) {
                            is TransactionListItem.Import -> {
                                ImportReceiptCard(
                                    receiptWithDetails = listItem.item,
                                    onClick = { onNavigateToImportDetail(listItem.item.receipt.id) },
                                    titleOverride = "Nhập kho"
                                )
                            }
                            is TransactionListItem.Export -> {
                                ExportReceiptCard(
                                    receiptWithDetails = listItem.item,
                                    onClick = { onNavigateToExportDetail(listItem.item.receipt.id) },
                                    titleOverride = "Xuất kho"
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet matching image {03BEF7E6-BFC2-4C45-B2AC-8F2C4A628575}.png
    if (showSelectTransactionSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSelectTransactionSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = "Chọn giao dịch",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )

                HorizontalDivider(color = Color(0xFFF0F0F0))

                // Option 1: Nhập kho
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showSelectTransactionSheet = false
                            onNavigateToImportCreate()
                        }
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.South,
                        contentDescription = "Nhập kho",
                        tint = PrimaryBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Nhập kho",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color.LightGray,
                        modifier = Modifier.size(22.dp)
                    )
                }

                HorizontalDivider(color = Color(0xFFF0F0F0))

                // Option 2: Xuất kho
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showSelectTransactionSheet = false
                            onNavigateToExportCreate()
                        }
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.North,
                        contentDescription = "Xuất kho",
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Xuất kho",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color.LightGray,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

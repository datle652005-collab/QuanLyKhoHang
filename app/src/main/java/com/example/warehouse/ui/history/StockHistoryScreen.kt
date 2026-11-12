package com.example.warehouse.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.warehouse.ui.component.StockHistoryItemCard
import com.example.warehouse.ui.component.WarehouseEmptyView
import com.example.warehouse.viewmodel.HistoryViewModel

@Composable
fun StockHistoryScreen(
    viewModel: HistoryViewModel
) {
    val historyList by viewModel.stockHistory.collectAsState()
    val products by viewModel.products.collectAsState()
    val selectedProductId by viewModel.selectedProductId.collectAsState()
    val actionFilter by viewModel.actionFilter.collectAsState()

    val actionTypes = listOf(
        Pair("Tất cả", null),
        Pair("Nhập kho", "IMPORT"),
        Pair("Xuất kho", "EXPORT"),
        Pair("Kiểm kê", "INVENTORY_ADJUST"),
        Pair("Khác", "MANUAL")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Text("Lọc theo loại giao dịch", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(actionTypes) { (label, action) ->
                FilterChip(
                    selected = actionFilter == action,
                    onClick = { viewModel.selectActionFilter(action) },
                    label = { Text(label) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (historyList.isEmpty()) {
            WarehouseEmptyView(
                title = "Chưa có lịch sử giao dịch",
                message = "Không tìm thấy lịch sử phù hợp với bộ lọc",
                icon = Icons.Default.History
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(historyList, key = { it.id }) { history ->
                    StockHistoryItemCard(history = history)
                }
            }
        }
    }
}

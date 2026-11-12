package com.example.warehouse.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.warehouse.viewmodel.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.*

import com.example.warehouse.ui.theme.PrimaryBlue
import com.example.warehouse.ui.theme.PrimaryBlueDark

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    userFullName: String,
    onNavigateToProducts: () -> Unit,
    onNavigateToImport: () -> Unit,
    onNavigateToExport: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToProductDetail: (Long) -> Unit,
    onNavigateToInventoryAlerts: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    // Date String e.g. "25 thg 7"
    val dateStr = remember {
        val cal = Calendar.getInstance()
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val month = cal.get(Calendar.MONTH) + 1
        "Hôm nay $day thg $month"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // --- 1. Header Card matching image 1 ---
        com.example.warehouse.ui.component.TodayStockOverviewCard(
            totalStockQty = state.totalStockQty,
            todayImportQty = state.todayImportsCount,
            todayExportQty = state.todayExportsCount
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- 2. Search Bar Card (matching ok.jpg) ---
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDBEAFE)),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToProducts() }
                .testTag("dashboard_search_bar")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color.Gray,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Tìm kiếm sản phẩm",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(20.dp)
                        .background(Color.LightGray)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "Scan",
                    tint = Color.DarkGray,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 3. Section: Sản phẩm ---
        DashboardSectionCard(title = "Sản phẩm") {
            DashboardItemRow(
                icon = Icons.Default.QrCode2,
                iconTint = PrimaryBlue,
                title = "Thêm sản phẩm",
                onClick = onNavigateToProducts
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 4. Section: Giao dịch ---
        DashboardSectionCard(title = "Giao dịch") {
            Column {
                DashboardItemRow(
                    icon = Icons.Default.South,
                    iconTint = PrimaryBlue,
                    title = "Nhập kho",
                    onClick = onNavigateToImport
                )
                HorizontalDivider(color = Color(0xFFF0F0F0))
                DashboardItemRow(
                    icon = Icons.Default.North,
                    iconTint = PrimaryBlue,
                    title = "Xuất kho",
                    onClick = onNavigateToExport
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 5. Section: Nhắc nhở tồn kho ---
        DashboardSectionCard(title = "Nhắc nhở tồn kho") {
            DashboardItemRow(
                icon = Icons.Default.NotificationsActive,
                iconTint = PrimaryBlue,
                title = "Cảnh báo tồn kho (${state.lowStockCount})",
                onClick = onNavigateToInventoryAlerts
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 6. Section: Kiểm kê ---
        DashboardSectionCard(title = "Kiểm kê") {
            DashboardItemRow(
                icon = Icons.Default.Numbers,
                iconTint = PrimaryBlue,
                title = "Bắt đầu kiểm kê",
                onClick = onNavigateToInventory
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun DashboardSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDBEAFE)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun DashboardItemRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF222222),
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color.LightGray,
            modifier = Modifier.size(20.dp)
        )
    }
}

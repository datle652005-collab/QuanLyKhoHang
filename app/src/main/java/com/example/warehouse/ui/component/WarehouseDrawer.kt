package com.example.warehouse.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.warehouse.ui.theme.PrimaryBlue
import com.example.warehouse.ui.theme.PrimaryBlueDark
import com.example.warehouse.ui.theme.PrimaryBlueLight

data class DrawerMenuItem(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val adminOnly: Boolean = false
)

@Composable
fun WarehouseDrawerContent(
    currentRoute: String,
    fullName: String,
    role: String,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    ModalDrawerSheet(
        modifier = Modifier.width(300.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Header Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PrimaryBlue)
                    .padding(24.dp)
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = fullName.take(1).uppercase(),
                            style = MaterialTheme.typography.headlineMedium,
                            color = PrimaryBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = fullName.ifEmpty { "Người dùng" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Surface(
                        color = if (role == "ADMIN") Color(0xFFFFEBEE) else PrimaryBlueLight,
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            text = if (role == "ADMIN") "QUẢN TRỊ VIÊN" else "NHÂN VIÊN KHO",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (role == "ADMIN") Color(0xFFD32F2F) else PrimaryBlueDark,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Navigation Items
            val menuItems = listOf(
                DrawerMenuItem("dashboard", "Tổng quan Dashboard", Icons.Default.Dashboard),
                DrawerMenuItem("products", "Quản lý Hàng hóa", Icons.Default.Inventory2),
                DrawerMenuItem("categories", "Quản lý Danh mục", Icons.Default.Category),
                DrawerMenuItem("suppliers", "Quản lý Nhà cung cấp", Icons.Default.LocalShipping),
                DrawerMenuItem("import_list", "Phiếu Nhập kho", Icons.Default.MoveToInbox),
                DrawerMenuItem("export_list", "Phiếu Xuất kho", Icons.Default.Outbox),
                DrawerMenuItem("inventory_list", "Kiểm kê Kho", Icons.Default.FactCheck),
                DrawerMenuItem("reports", "Báo cáo & Thống kê", Icons.Default.BarChart),
                DrawerMenuItem("user_management", "Quản lý Tài khoản", Icons.Default.People),
                DrawerMenuItem("profile", "Thông tin cá nhân", Icons.Default.Person)
            )

            menuItems.forEach { item ->
                val canShow = when (item.route) {
                    "user_management" -> com.example.warehouse.utils.PermissionManager.canViewUserManagement(role)
                    "reports" -> com.example.warehouse.utils.PermissionManager.canViewFullReportModule(role)
                    else -> !item.adminOnly || role == "ADMIN"
                }

                if (canShow) {
                    NavigationDrawerItem(
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.title) },
                        selected = currentRoute == item.route,
                        onClick = { onNavigate(item.route) },
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 2.dp)
                            .testTag("drawer_item_${item.route}")
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Logout Item
            NavigationDrawerItem(
                icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                label = { Text("Đăng xuất", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) },
                selected = false,
                onClick = onLogout,
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 2.dp)
                    .testTag("drawer_logout")
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

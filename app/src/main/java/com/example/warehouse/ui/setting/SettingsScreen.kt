package com.example.warehouse.ui.setting

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import com.example.warehouse.ui.component.WarehouseTopBar
import com.example.warehouse.viewmodel.AuthViewModel

import com.example.warehouse.ui.theme.PrimaryBlue
import com.example.warehouse.ui.theme.PrimaryBlueLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    authViewModel: AuthViewModel,
    onNavigateToCategories: () -> Unit,
    onNavigateToSuppliers: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToUserManagement: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onLogout: () -> Unit
) {
    val username by authViewModel.currentUsername.collectAsState()
    val fullName by authViewModel.currentFullName.collectAsState()
    val role by authViewModel.currentUserRole.collectAsState()

    var showChangePasswordDialog by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Profile Card Header
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(PrimaryBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = fullName.ifEmpty { username },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "@$username",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }

                    Surface(
                        color = if (role == "ADMIN") PrimaryBlueLight else Color(0xFFE2F0D9),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (role == "ADMIN") "ADMIN" else "STAFF",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (role == "ADMIN") PrimaryBlue else Color(0xFF388E3C),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Group 1: Quản lý danh mục & Đối tác
            Text(
                text = "Quản lý hệ thống",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SettingsItemRow(
                        icon = Icons.Default.Category,
                        title = "Danh mục hàng hóa",
                        onClick = onNavigateToCategories
                    )
                    HorizontalDivider(color = Color(0xFFF0F0F0))
                    SettingsItemRow(
                        icon = Icons.Default.Store,
                        title = "Nhà cung cấp",
                        onClick = onNavigateToSuppliers
                    )
                    HorizontalDivider(color = Color(0xFFF0F0F0))
                    SettingsItemRow(
                        icon = Icons.Default.FactCheck,
                        title = "Kiểm kê kho",
                        onClick = onNavigateToInventory
                    )
                    if (com.example.warehouse.utils.PermissionManager.canViewFullReportModule(role)) {
                        HorizontalDivider(color = Color(0xFFF0F0F0))
                        SettingsItemRow(
                            icon = Icons.Default.BarChart,
                            title = "Thống kê",
                            onClick = onNavigateToReports
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Group 2: Tài khoản & Bảo mật
            Text(
                text = "Tài khoản & Bảo mật",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    if (com.example.warehouse.utils.PermissionManager.canViewUserManagement(role)) {
                        SettingsItemRow(
                            icon = Icons.Default.Group,
                            title = "Quản lý người dùng",
                            subtitle = "Thêm nhân viên, phân quyền kho",
                            onClick = onNavigateToUserManagement
                        )
                        HorizontalDivider(color = Color(0xFFF0F0F0))
                    }
                    SettingsItemRow(
                        icon = Icons.Default.Notifications,
                        title = "Thông báo",
                        subtitle = "Cảnh báo và nhắc nhở tồn kho",
                        onClick = onNavigateToNotifications
                    )
                    HorizontalDivider(color = Color(0xFFF0F0F0))
                    SettingsItemRow(
                        icon = Icons.Default.Lock,
                        title = "Đổi mật khẩu",
                        onClick = { showChangePasswordDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Logout Button
            Button(
                onClick = {
                    authViewModel.logout()
                    onLogout()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEBEE)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("settings_logout_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = null,
                    tint = Color(0xFFD32F2F)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ĐĂNG XUẤT",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD32F2F),
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            authViewModel = authViewModel,
            onDismiss = { showChangePasswordDialog = false },
            onLogoutRequired = onLogout
        )
    }
}

@Composable
fun SettingsItemRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = PrimaryBlue,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color.LightGray,
            modifier = Modifier.size(20.dp)
        )
    }
}

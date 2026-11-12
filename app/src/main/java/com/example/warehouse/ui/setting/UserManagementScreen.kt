package com.example.warehouse.ui.setting

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.example.warehouse.data.database.entity.User
import com.example.warehouse.ui.component.ConfirmDeleteDialog
import com.example.warehouse.ui.theme.PrimaryBlue
import com.example.warehouse.ui.theme.PrimaryBlueLight
import com.example.warehouse.viewmodel.AuthViewModel
import com.example.warehouse.viewmodel.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(
    userViewModel: UserViewModel,
    authViewModel: AuthViewModel,
    userRole: String? = null
) {
    val users by userViewModel.users.collectAsState()
    val searchQuery by userViewModel.searchQuery.collectAsState()
    val selectedRoleFilter by userViewModel.selectedRoleFilter.collectAsState()
    val selectedStatusFilter by userViewModel.selectedStatusFilter.collectAsState()
    val currentAdminUserId by authViewModel.currentUserId.collectAsState()
    val currentAdminUsername by authViewModel.currentUsername.collectAsState()

    var showAddUserDialog by remember { mutableStateOf(false) }
    var userToEdit by remember { mutableStateOf<User?>(null) }
    var userToResetPassword by remember { mutableStateOf<User?>(null) }
    var userToToggleLock by remember { mutableStateOf<User?>(null) }
    var roleMenuExpanded by remember { mutableStateOf(false) }
    var statusMenuExpanded by remember { mutableStateOf(false) }

    var alertErrorMessage by remember { mutableStateOf<String?>(null) }
    var alertSuccessMessage by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val dateFormat = remember { SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault()) }

    val isAdminUser = com.example.warehouse.utils.PermissionManager.isAdmin(userRole)
    val displayedUsers = remember(users, isAdminUser, currentAdminUserId) {
        if (isAdminUser) users else users.filter { it.id == currentAdminUserId }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            if (com.example.warehouse.utils.PermissionManager.canManageUsers(userRole)) {
                FloatingActionButton(
                    onClick = { showAddUserDialog = true },
                    containerColor = PrimaryBlue,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_user_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Thêm người dùng")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(innerPadding)
        ) {
            // Search and Filters Section
            if (isAdminUser) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { userViewModel.searchQuery.value = it },
                        placeholder = { Text("Tìm theo tên, username, SĐT...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { userViewModel.searchQuery.value = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = null, tint = Color.Gray)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("user_search_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Role & Status Dropdown Selectors (Comboboxes)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Dropdown for Role Filter
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedCard(
                                onClick = { roleMenuExpanded = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.outlinedCardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth().height(56.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(verticalArrangement = Arrangement.Center) {
                                        Text("Vai trò", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                                        Text(
                                            text = when (selectedRoleFilter) {
                                                "ADMIN" -> "Quản lý kho"
                                                "STAFF" -> "Nhân viên kho"
                                                else -> "Tất cả"
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF0F172A)
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = PrimaryBlue
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = roleMenuExpanded,
                                onDismissRequest = { roleMenuExpanded = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Tất cả vai trò", fontWeight = FontWeight.Medium) },
                                    onClick = {
                                        userViewModel.selectedRoleFilter.value = "ALL"
                                        roleMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Quản lý kho", fontWeight = FontWeight.Medium) },
                                    onClick = {
                                        userViewModel.selectedRoleFilter.value = "ADMIN"
                                        roleMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Nhân viên kho", fontWeight = FontWeight.Medium) },
                                    onClick = {
                                        userViewModel.selectedRoleFilter.value = "STAFF"
                                        roleMenuExpanded = false
                                    }
                                )
                            }
                        }

                        // Dropdown for Status Filter
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedCard(
                                onClick = { statusMenuExpanded = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.outlinedCardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth().height(56.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(verticalArrangement = Arrangement.Center) {
                                        Text("Trạng thái", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                                        Text(
                                            text = when (selectedStatusFilter) {
                                                "ACTIVE" -> "Hoạt động"
                                                "LOCKED" -> "Đã khóa"
                                                else -> "Tất cả"
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF0F172A)
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = PrimaryBlue
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = statusMenuExpanded,
                                onDismissRequest = { statusMenuExpanded = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Tất cả trạng thái", fontWeight = FontWeight.Medium) },
                                    onClick = {
                                        userViewModel.selectedStatusFilter.value = "ALL"
                                        statusMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Hoạt động", fontWeight = FontWeight.Medium) },
                                    onClick = {
                                        userViewModel.selectedStatusFilter.value = "ACTIVE"
                                        statusMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Đã khóa", fontWeight = FontWeight.Medium) },
                                    onClick = {
                                        userViewModel.selectedStatusFilter.value = "LOCKED"
                                        statusMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(color = Color(0xFFE2E8F0))
            }

            if (displayedUsers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Không tìm thấy tài khoản nào", color = Color(0xFF64748B))
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(displayedUsers, key = { it.id }) { user ->
                        val isSelf = (user.id == currentAdminUserId)

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = user.fullName,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = Color(0xFF0F172A)
                                            )
                                            if (isSelf) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    color = Color(0xFFFEF3C7),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = "Bạn",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFFD97706),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(2.dp))

                                        Text(
                                            text = "@${user.username}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = PrimaryBlue
                                        )
                                    }

                                    // Role & Status Badges
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Surface(
                                            color = if (user.role == "ADMIN") PrimaryBlueLight else Color(0xFFE2F0D9),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = if (user.role == "ADMIN") "Quản lý kho" else "Nhân viên kho",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (user.role == "ADMIN") PrimaryBlue else Color(0xFF2E7D32),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }

                                        Surface(
                                            color = if (user.status.uppercase() == "ACTIVE") Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = if (user.status.uppercase() == "ACTIVE") "Hoạt động" else "Đã khóa",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (user.status.uppercase() == "ACTIVE") PrimaryBlue else PrimaryBlue,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    if (user.phone.isNotEmpty()) {
                                        Text("SĐT: ${user.phone}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF475569))
                                    }
                                    if (user.email.isNotEmpty()) {
                                        Text("Email: ${user.email}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF475569))
                                    }
                                    if (user.createdBy != null) {
                                        Text("Người tạo: ID #${user.createdBy}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                                    }
                                    Text("Thời gian tạo: ${dateFormat.format(Date(user.createdAt))}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                                    Text(
                                        text = "Đăng nhập gần nhất: ${if (user.lastLoginAt != null) dateFormat.format(Date(user.lastLoginAt)) else "Chưa từng đăng nhập"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF64748B)
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                HorizontalDivider(color = Color(0xFFF1F5F9))

                                // Action Buttons
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Edit button
                                    TextButton(
                                        onClick = { userToEdit = user },
                                        modifier = Modifier.testTag("edit_user_${user.id}")
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Sửa")
                                    }

                                    if (isAdminUser) {
                                        // Reset Password button
                                        TextButton(
                                            onClick = { userToResetPassword = user },
                                            enabled = !isSelf,
                                            modifier = Modifier.testTag("reset_password_user_${user.id}")
                                        ) {
                                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Reset Password")
                                        }

                                        // Lock/Unlock button
                                        TextButton(
                                            onClick = { userToToggleLock = user },
                                            enabled = !isSelf,
                                            modifier = Modifier.testTag("lock_user_${user.id}")
                                        ) {
                                            Icon(
                                                imageVector = if (user.status.uppercase() == "ACTIVE") Icons.Default.Lock else Icons.Default.LockOpen,
                                                contentDescription = null,
                                                tint = if (isSelf) Color.Gray else if (user.status.uppercase() == "ACTIVE") Color(0xFFD97706) else Color(0xFF16A34A),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (user.status.uppercase() == "ACTIVE") "Khóa" else "Mở khóa",
                                                color = if (isSelf) Color.Gray else if (user.status.uppercase() == "ACTIVE") Color(0xFFD97706) else Color(0xFF16A34A)
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
    }

    if (showAddUserDialog) {
        AddUserDialog(
            userViewModel = userViewModel,
            currentAdminUserId = currentAdminUserId,
            userRole = userRole,
            onDismiss = { showAddUserDialog = false },
            onSuccessMessage = { msg ->
                coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
            }
        )
    }

    userToEdit?.let { u ->
        EditUserDialog(
            user = u,
            isSelf = (u.id == currentAdminUserId),
            currentAdminUserId = currentAdminUserId,
            userViewModel = userViewModel,
            userRole = userRole,
            onDismiss = { userToEdit = null },
            onError = { alertErrorMessage = it },
            onSuccess = { msg ->
                coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
            }
        )
    }

    userToResetPassword?.let { u ->
        ResetPasswordDialog(
            user = u,
            currentAdminUserId = currentAdminUserId,
            currentAdminUsername = currentAdminUsername,
            userViewModel = userViewModel,
            userRole = userRole,
            onDismiss = { userToResetPassword = null },
            onError = { alertErrorMessage = it },
            onSuccess = { msg ->
                coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
            }
        )
    }

    userToToggleLock?.let { u ->
        val isLocking = u.status.uppercase() == "ACTIVE"
        AlertDialog(
            containerColor = Color.White,
            onDismissRequest = { userToToggleLock = null },
            title = { Text(if (isLocking) "Xác nhận khóa tài khoản" else "Xác nhận mở khóa tài khoản", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    if (isLocking)
                        "Khóa tài khoản '${u.username}' sẽ THU HỒI TOÀN BỘ session/token đang hoạt động ngay lập tức. Người dùng sẽ bị đăng xuất khỏi mọi thiết bị."
                    else
                        "Mở khóa tài khoản '${u.username}'. Người dùng có thể đăng nhập lại bằng mật khẩu của họ."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = userToToggleLock
                        userToToggleLock = null
                        if (target != null) {
                            userViewModel.toggleUserStatus(target, currentAdminUserId, userRole) { success, msg ->
                                if (success) {
                                    coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
                                } else {
                                    alertErrorMessage = msg
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text(if (isLocking) "Khóa ngay" else "Mở khóa")
                }
            },
            dismissButton = {
                TextButton(onClick = { userToToggleLock = null }) { Text("Hủy") }
            }
        )
    }

    if (alertErrorMessage != null) {
        AlertDialog(
            containerColor = Color.White,
            onDismissRequest = { alertErrorMessage = null },
            title = { Text("Không thể thực hiện", fontWeight = FontWeight.Bold, color = PrimaryBlue) },
            text = { Text(alertErrorMessage ?: "", color = Color(0xFF1E293B)) },
            confirmButton = {
                TextButton(onClick = { alertErrorMessage = null }) {
                    Text("Đóng", color = PrimaryBlue, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun AddUserDialog(
    userViewModel: UserViewModel,
    currentAdminUserId: Long?,
    userRole: String? = null,
    onDismiss: () -> Unit,
    onSuccessMessage: (String) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("123456") }
    var fullName by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("STAFF") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "Thêm người dùng mới",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (error.isNotEmpty()) {
                    Surface(
                        color = Color(0xFFFEF2F2),
                        border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = error,
                            color = PrimaryBlue,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Tên đăng nhập (Username) *") },
                    placeholder = { Text("VD: nguyenvana") },
                    supportingText = { Text("Không được sửa sau khi tạo", style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryBlue) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        focusedLabelColor = PrimaryBlue
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("add_user_username_input")
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Mật khẩu * (Tối thiểu 6 ký tự)") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryBlue) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        focusedLabelColor = PrimaryBlue
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("add_user_password_input")
                )

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Họ và tên *") },
                    placeholder = { Text("VD: Nguyễn Văn A") },
                    leadingIcon = { Icon(Icons.Default.AccountCircle, contentDescription = null, tint = PrimaryBlue) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        focusedLabelColor = PrimaryBlue
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("add_user_fullname_input")
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Vai trò hệ thống *",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF334155)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Staff Card Selector
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { role = "STAFF" },
                            shape = RoundedCornerShape(10.dp),
                            color = if (role == "STAFF") PrimaryBlueLight else Color.White,
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (role == "STAFF") PrimaryBlue else Color(0xFFCBD5E1)
                            )
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Nhân viên kho",
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (role == "STAFF") PrimaryBlue else Color(0xFF475569),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        // Admin Card Selector
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { role = "ADMIN" },
                            shape = RoundedCornerShape(10.dp),
                            color = if (role == "ADMIN") PrimaryBlueLight else Color.White,
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (role == "ADMIN") PrimaryBlue else Color(0xFFCBD5E1)
                            )
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Quản lý kho",
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (role == "ADMIN") PrimaryBlue else Color(0xFF475569),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Số điện thoại") },
                    placeholder = { Text("VD: 0912345678") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = PrimaryBlue) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        focusedLabelColor = PrimaryBlue
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    placeholder = { Text("VD: example@email.com") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = PrimaryBlue) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        focusedLabelColor = PrimaryBlue
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (username.isBlank() || fullName.isBlank() || password.isBlank()) {
                        error = "Vui lòng nhập đầy đủ các thông tin bắt buộc (*)"
                    } else if (password.length < 6) {
                        error = "Mật khẩu phải từ 6 ký tự trở lên"
                    } else {
                        userViewModel.addUser(
                            usernameInput = username,
                            passwordInput = password,
                            fullNameInput = fullName,
                            role = role,
                            phoneInput = phone,
                            emailInput = email,
                            currentAdminUserId = currentAdminUserId,
                            actingUserRole = userRole
                        ) { success, msg ->
                            if (success) {
                                onDismiss()
                                onSuccessMessage(msg)
                            } else {
                                error = msg
                            }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryBlue,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("save_user_btn")
            ) {
                Text("Lưu tài khoản", fontWeight = FontWeight.Bold)
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

@Composable
fun EditUserDialog(
    user: User,
    isSelf: Boolean,
    currentAdminUserId: Long?,
    userViewModel: UserViewModel,
    userRole: String? = null,
    onDismiss: () -> Unit,
    onError: (String) -> Unit,
    onSuccess: (String) -> Unit
) {
    var fullName by remember { mutableStateOf(user.fullName) }
    var phone by remember { mutableStateOf(user.phone) }
    var email by remember { mutableStateOf(user.email) }
    var role by remember { mutableStateOf(user.role) }
    var status by remember { mutableStateOf(user.status.uppercase()) }

    val canEditRoleAndStatus = com.example.warehouse.utils.PermissionManager.isAdmin(userRole) && !isSelf

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "Sửa thông tin tài khoản",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = user.username,
                    onValueChange = {},
                    enabled = false,
                    label = { Text("Tên đăng nhập (Username)") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledBorderColor = Color(0xFFCBD5E1),
                        disabledLabelColor = Color(0xFF64748B),
                        disabledTextColor = Color(0xFF64748B)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Họ và tên *") },
                    leadingIcon = { Icon(Icons.Default.AccountCircle, contentDescription = null, tint = PrimaryBlue) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        focusedLabelColor = PrimaryBlue
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Số điện thoại") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = PrimaryBlue) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        focusedLabelColor = PrimaryBlue
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = PrimaryBlue) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        focusedLabelColor = PrimaryBlue
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Vai trò hệ thống",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF334155)
                        )
                        if (!canEditRoleAndStatus) {
                            Text(
                                text = if (isSelf) "Không thể tự đổi" else "Chỉ Quản lý được đổi",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Staff Selector
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(enabled = canEditRoleAndStatus) { role = "STAFF" },
                            shape = RoundedCornerShape(10.dp),
                            color = if (role == "STAFF") PrimaryBlueLight else Color.White,
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (role == "STAFF") PrimaryBlue else Color(0xFFCBD5E1)
                            )
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Nhân viên kho",
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (role == "STAFF") PrimaryBlue else if (!canEditRoleAndStatus) Color(0xFF94A3B8) else Color(0xFF475569),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        // Admin Selector
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(enabled = canEditRoleAndStatus) { role = "ADMIN" },
                            shape = RoundedCornerShape(10.dp),
                            color = if (role == "ADMIN") PrimaryBlueLight else Color.White,
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (role == "ADMIN") PrimaryBlue else Color(0xFFCBD5E1)
                            )
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Quản lý kho",
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (role == "ADMIN") PrimaryBlue else if (!canEditRoleAndStatus) Color(0xFF94A3B8) else Color(0xFF475569),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Trạng thái tài khoản",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF334155)
                        )
                        if (!canEditRoleAndStatus) {
                            Text(
                                text = if (isSelf) "Không thể tự khóa" else "Chỉ Quản lý được đổi",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Active Selector
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(enabled = canEditRoleAndStatus) { status = "ACTIVE" },
                            shape = RoundedCornerShape(10.dp),
                            color = if (status == "ACTIVE") PrimaryBlueLight else Color.White,
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (status == "ACTIVE") PrimaryBlue else Color(0xFFCBD5E1)
                            )
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Hoạt động",
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (status == "ACTIVE") PrimaryBlue else if (!canEditRoleAndStatus) Color(0xFF94A3B8) else Color(0xFF475569),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        // Locked Selector
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(enabled = canEditRoleAndStatus) { status = "LOCKED" },
                            shape = RoundedCornerShape(10.dp),
                            color = if (status == "LOCKED") Color(0xFFFEE2E2) else Color.White,
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (status == "LOCKED") Color(0xFFDC2626) else Color(0xFFCBD5E1)
                            )
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Khóa",
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (status == "LOCKED") Color(0xFFDC2626) else if (!canEditRoleAndStatus) Color(0xFF94A3B8) else Color(0xFF475569),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fullName.isBlank()) {
                        onError("Họ và tên không được để trống")
                    } else {
                        val updated = user.copy(
                            fullName = fullName.trim(),
                            phone = phone.trim(),
                            email = email.trim(),
                            roleId = if (role == "ADMIN") 1L else 2L,
                            status = status
                        )
                        userViewModel.updateUser(
                            userToUpdate = updated,
                            currentAdminUserId = currentAdminUserId,
                            actingUserRole = userRole
                        ) { success, msg ->
                            onDismiss()
                            if (success) onSuccess(msg) else onError(msg)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryBlue,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Lưu thay đổi", fontWeight = FontWeight.Bold)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetPasswordDialog(
    user: User,
    currentAdminUserId: Long?,
    currentAdminUsername: String,
    userViewModel: UserViewModel,
    userRole: String? = null,
    onDismiss: () -> Unit,
    onError: (String) -> Unit,
    onSuccess: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "Reset mật khẩu",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (error.isNotEmpty()) {
                    Surface(
                        color = Color(0xFFFEF2F2),
                        border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = error,
                            color = PrimaryBlue,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                // Show target account username
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF1F5F9), shape = RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Tài khoản:",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF64748B)
                    )
                    Text(
                        text = user.username,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF0F172A)
                    )
                }

                // New Password Input
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Mật khẩu mới *") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryBlue) },
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        focusedLabelColor = PrimaryBlue
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("reset_password_new_input")
                )

                // Confirm Password Input
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Xác nhận mật khẩu *") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryBlue) },
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        focusedLabelColor = PrimaryBlue
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("reset_password_confirm_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanPassword = password.trim()
                    val cleanConfirm = confirmPassword.trim()

                    if (cleanPassword.isEmpty()) {
                        error = "Mật khẩu mới bắt buộc nhập."
                    } else if (cleanPassword.length < 6) {
                        error = "Độ dài tối thiểu 6 ký tự."
                    } else if (cleanPassword != cleanConfirm) {
                        error = "Hai mật khẩu phải giống nhau."
                    } else {
                        userViewModel.resetPassword(
                            userToReset = user,
                            newPasswordInput = cleanPassword,
                            currentAdminUserId = currentAdminUserId,
                            currentAdminUsername = currentAdminUsername,
                            actingUserRole = userRole
                        ) { success, msg ->
                            onDismiss()
                            if (success) {
                                onSuccess(msg)
                            } else {
                                onError(msg)
                            }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryBlue,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("reset_password_confirm_btn")
            ) {
                Text("Xác nhận", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF64748B)),
                modifier = Modifier.testTag("reset_password_cancel_btn")
            ) {
                Text("Hủy", fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

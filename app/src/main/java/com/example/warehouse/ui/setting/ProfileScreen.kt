package com.example.warehouse.ui.setting

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.warehouse.ui.theme.PrimaryBlue
import com.example.warehouse.ui.theme.PrimaryBlueLight
import com.example.warehouse.viewmodel.AuthViewModel

@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    onNavigateToUserManagement: () -> Unit,
    onLogout: () -> Unit
) {
    val username by authViewModel.currentUsername.collectAsState()
    val fullName by authViewModel.currentFullName.collectAsState()
    val role by authViewModel.currentUserRole.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }

    var alertMsg by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Avatar
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(PrimaryBlue),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = fullName.ifEmpty { username },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A)
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = "@$username",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF64748B)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Surface(
            color = if (role == "ADMIN") PrimaryBlueLight else Color(0xFFE2F0D9),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (role == "ADMIN") "QUẢN LÝ KHO (ADMIN)" else "NHÂN VIÊN KHO (STAFF)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (role == "ADMIN") PrimaryBlue else Color(0xFF2E7D32),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Profile Details Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                ListItem(
                    headlineContent = { Text("Chỉnh sửa thông tin cá nhân", fontWeight = FontWeight.Medium, color = Color(0xFF0F172A)) },
                    supportingContent = { Text(text = "${currentUser?.phone.orEmpty().ifEmpty { "Chưa cập nhật SĐT" }} • ${currentUser?.email.orEmpty().ifEmpty { "Chưa cập nhật Email" }}") },
                    leadingContent = { Icon(Icons.Default.EditNote, contentDescription = null, tint = PrimaryBlue) },
                    trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray) },
                    modifier = Modifier
                        .clickable { showEditProfileDialog = true }
                        .testTag("edit_profile_item")
                )

                HorizontalDivider(color = Color(0xFFF0F0F0))

                ListItem(
                    headlineContent = { Text("Đổi mật khẩu", fontWeight = FontWeight.Medium, color = Color(0xFF0F172A)) },
                    supportingContent = { Text("Sau khi đổi thành công, hệ thống sẽ yêu cầu đăng nhập lại") },
                    leadingContent = { Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryBlue) },
                    trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray) },
                    modifier = Modifier
                        .clickable { showChangePasswordDialog = true }
                        .testTag("change_password_item")
                )

                HorizontalDivider(color = Color(0xFFF0F0F0))
                ListItem(
                    headlineContent = { Text("Quản lý tài khoản & phân quyền", fontWeight = FontWeight.Medium, color = Color(0xFF0F172A)) },
                    supportingContent = { Text("Thêm, sửa, khóa, xóa tài khoản người dùng", color = Color.Gray) },
                    leadingContent = { Icon(Icons.Default.Group, contentDescription = null, tint = PrimaryBlue) },
                    trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray) },
                    modifier = Modifier
                        .clickable { onNavigateToUserManagement() }
                        .testTag("user_management_item")
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Logout Button
        Button(
            onClick = {
                authViewModel.logout()
                onLogout()
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEBEE)),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("logout_btn"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Logout, contentDescription = null, tint = Color(0xFFD32F2F))
            Spacer(modifier = Modifier.width(8.dp))
            Text("ĐĂNG XUẤT", fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showEditProfileDialog) {
        EditProfileDialog(
            currentFullName = fullName,
            currentPhone = currentUser?.phone.orEmpty(),
            currentEmail = currentUser?.email.orEmpty(),
            onDismiss = { showEditProfileDialog = false },
            onSave = { name, phone, email ->
                authViewModel.updateProfile(name, phone, email) { success, msg ->
                    showEditProfileDialog = false
                    alertMsg = msg
                }
            }
        )
    }

    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            authViewModel = authViewModel,
            onDismiss = { showChangePasswordDialog = false },
            onLogoutRequired = onLogout
        )
    }

    alertMsg?.let { msg ->
        AlertDialog(
            onDismissRequest = { alertMsg = null },
            title = { Text("Thông báo", fontWeight = FontWeight.Bold) },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { alertMsg = null }) { Text("OK", fontWeight = FontWeight.Bold) }
            }
        )
    }
}

@Composable
fun EditProfileDialog(
    currentFullName: String,
    currentPhone: String,
    currentEmail: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var fullName by remember { mutableStateOf(currentFullName) }
    var phone by remember { mutableStateOf(currentPhone) }
    var email by remember { mutableStateOf(currentEmail) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chỉnh sửa thông tin cá nhân", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Họ và tên *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Số điện thoại") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(fullName, phone, email) }
            ) {
                Text("Lưu")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}

@Composable
fun ChangePasswordDialog(
    authViewModel: AuthViewModel,
    onDismiss: () -> Unit,
    onLogoutRequired: () -> Unit
) {
    var oldPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Đổi mật khẩu", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (msg.isNotEmpty()) {
                    Text(
                        text = msg,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                OutlinedTextField(
                    value = oldPass,
                    onValueChange = { oldPass = it },
                    label = { Text("Mật khẩu hiện tại *") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = newPass,
                    onValueChange = { newPass = it },
                    label = { Text("Mật khẩu mới * (>= 6 ký tự)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = confirmPass,
                    onValueChange = { confirmPass = it },
                    label = { Text("Xác nhận mật khẩu mới *") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (oldPass.isBlank() || newPass.isBlank() || confirmPass.isBlank()) {
                        msg = "Vui lòng nhập đầy đủ các trường"
                    } else if (newPass != confirmPass) {
                        msg = "Mật khẩu xác nhận không khớp"
                    } else if (newPass.length < 6) {
                        msg = "Mật khẩu mới tối thiểu 6 ký tự"
                    } else if (oldPass == newPass) {
                        msg = "Mật khẩu mới không được trùng mật khẩu cũ"
                    } else {
                        authViewModel.changePassword(oldPass, newPass) { success, resultMsg ->
                            if (success) {
                                onDismiss()
                                onLogoutRequired()
                            } else {
                                msg = resultMsg
                            }
                        }
                    }
                }
            ) {
                Text("Xác nhận đổi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}

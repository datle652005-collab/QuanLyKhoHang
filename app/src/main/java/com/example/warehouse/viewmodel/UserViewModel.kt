package com.example.warehouse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.warehouse.data.database.entity.User
import com.example.warehouse.data.repository.WarehouseRepository
import com.example.warehouse.utils.PasswordUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class UserViewModel(private val repository: WarehouseRepository) : ViewModel() {

    val searchQuery = MutableStateFlow("")
    val selectedRoleFilter = MutableStateFlow("ALL") // "ALL", "ADMIN", "STAFF"
    val selectedStatusFilter = MutableStateFlow("ALL") // "ALL", "ACTIVE", "LOCKED"

    private val allUsersFlow = repository.getAllUsers()

    val users: StateFlow<List<User>> = combine(
        allUsersFlow,
        searchQuery,
        selectedRoleFilter,
        selectedStatusFilter
    ) { list, query, roleFilter, statusFilter ->
        list.filter { user ->
            val matchesQuery = query.isBlank() ||
                    user.username.contains(query, ignoreCase = true) ||
                    user.fullName.contains(query, ignoreCase = true) ||
                    user.phone.contains(query, ignoreCase = true) ||
                    user.email.contains(query, ignoreCase = true)

            val matchesRole = when (roleFilter) {
                "ADMIN" -> user.role == "ADMIN"
                "STAFF" -> user.role == "STAFF"
                else -> true
            }

            val matchesStatus = when (statusFilter) {
                "ACTIVE" -> user.status.uppercase() == "ACTIVE"
                "LOCKED" -> user.status.uppercase() == "LOCKED"
                else -> true
            }

            matchesQuery && matchesRole && matchesStatus
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun addUser(
        usernameInput: String,
        passwordInput: String,
        fullNameInput: String,
        role: String,
        phoneInput: String,
        emailInput: String,
        currentAdminUserId: Long?,
        actingUserRole: String? = null,
        onResult: (Boolean, String) -> Unit
    ) {
        if (!com.example.warehouse.utils.PermissionManager.canManageUsers(actingUserRole)) {
            onResult(false, "Bạn không có quyền quản lý người dùng")
            return
        }
        val cleanUsername = usernameInput.trim()
        val cleanPassword = passwordInput.trim()
        val cleanFullName = fullNameInput.trim()

        if (cleanUsername.isBlank()) {
            return onResult(false, "Tên đăng nhập không được để trống")
        }

        if (cleanPassword.length < 6) {
            return onResult(false, "Mật khẩu tối thiểu 6 ký tự")
        }

        if (cleanFullName.isBlank()) {
            return onResult(false, "Họ và tên không được để trống")
        }

        viewModelScope.launch {
            val existing = repository.getUserByUsername(cleanUsername)
            if (existing != null) {
                onResult(false, "Tên đăng nhập đã tồn tại")
                return@launch
            }

            val hashedPassword = PasswordUtils.hashPassword(cleanPassword)
            val newUserId = repository.insertUser(
                User(
                    username = cleanUsername,
                    password = hashedPassword,
                    fullName = cleanFullName,
                    roleId = if (role == "ADMIN") 1L else 2L,
                    phone = phoneInput.trim(),
                    email = emailInput.trim(),
                    status = "ACTIVE",
                    createdBy = currentAdminUserId,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )

            repository.logUserAction(
                action = "CREATE_ACCOUNT",
                targetUserId = newUserId,
                targetUsername = cleanUsername,
                performedByUserId = currentAdminUserId,
                details = "Tạo tài khoản mới ($role) bởi user_id=$currentAdminUserId"
            )

            onResult(true, "Tạo tài khoản thành công")
        }
    }

    fun updateUser(
        userToUpdate: User,
        currentAdminUserId: Long?,
        actingUserRole: String? = null,
        onResult: (Boolean, String) -> Unit
    ) {
        val isAdmin = com.example.warehouse.utils.PermissionManager.isAdmin(actingUserRole)
        val isSelf = (currentAdminUserId != null && userToUpdate.id == currentAdminUserId)

        if (!isAdmin && !isSelf) {
            onResult(false, "Bạn không có quyền quản lý người dùng")
            return
        }
        viewModelScope.launch {
            val originalUser = repository.getUserById(userToUpdate.id)
            if (originalUser == null) {
                return@launch onResult(false, "Không tìm thấy người dùng")
            }

            // Cannot edit username
            if (userToUpdate.username != originalUser.username) {
                return@launch onResult(false, "Không được phép thay đổi tên đăng nhập sau khi tạo")
            }

            // Non-admin or self cannot edit own role or status
            if (!isAdmin || isSelf) {
                if (userToUpdate.roleId != originalUser.roleId || userToUpdate.status != originalUser.status) {
                    return@launch onResult(false, "Không được thay đổi vai trò hoặc trạng thái của tài khoản này")
                }
            }

            // Protect last active admin check if role changed to STAFF or status changed to LOCKED
            val isOriginalAdmin = originalUser.role == "ADMIN"
            val isNowStaff = userToUpdate.role != "ADMIN"
            val isNowLocked = userToUpdate.status.uppercase() == "LOCKED"

            if (isOriginalAdmin && (isNowStaff || isNowLocked)) {
                val activeAdminsCount = repository.countActiveAdminsExcluding(userToUpdate.id)
                if (activeAdminsCount <= 0) {
                    return@launch onResult(false, "Không thể thực hiện: đây là tài khoản Quản lý kho cuối cùng đang hoạt động")
                }
            }

            repository.updateUser(userToUpdate.copy(updatedAt = System.currentTimeMillis()))

            val actionType = if (originalUser.status != userToUpdate.status) {
                if (userToUpdate.status.uppercase() == "LOCKED") "LOCK_ACCOUNT" else "UNLOCK_ACCOUNT"
            } else if (originalUser.roleId != userToUpdate.roleId) {
                "CHANGE_ROLE"
            } else {
                "EDIT_USER"
            }

            repository.logUserAction(
                action = actionType,
                targetUserId = userToUpdate.id,
                targetUsername = userToUpdate.username,
                performedByUserId = currentAdminUserId,
                details = "Cập nhật tài khoản: vai trò=${userToUpdate.role}, trạng thái=${userToUpdate.status}"
            )

            onResult(true, "Cập nhật tài khoản thành công")
        }
    }

    fun toggleUserStatus(
        user: User,
        currentAdminUserId: Long?,
        actingUserRole: String? = null,
        onResult: (Boolean, String) -> Unit
    ) {
        if (!com.example.warehouse.utils.PermissionManager.canManageUsers(actingUserRole)) {
            onResult(false, "Bạn không có quyền quản lý người dùng")
            return
        }
        viewModelScope.launch {
            if (currentAdminUserId != null && user.id == currentAdminUserId) {
                return@launch onResult(false, "Không thể tự khóa tài khoản của chính mình")
            }

            val newStatus = if (user.status.uppercase() == "ACTIVE") "LOCKED" else "ACTIVE"

            // Last active admin protection when locking
            if (user.role == "ADMIN" && newStatus == "LOCKED") {
                val activeAdminsCount = repository.countActiveAdminsExcluding(user.id)
                if (activeAdminsCount <= 0) {
                    return@launch onResult(false, "Không thể thực hiện: đây là tài khoản Quản lý kho cuối cùng đang hoạt động")
                }
            }

            val updatedUser = user.copy(
                status = newStatus,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateUser(updatedUser)

            val action = if (newStatus == "LOCKED") "LOCK_ACCOUNT" else "UNLOCK_ACCOUNT"
            repository.logUserAction(
                action = action,
                targetUserId = user.id,
                targetUsername = user.username,
                performedByUserId = currentAdminUserId,
                details = if (newStatus == "LOCKED") "Khóa tài khoản và thu hồi session" else "Mở khóa tài khoản"
            )

            onResult(
                true,
                if (newStatus == "LOCKED") "Đã khóa tài khoản thành công. Tự động thu hồi phiên đăng nhập ngay lập tức." else "Đã mở khóa tài khoản thành công"
            )
        }
    }


    fun resetPassword(
        userToReset: User,
        newPasswordInput: String,
        currentAdminUserId: Long?,
        currentAdminUsername: String,
        actingUserRole: String? = null,
        onResult: (Boolean, String) -> Unit
    ) {
        if (!com.example.warehouse.utils.PermissionManager.canManageUsers(actingUserRole)) {
            onResult(false, "Bạn không có quyền quản lý người dùng")
            return
        }
        val cleanPassword = newPasswordInput.trim()

        if (cleanPassword.isBlank()) {
            return onResult(false, "Mật khẩu mới bắt buộc nhập")
        }

        if (cleanPassword.length < 6) {
            return onResult(false, "Mật khẩu mới phải từ 6 ký tự trở lên")
        }

        viewModelScope.launch {
            val dbUser = repository.getUserById(userToReset.id)
            if (dbUser == null) {
                onResult(false, "Không tìm thấy tài khoản cần reset mật khẩu")
                return@launch
            }

            if (currentAdminUserId != null && dbUser.id == currentAdminUserId) {
                onResult(false, "Quản lý chỉ được reset mật khẩu tài khoản khác")
                return@launch
            }

            val hashedPassword = PasswordUtils.hashPassword(cleanPassword)
            val updatedUser = dbUser.copy(
                password = hashedPassword,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateUser(updatedUser)

            val formatter = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault())
            val timeStr = formatter.format(java.util.Date())

            val logDetails = "RESET_PASSWORD\nNgười thực hiện:\n$currentAdminUsername\nTài khoản bị reset:\n${dbUser.username}\nThời gian:\n$timeStr"

            repository.logUserAction(
                action = "RESET_PASSWORD",
                targetUserId = dbUser.id,
                targetUsername = dbUser.username,
                performedByUserId = currentAdminUserId,
                details = logDetails
            )

            onResult(true, "Reset mật khẩu tài khoản '${dbUser.username}' thành công")
        }
    }
}

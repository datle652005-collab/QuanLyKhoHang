package com.example.warehouse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.warehouse.data.database.entity.Role
import com.example.warehouse.data.database.entity.User
import com.example.warehouse.data.repository.WarehouseRepository
import com.example.warehouse.utils.PasswordUtils
import com.example.warehouse.utils.UserPreferences
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Authenticated(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(
    private val repository: WarehouseRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    val isLoggedIn: StateFlow<Boolean> = userPreferences.isLoggedIn.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val currentUserId: StateFlow<Long?> = userPreferences.currentUserId.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val currentUsername: StateFlow<String> = userPreferences.currentUsername.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )

    val currentFullName: StateFlow<String> = userPreferences.currentFullName.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )

    val currentUserRole: StateFlow<String> = userPreferences.currentUserRole.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "STAFF"
    )

    val currentUser: StateFlow<User?> = currentUserId.flatMapLatest { id ->
        if (id == null || id <= 0) flowOf<User?>(null) else flow<User?> { emit(repository.getUserById(id)) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    init {
        viewModelScope.launch {
            try {
                ensureDefaultUsersExist()
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "Error ensuring default users", e)
            }
        }

        // Active session revocation monitoring: if logged in user is locked or removed, force logout immediately
        viewModelScope.launch {
            currentUser.collect { user ->
                val loggedIn = isLoggedIn.value
                if (loggedIn && (user == null || user.status.uppercase() == "LOCKED")) {
                    logout()
                }
            }
        }
    }

    private suspend fun ensureDefaultUsersExist() {
        // Ensure ADMIN and STAFF roles exist
        if (repository.roleDao.getRoleById(1L) == null) {
            repository.roleDao.insertRole(Role(roleId = 1L, roleName = "ADMIN", description = "Quản trị viên hệ thống"))
        }
        if (repository.roleDao.getRoleById(2L) == null) {
            repository.roleDao.insertRole(Role(roleId = 2L, roleName = "STAFF", description = "Nhân viên kho"))
        }

        val defaultHashedPassword = PasswordUtils.hashPassword("123456")

        // Ensure default admin user
        val adminUser = repository.getUserByUsername("admin")
        if (adminUser == null) {
            repository.insertUser(
                User(
                    username = "admin",
                    password = defaultHashedPassword,
                    fullName = "Nguyễn Văn Quản Lý",
                    phone = "0987654321",
                    email = "admin@warehouse.com",
                    roleId = 1L,
                    status = "ACTIVE"
                )
            )
        }

        // Ensure default staff user nv01
        val staffUser = repository.getUserByUsername("nv01")
        if (staffUser == null) {
            repository.insertUser(
                User(
                    username = "nv01",
                    password = defaultHashedPassword,
                    fullName = "Trần Thị Kho",
                    phone = "0912345678",
                    email = "staff@warehouse.com",
                    roleId = 2L,
                    status = "ACTIVE"
                )
            )
        }
    }

    fun login(usernameInput: String, passwordInput: String, rememberMe: Boolean) {
        val cleanUsername = usernameInput.trim()
        val cleanPassword = passwordInput.trim()

        if (cleanUsername.isBlank() || cleanPassword.isBlank()) {
            _authState.value = AuthState.Error("Sai tên đăng nhập hoặc mật khẩu")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                ensureDefaultUsersExist()

                val user = repository.getUserByUsername(cleanUsername)

                // 1. Username check & Password hash check
                if (user == null || !PasswordUtils.verifyPassword(cleanPassword, user.password)) {
                    _authState.value = AuthState.Error("Sai tên đăng nhập hoặc mật khẩu")
                    return@launch
                }

                // 2. Status check
                if (user.status.uppercase() == "LOCKED" || user.status.uppercase() != "ACTIVE") {
                    _authState.value = AuthState.Error("Tài khoản đã bị khóa, vui lòng liên hệ quản lý")
                    return@launch
                }

                // 3. Login success -> Update last_login_at
                val now = System.currentTimeMillis()
                repository.updateLastLoginAt(user.id, now)

                userPreferences.saveUserSession(
                    userId = user.id,
                    username = user.username,
                    fullName = user.fullName,
                    role = user.role,
                    rememberMe = rememberMe
                )

                _authState.value = AuthState.Authenticated(user)
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "Login exception", e)
                _authState.value = AuthState.Error("Sai tên đăng nhập hoặc mật khẩu")
            }
        }
    }

    fun changePassword(currentPassword: String, newPassword: String, onResult: (Boolean, String) -> Unit) {
        val userId = currentUserId.value ?: return onResult(false, "Chưa đăng nhập")
        val cleanCurrent = currentPassword.trim()
        val cleanNew = newPassword.trim()

        if (cleanCurrent.isBlank() || cleanNew.isBlank()) {
            return onResult(false, "Vui lòng nhập đầy đủ mật khẩu")
        }

        if (cleanNew.length < 6) {
            return onResult(false, "Mật khẩu mới phải có tối thiểu 6 ký tự")
        }

        if (cleanCurrent == cleanNew) {
            return onResult(false, "Mật khẩu mới không được trùng với mật khẩu cũ")
        }

        viewModelScope.launch {
            val user = repository.getUserById(userId)
            if (user == null || !PasswordUtils.verifyPassword(cleanCurrent, user.password)) {
                onResult(false, "Mật khẩu hiện tại không chính xác")
            } else {
                val newPasswordHash = PasswordUtils.hashPassword(cleanNew)
                val updatedUser = user.copy(
                    password = newPasswordHash,
                    updatedAt = System.currentTimeMillis()
                )
                repository.updateUser(updatedUser)

                repository.logUserAction(
                    action = "CHANGE_PASSWORD",
                    targetUserId = user.id,
                    targetUsername = user.username,
                    performedByUserId = user.id,
                    details = "Đổi mật khẩu thành công. Thu hồi session hiện tại."
                )

                // Mandatory security rule: Immediately revoke session and force user to re-login with new password
                logout()

                onResult(true, "Đổi mật khẩu thành công. Vui lòng đăng nhập lại bằng mật khẩu mới.")
            }
        }
    }

    fun updateProfile(fullName: String, phone: String, email: String, onResult: (Boolean, String) -> Unit) {
        val userId = currentUserId.value ?: return onResult(false, "Chưa đăng nhập")
        val cleanFullName = fullName.trim()

        if (cleanFullName.isBlank()) {
            return onResult(false, "Họ và tên không được để trống")
        }

        viewModelScope.launch {
            val user = repository.getUserById(userId)
            if (user == null) {
                onResult(false, "Không tìm thấy người dùng")
            } else {
                val updatedUser = user.copy(
                    fullName = cleanFullName,
                    phone = phone.trim(),
                    email = email.trim(),
                    updatedAt = System.currentTimeMillis()
                )
                repository.updateUser(updatedUser)

                userPreferences.saveUserSession(
                    userId = updatedUser.id,
                    username = updatedUser.username,
                    fullName = updatedUser.fullName,
                    role = updatedUser.role,
                    rememberMe = true
                )

                repository.logUserAction(
                    action = "EDIT_PROFILE",
                    targetUserId = updatedUser.id,
                    targetUsername = updatedUser.username,
                    performedByUserId = updatedUser.id,
                    details = "Cập nhật thông tin cá nhân: $cleanFullName"
                )

                onResult(true, "Cập nhật thông tin cá nhân thành công")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            userPreferences.clearSession()
            _authState.value = AuthState.Idle
        }
    }

    fun clearError() {
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.Idle
        }
    }
}

package com.example.warehouse.ui.supplier

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.warehouse.data.database.entity.Supplier
import com.example.warehouse.ui.component.*
import com.example.warehouse.ui.theme.PrimaryBlue
import com.example.warehouse.viewmodel.SupplierViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun SupplierScreen(
    viewModel: SupplierViewModel,
    userRole: String? = null
) {
    val suppliers by viewModel.suppliers.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showAddEditDialog by remember { mutableStateOf(false) }
    var selectedSupplierForEdit by remember { mutableStateOf<Supplier?>(null) }
    var selectedSupplierForDelete by remember { mutableStateOf<Supplier?>(null) }

    var dialogErrorMessage by remember { mutableStateOf<String?>(null) }
    var supplierInfoMessage by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    var activeSnackbarMessage by remember { mutableStateOf<String?>(null) }
    var snackbarJob by remember { mutableStateOf<Job?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

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
                onClick = {
                    selectedSupplierForEdit = null
                    dialogErrorMessage = null
                    showAddEditDialog = true
                },
                containerColor = PrimaryBlue,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_supplier_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Thêm nhà cung cấp")
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
            WarehouseSearchBar(
                query = searchQuery,
                onQueryChange = { viewModel.onSearchQueryChange(it) },
                placeholder = "Tìm kiếm nhà cung cấp..."
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (suppliers.isEmpty()) {
                WarehouseEmptyView(
                    title = "Chưa có nhà cung cấp",
                    message = if (searchQuery.isNotEmpty()) "Không tìm thấy nhà cung cấp phù hợp" else "Bấm '+' để thêm nhà cung cấp mới",
                    icon = Icons.Default.LocalShipping,
                    onActionClick = {
                        selectedSupplierForEdit = null
                        dialogErrorMessage = null
                        showAddEditDialog = true
                    },
                    actionTitle = "Thêm nhà cung cấp"
                )
            } else {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(suppliers, key = { it.id }) { sup ->
                        SupplierItemCard(
                            supplier = sup,
                            onEdit = {
                                selectedSupplierForEdit = sup
                                dialogErrorMessage = null
                                showAddEditDialog = true
                            },
                            onDelete = {
                                selectedSupplierForDelete = sup
                            },
                            onClick = {
                                // Nếu click item thì mở chi tiết, giả sử chi tiết là form edit (nhưng Staff ko xem đc form edit)
                                // Yêu cầu chỉ nói ẩn edit/delete icon. Click vào để xem chi tiết?
                                // "Xem danh sách nhà cung cấp ✅". Nhưng nếu click mở dialog AddEdit, có thể staff không được phép sửa.
                                // Sửa thông tin nhà cung cấp ❌. Vậy nếu click mà mở dialog thì sao?
                                // Thực ra form dialog nếu được submit (confirm) sẽ gọi updateSupplier (sẽ bị chặn ở viewModel).
                                selectedSupplierForEdit = sup
                                dialogErrorMessage = null
                                showAddEditDialog = true
                            },
                            userRole = userRole
                        )
                    }
                }
            }
        }
    }

    if (showAddEditDialog) {
        SupplierAddEditDialog(
            supplier = selectedSupplierForEdit,
            errorMessage = dialogErrorMessage,
            onConfirm = { sup ->
                if (selectedSupplierForEdit != null) {
                    viewModel.updateSupplier(
                        supplier = sup,
                        actingUserRole = userRole,
                        onSuccess = { msg ->
                            dialogErrorMessage = null
                            showAddEditDialog = false
                            showSingleSnackbar(msg)
                            coroutineScope.launch {
                                listState.animateScrollToItem(0)
                            }
                        },
                        onError = { msg ->
                            dialogErrorMessage = msg
                        }
                    )
                } else {
                    viewModel.addSupplier(
                        supplier = sup,
                        onSuccess = { msg ->
                            dialogErrorMessage = null
                            showAddEditDialog = false
                            showSingleSnackbar(msg)
                            coroutineScope.launch {
                                listState.animateScrollToItem(0)
                            }
                        },
                        onError = { msg ->
                            dialogErrorMessage = msg
                        }
                    )
                }
            },
            onDismiss = {
                showAddEditDialog = false
                dialogErrorMessage = null
            }
        )
    }

    selectedSupplierForDelete?.let { sup ->
        ConfirmDeleteDialog(
            title = "Xóa nhà cung cấp",
            message = "Bạn có chắc chắn muốn xóa nhà cung cấp '${sup.name}'?",
            onConfirm = {
                val targetSup = sup
                selectedSupplierForDelete = null
                viewModel.deleteSupplier(
                    supplier = targetSup,
                    actingUserRole = userRole,
                    onSuccess = { msg ->
                        showSingleSnackbar(msg)
                    },
                    onDeactivated = { msg ->
                        supplierInfoMessage = msg
                    },
                    onError = { msg ->
                        supplierInfoMessage = msg
                    }
                )
            },
            onDismiss = { selectedSupplierForDelete = null }
        )
    }

    if (supplierInfoMessage != null) {
        AlertDialog(
            onDismissRequest = { supplierInfoMessage = null },
            title = { Text("Thông báo", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)) },
            text = { Text(supplierInfoMessage ?: "", color = Color(0xFF334155)) },
            confirmButton = {
                TextButton(onClick = { supplierInfoMessage = null }) {
                    Text("Hiểu rồi", color = PrimaryBlue, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierAddEditDialog(
    supplier: Supplier?,
    errorMessage: String? = null,
    onConfirm: (Supplier) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(supplier?.name ?: "") }
    var contactPerson by remember { mutableStateOf(supplier?.contactPerson ?: "") }
    var phone by remember { mutableStateOf(supplier?.phone ?: "") }
    var email by remember { mutableStateOf(supplier?.email ?: "") }
    var taxCode by remember { mutableStateOf(supplier?.taxCode ?: "") }
    var address by remember { mutableStateOf(supplier?.address ?: "") }
    var note by remember { mutableStateOf(supplier?.note ?: "") }
    var status by remember { mutableStateOf(supplier?.status ?: "ACTIVE") }
    var localError by remember { mutableStateOf("") }

    var statusDropdownExpanded by remember { mutableStateOf(false) }

    val activeError = errorMessage.takeIf { !it.isNullOrBlank() } ?: localError

    val tfColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = PrimaryBlue,
        focusedLabelColor = PrimaryBlue,
        cursorColor = PrimaryBlue,
        unfocusedBorderColor = Color(0xFFCBD5E1)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        icon = {
            Icon(
                imageVector = Icons.Default.LocalShipping,
                contentDescription = null,
                tint = PrimaryBlue,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = if (supplier == null) "Thêm nhà cung cấp" else "Sửa nhà cung cấp",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Tên nhà cung cấp
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        localError = ""
                    },
                    label = { Text("Tên nhà cung cấp *") },
                    isError = activeError.isNotEmpty(),
                    singleLine = true,
                    colors = tfColors,
                    modifier = Modifier.fillMaxWidth().testTag("supplier_name_input")
                )
                if (activeError.isNotEmpty()) {
                    Text(
                        text = activeError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Trạng thái (Dropdown)
                ExposedDropdownMenuBox(
                    expanded = statusDropdownExpanded,
                    onExpandedChange = { statusDropdownExpanded = !statusDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = if (status.equals("ACTIVE", ignoreCase = true)) "Đang hoạt động" else "Ngừng hoạt động",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Trạng thái *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusDropdownExpanded) },
                        colors = tfColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = statusDropdownExpanded,
                        onDismissRequest = { statusDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Đang hoạt động") },
                            onClick = {
                                status = "ACTIVE"
                                statusDropdownExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Ngừng hoạt động") },
                            onClick = {
                                status = "INACTIVE"
                                statusDropdownExpanded = false
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Người liên hệ
                OutlinedTextField(
                    value = contactPerson,
                    onValueChange = { contactPerson = it },
                    label = { Text("Người liên hệ") },
                    singleLine = true,
                    colors = tfColors,
                    modifier = Modifier.fillMaxWidth().testTag("supplier_contact_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Số điện thoại & Email
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Số điện thoại") },
                        singleLine = true,
                        colors = tfColors,
                        modifier = Modifier.weight(1f).testTag("supplier_phone_input")
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        singleLine = true,
                        colors = tfColors,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Mã số thuế
                OutlinedTextField(
                    value = taxCode,
                    onValueChange = { taxCode = it },
                    label = { Text("Mã số thuế") },
                    singleLine = true,
                    colors = tfColors,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Địa chỉ
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Địa chỉ") },
                    maxLines = 2,
                    colors = tfColors,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Ghi chú
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Ghi chú") },
                    maxLines = 2,
                    colors = tfColors,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        localError = "Vui lòng nhập tên nhà cung cấp"
                    } else {
                        val newSupplier = (supplier ?: Supplier(supplierName = "")).copy(
                            supplierName = name.trim(),
                            contactPerson = contactPerson.trim(),
                            phone = phone.trim(),
                            email = email.trim(),
                            taxCode = taxCode.trim(),
                            address = address.trim(),
                            note = note.trim(),
                            status = status,
                            updatedAt = System.currentTimeMillis()
                        )
                        onConfirm(newSupplier)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("save_supplier_btn")
            ) {
                Text("Lưu", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Hủy", color = Color(0xFF475569))
            }
        }
    )
}

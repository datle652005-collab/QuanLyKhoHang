package com.example.warehouse.ui.category

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.warehouse.data.database.entity.Category
import com.example.warehouse.ui.component.*
import com.example.warehouse.ui.theme.PrimaryBlue
import com.example.warehouse.viewmodel.CategoryViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun CategoryScreen(
    viewModel: CategoryViewModel,
    userRole: String? = null
) {
    val categories by viewModel.categories.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showAddEditDialog by remember { mutableStateOf(false) }
    var selectedCategoryForEdit by remember { mutableStateOf<Category?>(null) }
    var selectedCategoryForDelete by remember { mutableStateOf<Category?>(null) }
    var dialogErrorMessage by remember { mutableStateOf<String?>(null) }
    var categoryErrorMessage by remember { mutableStateOf<String?>(null) }

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
            if (com.example.warehouse.utils.PermissionManager.canManageCategory(userRole)) {
                FloatingActionButton(
                    onClick = {
                        selectedCategoryForEdit = null
                        dialogErrorMessage = null
                        showAddEditDialog = true
                    },
                    containerColor = PrimaryBlue,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_category_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Thêm danh mục")
                }
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
                placeholder = "Tìm kiếm danh mục..."
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (categories.isEmpty()) {
                WarehouseEmptyView(
                    title = "Chưa có danh mục",
                    message = if (searchQuery.isNotEmpty()) "Không tìm thấy danh mục phù hợp" else "Bấm '+' để thêm danh mục hàng hóa mới",
                    icon = Icons.Default.Category,
                    onActionClick = {
                        selectedCategoryForEdit = null
                        dialogErrorMessage = null
                        showAddEditDialog = true
                    },
                    actionTitle = "Thêm danh mục"
                )
            } else {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(categories, key = { it.id }) { cat ->
                        CategoryItemCard(
                            category = cat,
                            onEdit = {
                                selectedCategoryForEdit = cat
                                dialogErrorMessage = null
                                showAddEditDialog = true
                            },
                            onDelete = {
                                selectedCategoryForDelete = cat
                            },
                            userRole = userRole
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Dialog
    if (showAddEditDialog) {
        CategoryAddEditDialog(
            initialName = selectedCategoryForEdit?.name ?: "",
            initialDescription = selectedCategoryForEdit?.description ?: "",
            errorMessage = dialogErrorMessage,
            onConfirm = { name, desc ->
                val category = selectedCategoryForEdit
                if (category != null) {
                    viewModel.updateCategory(
                        category = category,
                        newName = name,
                        newDescription = desc,
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
                    viewModel.addCategory(
                        name = name,
                        description = desc,
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
                }
            },
            onDismiss = {
                showAddEditDialog = false
                dialogErrorMessage = null
            }
        )
    }

    // Confirm Delete Dialog
    selectedCategoryForDelete?.let { cat ->
        ConfirmDeleteDialog(
            title = "Xóa danh mục",
            message = "Bạn có chắc chắn muốn xóa danh mục '${cat.name}'?",
            onConfirm = {
                viewModel.deleteCategory(
                    category = cat,
                    actingUserRole = userRole,
                    onSuccess = { msg ->
                        selectedCategoryForDelete = null
                        showSingleSnackbar(msg)
                    },
                    onError = { msg ->
                        selectedCategoryForDelete = null
                        categoryErrorMessage = msg
                    }
                )
            },
            onDismiss = { selectedCategoryForDelete = null }
        )
    }

    if (categoryErrorMessage != null) {
        AlertDialog(
            onDismissRequest = { categoryErrorMessage = null },
            title = { Text("Không thể xóa", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)) },
            text = { Text(categoryErrorMessage ?: "", color = Color(0xFF334155)) },
            confirmButton = {
                TextButton(onClick = { categoryErrorMessage = null }) {
                    Text("Đóng", color = PrimaryBlue, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

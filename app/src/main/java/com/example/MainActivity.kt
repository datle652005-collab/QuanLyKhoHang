package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.warehouse.ui.theme.PrimaryBlue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.warehouse.data.database.AppDatabase
import com.example.warehouse.data.repository.WarehouseRepository
import com.example.warehouse.ui.category.CategoryScreen
import com.example.warehouse.ui.component.WarehouseBottomBar
import com.example.warehouse.ui.component.WarehouseDrawerContent
import com.example.warehouse.ui.component.WarehouseTopBar
import com.example.warehouse.ui.dashboard.DashboardScreen
import com.example.warehouse.ui.export.ExportCreateScreen
import com.example.warehouse.ui.export.ExportDetailScreen
import com.example.warehouse.ui.export.ExportListScreen
import com.example.warehouse.ui.history.StockHistoryScreen
import com.example.warehouse.ui.import.ImportCreateScreen
import com.example.warehouse.ui.import.ImportDetailScreen
import com.example.warehouse.ui.import.ImportEditScreen
import com.example.warehouse.ui.import.ImportListScreen
import com.example.warehouse.ui.inventory.InventoryCreateScreen
import com.example.warehouse.ui.inventory.InventoryDetailScreen
import com.example.warehouse.ui.inventory.InventoryListScreen
import com.example.warehouse.ui.inventory.InventoryAlertsScreen
import com.example.warehouse.ui.inventory.ResetMinStockScreen
import com.example.warehouse.ui.login.LoginScreen
import com.example.warehouse.ui.product.ProductAddEditScreen
import com.example.warehouse.ui.product.ProductDetailScreen
import com.example.warehouse.ui.product.ProductListScreen
import com.example.warehouse.ui.report.ReportScreen
import com.example.warehouse.ui.setting.ProfileScreen
import com.example.warehouse.ui.setting.SettingsScreen
import com.example.warehouse.ui.setting.UserManagementScreen
import com.example.warehouse.ui.supplier.SupplierScreen
import com.example.warehouse.ui.theme.WarehouseTheme
import com.example.warehouse.ui.transaction.TransactionListScreen
import com.example.warehouse.utils.UserPreferences
import com.example.warehouse.viewmodel.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext)
        val repository = WarehouseRepository(database)
        
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            database.invalidationTracker.addObserver(object : androidx.room.InvalidationTracker.Observer("inventory") {
                override fun onInvalidated(tables: Set<String>) {
                    repository.inventoryChangedFlow.tryEmit(Unit)
                }
            })
        }
        
        val userPreferences = UserPreferences(applicationContext)
        val viewModelFactory = AppViewModelFactory(repository, userPreferences)

        setContent {
            WarehouseTheme {
                WarehouseApp(viewModelFactory, repository)
            }
        }
    }
}

@Composable
fun WarehouseApp(
    viewModelFactory: AppViewModelFactory,
    repository: WarehouseRepository
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val authViewModel: AuthViewModel = viewModel(factory = viewModelFactory)
    val dashboardViewModel: DashboardViewModel = viewModel(factory = viewModelFactory)
    val categoryViewModel: CategoryViewModel = viewModel(factory = viewModelFactory)
    val supplierViewModel: SupplierViewModel = viewModel(factory = viewModelFactory)
    val productViewModel: ProductViewModel = viewModel(factory = viewModelFactory)
    val importViewModel: ImportViewModel = viewModel(factory = viewModelFactory)
    val exportViewModel: ExportViewModel = viewModel(factory = viewModelFactory)
    val inventoryViewModel: InventoryViewModel = viewModel(factory = viewModelFactory)
    val historyViewModel: HistoryViewModel = viewModel(factory = viewModelFactory)
    val notificationViewModel: NotificationViewModel = viewModel()
    
    val reportViewModel: ReportViewModel = viewModel(factory = viewModelFactory)
    val userViewModel: UserViewModel = viewModel(factory = viewModelFactory)
    val inventoryAlertsViewModel: InventoryAlertsViewModel = viewModel(factory = viewModelFactory)

    val notificationService = remember {
        com.example.warehouse.utils.NotificationService(
            context.applicationContext,
            repository,
            com.example.warehouse.utils.NotificationPreferencesManager(context.applicationContext)
        )
    }

    LaunchedEffect(Unit) {
        val activity = context as? android.app.Activity
        val intent = activity?.intent
        if (intent != null && intent.hasExtra("NAVIGATE_TO")) {
            val navigateTo = intent.getStringExtra("NAVIGATE_TO")
            if (navigateTo == "inventory_alerts") {
                val alertType = intent.getStringExtra("ALERT_TYPE_FILTER")
                val filterType = when (alertType) {
                    "Đã hết hàng" -> "OUT"
                    "Tồn kho gần hết" -> "NEAR"
                    "Tồn kho thấp" -> "LOW"
                    else -> "ALL"
                }
                inventoryAlertsViewModel.setSelectedAlertType(filterType)
                
                val scrollToId = intent.getLongExtra("SCROLL_TO_PRODUCT_ID", -1L)
                if (scrollToId != -1L) {
                    inventoryAlertsViewModel.setScrollToProductId(scrollToId)
                }
                
                navController.navigate("inventory_alerts")
            }
            // Clear intent so it doesn't trigger again on rotation
            activity.intent = android.content.Intent()
        }
    }

    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    val currentUserId by authViewModel.currentUserId.collectAsState()
    val userFullName by authViewModel.currentFullName.collectAsState()
    val userRole by authViewModel.currentUserRole.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "dashboard"

    val isMainTabRoute = currentRoute in listOf("dashboard", "products", "transactions", "settings")

    val showMainTopBar = currentRoute != "login" && currentRoute != "dashboard"

    val screenTitle = when {
        currentRoute == "products" -> "Danh sách sản phẩm"
        currentRoute == "transactions" -> "Giao dịch"
        currentRoute == "inventory_alerts" -> "Nhắc nhở tồn kho"
        currentRoute == "reset_min_stock" -> "Đặt lại tồn kho tối thiểu"
        currentRoute == "notification_settings" -> "Thông báo"
        currentRoute == "settings" -> "Cài đặt"
        currentRoute == "categories" -> "Danh mục hàng hóa"
        currentRoute == "suppliers" -> "Nhà cung cấp"
        currentRoute.startsWith("product_add_edit/0") || currentRoute == "product_add_edit" -> "Thêm sản phẩm"
        currentRoute.startsWith("product_add_edit") -> "Chỉnh sửa sản phẩm"
        currentRoute.startsWith("product_detail") -> "Chi tiết hàng hóa"
        currentRoute == "import_list" -> "Phiếu nhập kho"
        currentRoute == "import_create" -> "Tạo phiếu nhập kho"
        currentRoute.startsWith("import_detail") -> "Chi tiết phiếu nhập"
        currentRoute == "export_list" -> "Phiếu xuất kho"
        currentRoute == "export_create" -> "Tạo phiếu xuất kho"
        currentRoute.startsWith("export_detail") -> "Chi tiết phiếu xuất"
        currentRoute == "inventory_list" -> "Kiểm kê kho"
        currentRoute == "inventory_create" -> "Tạo phiếu kiểm kê"
        currentRoute.startsWith("inventory_detail") -> "Chi tiết kiểm kê"
        currentRoute == "history" -> "Lịch sử biến động"
        currentRoute == "reports" -> "Thống kê"
        currentRoute == "user_management" -> "Quản lý người dùng"
        currentRoute == "profile" -> "Hồ sơ cá nhân"
        else -> "Quản lý kho hàng"
    }

    val topBarActions: @Composable RowScope.() -> Unit = {
        when (currentRoute) {
            "inventory_alerts" -> {
                if (userRole == "ADMIN") {
                    Button(
                        onClick = { navController.navigate("reset_min_stock") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryBlue,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("navigate_reset_min_stock_btn")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Đặt lại", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    LaunchedEffect(currentUserId) {
        currentUserId?.let { id ->
            notificationViewModel.loadSettings(id)
        }
    }

    LaunchedEffect(currentUserId) {
        if (currentUserId != null && currentUserId != -1L) {
            repository.inventoryChangedFlow.collect {
                notificationService.checkAndSendImmediateNotification(currentUserId!!)
            }
        }
    }

    LaunchedEffect(isLoggedIn, currentRoute) {
        if (!isLoggedIn && currentRoute != "login") {
            navController.navigate("login") {
                popUpTo(0) { inclusive = true }
            }
        } else if (isLoggedIn && currentRoute == "login") {
            navController.navigate("dashboard") {
                popUpTo("login") { inclusive = true }
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = false,
        drawerContent = {
            WarehouseDrawerContent(
                currentRoute = currentRoute,
                fullName = userFullName,
                role = userRole,
                onNavigate = { route ->
                    scope.launch { drawerState.close() }
                    val targetRoute = when (route) {
                        "imports" -> "import_list"
                        "exports" -> "export_list"
                        "inventory" -> "inventory_list"
                        "users" -> "user_management"
                        else -> route
                    }
                    if (currentRoute != targetRoute) {
                        navController.navigate(targetRoute) {
                            popUpTo("dashboard") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                onLogout = {
                    scope.launch { drawerState.close() }
                    authViewModel.logout()
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                if (showMainTopBar) {
                    WarehouseTopBar(
                        title = screenTitle,
                        showBackButton = !isMainTabRoute,
                        onBackClick = { navController.navigateUp() },
                        actions = topBarActions
                    )
                }
            },
            bottomBar = {
                if (isLoggedIn && isMainTabRoute) {
                    WarehouseBottomBar(
                        currentRoute = currentRoute,
                        onNavigate = { route ->
                            if (currentRoute != route) {
                                navController.navigate(route) {
                                    popUpTo("dashboard") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                NavHost(
                    navController = navController,
                    startDestination = if (isLoggedIn) "dashboard" else "login",
                    enterTransition = { fadeIn(animationSpec = tween(120)) },
                    exitTransition = { fadeOut(animationSpec = tween(120)) },
                    popEnterTransition = { fadeIn(animationSpec = tween(120)) },
                    popExitTransition = { fadeOut(animationSpec = tween(120)) }
                ) {
                    composable("login") {
                        LoginScreen(
                            viewModel = authViewModel,
                            onLoginSuccess = {
                                navController.navigate("dashboard") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        )
                    }

                    composable("dashboard") {
                        DashboardScreen(
                            viewModel = dashboardViewModel,
                            userFullName = userFullName,
                            onNavigateToProducts = { navController.navigate("products") },
                            onNavigateToImport = { navController.navigate("import_create") },
                            onNavigateToExport = { navController.navigate("export_create") },
                            onNavigateToInventory = { navController.navigate("inventory_create") },
                            onNavigateToProductDetail = { id -> navController.navigate("product_detail/$id") },
                            onNavigateToInventoryAlerts = { navController.navigate("inventory_alerts") }
                        )
                    }

                    composable("inventory_alerts") {
                        InventoryAlertsScreen(
                            viewModel = inventoryAlertsViewModel,
                            userRole = userRole,
                            onNavigateToReset = { navController.navigate("reset_min_stock") },
                            onNavigateBack = { navController.navigateUp() }
                        )
                    }

                    composable("reset_min_stock") {
                        ResetMinStockScreen(
                            viewModel = inventoryAlertsViewModel,
                            userRole = userRole,
                            onNavigateBack = { navController.navigateUp() }
                        )
                    }

                    composable("products") { backStackEntry ->
                        val successMsg = backStackEntry.savedStateHandle.get<String>("success_message")
                        ProductListScreen(
                            viewModel = productViewModel,
                            onNavigateToAddProduct = { navController.navigate("product_add_edit/0") },
                            onNavigateToProductDetail = { id -> navController.navigate("product_detail/$id") },
                            successMessage = successMsg,
                            onClearSuccessMessage = { backStackEntry.savedStateHandle.remove<String>("success_message") }
                        )
                    }

                    composable("transactions") { backStackEntry ->
                        val successMsg = backStackEntry.savedStateHandle.get<String>("success_message")
                        TransactionListScreen(
                            importViewModel = importViewModel,
                            exportViewModel = exportViewModel,
                            onNavigateToImportCreate = { navController.navigate("import_create") },
                            onNavigateToExportCreate = { navController.navigate("export_create") },
                            onNavigateToImportDetail = { id -> navController.navigate("import_detail/$id") },
                            onNavigateToExportDetail = { id -> navController.navigate("export_detail/$id") },
                            successMessage = successMsg,
                            onClearSuccessMessage = { backStackEntry.savedStateHandle.remove<String>("success_message") }
                        )
                    }

                    composable("settings") {
                        SettingsScreen(
                            authViewModel = authViewModel,
                            onNavigateToCategories = { navController.navigate("categories") },
                            onNavigateToSuppliers = { navController.navigate("suppliers") },
                            onNavigateToInventory = { navController.navigate("inventory_list") },
                            onNavigateToHistory = { navController.navigate("history") },
                            onNavigateToReports = { navController.navigate("reports") },
                            onNavigateToUserManagement = { navController.navigate("user_management") },
                            onNavigateToNotifications = { navController.navigate("notification_settings") },
                            onLogout = {
                                navController.navigate("login") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable("categories") {
                        CategoryScreen(viewModel = categoryViewModel, userRole = userRole)
                    }

                    composable("suppliers") {
                        SupplierScreen(viewModel = supplierViewModel, userRole = userRole)
                    }

                    composable(
                        route = "product_add_edit/{productId}",
                        arguments = listOf(navArgument("productId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val pId = backStackEntry.arguments?.getLong("productId") ?: 0L
                        ProductAddEditScreen(
                            viewModel = productViewModel,
                            productId = if (pId > 0) pId else null,
                            userFullName = userFullName,
                            onNavigateBack = { navController.navigateUp() },
                            onSuccess = { msg ->
                                navController.previousBackStackEntry?.savedStateHandle?.set("success_message", msg)
                                navController.navigateUp()
                            }
                        )
                    }

                    composable(
                        route = "product_detail/{productId}",
                        arguments = listOf(navArgument("productId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val pId = backStackEntry.arguments?.getLong("productId") ?: 0L
                        val successMsg = backStackEntry.savedStateHandle.get<String>("success_message")
                        ProductDetailScreen(
                            productId = pId,
                            productViewModel = productViewModel,
                            historyViewModel = historyViewModel,
                            userRole = userRole,
                            onNavigateToEdit = { id -> navController.navigate("product_add_edit/$id") },
                            onNavigateBack = { navController.navigateUp() },
                            onNavigateBackWithMsg = { msg ->
                                navController.previousBackStackEntry?.savedStateHandle?.set("success_message", msg)
                                navController.navigateUp()
                            },
                            successMessage = successMsg,
                            onClearSuccessMessage = { backStackEntry.savedStateHandle.remove<String>("success_message") },
                            onNavigateToReceiptDetail = { action, id ->
                                when (action) {
                                    "IMPORT" -> navController.navigate("import_detail/$id")
                                    "EXPORT" -> navController.navigate("export_detail/$id")
                                    "INVENTORY_ADJUST" -> navController.navigate("inventory_detail/$id")
                                }
                            }
                        )
                    }

                    composable("import_list") { backStackEntry ->
                        val successMsg = backStackEntry.savedStateHandle.get<String>("success_message")
                        ImportListScreen(
                            viewModel = importViewModel,
                            onNavigateToCreate = { navController.navigate("import_create") },
                            onNavigateToDetail = { id -> navController.navigate("import_detail/$id") },
                            successMessage = successMsg,
                            onClearSuccessMessage = { backStackEntry.savedStateHandle.remove<String>("success_message") }
                        )
                    }

                    composable("import_create") {
                        ImportCreateScreen(
                            importViewModel = importViewModel,
                            productViewModel = productViewModel,
                            inventoryViewModel = inventoryViewModel,
                            currentUserId = currentUserId,
                            userFullName = userFullName,
                            onNavigateBack = { navController.navigateUp() },
                            onSuccess = { msg ->
                                navController.previousBackStackEntry?.savedStateHandle?.set("success_message", msg)
                                navController.navigateUp()
                            }
                        )
                    }

                    composable(
                        route = "import_detail/{receiptId}",
                        arguments = listOf(navArgument("receiptId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val rId = backStackEntry.arguments?.getLong("receiptId") ?: 0L
                        val successMsg = backStackEntry.savedStateHandle.get<String>("success_message")
                        ImportDetailScreen(
                            receiptId = rId,
                            viewModel = importViewModel,
                            authViewModel = authViewModel,
                            onNavigateToEdit = { id -> navController.navigate("import_edit/$id") },
                            onNavigateBack = { navController.navigateUp() },
                            onNavigateBackWithMsg = { msg ->
                                navController.previousBackStackEntry?.savedStateHandle?.set("success_message", msg)
                                navController.navigateUp()
                            },
                            successMessage = successMsg,
                            onClearSuccessMessage = { backStackEntry.savedStateHandle.remove<String>("success_message") }
                        )
                    }

                    composable(
                        route = "import_edit/{receiptId}",
                        arguments = listOf(navArgument("receiptId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val rId = backStackEntry.arguments?.getLong("receiptId") ?: 0L
                        ImportEditScreen(
                            receiptId = rId,
                            importViewModel = importViewModel,
                            productViewModel = productViewModel,
                            currentUserId = currentUserId,
                            userFullName = userFullName,
                            userRole = userRole,
                            onNavigateBack = { navController.navigateUp() },
                            onSuccess = { msg ->
                                navController.previousBackStackEntry?.savedStateHandle?.set("success_message", msg)
                                navController.navigateUp()
                            }
                        )
                    }

                    composable("export_list") {
                        ExportListScreen(
                            viewModel = exportViewModel,
                            onNavigateToCreate = { navController.navigate("export_create") },
                            onNavigateToDetail = { id -> navController.navigate("export_detail/$id") }
                        )
                    }

                    composable("export_create") {
                        ExportCreateScreen(
                            exportViewModel = exportViewModel,
                            productViewModel = productViewModel,
                            inventoryViewModel = inventoryViewModel,
                            currentUserId = currentUserId,
                            userFullName = userFullName,
                            onNavigateBack = { navController.navigateUp() }
                        )
                    }

                    composable(
                        route = "export_detail/{receiptId}",
                        arguments = listOf(navArgument("receiptId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val rId = backStackEntry.arguments?.getLong("receiptId") ?: 0L
                        ExportDetailScreen(
                            receiptId = rId,
                            viewModel = exportViewModel,
                            productViewModel = productViewModel,
                            userRole = userRole,
                            userFullName = userFullName,
                            onNavigateBack = { navController.navigateUp() }
                        )
                    }

                    composable("inventory_list") { backStackEntry ->
                        val successMsg = backStackEntry.savedStateHandle.getStateFlow<String?>("success_message", null).collectAsState().value
                        InventoryListScreen(
                            viewModel = inventoryViewModel,
                            userRole = userRole,
                            onNavigateToCreate = { navController.navigate("inventory_create") },
                            onNavigateToDetail = { id -> navController.navigate("inventory_detail/$id") },
                            successMessage = successMsg,
                            onClearSuccessMessage = { backStackEntry.savedStateHandle.remove<String>("success_message") }
                        )
                    }

                    composable("inventory_create") {
                        InventoryCreateScreen(
                            inventoryViewModel = inventoryViewModel,
                            currentUserId = currentUserId,
                            userFullName = userFullName,
                            userRole = userRole,
                            onNavigateToDetail = { id, msg ->
                                navController.navigate("inventory_detail/$id") {
                                    popUpTo("inventory_list")
                                }
                                navController.currentBackStackEntry?.savedStateHandle?.set("success_message", msg)
                            },
                            onNavigateBack = { navController.navigateUp() },
                            onSuccess = { msg ->
                                navController.previousBackStackEntry?.savedStateHandle?.set("success_message", msg)
                            }
                        )
                    }

                    composable(
                        route = "inventory_detail/{checkId}",
                        arguments = listOf(navArgument("checkId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val cId = backStackEntry.arguments?.getLong("checkId") ?: 0L
                        val successMsg = backStackEntry.savedStateHandle.getStateFlow<String?>("success_message", null).collectAsState().value
                        InventoryDetailScreen(
                            checkId = cId,
                            viewModel = inventoryViewModel,
                            userRole = userRole,
                            onNavigateBack = { navController.navigateUp() },
                            onNavigateBackWithMsg = { msg ->
                                navController.previousBackStackEntry?.savedStateHandle?.set("success_message", msg)
                                navController.navigateUp()
                            },
                            successMessage = successMsg,
                            onClearSuccessMessage = { backStackEntry.savedStateHandle.remove<String>("success_message") }
                        )
                    }

                    composable("reports") {
                        LaunchedEffect(userRole) {
                            if (!com.example.warehouse.utils.PermissionManager.canViewFullReportModule(userRole)) {
                                navController.popBackStack()
                            }
                        }
                        if (com.example.warehouse.utils.PermissionManager.canViewFullReportModule(userRole)) {
                            ReportScreen(
                                viewModel = reportViewModel,
                                userRole = userRole,
                                onNavigateToProductDetail = { id -> navController.navigate("product_detail/$id") }
                            )
                        }
                    }
                    
                    composable("notification_settings") {
                        com.example.warehouse.ui.setting.NotificationSettingsScreen(
                            viewModel = notificationViewModel,
                            onNavigateBack = { navController.navigateUp() }
                        )
                    }

                    composable("profile") {
                        ProfileScreen(
                            authViewModel = authViewModel,
                            onNavigateToUserManagement = { navController.navigate("user_management") },
                            onLogout = {
                                navController.navigate("login") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable("user_management") {
                        LaunchedEffect(userRole) {
                            if (!com.example.warehouse.utils.PermissionManager.canViewUserManagement(userRole)) {
                                navController.popBackStack()
                            }
                        }
                        if (com.example.warehouse.utils.PermissionManager.canViewUserManagement(userRole)) {
                            UserManagementScreen(
                                userViewModel = userViewModel,
                                authViewModel = authViewModel,
                                userRole = userRole
                            )
                        }
                    }
                }
            }
        }
    }
}

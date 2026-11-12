package com.example.warehouse.data.repository

import com.example.warehouse.data.database.AppDatabase
import com.example.warehouse.data.database.entity.*
import com.example.warehouse.data.database.relation.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

import kotlinx.coroutines.flow.MutableSharedFlow
import androidx.room.withTransaction

class WarehouseRepository(
    private val db: AppDatabase,
    var onInventoryChanged: (() -> Unit)? = null
) {
    val inventoryChangedFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    // DAOs
    val userDao = db.userDao()
    val roleDao = db.roleDao()
    val userAuditLogDao = db.userAuditLogDao()
    val categoryDao = db.categoryDao()
    val supplierDao = db.supplierDao()
    val productDao = db.productDao()
    val inventoryDao = db.inventoryDao()
    val importReceiptDao = db.importReceiptDao()
    val exportReceiptDao = db.exportReceiptDao()
    val inventoryCheckDao = db.inventoryCheckDao()

    // --- Users ---
    fun getAllUsers(): Flow<List<User>> = userDao.getAllUsers()
    suspend fun getUserByUsername(username: String): User? = userDao.getUserByUsername(username)
    suspend fun getUserById(id: Long): User? = userDao.getUserById(id)
    suspend fun insertUser(user: User): Long = userDao.insertUser(user)
    suspend fun updateUser(user: User) = userDao.updateUser(user)

    suspend fun updateLastLoginAt(userId: Long, timestamp: Long = System.currentTimeMillis()) {
        userDao.updateLastLoginAt(userId, timestamp)
    }

    suspend fun countActiveAdminsExcluding(excludeUserId: Long): Int {
        return userDao.countActiveAdminsExcluding(excludeUserId)
    }

    suspend fun countActiveAdmins(): Int {
        return userDao.countActiveAdmins()
    }

    suspend fun hasUserTransactions(userId: Long): Boolean {
        val importCount = userDao.countImportReceiptsByUser(userId)
        val exportCount = userDao.countExportReceiptsByUser(userId)
        val stockCheckCount = userDao.countStockChecksByUser(userId)
        return (importCount + exportCount + stockCheckCount) > 0
    }

    suspend fun logUserAction(
        action: String,
        targetUserId: Long?,
        targetUsername: String,
        performedByUserId: Long?,
        details: String
    ) {
        try {
            userAuditLogDao.insertLog(
                UserAuditLog(
                    action = action,
                    targetUserId = targetUserId,
                    targetUsername = targetUsername,
                    performedByUserId = performedByUserId,
                    details = details,
                    timestamp = System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            android.util.Log.e("WarehouseRepository", "Error writing audit log", e)
        }
    }

    // --- Categories ---
    fun getAllCategories(): Flow<List<Category>> = categoryDao.getAllCategories()
    fun searchCategories(query: String): Flow<List<Category>> = categoryDao.searchCategories(query)
    suspend fun getCategoryById(id: Long): Category? = categoryDao.getCategoryById(id)
    suspend fun countCategoryByName(name: String, excludeId: Long = 0): Int = categoryDao.countCategoryByName(name, excludeId)
    suspend fun getActiveProductCountByCategory(categoryId: Long): Int = productDao.getActiveProductCountByCategory(categoryId)
    suspend fun insertCategory(category: Category): Long = categoryDao.insertCategory(category)
    suspend fun updateCategory(category: Category) = categoryDao.updateCategory(category)
    suspend fun deleteCategory(category: Category) = categoryDao.deleteCategory(category)

    // --- Suppliers ---
    fun getAllSuppliers(): Flow<List<Supplier>> = supplierDao.getAllSuppliers()
    fun getActiveSuppliers(): Flow<List<Supplier>> = supplierDao.getActiveSuppliers()
    fun searchSuppliers(query: String): Flow<List<Supplier>> = supplierDao.searchSuppliers(query)
    suspend fun getSupplierById(id: Long): Supplier? = supplierDao.getSupplierById(id)
    suspend fun countSupplierByName(name: String, excludeId: Long = 0): Int = supplierDao.countSupplierByName(name, excludeId)
    suspend fun getImportReceiptCountBySupplier(supplierId: Long): Int = importReceiptDao.getImportReceiptCountBySupplier(supplierId)
    suspend fun insertSupplier(supplier: Supplier): Long = supplierDao.insertSupplier(supplier)
    suspend fun updateSupplier(supplier: Supplier) = supplierDao.updateSupplier(supplier)
    suspend fun deleteSupplier(supplier: Supplier) = supplierDao.deleteSupplier(supplier)

    // --- Products ---
    fun getActiveProductsWithDetails(): Flow<List<ProductWithCategoryAndSupplier>> = productDao.getActiveProductsWithDetails()
    fun getAllProductsWithDetails(): Flow<List<ProductWithCategoryAndSupplier>> = productDao.getAllProductsWithDetails()

    fun searchProducts(query: String, includeInactive: Boolean = false): Flow<List<ProductWithCategoryAndSupplier>> {
        return if (includeInactive) {
            productDao.searchAllProducts(query)
        } else {
            productDao.searchActiveProducts(query)
        }
    }

    fun getProductsByCategory(categoryId: Long, includeInactive: Boolean = false): Flow<List<ProductWithCategoryAndSupplier>> {
        return if (includeInactive) {
            productDao.getProductsByCategory(categoryId)
        } else {
            productDao.getActiveProductsByCategory(categoryId)
        }
    }

    fun getLowStockProducts(): Flow<List<ProductWithCategoryAndSupplier>> = productDao.getLowStockProducts()
    fun getLowStockProductsFiltered(categoryId: Long?): Flow<List<ProductWithCategoryAndSupplier>> = productDao.getLowStockProductsFiltered(categoryId)
    
    fun getProductCount(): Flow<Int> = productDao.getProductCount()
    fun getTotalStockQuantity(): Flow<Int> = productDao.getTotalStockQuantity()
    fun getCategoryStockStats(): Flow<List<com.example.warehouse.data.database.dao.CategoryStockStat>> = productDao.getCategoryStockStats()
    fun getLowStockCount(): Flow<Int> = productDao.getLowStockCount()

    fun getTotalImportQuantityByDateRange(startTime: Long, endTime: Long): Flow<Int> = importReceiptDao.getTotalImportQuantityByDateRange(startTime, endTime)
    fun getDailyImportStats(startTime: Long, endTime: Long): Flow<List<com.example.warehouse.data.database.dao.DailyQuantityStat>> = importReceiptDao.getDailyImportStats(startTime, endTime)
    fun getTopImportedProducts(startTime: Long, endTime: Long, limit: Int = 10): Flow<List<com.example.warehouse.data.database.dao.TopProductStat>> = importReceiptDao.getTopImportedProducts(startTime, endTime, limit)

    fun getTotalExportQuantityByDateRange(startTime: Long, endTime: Long): Flow<Int> = exportReceiptDao.getTotalExportQuantityByDateRange(startTime, endTime)
    fun getDailyExportStats(startTime: Long, endTime: Long): Flow<List<com.example.warehouse.data.database.dao.DailyQuantityStat>> = exportReceiptDao.getDailyExportStats(startTime, endTime)
    fun getTopExportedProducts(startTime: Long, endTime: Long, limit: Int = 10): Flow<List<com.example.warehouse.data.database.dao.TopProductStat>> = exportReceiptDao.getTopExportedProducts(startTime, endTime, limit)

    suspend fun getProductWithDetailsById(id: Long): ProductWithCategoryAndSupplier? = productDao.getProductWithDetailsById(id)
    suspend fun getProductById(id: Long): Product? = productDao.getProductById(id)
    suspend fun getProductByBarcodeOrCode(code: String): ProductWithCategoryAndSupplier? = productDao.getProductByBarcodeOrCode(code)

    suspend fun isProductCodeDuplicate(code: String, excludeId: Long = 0): Boolean {
        return productDao.countActiveProductCode(code.trim(), excludeId) > 0
    }

    suspend fun isProductNameDuplicate(name: String, excludeId: Long = 0): Boolean {
        return productDao.countActiveProductName(name.trim(), excludeId) > 0
    }

    // Creates PRODUCT and automatically creates INVENTORY with quantity = 0 inside an atomic transaction
    suspend fun createProductAtomic(product: Product): Long {
        return db.withTransaction {
            val validCategory = if (product.categoryId != null && product.categoryId!! > 0 && categoryDao.getCategoryById(product.categoryId!!) != null) product.categoryId else null
            val sanitizedProduct = product.copy(
                categoryId = validCategory
            )
            val newId = productDao.insertProduct(sanitizedProduct)
            inventoryDao.insertOrUpdateInventory(
                Inventory(
                    productId = newId,
                    quantity = 0,
                    updatedAt = System.currentTimeMillis()
                )
            )
            newId
        }
    }

    suspend fun insertProduct(product: Product, initialQuantity: Int = 0): Long {
        return createProductAtomic(product)
    }

    suspend fun updateProduct(product: Product) {
        val validCategory = if (product.categoryId != null && product.categoryId!! > 0 && categoryDao.getCategoryById(product.categoryId!!) != null) product.categoryId else null
        val updatedProduct = product.copy(
            categoryId = validCategory,
            updatedAt = System.currentTimeMillis()
        )
        productDao.updateProduct(updatedProduct)
    }

    suspend fun deleteProductSmart(product: Product): Pair<Boolean, String> {
        return db.withTransaction {
            val importCount = productDao.countImportDetailsByProduct(product.productId)
            val exportCount = productDao.countExportDetailsByProduct(product.productId)
            val stockCheckCount = productDao.countStockCheckDetailsByProduct(product.productId)
            val hasTransactions = (importCount + exportCount + stockCheckCount) > 0

            val inv = inventoryDao.getInventoryByProductId(product.productId)
            val currentQty = inv?.quantity ?: 0

            if (!hasTransactions && currentQty <= 0) {
                // Hard delete
                if (inv != null) {
                    // Delete inventory record
                    inventoryDao.deleteInventoryByProductId(product.productId)
                }
                productDao.deleteProduct(product)
                Pair(true, "Đã xóa hoàn toàn sản phẩm '${product.productName}'.")
            } else {
                // Soft delete (set status = "INACTIVE")
                val inactiveProduct = product.copy(
                    status = "INACTIVE",
                    updatedAt = System.currentTimeMillis()
                )
                productDao.updateProduct(inactiveProduct)
                Pair(
                    false,
                    "Sản phẩm '${product.productName}' đã từng có giao dịch hoặc còn tồn kho nên được chuyển sang trạng thái Ngừng kinh doanh (Ẩn khỏi danh sách) thay vì xóa hoàn toàn."
                )
            }
        }
    }

    // --- Import Receipt (Transaction) ---
    fun getAllImportReceipts(): Flow<List<ImportReceiptWithDetails>> = importReceiptDao.getAllImportReceipts()
    fun getTodayImportCount(startOfDay: Long): Flow<Int> = importReceiptDao.getTodayImportCount(startOfDay)
    suspend fun getImportReceiptById(id: Long): ImportReceiptWithDetails? = importReceiptDao.getImportReceiptById(id)

    suspend fun createImportReceipt(
        code: String?,
        supplierId: Long?,
        supplierNameText: String?,
        userId: Long?,
        importDate: Long,
        note: String,
        rawItems: List<Pair<Product, Pair<Int, Double>>>
    ): Long = db.withTransaction {
        // 1. Validate supplier exists if ID is provided
        if (supplierId != null && supplierId > 0 && supplierDao.getSupplierById(supplierId) == null) {
            throw IllegalArgumentException("Nhà cung cấp không tồn tại trong hệ thống.")
        }


        // 2. Validate importDate is not in the future
        val now = System.currentTimeMillis()
        if (importDate > now + 60_000L) {
            throw IllegalArgumentException("Ngày nhập kho không được chọn trong tương lai.")
        }

        // 3. Validate items non-empty
        if (rawItems.isEmpty()) {
            throw IllegalArgumentException("Phiếu nhập kho phải chứa ít nhất 1 sản phẩm.")
        }

        // 4. Consolidate duplicate products in the same receipt cart
        val consolidatedMap = mutableMapOf<Long, Triple<Product, Int, Double>>()
        for ((product, pair) in rawItems) {
            val (qty, price) = pair
            if (qty <= 0) {
                throw IllegalArgumentException("Số lượng nhập của sản phẩm '${product.productName}' phải lớn hơn 0.")
            }
            if (price < 0.0) {
                throw IllegalArgumentException("Đơn giá nhập của sản phẩm '${product.productName}' không được nhỏ hơn 0.")
            }
            val existing = consolidatedMap[product.productId]
            if (existing == null) {
                consolidatedMap[product.productId] = Triple(product, qty, price)
            } else {
                val newQty = existing.second + qty
                consolidatedMap[product.productId] = Triple(product, newQty, price)
            }
        }

        // 5. Verify product existence and ACTIVE status
        for ((productId, triple) in consolidatedMap) {
            val dbProduct = productDao.getProductById(productId)
                ?: throw IllegalArgumentException("Sản phẩm mã '${triple.first.productCode}' không tồn tại trong hệ thống.")
            if (!dbProduct.status.equals("ACTIVE", ignoreCase = true)) {
                throw IllegalArgumentException("Sản phẩm '${dbProduct.productName}' hiện bị ngừng kinh doanh (trạng thái: ${dbProduct.status}).")
            }
        }

        // 6. Generate import_code if not supplied
        val finalCode = if (!code.isNullOrBlank()) {
            code.trim()
        } else {
            val dateStr = com.example.warehouse.utils.DateUtils.formatDateCode(importDate)
            val randomSuffix = (100..999).random()
            "PN-$dateStr-$randomSuffix"
        }

        // 7. Calculate total amount
        var totalAmount = 0.0
        val importDetails = mutableListOf<ImportDetail>()
        val createdAt = System.currentTimeMillis()

        // 8. Create & Insert Receipt record with status "Đã lưu"
        val validUserId = if (userId != null && userId > 0 && userDao.getUserById(userId) != null) {
            userId
        } else {
            null
        }

        val validSupplierId = if (supplierId != null && supplierId > 0) supplierId else null

        val receipt = ImportReceipt(
            importCode = finalCode,
            supplierId = validSupplierId,
            userId = validUserId,
            importDate = importDate,
            createdAt = createdAt,
            totalAmount = 0.0,
            status = "Đã lưu",
            note = note.trim(),
            supplierNameText = if (validSupplierId == null) supplierNameText?.trim() else null
        )
        val receiptId = importReceiptDao.insertImportReceipt(receipt)

        // 9. Process consolidated detail rows & update inventory
        for ((productId, triple) in consolidatedMap) {
            val (product, qty, price) = triple
            val subtotal = qty * price
            totalAmount += subtotal

            importDetails.add(
                ImportDetail(
                    importId = receiptId,
                    productId = productId,
                    quantity = qty,
                    unitPrice = price,
                    subtotal = subtotal
                )
            )

            // Update Inventory (quantity = quantity + importQuantity)
            val inv = inventoryDao.getInventoryByProductId(productId)
            val currentStock = inv?.quantity ?: 0
            val newStock = currentStock + qty
            inventoryDao.insertOrUpdateInventory(
                Inventory(
                    inventoryId = inv?.inventoryId ?: 0,
                    productId = productId,
                    quantity = newStock,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }

        // Insert all import details
        importReceiptDao.insertImportDetails(importDetails)

        // Update receipt total amount
        importReceiptDao.updateImportReceipt(receipt.copy(importId = receiptId, totalAmount = totalAmount))

        receiptId
    }

    suspend fun cancelImportReceipt(
        receiptId: Long,
        user: User,
        deleteReason: String
    ) = db.withTransaction {
        if (!user.role.equals("ADMIN", ignoreCase = true) && user.roleId != 1L) {
            throw IllegalAccessException("Chỉ Quản lý kho mới có quyền hủy phiếu nhập kho!")
        }

        val receiptWithDetails = importReceiptDao.getImportReceiptById(receiptId)
            ?: throw IllegalArgumentException("Không tìm thấy phiếu nhập kho ID: $receiptId")

        val receipt = receiptWithDetails.receipt
        if (receipt.status.equals("Đã hủy", ignoreCase = true)) {
            throw IllegalStateException("Phiếu nhập kho này đã bị hủy từ trước.")
        }

        // Check inventory for ALL products in the receipt
        for (detailWithProd in receiptWithDetails.details) {
            val p = detailWithProd.product
            val d = detailWithProd.detail
            val inv = inventoryDao.getInventoryByProductId(d.productId)
            val currentStock = inv?.quantity ?: 0

            if (currentStock < d.quantity) {
                throw IllegalStateException(
                    "Không thể hủy: hàng đã được xuất kho một phần (Sản phẩm '${p.productName}' hiện còn tồn $currentStock, không đủ để trừ lại ${d.quantity})"
                )
            }
        }

        // Subtract imported quantities back from INVENTORY
        val now = System.currentTimeMillis()
        for (detailWithProd in receiptWithDetails.details) {
            val d = detailWithProd.detail
            val inv = inventoryDao.getInventoryByProductId(d.productId)!!
            val newStock = inv.quantity - d.quantity
            inventoryDao.updateStock(d.productId, newStock, now)
        }

        // Update receipt status to "Đã hủy"
        val cancelLog = "[Đã hủy bởi ${user.fullName} lúc ${com.example.warehouse.utils.DateUtils.formatDateTime(now)}]"
        val newNote = if (receipt.note.isBlank()) cancelLog else "${receipt.note}\n$cancelLog"

        val updatedReceipt = receipt.copy(
            status = "Đã hủy",
            note = newNote,
            deleteReason = deleteReason
        )
        importReceiptDao.updateImportReceipt(updatedReceipt)
    }

    suspend fun updateImportReceiptFull(
        receiptId: Long,
        supplierId: Long?,
        supplierNameText: String?,
        importDate: Long,
        note: String,
        rawItems: List<Pair<Product, Pair<Int, Double>>>,
        user: User
    ) = db.withTransaction {
        if (!com.example.warehouse.utils.PermissionManager.canEditOrDeleteImportDetail(user.role)) {
            throw IllegalAccessException("Bạn không có quyền thực hiện thao tác này")
        }
        val now = System.currentTimeMillis()
        if (importDate > now + 60_000L) {
            throw IllegalArgumentException("Ngày nhập kho không được chọn trong tương lai.")
        }
        
        if (supplierId != null && supplierId > 0 && supplierDao.getSupplierById(supplierId) == null) {
            throw IllegalArgumentException("Nhà cung cấp không tồn tại trong hệ thống.")
        }

        if (rawItems.isEmpty()) {
            throw IllegalArgumentException("Phiếu nhập kho phải chứa ít nhất 1 sản phẩm.")
        }

        val receiptWithDetails = importReceiptDao.getImportReceiptById(receiptId)
            ?: throw IllegalArgumentException("Không tìm thấy phiếu nhập kho ID: $receiptId")

        val receipt = receiptWithDetails.receipt
        if (receipt.status.equals("Đã hủy", ignoreCase = true)) {
            throw IllegalStateException("Không thể sửa phiếu đã bị hủy.")
        }

        // Revert old inventory changes first
        for (detailWithProd in receiptWithDetails.details) {
            val d = detailWithProd.detail
            val p = detailWithProd.product
            val inv = inventoryDao.getInventoryByProductId(d.productId)
            val currentStock = inv?.quantity ?: 0
            
            if (currentStock < d.quantity) {
                throw IllegalStateException("Không thể sửa: sản phẩm '${p.productName}' hiện còn tồn $currentStock, không đủ để trừ lại ${d.quantity} từ phiếu cũ.")
            }
            
            val newStock = currentStock - d.quantity
            inventoryDao.updateStock(d.productId, newStock, now)
        }
        
        // Delete old details
        importReceiptDao.deleteImportDetailsForReceipt(receiptId)

        // Consolidate new items
        val consolidatedMap = mutableMapOf<Long, Triple<Product, Int, Double>>()
        for ((product, pair) in rawItems) {
            val (qty, price) = pair
            if (qty <= 0) throw IllegalArgumentException("Số lượng nhập phải lớn hơn 0.")
            if (price < 0.0) throw IllegalArgumentException("Đơn giá nhập không được nhỏ hơn 0.")
            
            val existing = consolidatedMap[product.productId]
            if (existing == null) {
                consolidatedMap[product.productId] = Triple(product, qty, price)
            } else {
                consolidatedMap[product.productId] = Triple(product, existing.second + qty, price)
            }
        }

        // Verify products and apply new inventory changes
        var totalAmount = 0.0
        val importDetails = mutableListOf<ImportDetail>()
        for ((productId, triple) in consolidatedMap) {
            val (product, qty, price) = triple
            val dbProduct = productDao.getProductById(productId)
                ?: throw IllegalArgumentException("Sản phẩm không tồn tại trong hệ thống.")
            
            val subtotal = qty * price
            totalAmount += subtotal
            
            importDetails.add(
                ImportDetail(
                    importId = receiptId,
                    productId = productId,
                    quantity = qty,
                    unitPrice = price,
                    subtotal = subtotal
                )
            )
            
            val inv = inventoryDao.getInventoryByProductId(productId)
            val currentStock = inv?.quantity ?: 0
            val newStock = currentStock + qty
            inventoryDao.insertOrUpdateInventory(
                Inventory(
                    inventoryId = inv?.inventoryId ?: 0,
                    productId = productId,
                    quantity = newStock,
                    updatedAt = now
                )
            )
        }
        
        importReceiptDao.insertImportDetails(importDetails)

        val validSupplierId = if (supplierId != null && supplierId > 0) supplierId else null
        val editLog = "[Sửa phiếu bởi ${user.fullName} lúc ${com.example.warehouse.utils.DateUtils.formatDateTime(now)}]"
        val newNote = if (note.isBlank()) editLog else "${note.trim()}\n$editLog"

        val updatedReceipt = receipt.copy(
            supplierId = validSupplierId,
            supplierNameText = if (validSupplierId == null) supplierNameText?.trim() else null,
            importDate = importDate,
            note = newNote,
            totalAmount = totalAmount,
            status = "Đã chỉnh sửa"
        )
        importReceiptDao.updateImportReceipt(updatedReceipt)
    }

    suspend fun updateImportReceiptDate(
        receiptId: Long,
        newImportDate: Long,
        user: User
    ) = db.withTransaction {
        val now = System.currentTimeMillis()
        if (newImportDate > now + 60_000L) {
            throw IllegalArgumentException("Ngày nhập kho không được chọn trong tương lai.")
        }

        val receiptWithDetails = importReceiptDao.getImportReceiptById(receiptId)
            ?: throw IllegalArgumentException("Không tìm thấy phiếu nhập kho ID: $receiptId")

        val receipt = receiptWithDetails.receipt
        if (receipt.status.equals("Đã hủy", ignoreCase = true)) {
            throw IllegalStateException("Không thể sửa ngày của phiếu đã bị hủy.")
        }

        val oldDateStr = com.example.warehouse.utils.DateUtils.formatDateOnly(receipt.importDate)
        val newDateStr = com.example.warehouse.utils.DateUtils.formatDateOnly(newImportDate)
        val editLog = "[Sửa ngày nhập bởi ${user.fullName} lúc ${com.example.warehouse.utils.DateUtils.formatDateTime(now)}: $oldDateStr -> $newDateStr]"
        val newNote = if (receipt.note.isBlank()) editLog else "${receipt.note}\n$editLog"

        val updatedReceipt = receipt.copy(
            importDate = newImportDate,
            note = newNote
        )
        importReceiptDao.updateImportReceipt(updatedReceipt)
    }

    // --- Export Receipt (Transaction) ---
    fun getAllExportReceipts(): Flow<List<ExportReceiptWithDetails>> = exportReceiptDao.getAllExportReceipts()
    fun getTodayExportCount(startOfDay: Long): Flow<Int> = exportReceiptDao.getTodayExportCount(startOfDay)
    suspend fun getExportReceiptById(id: Long): ExportReceiptWithDetails? = exportReceiptDao.getExportReceiptById(id)

    suspend fun generateExportCode(exportDate: Long): String {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = exportDate
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        val startOfDay = cal.timeInMillis

        cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
        cal.set(java.util.Calendar.MINUTE, 59)
        cal.set(java.util.Calendar.SECOND, 59)
        cal.set(java.util.Calendar.MILLISECOND, 999)
        val endOfDay = cal.timeInMillis

        val count = exportReceiptDao.getExportCountForDay(startOfDay, endOfDay)
        val year = cal.get(java.util.Calendar.YEAR)
        val month = cal.get(java.util.Calendar.MONTH) + 1
        val day = cal.get(java.util.Calendar.DAY_OF_MONTH)
        val dateStr = String.format("%04d%02d%02d", year, month, day)
        val seq = String.format("%03d", count + 1)
        return "PXK-$dateStr-$seq"
    }

    suspend fun createExportReceipt(
        code: String,
        userId: Long?,
        exportDate: Long,
        recipient: String,
        note: String,
        details: List<Pair<Product, Pair<Int, Double>>>,
        createdBy: String
    ): Long {
        return db.withTransaction {
            var total = 0.0
            details.forEach { (_, pair) ->
                total += pair.first * pair.second
            }

            val validUserId = if (userId != null && userId > 0 && userDao.getUserById(userId) != null) {
                userId
            } else {
                null
            }

            val receipt = ExportReceipt(
                exportCode = code,
                userId = validUserId,
                exportDate = exportDate,
                createdAt = System.currentTimeMillis(),
                recipient = recipient,
                totalAmount = total,
                status = "Đã lưu",
                note = note
            )
            val receiptId = exportReceiptDao.insertExportReceipt(receipt)

            val exportDetails = mutableListOf<ExportDetail>()
            for ((product, pair) in details) {
                val (qty, price) = pair
                val subtotal = qty * price
                exportDetails.add(
                    ExportDetail(
                        exportId = receiptId,
                        productId = product.productId,
                        quantity = qty,
                        unitPrice = price,
                        subtotal = subtotal
                    )
                )

                // Securely decrease stock in inventory
                val rowsAffected = inventoryDao.decreaseStockSecure(product.productId, qty)
                if (rowsAffected == 0) {
                    throw IllegalStateException("Sản phẩm '${product.productName}' chỉ còn ${inventoryDao.getInventoryByProductId(product.productId)?.quantity ?: 0} trong kho, không đủ để xuất $qty")
                }
            }

            exportReceiptDao.insertExportDetails(exportDetails)
            receiptId
        }
    }

    suspend fun updateExportReceipt(
        receiptId: Long,
        newExportDate: Long,
        newRecipient: String,
        newNote: String,
        newDetails: List<Pair<Long, Int>>, // List of Pair(productId, quantity)
        userFullName: String
    ) {
        db.withTransaction {
            val oldReceiptWithDetails = exportReceiptDao.getExportReceiptById(receiptId)
                ?: throw IllegalArgumentException("Không tìm thấy phiếu xuất kho")

            val oldReceipt = oldReceiptWithDetails.receipt
            val oldDetails = oldReceiptWithDetails.details

            val changesLog = mutableListOf<String>()

            // 1. Map old details for easy lookup
            val oldDetailsMap = oldDetails.associateBy { it.detail.productId }

            // 2. Map new details to consolidate duplicates
            val consolidatedNewDetails = newDetails.groupBy { it.first }
                .mapValues { entry -> entry.value.sumOf { it.second } }

            // Track details to insert or update
            val detailsToInsertOrUpdate = mutableListOf<ExportDetail>()

            // 3. Process products in consolidated new details
            for ((productId, newQty) in consolidatedNewDetails) {
                val oldDetailWithProduct = oldDetailsMap[productId]
                val product = productDao.getProductById(productId)
                    ?: throw IllegalArgumentException("Không tìm thấy sản phẩm")

                if (oldDetailWithProduct != null) {
                    // Existing line, quantity might have changed
                    val oldQty = oldDetailWithProduct.detail.quantity
                    val diff = newQty - oldQty

                    if (diff > 0) {
                        // Increasing quantity, decrease stock in inventory
                        val affected = inventoryDao.decreaseStockSecure(productId, diff)
                        if (affected == 0) {
                            throw IllegalStateException("Không đủ tồn kho để tăng số lượng xuất cho sản phẩm '${product.productName}'")
                        }
                        changesLog.add("Sản phẩm '${product.productName}': tăng số lượng xuất từ $oldQty lên $newQty")
                    } else if (diff < 0) {
                        // Decreasing quantity, increase stock in inventory
                        inventoryDao.increaseStockSecure(productId, -diff)
                        changesLog.add("Sản phẩm '${product.productName}': giảm số lượng xuất từ $oldQty xuống $newQty")
                    }

                    // Keep original unitPrice
                    detailsToInsertOrUpdate.add(
                        ExportDetail(
                            exportDetailId = oldDetailWithProduct.detail.exportDetailId,
                            exportId = receiptId,
                            productId = productId,
                            quantity = newQty,
                            unitPrice = oldDetailWithProduct.detail.unitPrice,
                            subtotal = newQty * oldDetailWithProduct.detail.unitPrice
                        )
                    )
                } else {
                    // New line added
                    val unitPrice = product.exportPrice
                    if (unitPrice <= 0.0) {
                        throw IllegalStateException("Sản phẩm '${product.productName}' chưa được thiết lập giá bán. Vui lòng cập nhật giá ở module Sản phẩm.")
                    }

                    val affected = inventoryDao.decreaseStockSecure(productId, newQty)
                    if (affected == 0) {
                        val availableStock = inventoryDao.getInventoryByProductId(productId)?.quantity ?: 0
                        throw IllegalStateException("Sản phẩm '${product.productName}' chỉ còn $availableStock trong kho, không đủ để xuất $newQty")
                    }

                    changesLog.add("Thêm sản phẩm '${product.productName}' mới vào phiếu với số lượng $newQty, đơn giá ${unitPrice}")
                    detailsToInsertOrUpdate.add(
                        ExportDetail(
                            exportId = receiptId,
                            productId = productId,
                            quantity = newQty,
                            unitPrice = unitPrice,
                            subtotal = newQty * unitPrice
                        )
                    )
                }
            }

            // 4. Process deleted products
            for (oldDetailWithProduct in oldDetails) {
                val productId = oldDetailWithProduct.detail.productId
                if (!consolidatedNewDetails.containsKey(productId)) {
                    val oldQty = oldDetailWithProduct.detail.quantity
                    val product = oldDetailWithProduct.product

                    // Add back entire quantity to inventory
                    inventoryDao.increaseStockSecure(productId, oldQty)
                    changesLog.add("Xóa sản phẩm '${product.productName}' khỏi phiếu, hoàn trả $oldQty vào kho")
                }
            }

            // 5. Update database details
            exportReceiptDao.deleteExportDetailsForReceipt(receiptId)
            exportReceiptDao.insertExportDetails(detailsToInsertOrUpdate)

            // 6. Calculate new total amount
            val newTotalAmount = detailsToInsertOrUpdate.sumOf { it.subtotal }

            // 7. Write Edit Log to Receipt Notes or Logcat
            val timestampStr = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
            android.util.Log.i("ExportEdit", "User $userFullName edited ExportReceipt $receiptId at $timestampStr:\n" + changesLog.joinToString("\n"))

            val loggedNote = if (changesLog.isNotEmpty()) {
                val logBlock = "\n[Log sửa $timestampStr bởi $userFullName]:\n" + changesLog.joinToString("\n") { "- $it" }
                newNote + logBlock
            } else {
                newNote
            }

            // 8. Update Export Receipt
            val updatedReceipt = oldReceipt.copy(
                exportDate = newExportDate,
                recipient = newRecipient,
                note = loggedNote,
                totalAmount = newTotalAmount,
                status = "Đã chỉnh sửa"
            )
            exportReceiptDao.updateExportReceipt(updatedReceipt)
        }
    }

    suspend fun deleteExportReceiptSecure(receiptId: Long, deleteReason: String) {
        db.withTransaction {
            val receiptWithDetails = exportReceiptDao.getExportReceiptById(receiptId)
                ?: throw IllegalArgumentException("Không tìm thấy phiếu xuất kho")

            val receipt = receiptWithDetails.receipt
            if (receipt.status == "Đã hủy") {
                return@withTransaction // Already deleted
            }

            // Credit back all details to inventory
            for (detailWithProduct in receiptWithDetails.details) {
                val detail = detailWithProduct.detail
                inventoryDao.increaseStockSecure(detail.productId, detail.quantity)
            }

            // Update status and delete reason
            val updatedReceipt = receipt.copy(
                status = "Đã hủy",
                deleteReason = deleteReason
            )
            exportReceiptDao.updateExportReceipt(updatedReceipt)
        }
    }

    // --- Stock Check (Inventory Check) ---
    fun getAllInventoryChecks(): Flow<List<InventoryCheckWithDetails>> = inventoryCheckDao.getAllInventoryChecks()
    suspend fun getInventoryCheckById(id: Long): InventoryCheckWithDetails? = inventoryCheckDao.getInventoryCheckById(id)
    suspend fun getActiveChecksForProducts(productIds: List<Long>): List<InventoryCheckWithDetails> = 
        inventoryCheckDao.getActiveChecksForProducts(productIds)

    suspend fun createStockCheck(
        title: String,
        note: String,
        checkDate: Long,
        userId: Long?,
        productIds: List<Long>
    ): Long {
        val validUserId = if (userId != null && userId > 0 && userDao.getUserById(userId) != null) {
            userId
        } else {
            null
        }

        val code = "KKK-${System.currentTimeMillis().toString().takeLast(6)}"
        val check = StockCheck(
            checkCode = code,
            title = title,
            note = note,
            checkDate = checkDate,
            userId = validUserId,
            createdAt = System.currentTimeMillis(),
            status = "Đang tiến hành"
        )
        val checkId = inventoryCheckDao.insertInventoryCheck(check)

        val details = mutableListOf<StockCheckDetail>()
        for (productId in productIds) {
            val inv = inventoryDao.getInventoryByProductId(productId)
            val systemQty = inv?.quantity ?: 0

            details.add(
                StockCheckDetail(
                    checkId = checkId,
                    productId = productId,
                    systemQuantity = systemQty,
                    actualQuantity = null,
                    difference = null,
                    reason = ""
                )
            )
        }

        inventoryCheckDao.insertInventoryDetails(details)
        return checkId
    }

    suspend fun updateStockCheckHeaderAndProducts(
        checkId: Long,
        title: String,
        note: String,
        checkDate: Long,
        productIds: List<Long>
    ) {
        val existingWithDetails = inventoryCheckDao.getInventoryCheckById(checkId)
            ?: throw IllegalStateException("Không tìm thấy đợt kiểm kê #$checkId")

        if (existingWithDetails.check.status !in listOf("Đang tiến hành", "Yêu cầu kiểm kê lại")) {
            throw IllegalStateException("Chỉ được sửa đợt kiểm kê khi ở trạng thái 'Đang tiến hành' hoặc 'Yêu cầu kiểm kê lại'")
        }

        val updatedCheck = existingWithDetails.check.copy(
            title = title,
            note = note,
            checkDate = checkDate
        )
        inventoryCheckDao.updateInventoryCheck(updatedCheck)

        val existingDetailMap = existingWithDetails.details.associateBy { it.detail.productId }
        val newDetails = mutableListOf<StockCheckDetail>()

        for (productId in productIds) {
            val existing = existingDetailMap[productId]
            if (existing != null) {
                newDetails.add(existing.detail)
            } else {
                val inv = inventoryDao.getInventoryByProductId(productId)
                val systemQty = inv?.quantity ?: 0
                newDetails.add(
                    StockCheckDetail(
                        checkId = checkId,
                        productId = productId,
                        systemQuantity = systemQty,
                        actualQuantity = null,
                        difference = null,
                        reason = ""
                    )
                )
            }
        }

        db.withTransaction {
            inventoryCheckDao.deleteInventoryDetailsByCheckId(checkId)
            inventoryCheckDao.insertInventoryDetails(newDetails)
        }
    }

    suspend fun saveActualQuantities(
        checkId: Long,
        updatedDetails: List<StockCheckDetail>
    ) {
        val checkWithDetails = inventoryCheckDao.getInventoryCheckById(checkId)
            ?: throw IllegalStateException("Không tìm thấy đợt kiểm kê #$checkId")

        if (checkWithDetails.check.status !in listOf("Đang tiến hành", "Yêu cầu kiểm kê lại")) {
            throw IllegalStateException("Chỉ được nhập số liệu khi đợt kiểm kê ở trạng thái 'Đang tiến hành' hoặc 'Yêu cầu kiểm kê lại'")
        }

        inventoryCheckDao.updateInventoryDetails(updatedDetails)
    }

    suspend fun submitForApproval(checkId: Long) {
        val checkWithDetails = inventoryCheckDao.getInventoryCheckById(checkId)
            ?: throw IllegalStateException("Không tìm thấy đợt kiểm kê #$checkId")

        if (checkWithDetails.check.status !in listOf("Đang tiến hành", "Yêu cầu kiểm kê lại")) {
            throw IllegalStateException("Đợt kiểm kê không ở trạng thái 'Đang tiến hành' hoặc 'Yêu cầu kiểm kê lại'")
        }

        val uncounted = checkWithDetails.details.filter { it.detail.actualQuantity == null }
        if (uncounted.isNotEmpty()) {
            throw IllegalStateException("Còn ${uncounted.size} sản phẩm chưa đếm số lượng thực tế!")
        }

        val updatedCheck = checkWithDetails.check.copy(status = "Đang chờ xét duyệt")
        inventoryCheckDao.updateInventoryCheck(updatedCheck)
    }

    suspend fun returnToDraft(checkId: Long) {
        val checkWithDetails = inventoryCheckDao.getInventoryCheckById(checkId)
            ?: throw IllegalStateException("Không tìm thấy đợt kiểm kê #$checkId")

        if (checkWithDetails.check.status != "Đang chờ xét duyệt") {
            throw IllegalStateException("Chỉ có thể trả lại khi đang ở trạng thái 'Đang chờ xét duyệt'")
        }

        val updatedCheck = checkWithDetails.check.copy(status = "Đang tiến hành")
        inventoryCheckDao.updateInventoryCheck(updatedCheck)
    }

    suspend fun rejectStockCheck(checkId: Long, reason: String) {
        val checkWithDetails = inventoryCheckDao.getInventoryCheckById(checkId)
            ?: throw IllegalStateException("Không tìm thấy đợt kiểm kê #$checkId")

        if (checkWithDetails.check.status != "Đang chờ xét duyệt") {
            throw IllegalStateException("Chỉ có thể từ chối khi đang ở trạng thái 'Đang chờ xét duyệt'")
        }

        val dateStr = com.example.warehouse.utils.DateUtils.formatDate(System.currentTimeMillis(), "HH:mm dd/MM/yyyy")
        val reasonHeader = "[Lý do từ chối - $dateStr]: $reason"
        val newNote = if (checkWithDetails.check.note.isBlank()) {
            reasonHeader
        } else {
            "${checkWithDetails.check.note}\n$reasonHeader"
        }

        val updatedCheck = checkWithDetails.check.copy(
            status = "Yêu cầu kiểm kê lại",
            note = newNote
        )
        inventoryCheckDao.updateInventoryCheck(updatedCheck)
    }

    suspend fun approveStockCheck(checkId: Long) {
        val checkWithDetails = inventoryCheckDao.getInventoryCheckById(checkId)
            ?: throw IllegalStateException("Không tìm thấy đợt kiểm kê #$checkId")

        if (checkWithDetails.check.status != "Đang chờ xét duyệt") {
            throw IllegalStateException("Chỉ có thể duyệt đợt kiểm kê khi đang ở trạng thái 'Đang chờ xét duyệt'")
        }

        val now = System.currentTimeMillis()

        // ATOMIC TRANSACTION: Update inventory for all details + update status to "Đã hoàn thành"
        db.withTransaction {
            for (detailWithProd in checkWithDetails.details) {
                val detail = detailWithProd.detail
                val actual = detail.actualQuantity
                    ?: throw IllegalStateException("Sản phẩm '${detailWithProd.product.productName}' chưa có số lượng thực tế")

                val existingInv = inventoryDao.getInventoryByProductId(detail.productId)
                inventoryDao.insertOrUpdateInventory(
                    Inventory(
                        inventoryId = existingInv?.inventoryId ?: 0,
                        productId = detail.productId,
                        quantity = actual,
                        updatedAt = now
                    )
                )
            }

            val completedCheck = checkWithDetails.check.copy(status = "Đã hoàn thành")
            inventoryCheckDao.updateInventoryCheck(completedCheck)
        }
    }

    suspend fun cancelStockCheck(checkId: Long, deleteReason: String) {
        val checkWithDetails = inventoryCheckDao.getInventoryCheckById(checkId)
            ?: throw IllegalStateException("Không tìm thấy đợt kiểm kê #$checkId")

        if (checkWithDetails.check.status == "Đã hoàn thành") {
            throw IllegalStateException("Không thể hủy đợt kiểm kê đã hoàn thành!")
        }

        val cancelledCheck = checkWithDetails.check.copy(
            status = "Đã hủy",
            deleteReason = deleteReason
        )
        inventoryCheckDao.updateInventoryCheck(cancelledCheck)
    }

    suspend fun deleteStockCheck(checkId: Long) {
        val checkWithDetails = inventoryCheckDao.getInventoryCheckById(checkId)
            ?: throw IllegalStateException("Không tìm thấy đợt kiểm kê #$checkId")

        if (checkWithDetails.check.status == "Đã hoàn thành") {
            throw IllegalStateException("Không thể xóa đợt kiểm kê đã hoàn thành!")
        }

        db.withTransaction {
            inventoryCheckDao.deleteInventoryDetailsByCheckId(checkId)
            inventoryCheckDao.deleteInventoryCheckById(checkId)
        }
    }

    // --- Stock History ---
    fun getAllStockHistory(): Flow<List<StockHistory>> {
        return combine(
            importReceiptDao.getAllImportReceipts(),
            exportReceiptDao.getAllExportReceipts(),
            inventoryCheckDao.getAllInventoryChecks()
        ) { imports, exports, checks ->
            val list = mutableListOf<StockHistory>()

            imports.forEach { item ->
                item.details.forEach { detailWithProd ->
                    list.add(
                        StockHistory(
                            id = detailWithProd.detail.importDetailId,
                            productId = detailWithProd.detail.productId,
                            action = "IMPORT",
                            quantityBefore = 0,
                            quantityChange = detailWithProd.detail.quantity,
                            quantityAfter = detailWithProd.detail.quantity,
                            referenceId = item.receipt.importId,
                            referenceType = "IMPORT_RECEIPT",
                            createdBy = item.user?.fullName ?: "Admin",
                            createdAt = item.receipt.importDate,
                            referenceCode = item.receipt.importCode,
                            partnerName = item.supplier?.name ?: item.receipt.supplierNameText,
                            status = item.receipt.status,
                            unitPrice = detailWithProd.detail.unitPrice
                        )
                    )
                }
            }

            exports.forEach { item ->
                item.details.forEach { detailWithProd ->
                    list.add(
                        StockHistory(
                            id = detailWithProd.detail.exportDetailId,
                            productId = detailWithProd.detail.productId,
                            action = "EXPORT",
                            quantityBefore = 0,
                            quantityChange = -detailWithProd.detail.quantity,
                            quantityAfter = 0,
                            referenceId = item.receipt.exportId,
                            referenceType = "EXPORT_RECEIPT",
                            createdBy = item.user?.fullName ?: "Staff",
                            createdAt = item.receipt.exportDate,
                            referenceCode = item.receipt.exportCode,
                            partnerName = null,
                            status = item.receipt.status,
                            unitPrice = detailWithProd.detail.unitPrice
                        )
                    )
                }
            }

            checks.forEach { item ->
                item.details.forEach { detailWithProd ->
                    val diff = detailWithProd.detail.difference ?: 0
                    val actual = detailWithProd.detail.actualQuantity ?: detailWithProd.detail.systemQuantity
                    list.add(
                        StockHistory(
                            id = detailWithProd.detail.detailId,
                            productId = detailWithProd.detail.productId,
                            action = "INVENTORY_ADJUST",
                            quantityBefore = detailWithProd.detail.systemQuantity,
                            quantityChange = diff,
                            quantityAfter = actual,
                            referenceId = item.check.checkId,
                            referenceType = "STOCK_CHECK",
                            createdBy = item.user?.fullName ?: "Quản lý kho",
                            createdAt = item.check.checkDate,
                            referenceCode = item.check.code,
                            partnerName = null,
                            status = item.check.status,
                            unitPrice = 0.0
                        )
                    )
                }
            }

            list.sortedByDescending { it.createdAt }
        }
    }

    fun getStockHistoryByProduct(productId: Long): Flow<List<StockHistory>> {
        return getAllStockHistory().map { list -> list.filter { it.productId == productId } }
    }
}

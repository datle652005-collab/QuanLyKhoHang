import java.io.File
import kotlin.random.Random

fun main() {
    val builder = StringBuilder()
    builder.appendLine("        private suspend fun populateInitialData(db: AppDatabase) {")
    builder.appendLine("            try {")
    builder.appendLine("                db.withTransaction {")
    builder.appendLine("                    // Seed Roles")
    builder.appendLine("                    val adminRoleId = db.roleDao().insertRole(Role(roleName = \"ADMIN\", description = \"Quản trị viên hệ thống\"))")
    builder.appendLine("                    val staffRoleId = db.roleDao().insertRole(Role(roleName = \"STAFF\", description = \"Nhân viên kho\"))")
    
    builder.appendLine("                    // Seed Users")
    builder.appendLine("                    val userIds = mutableListOf<Long>()")
    val users = listOf(
        "admin" to "Nguyễn Văn Quản Lý",
        "admin2" to "Trần Thị Quản Lý",
        "nv01" to "Lê Văn Kho",
        "nv02" to "Phạm Thị Kho",
        "nv03" to "Hoàng Văn Hàng",
        "nv04" to "Đinh Thị Hàng"
    )
    for (i in users.indices) {
        val (username, fullName) = users[i]
        val role = if (i < 2) "adminRoleId" else "staffRoleId"
        builder.appendLine("                    userIds.add(db.userDao().insertUser(User(username = \"$username\", password = \"123456\", fullName = \"$fullName\", roleId = $role, status = \"ACTIVE\")))")
    }
    
    builder.appendLine("                    // Seed Categories")
    builder.appendLine("                    val catIds = mutableListOf<Long>()")
    val cats = listOf("Điện tử & Thiết bị", "Gia dụng & Đời sống", "Thiết bị văn phòng", "Thực phẩm", "Mỹ phẩm", "Thời trang", "Nội thất", "Đồ chơi")
    for (cat in cats) {
        builder.appendLine("                    catIds.add(db.categoryDao().insertCategory(Category(categoryName = \"$cat\", description = \"Mô tả $cat\")))")
    }
    
    builder.appendLine("                    // Seed Suppliers")
    builder.appendLine("                    val supIds = mutableListOf<Long>()")
    for (i in 1..12) {
        builder.appendLine("                    supIds.add(db.supplierDao().insertSupplier(Supplier(supplierCode = \"SUP${String.format("%03d", i)}\", supplierName = \"Nhà cung cấp $i\", phone = \"09000000${String.format("%02d", i)}\", address = \"Địa chỉ $i\")))")
    }
    
    builder.appendLine("                    // Seed Products & Inventory")
    builder.appendLine("                    val productIds = mutableListOf<Long>()")
    val random = Random(42)
    for (i in 1..50) {
        val catIdx = i % 8
        val minStock = random.nextInt(5, 20)
        val initialStock = if (i % 10 == 0) 0 else if (i % 7 == 0) minStock - 2 else minStock + random.nextInt(10, 50)
        builder.appendLine("                    val p$i = db.productDao().insertProduct(Product(categoryId = catIds[$catIdx], productCode = \"SP${String.format("%03d", i)}\", productName = \"Sản phẩm $i\", importPrice = ${(random.nextInt(1, 100) * 10000).toDouble()}, exportPrice = ${(random.nextInt(110, 150) * 10000).toDouble()}, minStock = $minStock))")
        builder.appendLine("                    productIds.add(p$i)")
        builder.appendLine("                    db.inventoryDao().insertOrUpdateInventory(Inventory(productId = p$i, quantity = $initialStock))")
    }
    
    builder.appendLine("                    // Seed Import Receipts")
    val minDate = 1767225600000L // 01/01/2026
    val maxDate = 1786752000000L // 15/08/2026
    
    for (i in 1..40) {
        val date = minDate + (maxDate - minDate) / 40 * i
        builder.appendLine("                    val ir$i = db.importReceiptDao().insertImportReceipt(ImportReceipt(importCode = \"PNK-2026-${String.format("%03d", i)}\", supplierId = supIds[${i % 12}], userId = userIds[${i % 2}], importDate = ${date}L, createdAt = ${date}L, totalAmount = 0.0, status = \"Completed\", note = \"Nhập kho đợt $i\"))")
        
        val detailsCount = if (i % 5 == 0) 6 else 4
        builder.appendLine("                    val irDetails$i = mutableListOf<ImportDetail>()")
        for (j in 1..detailsCount) {
            val pIdx = random.nextInt(0, 50)
            val qty = random.nextInt(10, 50)
            builder.appendLine("                    irDetails$i.add(ImportDetail(importId = ir$i, productId = productIds[$pIdx], quantity = $qty, unitPrice = 100000.0, subtotal = ${(qty * 100000.0).toLong()}.0))")
        }
        builder.appendLine("                    db.importReceiptDao().insertImportDetails(irDetails$i)")
    }
    
    builder.appendLine("                    // Seed Export Receipts")
    for (i in 1..30) {
        val date = minDate + (maxDate - minDate) / 30 * i + 86400000L
        builder.appendLine("                    val er$i = db.exportReceiptDao().insertExportReceipt(ExportReceipt(exportCode = \"PXK-2026-${String.format("%03d", i)}\", userId = userIds[${(i % 4) + 2}], exportDate = ${date}L, createdAt = ${date}L, totalAmount = 0.0, status = \"Completed\", note = \"Xuất kho đợt $i\", recipient = \"Khách hàng $i\"))")
        
        val detailsCount = 4
        builder.appendLine("                    val erDetails$i = mutableListOf<ExportDetail>()")
        for (j in 1..detailsCount) {
            val pIdx = random.nextInt(0, 50)
            val qty = random.nextInt(2, 10)
            builder.appendLine("                    erDetails$i.add(ExportDetail(exportId = er$i, productId = productIds[$pIdx], quantity = $qty, unitPrice = 120000.0, subtotal = ${(qty * 120000.0).toLong()}.0))")
        }
        builder.appendLine("                    db.exportReceiptDao().insertExportDetails(erDetails$i)")
    }
    
    builder.appendLine("                    // Seed Stock Checks")
    for (i in 1..6) {
        val date = minDate + (maxDate - minDate) / 6 * i + 43200000L
        val statusList = listOf("Đang tiến hành", "Đang chờ xét duyệt", "Đã duyệt", "Đã hoàn thành", "Đã hủy", "Đã hoàn thành")
        val st = statusList[i - 1]
        builder.appendLine("                    val sc$i = db.inventoryCheckDao().insertInventoryCheck(StockCheck(checkCode = \"KK-2026-${String.format("%03d", i)}\", title = \"Kiểm kê định kỳ tháng ${i + 1}\", note = \"\", checkDate = ${date}L, userId = userIds[${i % 6}], createdAt = ${date}L, status = \"$st\"))")
        
        val detailsCount = 15
        builder.appendLine("                    val scDetails$i = mutableListOf<StockCheckDetail>()")
        for (j in 1..detailsCount) {
            val pIdx = random.nextInt(0, 50)
            val sysQty = random.nextInt(10, 50)
            val diff = if (j % 3 == 0) random.nextInt(-5, 5) else 0
            val actualQty = sysQty + diff
            builder.appendLine("                    scDetails$i.add(StockCheckDetail(checkId = sc$i, productId = productIds[$pIdx], systemQuantity = $sysQty, actualQuantity = $actualQty, reason = if ($diff != 0) \"Lệch\" else \"\"))")
        }
        builder.appendLine("                    db.inventoryCheckDao().insertInventoryDetails(scDetails$i)")
    }
    
    builder.appendLine("                }")
    builder.appendLine("            } catch (e: Exception) {")
    builder.appendLine("                android.util.Log.e(\"AppDatabase\", \"Error populating initial database data\", e)")
    builder.appendLine("            }")
    builder.appendLine("        }")
    builder.appendLine("    }")
    builder.appendLine("}")
    
    File("SeederOutput.kt").writeText(builder.toString())
}

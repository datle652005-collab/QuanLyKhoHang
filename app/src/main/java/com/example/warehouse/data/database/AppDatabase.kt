package com.example.warehouse.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.warehouse.data.database.dao.*
import com.example.warehouse.data.database.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Database(
    entities = [
        Role::class,
        User::class,
        Category::class,
        Supplier::class,
        Product::class,
        Inventory::class,
        ImportReceipt::class,
        ImportDetail::class,
        ExportReceipt::class,
        ExportDetail::class,
        StockCheck::class,
        StockCheckDetail::class,
        UserAuditLog::class
    ],
    version = 14,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun roleDao(): RoleDao
    abstract fun userDao(): UserDao
    abstract fun userAuditLogDao(): UserAuditLogDao
    abstract fun categoryDao(): CategoryDao
    abstract fun supplierDao(): SupplierDao
    abstract fun productDao(): ProductDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun importReceiptDao(): ImportReceiptDao
    abstract fun exportReceiptDao(): ExportReceiptDao
    abstract fun inventoryCheckDao(): InventoryCheckDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        private val seedMutex = Mutex()

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val appContext = context.applicationContext
                val instance = Room.databaseBuilder(
                    appContext,
                    AppDatabase::class.java,
                    "warehouse_database"
                )
                    .addCallback(DatabaseCallback(appContext))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance

                CoroutineScope(Dispatchers.IO).launch {
                    ensureDataSeeded(instance)
                }

                instance
            }
        }

        private class DatabaseCallback(private val context: Context) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        ensureDataSeeded(database)
                    }
                }
            }
        }

        private suspend fun ensureDataSeeded(db: AppDatabase) {
            seedMutex.withLock {
                try {
                    if (db.userDao().getUserCount() == 0) {
                        populateInitialData(db)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AppDatabase", "Error checking or seeding database", e)
                }
            }
        }

        private suspend fun populateInitialData(db: AppDatabase) {
            try {
                db.withTransaction {
                    // Seed Roles
                    val adminRoleId = db.roleDao().insertRole(Role(roleName = "ADMIN", description = "Quản lý kho"))
                    val staffRoleId = db.roleDao().insertRole(Role(roleName = "STAFF", description = "Nhân viên kho"))

                    // Seed Users
                    val users = listOf(
                        User(username = "admin1", password = "123", fullName = "Nguyễn Văn Quản", phone = "0901000001", email = "admin1@kho.vn", roleId = adminRoleId, status = "ACTIVE"),
                        User(username = "admin2", password = "123", fullName = "Trần Hữu Lý", phone = "0901000002", email = "admin2@kho.vn", roleId = adminRoleId, status = "ACTIVE"),
                        User(username = "nv01", password = "123", fullName = "Lê Thị Lan", phone = "0901000003", email = "nv01@kho.vn", roleId = staffRoleId, status = "ACTIVE"),
                        User(username = "nv02", password = "123", fullName = "Phạm Văn Nam", phone = "0901000004", email = "nv02@kho.vn", roleId = staffRoleId, status = "ACTIVE"),
                        User(username = "nv03", password = "123", fullName = "Hoàng Thị Mai", phone = "0901000005", email = "nv03@kho.vn", roleId = staffRoleId, status = "ACTIVE"),
                        User(username = "nv04", password = "123", fullName = "Đặng Văn Hùng", phone = "0901000006", email = "nv04@kho.vn", roleId = staffRoleId, status = "ACTIVE")
                    )
                    val userIds = users.map { db.userDao().insertUser(it) }
                    val adminUserIds = userIds.take(2)
                    val staffUserIds = userIds.drop(2)

                    // Seed Categories
                    val categories = listOf(
                        "Điện thoại & Phụ kiện" to "Các dòng điện thoại thông minh và phụ kiện đi kèm",
                        "Laptop & Máy tính" to "Máy tính xách tay, PC và linh kiện",
                        "Điện gia dụng" to "Thiết bị điện sử dụng trong gia đình",
                        "Thiết bị văn phòng" to "Máy in, máy scan, máy chiếu",
                        "Máy ảnh & Quay phim" to "Máy ảnh kỹ thuật số, máy quay và phụ kiện",
                        "Thiết bị mạng" to "Router, switch, cáp mạng",
                        "Thiết bị âm thanh" to "Loa, tai nghe, amply",
                        "Phụ kiện điện tử" to "Cáp sạc, sạc dự phòng, ổ cứng di động"
                    )
                    val categoryIds = categories.map { 
                        db.categoryDao().insertCategory(Category(categoryName = it.first, description = it.second)) 
                    }

                    // Seed Suppliers
                    val suppliers = listOf(
                        Supplier(supplierCode = "NCC01", supplierName = "Công ty TNHH Công Nghệ Việt", phone = "02838112233", email = "contact@viettech.vn", address = "123 Lê Lợi, Q.1, TP.HCM", contactPerson = "Nguyễn Văn A", taxCode = "0301234567"),
                        Supplier(supplierCode = "NCC02", supplierName = "Tập đoàn Điện Máy Toàn Cầu", phone = "02439887766", email = "sales@toancau.vn", address = "456 Hoàng Hoa Thám, Hà Nội", contactPerson = "Trần B", taxCode = "0109876543"),
                        Supplier(supplierCode = "NCC03", supplierName = "Công ty CP Phân phối FPT", phone = "02873006666", email = "phanphoi@fpt.com.vn", address = "Tòa nhà FPT, Q.7, TP.HCM", contactPerson = "Lê C", taxCode = "0304567890"),
                        Supplier(supplierCode = "NCC04", supplierName = "Digiworld Corporation", phone = "02839290059", email = "info@digiworld.com.vn", address = "201-203 CMT8, Q.3, TP.HCM", contactPerson = "Phạm D", taxCode = "0301112223"),
                        Supplier(supplierCode = "NCC05", supplierName = "Công ty Thiết Bị Số Tương Lai", phone = "02363889900", email = "contact@tuonglai.vn", address = "789 Nguyễn Văn Linh, Đà Nẵng", contactPerson = "Hoàng E", taxCode = "0403334445"),
                        Supplier(supplierCode = "NCC06", supplierName = "Viettel Commerce", phone = "18008123", email = "commerce@viettel.vn", address = "Số 1 Trần Hữu Dực, Nam Từ Liêm, HN", contactPerson = "Đặng F", taxCode = "0100109106"),
                        Supplier(supplierCode = "NCC07", supplierName = "Phong Vũ IT", phone = "18006867", email = "cskh@phongvu.vn", address = "264 Nguyễn Thị Minh Khai, Q.3, TP.HCM", contactPerson = "Ngô G", taxCode = "0304998877"),
                        Supplier(supplierCode = "NCC08", supplierName = "HanoiComputer", phone = "19001903", email = "hnc@hanoicomputer.com", address = "129 Lê Thanh Nghị, Hai Bà Trưng, HN", contactPerson = "Bùi H", taxCode = "0101567890"),
                        Supplier(supplierCode = "NCC09", supplierName = "GearVN", phone = "18006975", email = "cskh@gearvn.com", address = "78-80-82 Hoàng Hoa Thám, Tân Bình, TP.HCM", contactPerson = "Vũ K", taxCode = "0313556677"),
                        Supplier(supplierCode = "NCC10", supplierName = "Samsung Vina", phone = "02838211111", email = "contact@samsung.com", address = "Bitexco Financial Tower, Q.1, TP.HCM", contactPerson = "Đỗ L", taxCode = "0300123456"),
                        Supplier(supplierCode = "NCC11", supplierName = "Sony Electronics VN", phone = "1800588885", email = "sev.contact@sony.com", address = "Vincom Center, Q.1, TP.HCM", contactPerson = "Lý M", taxCode = "0303987654"),
                        Supplier(supplierCode = "NCC12", supplierName = "LG Electronics VN", phone = "18001509", email = "lge@lg.com", address = "Keangnam Landmark 72, Nam Từ Liêm, HN", contactPerson = "Châu N", taxCode = "0100123987")
                    )
                    val supplierIds = suppliers.map { db.supplierDao().insertSupplier(it) }

                    // Seed Products
                    val productData = listOf(
                        // Điện thoại
                        Triple("iPhone 15 Pro Max 256GB", 25000000.0, 30000000.0), Triple("iPhone 15 Plus 128GB", 20000000.0, 24000000.0), Triple("Samsung Galaxy S24 Ultra", 24000000.0, 29000000.0), Triple("Samsung Galaxy Z Fold5", 30000000.0, 36000000.0), Triple("Xiaomi 14 Pro", 18000000.0, 21000000.0), Triple("OPPO Find X7 Ultra", 19000000.0, 23000000.0), Triple("Ốp lưng iPhone 15 Pro Max", 150000.0, 350000.0), Triple("Kính cường lực Samsung S24", 80000.0, 200000.0),
                        // Laptop
                        Triple("MacBook Air M3 8GB/256GB", 22000000.0, 27000000.0), Triple("MacBook Pro 14 M3 Max", 60000000.0, 75000000.0), Triple("Dell XPS 15 9530", 35000000.0, 42000000.0), Triple("Lenovo ThinkPad X1 Carbon Gen 11", 33000000.0, 40000000.0), Triple("ASUS ROG Strix G16", 28000000.0, 34000000.0), Triple("HP Spectre x360", 30000000.0, 36000000.0), Triple("Màn hình Dell UltraSharp 27", 9000000.0, 12000000.0),
                        // Gia dụng
                        Triple("Tủ lạnh Samsung Inverter 300L", 6000000.0, 8500000.0), Triple("Máy giặt LG AI DD 10kg", 8000000.0, 11000000.0), Triple("Điều hòa Panasonic 1.5HP", 9000000.0, 12500000.0), Triple("Nồi chiên không dầu Philips", 2000000.0, 3200000.0), Triple("Lò vi sóng Electrolux", 1500000.0, 2300000.0), Triple("Máy lọc nước Karofi", 4000000.0, 6000000.0), Triple("Máy hút bụi Dyson V12", 12000000.0, 16000000.0),
                        // Văn phòng
                        Triple("Máy in Canon LBP 2900", 2500000.0, 3500000.0), Triple("Máy in Brother HL-L2321D", 2000000.0, 2800000.0), Triple("Máy chiếu Epson EB-E01", 8000000.0, 10500000.0), Triple("Máy chấm công Ronald Jack", 1800000.0, 2600000.0), Triple("Giấy in Double A A4 80gsm", 50000.0, 75000.0),
                        // Máy ảnh
                        Triple("Máy ảnh Sony Alpha A7 IV", 45000000.0, 55000000.0), Triple("Máy ảnh Canon EOS R6", 48000000.0, 58000000.0), Triple("Ống kính Sony FE 24-70mm f/2.8", 35000000.0, 42000000.0), Triple("GoPro Hero 12 Black", 8000000.0, 10500000.0), Triple("DJI Osmo Pocket 3", 10000000.0, 13000000.0),
                        // Mạng
                        Triple("Router WiFi 6 TP-Link AX53", 1000000.0, 1500000.0), Triple("Mesh WiFi TP-Link Deco X20", 3000000.0, 4200000.0), Triple("Switch Cisco 24 Port Gigabit", 5000000.0, 7500000.0), Triple("Cáp mạng Cat6 Dintek (Thùng 305m)", 1500000.0, 2200000.0), Triple("Bộ phát 4G/5G Huawei", 1200000.0, 1800000.0),
                        // Linh kiện
                        Triple("CPU Intel Core i9-14900K", 12000000.0, 15500000.0), Triple("CPU AMD Ryzen 9 7950X", 11000000.0, 14500000.0), Triple("VGA NVIDIA RTX 4090", 45000000.0, 55000000.0), Triple("RAM Corsair Vengeance 32GB DDR5", 2500000.0, 3500000.0), Triple("SSD Samsung 990 Pro 2TB", 3500000.0, 4800000.0), Triple("Mainboard ASUS ROG Maximus", 10000000.0, 13000000.0), Triple("Nguồn Corsair RM1000x", 3000000.0, 4200000.0),
                        // Âm thanh
                        Triple("Tai nghe AirPods Pro Gen 2", 4500000.0, 6000000.0), Triple("Tai nghe Sony WH-1000XM5", 5500000.0, 7500000.0), Triple("Loa Bluetooth JBL Charge 5", 2500000.0, 3500000.0), Triple("Loa Marshall Stanmore III", 7000000.0, 9500000.0), Triple("Soundbar Samsung Q990C", 15000000.0, 20000000.0), Triple("Micro thu âm Rode NT1", 5000000.0, 7000000.0)
                    )

                    val productCategoryMap = mutableListOf<Long>()
                    productCategoryMap.addAll(List(8) { categoryIds[0] })
                    productCategoryMap.addAll(List(7) { categoryIds[1] })
                    productCategoryMap.addAll(List(7) { categoryIds[2] })
                    productCategoryMap.addAll(List(5) { categoryIds[3] })
                    productCategoryMap.addAll(List(5) { categoryIds[4] })
                    productCategoryMap.addAll(List(5) { categoryIds[5] })
                    productCategoryMap.addAll(List(7) { categoryIds[6] })
                    productCategoryMap.addAll(List(6) { categoryIds[7] })

                    val random = java.util.Random(2026)
                    val productIds = mutableListOf<Long>()
                    val minStocks = IntArray(50)
                    for (i in productData.indices) {
                        val (name, inPrice, outPrice) = productData[i]
                        val minStock = random.nextInt(11) + 5
                        minStocks[i] = minStock
                        val pId = db.productDao().insertProduct(
                            Product(
                                categoryId = productCategoryMap[i],
                                productCode = "SP${(i+1).toString().padStart(3, '0')}",
                                productName = name,
                                importPrice = inPrice,
                                exportPrice = outPrice,
                                minStock = minStock,
                                imagePath = "",
                                description = "Sản phẩm chính hãng"
                            )
                        )
                        productIds.add(pId)
                    }

                    // Transactions logic
                    val inventoryCounts = IntArray(50) { 0 }
                    val startTime = 1767225600000L // Jan 1, 2026
                    val endTime = 1785369600000L // Jul 30, 2026

                    // 40 Import Receipts
                    for (i in 1..40) {
                        val importTime = startTime + (random.nextDouble() * (endTime - startTime)).toLong()
                        val supplierId = supplierIds[random.nextInt(supplierIds.size)]
                        val userId = adminUserIds[random.nextInt(adminUserIds.size)]
                        val numItems = random.nextInt(5) + 3 
                        
                        val selectedIndices = mutableListOf<Int>()
                        while(selectedIndices.size < numItems) {
                            val idx = random.nextInt(productIds.size)
                            if(!selectedIndices.contains(idx)) selectedIndices.add(idx)
                        }
                        
                        var totalAmount = 0.0
                        val detailsData = mutableListOf<Triple<Long, Int, Double>>()
                        for (idx in selectedIndices) {
                            val qty = random.nextInt(40) + 15
                            inventoryCounts[idx] += qty
                            val inPrice = productData[idx].second
                            totalAmount += qty * inPrice
                            detailsData.add(Triple(productIds[idx], qty, inPrice))
                        }
                        
                        val importId = db.importReceiptDao().insertImportReceipt(
                            ImportReceipt(
                                importCode = "PNK-2026-${i.toString().padStart(3, '0')}",
                                supplierId = supplierId,
                                userId = userId,
                                importDate = importTime,
                                createdAt = importTime,
                                totalAmount = totalAmount,
                                status = "Hoàn thành",
                                note = "Nhập hàng định kỳ"
                            )
                        )
                        
                        val details = detailsData.map {
                            ImportDetail(importId = importId, productId = it.first, quantity = it.second, unitPrice = it.third, subtotal = it.second * it.third)
                        }
                        db.importReceiptDao().insertImportDetails(details)
                    }

                    // 30 Export Receipts
                    for (i in 1..30) {
                        val exportTime = startTime + (random.nextDouble() * (endTime - startTime)).toLong()
                        val userId = staffUserIds[random.nextInt(staffUserIds.size)]
                        
                        val numItems = random.nextInt(4) + 2
                        val selectedIndices = mutableListOf<Int>()
                        val availableIndices = inventoryCounts.indices.filter { inventoryCounts[it] > 0 }
                        if (availableIndices.isEmpty()) continue
                        
                        val actualNumItems = Math.min(numItems, availableIndices.size)
                        while(selectedIndices.size < actualNumItems) {
                            val idx = availableIndices[random.nextInt(availableIndices.size)]
                            if(!selectedIndices.contains(idx)) selectedIndices.add(idx)
                        }
                        
                        var totalAmount = 0.0
                        val detailsData = mutableListOf<Triple<Long, Int, Double>>()
                        
                        for (idx in selectedIndices) {
                            val maxQty = inventoryCounts[idx]
                            val qty = if (maxQty <= 1) 1 else random.nextInt(maxQty / 2 + 1) + 1
                            inventoryCounts[idx] -= qty
                            val outPrice = productData[idx].third
                            totalAmount += qty * outPrice
                            detailsData.add(Triple(productIds[idx], qty, outPrice))
                        }
                        
                        val exportId = db.exportReceiptDao().insertExportReceipt(
                            ExportReceipt(
                                exportCode = "PXK-2026-${i.toString().padStart(3, '0')}",
                                userId = userId,
                                exportDate = exportTime,
                                createdAt = exportTime,
                                recipient = "Khách hàng ${random.nextInt(100) + 1}",
                                totalAmount = totalAmount,
                                status = "Hoàn thành",
                                note = "Xuất bán hàng"
                            )
                        )
                        
                        val details = detailsData.map {
                            ExportDetail(exportId = exportId, productId = it.first, quantity = it.second, unitPrice = it.third, subtotal = it.second * it.third)
                        }
                        db.exportReceiptDao().insertExportDetails(details)
                    }

                    // 6 Stock Checks
                    val statuses = listOf("Đang tiến hành", "Đang chờ xét duyệt", "Đã hoàn thành", "Đã hoàn thành", "Đã hoàn thành", "Đã hủy")
                    for (i in 0 until 6) {
                        val checkTime = startTime + (random.nextDouble() * (endTime - startTime)).toLong()
                        val userId = adminUserIds[random.nextInt(adminUserIds.size)]
                        val status = statuses[i]
                        
                        val numItems = random.nextInt(10) + 10
                        val selectedIndices = mutableListOf<Int>()
                        while(selectedIndices.size < numItems) {
                            val idx = random.nextInt(productIds.size)
                            if(!selectedIndices.contains(idx)) selectedIndices.add(idx)
                        }
                        
                        val checkId = db.inventoryCheckDao().insertInventoryCheck(
                            StockCheck(
                                checkCode = "KK-2026-${(i+1).toString().padStart(3, '0')}",
                                userId = userId,
                                checkDate = checkTime,
                                createdAt = checkTime,
                                status = status,
                                note = "Kiểm kê định kỳ"
                            )
                        )
                        
                        val details = mutableListOf<StockCheckDetail>()
                        for (idx in selectedIndices) {
                            val expected = inventoryCounts[idx]
                            var actual = expected
                            if (random.nextDouble() < 0.3) {
                                val diff = random.nextInt(5) - 2
                                actual = Math.max(0, expected + diff)
                            }
                            
                            details.add(
                                StockCheckDetail(
                                    checkId = checkId,
                                    productId = productIds[idx],
                                    systemQuantity = expected,
                                    actualQuantity = actual,
                                    difference = actual - expected,
                                    reason = if (actual != expected) "Sai lệch kho" else ""
                                )
                            )
                            if (status == "Đã hoàn thành") {
                                inventoryCounts[idx] = actual
                            }
                        }
                        db.inventoryCheckDao().insertInventoryDetails(details)
                    }

                    // Force specific states via adjustment transactions
                    val forceExportTime = 1785369600000L
                    val forceDetailsData = mutableListOf<Triple<Long, Int, Double>>()
                    val forceImportDetails = mutableListOf<Triple<Long, Int, Double>>()
                    var forceTotal = 0.0
                    var forceImportTotal = 0.0

                    fun forceStock(pIndex: Int, target: Int) {
                        val current = inventoryCounts[pIndex]
                        if (current > target) {
                            val qty = current - target
                            inventoryCounts[pIndex] = target
                            val outPrice = productData[pIndex].third
                            forceTotal += qty * outPrice
                            forceDetailsData.add(Triple(productIds[pIndex], qty, outPrice))
                        } else if (current < target) {
                            val qty = target - current
                            inventoryCounts[pIndex] = target
                            val inPrice = productData[pIndex].second
                            forceImportTotal += qty * inPrice
                            forceImportDetails.add(Triple(productIds[pIndex], qty, inPrice))
                        }
                    }

                    forceStock(3, 0)
                    forceStock(12, 0)
                    forceStock(5, Math.max(0, minStocks[5] - 2))
                    forceStock(20, Math.max(0, minStocks[20] - 1))
                    forceStock(7, minStocks[7])
                    forceStock(22, minStocks[22])

                    if (forceImportDetails.isNotEmpty()) {
                        val importId = db.importReceiptDao().insertImportReceipt(
                            ImportReceipt(importCode = "PNK-2026-999", supplierId = supplierIds[0], userId = adminUserIds[0], importDate = forceExportTime, createdAt = forceExportTime, totalAmount = forceImportTotal, status = "Hoàn thành", note = "Nhập cân đối tồn kho")
                        )
                        db.importReceiptDao().insertImportDetails(forceImportDetails.map {
                            ImportDetail(importId = importId, productId = it.first, quantity = it.second, unitPrice = it.third, subtotal = it.second * it.third)
                        })
                    }

                    if (forceDetailsData.isNotEmpty()) {
                        val exportId = db.exportReceiptDao().insertExportReceipt(
                            ExportReceipt(exportCode = "PXK-2026-999", userId = adminUserIds[0], exportDate = forceExportTime, createdAt = forceExportTime, recipient = "Điều chỉnh tồn kho", totalAmount = forceTotal, status = "Hoàn thành", note = "Xuất cân đối tồn kho")
                        )
                        db.exportReceiptDao().insertExportDetails(forceDetailsData.map {
                            ExportDetail(exportId = exportId, productId = it.first, quantity = it.second, unitPrice = it.third, subtotal = it.second * it.third)
                        })
                    }

                    // Save final inventory
                    for (i in productIds.indices) {
                        db.inventoryDao().insertOrUpdateInventory(Inventory(productId = productIds[i], quantity = inventoryCounts[i]))
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("AppDatabase", "Error populating initial database data", e)
            }
        }
    }
}

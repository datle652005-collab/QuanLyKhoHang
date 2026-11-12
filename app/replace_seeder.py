import random

def generate_seeder():
    lines = []
    lines.append('        private suspend fun populateInitialData(db: AppDatabase) {')
    lines.append('            try {')
    lines.append('                db.withTransaction {')
    lines.append('                    // Seed Roles')
    lines.append('                    val adminRoleId = db.roleDao().insertRole(Role(roleName = "ADMIN", description = "Quản trị viên hệ thống"))')
    lines.append('                    val staffRoleId = db.roleDao().insertRole(Role(roleName = "STAFF", description = "Nhân viên kho"))')
    lines.append('')
    lines.append('                    // Seed Users')
    lines.append('                    val userIds = mutableListOf<Long>()')
    
    users = [
        ("admin", "Nguyễn Văn Quản Lý"),
        ("admin2", "Trần Thị Quản Lý"),
        ("nv01", "Lê Văn Kho"),
        ("nv02", "Phạm Thị Kho"),
        ("nv03", "Hoàng Văn Hàng"),
        ("nv04", "Đinh Thị Hàng")
    ]
    
    for i, (username, full_name) in enumerate(users):
        role = "adminRoleId" if i < 2 else "staffRoleId"
        lines.append(f'                    userIds.add(db.userDao().insertUser(User(username = "{username}", password = "123456", fullName = "{full_name}", roleId = {role}, status = "ACTIVE")))')

    lines.append('')
    lines.append('                    // Seed Categories')
    lines.append('                    val catIds = mutableListOf<Long>()')
    cats = ["Điện tử & Thiết bị", "Gia dụng & Đời sống", "Thiết bị văn phòng", "Thực phẩm", "Mỹ phẩm", "Thời trang", "Nội thất", "Đồ chơi"]
    for cat in cats:
        lines.append(f'                    catIds.add(db.categoryDao().insertCategory(Category(categoryName = "{cat}", description = "Mô tả {cat}")))')

    lines.append('')
    lines.append('                    // Seed Suppliers')
    lines.append('                    val supIds = mutableListOf<Long>()')
    for i in range(1, 13):
        lines.append(f'                    supIds.add(db.supplierDao().insertSupplier(Supplier(supplierCode = "SUP{i:03d}", supplierName = "Nhà cung cấp {i}", phone = "09000000{i:02d}", address = "Địa chỉ {i}")))')

    lines.append('')
    lines.append('                    // Seed Products & Inventory')
    lines.append('                    val productIds = mutableListOf<Long>()')
    
    random.seed(42)
    for i in range(1, 51):
        cat_idx = (i - 1) % 8
        min_stock = random.randint(5, 20)
        
        # Determine initial stock to have different states
        if i % 10 == 0:
            initial_stock = 0 # Out of stock
        elif i % 7 == 0:
            initial_stock = max(0, min_stock - 2) # Below min stock
        elif i % 5 == 0:
            initial_stock = min_stock # Exactly min stock
        else:
            initial_stock = min_stock + random.randint(10, 50) # Normal/High stock
            
        imp_price = random.randint(1, 100) * 10000
        exp_price = random.randint(110, 150) * 10000
        
        lines.append(f'                    val p{i} = db.productDao().insertProduct(Product(categoryId = catIds[{cat_idx}], productCode = "SP{i:03d}", productName = "Sản phẩm {i}", importPrice = {imp_price}.0, exportPrice = {exp_price}.0, minStock = {min_stock}))')
        lines.append(f'                    productIds.add(p{i})')
        lines.append(f'                    db.inventoryDao().insertOrUpdateInventory(Inventory(productId = p{i}, quantity = {initial_stock}))')

    lines.append('')
    lines.append('                    // Seed Import Receipts')
    min_date = 1767225600000 # 01/01/2026
    max_date = 1786752000000 # 15/08/2026
    
    for i in range(1, 41):
        date = min_date + int((max_date - min_date) / 40 * i)
        lines.append(f'                    val ir{i} = db.importReceiptDao().insertImportReceipt(ImportReceipt(importCode = "PNK-2026-{i:03d}", supplierId = supIds[{i % 12}], userId = userIds[{i % 2}], importDate = {date}L, createdAt = {date}L, totalAmount = 0.0, status = "Completed", note = "Nhập kho đợt {i}"))')
        
        details_count = 6 if i % 5 == 0 else 4
        lines.append(f'                    val irDetails{i} = mutableListOf<ImportDetail>()')
        
        for j in range(1, details_count + 1):
            p_idx = random.randint(0, 49)
            qty = random.randint(10, 50)
            lines.append(f'                    irDetails{i}.add(ImportDetail(importId = ir{i}, productId = productIds[{p_idx}], quantity = {qty}, unitPrice = 100000.0, subtotal = {qty * 100000}.0))')
            
        lines.append(f'                    db.importReceiptDao().insertImportDetails(irDetails{i})')

    lines.append('')
    lines.append('                    // Seed Export Receipts')
    for i in range(1, 31):
        date = min_date + int((max_date - min_date) / 30 * i) + 86400000
        lines.append(f'                    val er{i} = db.exportReceiptDao().insertExportReceipt(ExportReceipt(exportCode = "PXK-2026-{i:03d}", userId = userIds[{(i % 4) + 2}], exportDate = {date}L, createdAt = {date}L, totalAmount = 0.0, status = "Completed", note = "Xuất kho đợt {i}", recipient = "Khách hàng {i}"))')
        
        details_count = 4
        lines.append(f'                    val erDetails{i} = mutableListOf<ExportDetail>()')
        for j in range(1, details_count + 1):
            p_idx = random.randint(0, 49)
            qty = random.randint(2, 10)
            lines.append(f'                    erDetails{i}.add(ExportDetail(exportId = er{i}, productId = productIds[{p_idx}], quantity = {qty}, unitPrice = 120000.0, subtotal = {qty * 120000}.0))')
            
        lines.append(f'                    db.exportReceiptDao().insertExportDetails(erDetails{i})')

    lines.append('')
    lines.append('                    // Seed Stock Checks')
    for i in range(1, 7):
        date = min_date + int((max_date - min_date) / 6 * i) + 43200000
        status_list = ["Đang tiến hành", "Đang chờ xét", "Đã duyệt", "Đã hoàn thành", "Đã hủy", "Đã hoàn thành"]
        st = status_list[i - 1]
        lines.append(f'                    val sc{i} = db.inventoryCheckDao().insertInventoryCheck(StockCheck(checkCode = "KK-2026-{i:03d}", title = "Kiểm kê định kỳ tháng {i + 1}", note = "", checkDate = {date}L, userId = userIds[{i % 6}], createdAt = {date}L, status = "{st}"))')
        
        details_count = 17
        lines.append(f'                    val scDetails{i} = mutableListOf<StockCheckDetail>()')
        for j in range(1, details_count + 1):
            p_idx = random.randint(0, 49)
            sys_qty = random.randint(10, 50)
            diff = random.randint(-5, 5) if j % 3 == 0 else 0
            actual_qty = sys_qty + diff
            reason = "Lệch" if diff != 0 else ""
            lines.append(f'                    scDetails{i}.add(StockCheckDetail(checkId = sc{i}, productId = productIds[{p_idx}], systemQuantity = {sys_qty}, actualQuantity = {actual_qty}, reason = "{reason}"))')
            
        lines.append(f'                    db.inventoryCheckDao().insertInventoryDetails(scDetails{i})')

    lines.append('                }')
    lines.append('            } catch (e: Exception) {')
    lines.append('                android.util.Log.e("AppDatabase", "Error populating initial database data", e)')
    lines.append('            }')
    lines.append('        }')
    lines.append('    }')
    lines.append('}')
    
    return "\n".join(lines)

with open('app/src/main/java/com/example/warehouse/data/database/AppDatabase.kt', 'r') as f:
    content = f.read()

# Find the start of populateInitialData
start_idx = content.find('        private suspend fun populateInitialData(db: AppDatabase)')
if start_idx != -1:
    new_content = content[:start_idx] + generate_seeder()
    with open('app/src/main/java/com/example/warehouse/data/database/AppDatabase.kt', 'w') as f:
        f.write(new_content)
    print("Successfully replaced populateInitialData")
else:
    print("Could not find populateInitialData")


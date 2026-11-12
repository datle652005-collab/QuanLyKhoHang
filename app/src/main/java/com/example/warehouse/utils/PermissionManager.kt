package com.example.warehouse.utils

object PermissionManager {
    fun isAdmin(role: String?): Boolean = role?.uppercase() in listOf("ADMIN", "MANAGER", "QUẢN LÝ KHO")
    fun isStaff(role: String?): Boolean = role?.uppercase() == "STAFF"

    // 1. Xem danh sách/chi tiết sản phẩm
    fun canViewProducts(role: String?): Boolean = true

    // 2. Thêm/sửa sản phẩm
    fun canCreateOrEditProduct(role: String?): Boolean = true

    // 3. Xóa sản phẩm
    fun canDeleteProduct(role: String?): Boolean = isAdmin(role)

    // 4. Xem danh mục
    fun canViewCategories(role: String?): Boolean = true

    // 5. Thêm/sửa/xóa danh mục
    fun canManageCategory(role: String?): Boolean = isAdmin(role)

    // 6. Xem/tạo nhanh nhà cung cấp
    fun canViewOrQuickCreateSupplier(role: String?): Boolean = true

    // 7. Sửa/xóa nhà cung cấp
    fun canEditOrDeleteSupplier(role: String?): Boolean = isAdmin(role)

    // 8. Tạo phiếu nhập kho
    fun canCreateImportReceipt(role: String?): Boolean = true

    // 9. Sửa/xóa dòng phiếu nhập đã lưu
    fun canEditOrDeleteImportDetail(role: String?): Boolean = isAdmin(role)

    // 10. Hủy phiếu nhập
    fun canCancelImportReceipt(role: String?): Boolean = isAdmin(role)

    // 11. Tạo phiếu xuất kho
    fun canCreateExportReceipt(role: String?): Boolean = true

    // 12. Sửa số lượng phiếu xuất đã lưu
    fun canEditExportDetailQuantity(role: String?): Boolean = isAdmin(role)

    // 13. Xóa phiếu xuất
    fun canDeleteExportReceipt(role: String?): Boolean = isAdmin(role)

    // 14. Tạo/sửa/xóa kiểm kê
    fun canManageStockCheckSession(role: String?): Boolean = isAdmin(role)

    // 15. Nhập actual_quantity khi kiểm kê
    fun canEnterStockCheckActualQuantity(role: String?): Boolean = true

    // 16. Gửi xét duyệt / Duyệt kiểm kê
    fun canApproveStockCheck(role: String?): Boolean = isAdmin(role)

    // 17. Xem thống kê cơ bản (trang chủ)
    fun canViewBasicStats(role: String?): Boolean = true

    // 18. Xem module Thống kê đầy đủ
    fun canViewFullReportModule(role: String?): Boolean = isAdmin(role)

    // 19. Xem danh sách tài khoản người dùng
    fun canViewUserManagement(role: String?): Boolean = true

    // 20. Tạo/sửa/khóa/xóa tài khoản người dùng
    fun canManageUsers(role: String?): Boolean = isAdmin(role)

    // 21. Cập nhật ngưỡng tồn kho tối thiểu
    fun canUpdateMinStock(role: String?): Boolean = isAdmin(role)
}

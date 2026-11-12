package com.example.warehouse.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionManagerTest {

    @Test
    fun `test canDeleteProduct - only admin can delete`() {
        assertTrue(PermissionManager.canDeleteProduct("ADMIN"))
        assertTrue(PermissionManager.canDeleteProduct("admin"))
        assertTrue(PermissionManager.canDeleteProduct("Quản lý kho"))
        
        assertFalse(PermissionManager.canDeleteProduct("STAFF"))
        assertFalse(PermissionManager.canDeleteProduct("Nhân viên kho"))
        assertFalse(PermissionManager.canDeleteProduct(null))
        assertFalse(PermissionManager.canDeleteProduct(""))
    }

    @Test
    fun `test canManageCategory - only admin can manage`() {
        assertTrue(PermissionManager.canManageCategory("ADMIN"))
        assertFalse(PermissionManager.canManageCategory("STAFF"))
    }

    @Test
    fun `test canEditOrDeleteSupplier - only admin can manage`() {
        assertTrue(PermissionManager.canEditOrDeleteSupplier("ADMIN"))
        assertFalse(PermissionManager.canEditOrDeleteSupplier("STAFF"))
    }

    @Test
    fun `test canCancelImportReceipt - only admin can cancel`() {
        assertTrue(PermissionManager.canCancelImportReceipt("ADMIN"))
        assertFalse(PermissionManager.canCancelImportReceipt("STAFF"))
    }

    @Test
    fun `test canEditOrDeleteImportDetail - only admin can edit`() {
        assertTrue(PermissionManager.canEditOrDeleteImportDetail("ADMIN"))
        assertFalse(PermissionManager.canEditOrDeleteImportDetail("STAFF"))
    }

    @Test
    fun `test canEditExportDetailQuantity - only admin can edit`() {
        assertTrue(PermissionManager.canEditExportDetailQuantity("ADMIN"))
        assertFalse(PermissionManager.canEditExportDetailQuantity("STAFF"))
    }

    @Test
    fun `test canDeleteExportReceipt - only admin can delete`() {
        assertTrue(PermissionManager.canDeleteExportReceipt("ADMIN"))
        assertFalse(PermissionManager.canDeleteExportReceipt("STAFF"))
    }

    @Test
    fun `test canManageStockCheckSession - only admin can manage`() {
        assertTrue(PermissionManager.canManageStockCheckSession("ADMIN"))
        assertFalse(PermissionManager.canManageStockCheckSession("STAFF"))
    }

    @Test
    fun `test canApproveStockCheck - only admin can approve`() {
        assertTrue(PermissionManager.canApproveStockCheck("ADMIN"))
        assertFalse(PermissionManager.canApproveStockCheck("STAFF"))
    }

    @Test
    fun `test canUpdateMinStock - only admin can update`() {
        assertTrue(PermissionManager.canUpdateMinStock("ADMIN"))
        assertFalse(PermissionManager.canUpdateMinStock("STAFF"))
    }

    @Test
    fun `test canViewFullReportModule - only admin can view`() {
        assertTrue(PermissionManager.canViewFullReportModule("ADMIN"))
        assertFalse(PermissionManager.canViewFullReportModule("STAFF"))
    }

    @Test
    fun `test canViewUserManagement - both admin and staff can view`() {
        assertTrue(PermissionManager.canViewUserManagement("ADMIN"))
        assertTrue(PermissionManager.canViewUserManagement("STAFF"))
    }

    @Test
    fun `test canManageUsers - only admin can manage`() {
        assertTrue(PermissionManager.canManageUsers("ADMIN"))
        assertFalse(PermissionManager.canManageUsers("STAFF"))
    }
}
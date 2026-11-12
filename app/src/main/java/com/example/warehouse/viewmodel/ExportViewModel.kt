package com.example.warehouse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.warehouse.data.database.entity.Product
import com.example.warehouse.data.database.relation.ExportReceiptWithDetails
import com.example.warehouse.data.database.relation.ProductWithCategoryAndSupplier
import com.example.warehouse.data.repository.WarehouseRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ExportCartItem(
    val product: ProductWithCategoryAndSupplier,
    var quantity: Int,
    var price: Double
)

class ExportViewModel(private val repository: WarehouseRepository) : ViewModel() {

    val exportReceipts: StateFlow<List<ExportReceiptWithDetails>> = repository.getAllExportReceipts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _cartItems = MutableStateFlow<List<ExportCartItem>>(emptyList())
    val cartItems: StateFlow<List<ExportCartItem>> = _cartItems.asStateFlow()

    private val _note = MutableStateFlow("")
    val note: StateFlow<String> = _note.asStateFlow()

    private val _recipient = MutableStateFlow("")
    val recipient: StateFlow<String> = _recipient.asStateFlow()

    private val _exportDate = MutableStateFlow(System.currentTimeMillis())
    val exportDate: StateFlow<Long> = _exportDate.asStateFlow()

    fun setNote(newNote: String) {
        _note.value = newNote
    }

    fun setRecipient(newRecipient: String) {
        _recipient.value = newRecipient
    }

    fun setExportDate(date: Long) {
        _exportDate.value = date
    }

    fun addProductToCart(productWithDetails: ProductWithCategoryAndSupplier, quantity: Int = 1): String? {
        val sellingPrice = productWithDetails.product.exportPrice
        if (sellingPrice <= 0.0) {
            return "Sản phẩm '${productWithDetails.product.productName}' chưa được thiết lập giá bán ở module Sản phẩm. Không thể xuất hàng!"
        }

        val stock = productWithDetails.quantityInStock
        if (stock <= 0) return "Sản phẩm đã hết hàng trong kho!"

        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.product.id == productWithDetails.product.id }
        if (index >= 0) {
            val existing = current[index]
            val newQty = existing.quantity + quantity
            if (newQty > stock) return "Số lượng xuất ($newQty) vượt quá tồn kho ($stock)!"
            current[index] = existing.copy(quantity = newQty)
        } else {
            if (quantity > stock) return "Số lượng xuất ($quantity) vượt quá tồn kho ($stock)!"
            current.add(
                ExportCartItem(
                    product = productWithDetails,
                    quantity = quantity,
                    price = sellingPrice
                )
            )
        }
        _cartItems.value = current
        return null
    }

    fun updateCartItem(productId: Long, newQuantity: Int, newPrice: Double): String? {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.product.id == productId }
        if (index >= 0) {
            val item = current[index]
            val stock = item.product.quantityInStock
            if (newQuantity > stock) return "Số lượng vượt quá tồn kho ($stock)!"

            current[index] = item.copy(
                quantity = newQuantity.coerceAtLeast(0),
                price = item.product.product.exportPrice
            )
            _cartItems.value = current
        }
        return null
    }

    fun removeCartItem(productId: Long) {
        _cartItems.value = _cartItems.value.filterNot { it.product.product.id == productId }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        _note.value = ""
        _recipient.value = ""
        _exportDate.value = System.currentTimeMillis()
    }

    fun createReceipt(userId: Long?, createdBy: String, onComplete: (Boolean, String) -> Unit) {
        if (_cartItems.value.isEmpty()) {
            onComplete(false, "Vui lòng chọn ít nhất 1 sản phẩm để xuất")
            return
        }

        for (item in _cartItems.value) {
            if (item.quantity <= 0) {
                onComplete(false, "Sản phẩm '${item.product.product.productName}' phải có số lượng > 0")
                return
            }
        }

        viewModelScope.launch {
            try {
                val dateVal = _exportDate.value
                val code = repository.generateExportCode(dateVal)
                val details = _cartItems.value.map {
                    Pair(it.product.product, Pair(it.quantity, it.price))
                }
                repository.createExportReceipt(
                    code = code,
                    userId = userId,
                    exportDate = dateVal,
                    recipient = _recipient.value.trim(),
                    note = _note.value.trim(),
                    details = details,
                    createdBy = createdBy
                )
                clearCart()
                onComplete(true, "Tạo phiếu xuất $code thành công")
            } catch (e: Exception) {
                onComplete(false, "Lỗi: ${e.message}")
            }
        }
    }

    fun updateReceipt(
        receiptId: Long,
        exportDate: Long,
        recipient: String,
        note: String,
        details: List<Pair<Long, Int>>, // List of Pair(productId, quantity)
        userFullName: String,
        actingUserRole: String? = null,
        onComplete: (Boolean, String) -> Unit
    ) {
        if (!com.example.warehouse.utils.PermissionManager.canEditExportDetailQuantity(actingUserRole)) {
            onComplete(false, "Bạn không có quyền thực hiện thao tác này")
            return
        }
        viewModelScope.launch {
            try {
                repository.updateExportReceipt(
                    receiptId = receiptId,
                    newExportDate = exportDate,
                    newRecipient = recipient,
                    newNote = note,
                    newDetails = details,
                    userFullName = userFullName
                )
                onComplete(true, "Cập nhật phiếu xuất kho thành công")
            } catch (e: Exception) {
                onComplete(false, e.message ?: "Lỗi khi cập nhật phiếu xuất kho")
            }
        }
    }

    fun deleteReceipt(
        receiptId: Long,
        deleteReason: String,
        actingUserRole: String? = null,
        onComplete: (Boolean, String) -> Unit
    ) {
        if (!com.example.warehouse.utils.PermissionManager.canDeleteExportReceipt(actingUserRole)) {
            onComplete(false, "Bạn không có quyền thực hiện thao tác này")
            return
        }
        viewModelScope.launch {
            try {
                repository.deleteExportReceiptSecure(receiptId, deleteReason)
                onComplete(true, "Xóa phiếu xuất kho thành công")
            } catch (e: Exception) {
                onComplete(false, e.message ?: "Lỗi khi xóa phiếu xuất kho")
            }
        }
    }

    suspend fun getExportReceiptById(id: Long): ExportReceiptWithDetails? {
        return repository.getExportReceiptById(id)
    }
}

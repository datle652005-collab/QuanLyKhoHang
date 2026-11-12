package com.example.warehouse.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.unit.sp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.warehouse.data.database.entity.Category
import com.example.warehouse.data.database.entity.StockHistory
import com.example.warehouse.data.database.entity.Supplier
import com.example.warehouse.data.database.relation.ExportReceiptWithDetails
import com.example.warehouse.data.database.relation.ImportReceiptWithDetails
import com.example.warehouse.data.database.relation.ProductWithCategoryAndSupplier
import com.example.warehouse.ui.theme.PrimaryBlue
import com.example.warehouse.ui.theme.PrimaryBlueDark
import com.example.warehouse.ui.theme.PrimaryBlueLight
import com.example.warehouse.ui.theme.StockDanger
import com.example.warehouse.ui.theme.StockNormal
import com.example.warehouse.utils.CurrencyFormatter
import com.example.warehouse.utils.DateUtils

@Composable
fun TodayStockOverviewCard(
    totalStockQty: Int,
    todayImportQty: Int,
    todayExportQty: Int,
    modifier: Modifier = Modifier
) {
    val dateStr = try {
        java.text.SimpleDateFormat("d 'thg' M", java.util.Locale("vi", "VN")).format(java.util.Date())
    } catch (e: Exception) {
        "27 thg 7"
    }

    val numberFormatter = java.text.NumberFormat.getIntegerInstance(java.util.Locale("vi", "VN"))
    val formattedTotal = try { numberFormatter.format(totalStockQty) } catch (e: Exception) { "$totalStockQty" }
    val formattedImport = try { numberFormatter.format(todayImportQty) } catch (e: Exception) { "$todayImportQty" }
    val formattedExport = try { numberFormatter.format(todayExportQty) } catch (e: Exception) { "$todayExportQty" }

    Card(
        colors = CardDefaults.cardColors(containerColor = PrimaryBlue),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("today_stock_overview_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Hôm nay",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = dateStr,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }


            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Column 1: Tổng tồn kho
                Column(
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = formattedTotal,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Tổng tồn kho",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(34.dp)
                        .background(Color.White.copy(alpha = 0.25f))
                )

                // Column 2: Nhập kho
                Column(
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.weight(1f).padding(start = 12.dp)
                ) {
                    Text(
                        text = formattedImport,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Nhập kho",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(34.dp)
                        .background(Color.White.copy(alpha = 0.25f))
                )

                // Column 3: Xuất kho
                Column(
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.weight(1f).padding(start = 12.dp)
                ) {
                    Text(
                        text = formattedExport,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Xuất kho",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun SummaryCard(
    title: String,
    value: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, Color(0xFFDBEAFE)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(contentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun ProductItemCard(
    item: ProductWithCategoryAndSupplier,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val product = item.product
    val qty = item.quantityInStock
    val minStock = product.minStock

    val (statusLabel, statusBg, statusColor) = when {
        qty <= 0 -> Triple("Hết hàng", Color(0xFFFEE2E2), Color(0xFFDC2626))
        qty <= minStock -> Triple("Sắp hết", Color(0xFFFEF3C7), Color(0xFFD97706))
        else -> Triple("Đủ hàng", Color(0xFFDCFCE7), Color(0xFF16A34A))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("product_card_${product.id}"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFDBEAFE)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Product Image or Placeholder
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFEFF6FF)),
                contentAlignment = Alignment.Center
            ) {
                if (product.image.isNotEmpty()) {
                    AsyncImage(
                        model = product.image,
                        contentDescription = product.productName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Main Product Details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color(0xFFDBEAFE),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = product.productCode,
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryBlueDark,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    item.category?.let { cat ->
                        Surface(
                            color = Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = cat.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF475569),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (product.status != "ACTIVE") {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Ẩn/Ngừng KD",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = product.productName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Giá: ${CurrencyFormatter.formatVnd(product.exportPrice)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Stock Badge with 3 States
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Surface(
                    color = statusBg,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "$qty",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }

                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
fun CategoryItemCard(
    category: Category,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    userRole: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFDBEAFE)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = Color(0xFFEFF6FF),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                    ) {
                        Text(
                            text = "CAT-${"%03d".format(category.id)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (com.example.warehouse.utils.PermissionManager.canManageCategory(userRole)) {
                    Row {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, contentDescription = "Sửa", tint = PrimaryBlue)
                        }
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = Color(0xFFDC2626))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Mô tả
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp).padding(top = 2.dp),
                    tint = Color(0xFF64748B)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (category.description.isNotBlank()) category.description else "Chưa có mô tả",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (category.description.isNotBlank()) Color(0xFF334155) else Color(0xFF94A3B8)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // ngày tạo & cập nhật
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Tạo: ${com.example.warehouse.utils.DateUtils.formatDateTime(category.createdAt)}",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
                if (category.updatedAt > category.createdAt) {
                    Text(
                        text = "Cập nhật: ${com.example.warehouse.utils.DateUtils.formatDateTime(category.updatedAt)}",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }
    }
}

@Composable
fun SupplierItemCard(
    supplier: Supplier,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    userRole: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFDBEAFE)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Code tag & Status tag on left, Edit/Delete on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Mã nhà cung cấp
                    Surface(
                        color = Color(0xFFEFF6FF),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                    ) {
                        Text(
                            text = supplier.supplierCode.ifEmpty { "SUP-${"%03d".format(supplier.id)}" },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Trạng thái
                    val isActive = supplier.status.equals("ACTIVE", ignoreCase = true)
                    Surface(
                        color = if (isActive) Color(0xFFDCFCE7) else Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, if (isActive) Color(0xFF86EFAC) else Color(0xFFCBD5E1))
                    ) {
                        Text(
                            text = if (isActive) "Đang hoạt động" else "Ngừng hoạt động",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isActive) Color(0xFF166534) else Color(0xFF64748B),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                if (com.example.warehouse.utils.PermissionManager.canEditOrDeleteSupplier(userRole)) {
                    Row {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, contentDescription = "Sửa", tint = PrimaryBlue)
                        }
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = Color(0xFFDC2626))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tên nhà cung cấp (ở dưới Mã NCC & Trạng thái)
            Text(
                text = supplier.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Người liên hệ
            if (supplier.contactPerson.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp), tint = PrimaryBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Người liên hệ: ${supplier.contactPerson}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1E293B)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // SĐT & Email
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (supplier.phone.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFF64748B))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = supplier.phone, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF334155))
                    }
                }
                if (supplier.email.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFF64748B))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = supplier.email, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF334155), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            // Địa chỉ
            if (supplier.address.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFF64748B))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = supplier.address, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF64748B))
                }
            }

            // Mã số thuế
            if (supplier.taxCode.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFF64748B))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "MST: ${supplier.taxCode}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF475569))
                }
            }

            // Ghi chú
            if (supplier.note.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notes, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFF64748B))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Ghi chú: ${supplier.note}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(6.dp))

            // Timestamps
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Ngày tạo: ${com.example.warehouse.utils.DateUtils.formatDateTime(supplier.createdAt)}",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
                if (supplier.updatedAt > supplier.createdAt) {
                    Text(
                        text = "Sửa: ${com.example.warehouse.utils.DateUtils.formatDateTime(supplier.updatedAt)}",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }
    }
}

@Composable
fun ImportReceiptCard(
    receiptWithDetails: ImportReceiptWithDetails,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    titleOverride: String? = null
) {
    val receipt = receiptWithDetails.receipt
    val totalAmount = if (receipt.totalAmount > 0) receipt.totalAmount else receiptWithDetails.details.sumOf { it.detail.quantity * it.detail.unitPrice }

    val isCancelled = receipt.status.equals("Đã hủy", ignoreCase = true)
    val isEdited = receipt.status.equals("Đã chỉnh sửa", ignoreCase = true) ||
                   receipt.note.contains("[Sửa phiếu") ||
                   receipt.note.contains("[Log sửa") ||
                   receipt.note.contains("[Sửa phiêu")

    val showStatusBadge = isCancelled || isEdited
    val statusBg = if (isCancelled) Color(0xFFFEE2E2) else Color(0xFFFEF3C7)
    val statusColor = if (isCancelled) Color(0xFFDC2626) else Color(0xFFB45309)
    val statusText = if (isCancelled) "Đã hủy" else "Đã chỉnh sửa"

    val isExport = titleOverride == "Xuất kho"
    val isImport = titleOverride == "Nhập kho"
    val tagBgColor = when {
        isExport -> Color(0xFFFFF3E0) // Cam nhạt
        isImport -> Color(0xFFEFF6FF) // Xanh nhạt
        else -> Color(0xFFDBEAFE) // Mặc định
    }
    val tagTextColor = when {
        isExport -> Color(0xFFD84315) // Cam đậm
        isImport -> Color(0xFF1D4ED8) // Xanh đậm
        else -> PrimaryBlueDark // Mặc định
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, if (isCancelled) Color(0xFFFCA5A5) else Color(0xFFDBEAFE)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = tagBgColor,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = titleOverride ?: receipt.code,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = tagTextColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    if (showStatusBadge) {
                        Surface(
                            color = statusBg,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = statusColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                val displayTime = receipt.createdAt.takeIf { it > 0 } ?: receipt.importDate
                Text(
                    text = DateUtils.formatDate(displayTime, "dd/MM/yyyy HH:mm"),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            receiptWithDetails.supplier?.let { supplier ->
                Text(
                    text = "Nhà cung cấp: ${supplier.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF0F172A)
                )
            }

            receiptWithDetails.user?.let { u ->
                Text(
                    text = "Người thực hiện: ${u.fullName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sản phẩm: ${receiptWithDetails.details.size} dòng",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )
                Text(
                    text = CurrencyFormatter.formatVnd(totalAmount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isCancelled) Color(0xFF94A3B8) else PrimaryBlue
                )
            }
        }
    }
}

@Composable
fun ExportReceiptCard(
    receiptWithDetails: ExportReceiptWithDetails,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    titleOverride: String? = null
) {
    val receipt = receiptWithDetails.receipt
    val totalAmount = receiptWithDetails.details.sumOf { it.detail.quantity * it.detail.unitPrice }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFDBEAFE)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val isExport = titleOverride == "Xuất kho"
                    val isImport = titleOverride == "Nhập kho"
                    val tagBgColor = when {
                        isExport -> Color(0xFFFFF3E0) // Cam nhạt
                        isImport -> Color(0xFFEFF6FF) // Xanh nhạt
                        else -> Color(0xFFDBEAFE) // Mặc định
                    }
                    val tagTextColor = when {
                        isExport -> Color(0xFFD84315) // Cam đậm
                        isImport -> Color(0xFF1D4ED8) // Xanh đậm
                        else -> PrimaryBlueDark // Mặc định
                    }

                    Surface(
                        color = tagBgColor,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = titleOverride ?: receipt.code,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = tagTextColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    val isDeleted = receipt.status == "Đã xóa" || receipt.status == "Đã hủy"
                    val isEdited = receipt.status == "Đã chỉnh sửa" ||
                                   receipt.note.contains("[Sửa phiếu") ||
                                   receipt.note.contains("[Log sửa") ||
                                   receipt.note.contains("[Sửa phiêu")

                    val showStatusBadge = isDeleted || isEdited
                    val statusBg = if (isDeleted) Color(0xFFFEE2E2) else Color(0xFFFEF3C7)
                    val statusColor = if (isDeleted) Color(0xFFDC2626) else Color(0xFFB45309)
                    val statusLabel = if (isDeleted) "Đã hủy" else "Đã chỉnh sửa"

                    if (showStatusBadge) {
                        Surface(
                            color = statusBg,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = statusLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = statusColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                val displayTime = receipt.createdAt.takeIf { it > 0 } ?: receipt.exportDate
                Text(
                    text = DateUtils.formatDate(displayTime, "dd/MM/yyyy HH:mm"),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    receiptWithDetails.user?.let { user ->
                        Text(
                            text = "Người lập: ${user.fullName}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF0F172A)
                        )
                    }
                    if (receipt.recipient.isNotBlank()) {
                        Text(
                            text = "Người nhận: ${receipt.recipient}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF475569)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Số lượng SP: ${receiptWithDetails.details.size} sản phẩm",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )
                Text(
                    text = CurrencyFormatter.formatVnd(totalAmount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue
                )
            }
        }
    }
}

@Composable
fun StockHistoryItemCard(
    history: StockHistory,
    productName: String = "",
    useTypeInsteadOfCode: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val isPositive = history.quantityChange > 0
    val color = if (isPositive) StockNormal else StockDanger

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFDBEAFE)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (history.action) {
                        "IMPORT" -> Icons.Default.MoveToInbox
                        "EXPORT" -> Icons.Default.Outbox
                        "INVENTORY_ADJUST" -> Icons.Default.Tune
                        else -> Icons.Default.History
                    },
                    contentDescription = null,
                    tint = color
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val titleText = if (useTypeInsteadOfCode) {
                        when (history.action) {
                            "IMPORT" -> "Nhập kho"
                            "EXPORT" -> "Xuất kho"
                            "INVENTORY_ADJUST" -> "Kiểm kê"
                            else -> history.action
                        }
                    } else {
                        history.referenceCode.ifEmpty { 
                            when (history.action) {
                                "IMPORT" -> "Nhập kho"
                                "EXPORT" -> "Xuất kho"
                                "INVENTORY_ADJUST" -> "Kiểm kê"
                                else -> history.action
                            }
                        }
                    }

                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    
                    val showStatus = history.status.isNotEmpty() && 
                            !history.status.equals("Đã lưu", ignoreCase = true) && 
                            !history.status.equals("Completed", ignoreCase = true)

                    if (showStatus) {
                        Surface(
                            color = if (history.status.contains("hủy", ignoreCase = true) || history.status.contains("xóa", ignoreCase = true)) Color(0xFFFEE2E2) else Color(0xFFFEF3C7),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = history.status,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (history.status.contains("hủy", ignoreCase = true) || history.status.contains("xóa", ignoreCase = true)) Color(0xFFDC2626) else Color(0xFFB45309),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                
                if (productName.isNotEmpty()) {
                    Text(
                        text = productName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF334155),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                if (!history.partnerName.isNullOrEmpty()) {
                    Text(
                        text = history.partnerName,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF475569),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = "${DateUtils.formatDateTime(history.createdAt)} • Bởi ${history.createdBy}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )
                
                if (history.unitPrice > 0) {
                    Text(
                        text = "Đơn giá: ${CurrencyFormatter.formatVnd(history.unitPrice)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (isPositive) "+${history.quantityChange}" else "${history.quantityChange}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = "Tồn: ${history.quantityAfter}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}

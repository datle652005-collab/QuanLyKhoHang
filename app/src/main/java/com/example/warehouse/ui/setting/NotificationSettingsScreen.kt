package com.example.warehouse.ui.setting

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.warehouse.ui.component.WarehouseTopBar
import com.example.warehouse.ui.theme.PrimaryBlue
import com.example.warehouse.utils.RepeatFrequency
import com.example.warehouse.utils.SendTiming
import com.example.warehouse.viewmodel.NotificationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    viewModel: NotificationViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    // Notification Permission Handling for Android 13+
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (isGranted) {
            viewModel.setNotificationEnabled(true)
        }
    }

    var showTimePicker by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Main Toggle
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Bật/Tắt thông báo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Nhận thông báo nhắc nhở tồn kho",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = state.isEnabled,
                        onCheckedChange = { checked ->
                            if (checked && !hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                viewModel.setNotificationEnabled(checked)
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = PrimaryBlue)
                    )
                }
            }

            if (state.isEnabled) {
                Spacer(modifier = Modifier.height(20.dp))

                // Alert Types
                Text(
                    text = "Loại cảnh báo",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        CheckboxRow("Đã hết hàng", state.outOfStockEnabled) { viewModel.setOutOfStockEnabled(it) }
                        HorizontalDivider(color = Color(0xFFF0F0F0))
                        CheckboxRow("Tồn kho gần hết", state.almostOutEnabled) { viewModel.setAlmostOutEnabled(it) }
                        HorizontalDivider(color = Color(0xFFF0F0F0))
                        CheckboxRow("Tồn kho thấp", state.lowStockEnabled) { viewModel.setLowStockEnabled(it) }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Send Timing
                Text(
                    text = "Chế độ gửi",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        RadioRow(
                            text = "Ngay khi phát sinh",
                            selected = state.sendTiming == SendTiming.IMMEDIATE,
                            onClick = { viewModel.setSendTiming(SendTiming.IMMEDIATE) }
                        )
                        HorizontalDivider(color = Color(0xFFF0F0F0))
                        RadioRow(
                            text = "Theo lịch",
                            selected = state.sendTiming == SendTiming.SCHEDULED,
                            onClick = { viewModel.setSendTiming(SendTiming.SCHEDULED) }
                        )
                    }
                }

                if (state.sendTiming == SendTiming.SCHEDULED) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Khung giờ thông báo",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            state.scheduledTimes.sorted().forEach { time ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = time, style = MaterialTheme.typography.bodyLarge, color = Color(0xFF1E293B))
                                    IconButton(onClick = { viewModel.removeScheduledTime(time) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = Color.Red)
                                    }
                                }
                                HorizontalDivider(color = Color(0xFFF0F0F0))
                            }
                            TextButton(
                                onClick = { showTimePicker = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = PrimaryBlue)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Thêm khung giờ", color = PrimaryBlue)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Repeat Frequency
                Text(
                    text = "Tần suất nhắc lại",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )
                var expandedFreq by remember { mutableStateOf(false) }
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ExposedDropdownMenuBox(
                        expanded = expandedFreq,
                        onExpandedChange = { expandedFreq = !expandedFreq }
                    ) {
                        OutlinedTextField(
                            value = when (state.repeatFrequency) {
                                RepeatFrequency.NO_REPEAT -> "Không nhắc lại"
                                RepeatFrequency.EVERY_6H -> "Mỗi 6 giờ"
                                RepeatFrequency.EVERY_12H -> "Mỗi 12 giờ"
                                RepeatFrequency.EVERY_DAY -> "Mỗi ngày"
                                RepeatFrequency.ONLY_STATUS_CHANGE -> "Chỉ khi trạng thái thay đổi"
                            },
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFreq) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .padding(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = Color(0xFF1E293B),
                                unfocusedTextColor = Color(0xFF1E293B)
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = expandedFreq,
                            onDismissRequest = { expandedFreq = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Không nhắc lại", color = Color(0xFF1E293B)) },
                                onClick = {
                                    viewModel.setRepeatFrequency(RepeatFrequency.NO_REPEAT)
                                    expandedFreq = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Mỗi 6 giờ", color = Color(0xFF1E293B)) },
                                onClick = {
                                    viewModel.setRepeatFrequency(RepeatFrequency.EVERY_6H)
                                    expandedFreq = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Mỗi 12 giờ", color = Color(0xFF1E293B)) },
                                onClick = {
                                    viewModel.setRepeatFrequency(RepeatFrequency.EVERY_12H)
                                    expandedFreq = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Mỗi ngày", color = Color(0xFF1E293B)) },
                                onClick = {
                                    viewModel.setRepeatFrequency(RepeatFrequency.EVERY_DAY)
                                    expandedFreq = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Chỉ khi trạng thái thay đổi", color = Color(0xFF1E293B)) },
                                onClick = {
                                    viewModel.setRepeatFrequency(RepeatFrequency.ONLY_STATUS_CHANGE)
                                    expandedFreq = false
                                }
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showTimePicker) {
        TimePickerDialog(
            onDismiss = { showTimePicker = false },
            onTimeSelected = { hour, minute ->
                val formattedTime = String.format("%02d:%02d", hour, minute)
                viewModel.addScheduledTime(formattedTime)
                showTimePicker = false
            }
        )
    }
}

@Composable
fun CheckboxRow(text: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge, color = Color(0xFF1E293B))
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun RadioRow(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge, color = Color(0xFF1E293B))
        RadioButton(selected = selected, onClick = onClick)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    onDismiss: () -> Unit,
    onTimeSelected: (Int, Int) -> Unit
) {
    val timePickerState = rememberTimePickerState()

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onTimeSelected(timePickerState.hour, timePickerState.minute) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        },
        text = {
            TimePicker(state = timePickerState)
        }
    )
}

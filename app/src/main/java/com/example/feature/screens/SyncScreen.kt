package com.example.feature.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.database.SyncLogEntity
import com.example.feature.task.TaskViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SyncScreen(
    viewModel: TaskViewModel,
    onBack: (() -> Unit)? = null
) {
    val isConnected by viewModel.isGoogleSheetConnected.collectAsState()
    val sheetId by viewModel.googleSheetId.collectAsState()
    val sheetName by viewModel.googleSheetName.collectAsState()
    val intervalMin by viewModel.syncIntervalMinutes.collectAsState()
    val syncHistory by viewModel.syncHistory.collectAsState()

    var sheetIdInput by remember { mutableStateOf("") }
    var sheetNameInput by remember { mutableStateOf("Bảng tính của tôi") }

    var isSyncing by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChronoBackground)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Screen Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onBack?.invoke() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = ChronoText)
            }
            Text(
                text = "Đồng bộ Google Sheets",
                color = ChronoText,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = {}) {
                Icon(Icons.Outlined.HelpOutline, contentDescription = "Help", tint = ChronoText)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Connection card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ChronoSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "TRẠNG THÁI",
                    color = ChronoTextDim,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (isConnected) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = QuadrantGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Đã kết nối với Google Sheets",
                            color = QuadrantGreen,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "File: $sheetName",
                        color = ChronoText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "ID: ${if (sheetId.length > 28) "${sheetId.take(12)}...${sheetId.takeLast(12)}" else sheetId}",
                        color = ChronoTextDim,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    if (isSyncing) {
                        Button(
                            onClick = {},
                            enabled = false,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ChronoPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = ChronoOnPrimary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Đang đồng bộ...", color = ChronoOnPrimary, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = {
                                isSyncing = true
                                viewModel.triggerGoogleSheetsSync()
                                // Re-enable button after simulated delay
                                coroutineScope.launch {
                                    kotlinx.coroutines.delay(1600)
                                    isSyncing = false
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ChronoPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, tint = ChronoOnPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Đồng bộ ngay", color = ChronoOnPrimary, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Ngắt kết nối",
                        color = QuadrantRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .clickable { viewModel.disconnectSheets() }
                            .padding(8.dp)
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = QuadrantRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Chưa kết nối bảng tính",
                            color = QuadrantRed,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = sheetNameInput,
                        onValueChange = { sheetNameInput = it },
                        label = { Text("Tên bảng tính", color = ChronoTextDim) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ChronoText,
                            unfocusedTextColor = ChronoText,
                            focusedBorderColor = ChronoPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = sheetIdInput,
                        onValueChange = { sheetIdInput = it },
                        label = { Text("Spreadsheet ID hoặc Link", color = ChronoTextDim) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ChronoText,
                            unfocusedTextColor = ChronoText,
                            focusedBorderColor = ChronoPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val finalId = if (sheetIdInput.isEmpty()) "1AbcD2eFgHijKlMnOpQrStUvwXxZ" else sheetIdInput
                            viewModel.updateSheetsConfig(finalId, sheetNameInput)
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ChronoPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CloudQueue, contentDescription = null, tint = ChronoOnPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Kết nối Google Drive", color = ChronoOnPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Quick Links and Excel Export Card
        val context = androidx.compose.ui.platform.LocalContext.current
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ChronoSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "TIỆN ÍCH DỮ LIỆU",
                    color = ChronoTextDim,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Open Spreadsheet button
                    Button(
                        onClick = { viewModel.openGoogleSheetInBrowser(context) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("link_to_sheet_button")
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = "Trang tính", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Mở Bảng Tính",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Export Excel button
                    Button(
                        onClick = { viewModel.exportTasksToExcel(context) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = QuadrantGreen),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_excel_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Xuất Excel", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Xuất Excel",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // History list logs
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ChronoSurface),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.0f)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LỊCH SỬ ĐỒNG BỘ",
                        color = ChronoTextDim,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Xóa lịch sử",
                        color = ChronoTextDim,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable { viewModel.clearSyncHistory() }
                            .padding(4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (syncHistory.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Chưa có lịch sử đồng bộ nào.", color = ChronoTextDim, fontSize = 12.sp)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(syncHistory) { log ->
                            SyncHistoryRow(log)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Sync cycle settings card at bottom
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ChronoSurface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Chu kỳ tự động đồng bộ",
                        color = ChronoText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    var showIntervalMenu by remember { mutableStateOf(false) }
                    Box {
                        Text(
                            text = "$intervalMin phút",
                            color = ChronoPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { showIntervalMenu = true }
                                .padding(8.dp)
                        )
                        DropdownMenu(
                            expanded = showIntervalMenu,
                            onDismissRequest = { showIntervalMenu = false },
                            modifier = Modifier.background(ChronoSurfaceElevated)
                        ) {
                            listOf(5, 15, 30, 60).forEach { mins ->
                                DropdownMenuItem(
                                    text = { Text("$mins phút", color = ChronoText) },
                                    onClick = {
                                        viewModel.setSyncInterval(mins)
                                        showIntervalMenu = false
                                    }
                                )
                            }
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Interval",
                        tint = ChronoPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SyncHistoryRow(log: SyncLogEntity) {
    val formatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val displayTime = remember(log.timestamp) { formatter.format(Date(log.timestamp)) }
    val isSuccess = log.status == "Thành công"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = displayTime,
                color = ChronoText,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = log.status,
                color = if (isSuccess) QuadrantGreen else QuadrantRed,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = log.message,
            color = ChronoTextDim,
            fontSize = 11.sp,
            lineHeight = 15.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Divider(color = Color.White.copy(alpha = 0.08f), thickness = 0.5.dp)
    }
}

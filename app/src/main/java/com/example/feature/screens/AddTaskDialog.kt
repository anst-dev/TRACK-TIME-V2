package com.example.feature.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.example.feature.task.TaskViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AddTaskDialog(
    viewModel: TaskViewModel,
    onDismiss: () -> Unit
) {
    var activeTabIsWork by remember { mutableStateOf(false) } // Tabs: Work vs Note

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        try {
            focusRequester.requestFocus()
        } catch (e: Exception) {
            // ignore
        }
    }

    var taskTitle by remember { mutableStateOf("") }
    
    LaunchedEffect(activeTabIsWork) {
        if (!activeTabIsWork && taskTitle.isBlank()) {
            val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            taskTitle = "Note $currentTime"
        }
    }
    var taskDescription by remember { mutableStateOf("") }
    var taskEstimatedMinutes by remember { mutableStateOf("30") }
    var selectedQuadrant by remember { mutableStateOf(3) } // Default to Q3 orange (Không quan trọng - Khẩn cấp)
    var startImmediately by remember { mutableStateOf(true) }

    Card(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = ChronoSurface),
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .navigationBarsPadding()
        ) {
            // Drag handle look
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .background(ChronoTextDim.copy(alpha = 0.3f), RoundedCornerShape(50))
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Title block with Close trigger
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Thêm công việc mới",
                    color = ChronoText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = ChronoText)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dual tabs (Công việc, Ghi chú)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .background(ChronoSurfaceElevated, RoundedCornerShape(12.dp))
                    .padding(3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(
                            if (activeTabIsWork) AccentBlue else Color.Transparent,
                            RoundedCornerShape(9.dp)
                        )
                        .clickable { activeTabIsWork = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Công việc",
                        color = if (activeTabIsWork) Color.White else ChronoTextDim,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(
                            if (!activeTabIsWork) AccentBlue else Color.Transparent,
                            RoundedCornerShape(9.dp)
                        )
                        .clickable { activeTabIsWork = false },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Ghi chú",
                        color = if (!activeTabIsWork) Color.White else ChronoTextDim,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Input field
            Text(
                text = if (activeTabIsWork) "TÊN CÔNG VIỆC" else "TIÊU ĐỀ GHI CHÚ",
                color = ChronoTextDim,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = taskTitle,
                onValueChange = { taskTitle = it },
                placeholder = { Text(if (activeTabIsWork) "Bạn định làm gì?" else "Tiêu đề nhanh...", color = ChronoTextDim, fontSize = 13.sp) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = ChronoText,
                    unfocusedTextColor = ChronoText,
                    focusedContainerColor = ChronoSurfaceElevated,
                    unfocusedContainerColor = ChronoSurfaceElevated,
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "NỘI DUNG CHÚ THÍCH",
                color = ChronoTextDim,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = taskDescription,
                onValueChange = { taskDescription = it },
                placeholder = { Text("Mô tả chi tiết nội dung công việc hoặc nội dung ghi chú tại đây...", color = ChronoTextDim, fontSize = 13.sp) },
                minLines = 2,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = ChronoText,
                    unfocusedTextColor = ChronoText,
                    focusedContainerColor = ChronoSurfaceElevated,
                    unfocusedContainerColor = ChronoSurfaceElevated,
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )

            if (activeTabIsWork) {
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "DỰ KIẾN (PHÚT)",
                    color = ChronoTextDim,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = taskEstimatedMinutes,
                    onValueChange = { taskEstimatedMinutes = it },
                    placeholder = { Text("Số phút dự kiến (Ví dụ: 30, 45, 60)...", color = ChronoTextDim, fontSize = 13.sp) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ChronoText,
                        unfocusedTextColor = ChronoText,
                        focusedContainerColor = ChronoSurfaceElevated,
                        unfocusedContainerColor = ChronoSurfaceElevated,
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (activeTabIsWork) {
                Spacer(modifier = Modifier.height(16.dp))

                // Quad cards (Eisenhower Priority)
                Text(
                    text = "MỨC ĐỘ ƯU TIÊN (EISENHOWER)",
                    color = ChronoTextDim,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(10.dp))

                val itemsQ = listOf(
                    PriorityItem(1, "Quan trọng, Khẩn cấp", QuadrantRed),
                    PriorityItem(2, "Quan trọng, Không khẩn cấp", QuadrantGreen),
                    PriorityItem(3, "Không quan trọng, Khẩn cấp", QuadrantOrange),
                    PriorityItem(4, "Không quan trọng, Không khẩn cấp", QuadrantGrey)
                )

                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1.0f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        PrioritySelectionCard(itemsQ[0], selectedQuadrant == 1) { selectedQuadrant = 1 }
                        PrioritySelectionCard(itemsQ[2], selectedQuadrant == 3) { selectedQuadrant = 3 }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1.0f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        PrioritySelectionCard(itemsQ[1], selectedQuadrant == 2) { selectedQuadrant = 2 }
                        PrioritySelectionCard(itemsQ[3], selectedQuadrant == 4) { selectedQuadrant = 4 }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Starts Immediately checkable Row
            if (activeTabIsWork) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = ChronoPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Bắt đầu đếm giờ ngay",
                            color = ChronoText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Switch(
                        checked = startImmediately,
                        onCheckedChange = { startImmediately = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = AccentBlue
                        )
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            } else {
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Action triggers row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, ChronoTextDim.copy(alpha = 0.3f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ChronoText),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Text("Hủy", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        val finalTitle = if (!activeTabIsWork) {
                            taskTitle.ifBlank { "Note ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())}" }
                        } else {
                            taskTitle
                        }
                        
                        if (finalTitle.isNotEmpty()) {
                            viewModel.addTask(
                                title = finalTitle,
                                description = taskDescription,
                                quadrant = if (activeTabIsWork) selectedQuadrant else 0,
                                estimatedMinutes = if (activeTabIsWork) (taskEstimatedMinutes.toIntOrNull() ?: 30) else 0,
                                isNote = !activeTabIsWork,
                                startTimerImmediately = if (activeTabIsWork) startImmediately else false
                            )
                        }
                        onDismiss()
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White),
                    modifier = Modifier
                        .weight(1.5f)
                        .height(50.dp)
                ) {
                    Text(
                        text = if (activeTabIsWork) "Thêm & Bắt đầu" else "Lưu Ghi Chú",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun PrioritySelectionCard(
    item: PriorityItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.5.dp,
            if (isSelected) item.color else item.color.copy(alpha = 0.2f)
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) item.color.copy(alpha = 0.12f) else ChronoSurfaceElevated
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1.0f)) {
                Text(
                    text = if (item.id == 1) "!" else if (item.id == 2) "📅" else if (item.id == 3) "🔔" else "⚪",
                    color = item.color,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.label,
                    color = item.color,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 14.sp
                )
            }
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = item.color,
                    unselectedColor = item.color.copy(alpha = 0.3f)
                ),
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

data class PriorityItem(val id: Int, val label: String, val color: Color)

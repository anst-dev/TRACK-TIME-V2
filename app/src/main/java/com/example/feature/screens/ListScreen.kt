package com.example.feature.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import com.example.core.database.TaskEntity
import com.example.feature.task.TaskViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ListScreen(viewModel: TaskViewModel) {
    val tasks by viewModel.allTasks.collectAsState()
    var activeTabIsTask by remember { mutableStateOf(true) } // Tabs: Tasks vs Notes
    var searchQuery by remember { mutableStateOf("") }
    var selectedQuadrantFilter by remember { mutableStateOf(0) } // 0 = All, 1..4 quadrant

    var selectedEditingTask by remember { mutableStateOf<TaskEntity?>(null) }

    if (selectedEditingTask != null) {
        EditSessionScreen(
            task = selectedEditingTask!!,
            onDismiss = { selectedEditingTask = null },
            onSave = { updated ->
                viewModel.updateTask(updated)
                selectedEditingTask = null
            },
            onDelete = { taskId ->
                viewModel.deleteTask(taskId)
                selectedEditingTask = null
            }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ChronoBackground)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Professional segment tab selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .background(ChronoSurface, RoundedCornerShape(12.dp))
                    .padding(3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(
                            if (activeTabIsTask) ChronoPrimary else Color.Transparent,
                            RoundedCornerShape(9.dp)
                        )
                        .clickable { activeTabIsTask = true },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.TaskAlt,
                            contentDescription = null,
                            tint = if (activeTabIsTask) ChronoOnPrimary else ChronoTextDim,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Tác Vụ Điện Tử",
                            color = if (activeTabIsTask) ChronoOnPrimary else ChronoTextDim,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(
                            if (!activeTabIsTask) ChronoPrimary else Color.Transparent,
                            RoundedCornerShape(9.dp)
                        )
                        .clickable { activeTabIsTask = false },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Book,
                            contentDescription = null,
                            tint = if (!activeTabIsTask) ChronoOnPrimary else ChronoTextDim,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Sổ Ghi Chú Riêng",
                            color = if (!activeTabIsTask) ChronoOnPrimary else ChronoTextDim,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Beautiful M3 Search Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(if (activeTabIsTask) "Tìm kiếm mục tiêu..." else "Tìm kiếm ghi chú...", color = ChronoTextDim, fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ChronoPrimary) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = ChronoText,
                    unfocusedTextColor = ChronoText,
                    focusedContainerColor = ChronoSurface,
                    unfocusedContainerColor = ChronoSurface,
                    focusedBorderColor = ChronoPrimary,
                    unfocusedBorderColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_bar")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Filtering Chips Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Horizontal scrolling container for filter chips
                val filters = listOf(
                    FilterChipData(0, "Tất cả"),
                    FilterChipData(1, "Q1"),
                    FilterChipData(2, "Q2"),
                    FilterChipData(3, "Q3"),
                    FilterChipData(4, "Q4")
                )

                filters.forEach { filter ->
                    val isSelected = selectedQuadrantFilter == filter.id
                    Box(
                        modifier = Modifier
                            .background(
                                if (isSelected) ChronoPrimary else ChronoSurface,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedQuadrantFilter = filter.id }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = filter.label,
                            color = if (isSelected) ChronoOnPrimary else ChronoText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            val filteredTasks = remember(tasks, searchQuery, selectedQuadrantFilter, activeTabIsTask) {
                tasks.filter { task ->
                    val matchesQuery = task.title.contains(searchQuery, ignoreCase = true) || 
                                       task.description.contains(searchQuery, ignoreCase = true)
                    val matchesQuadrant = selectedQuadrantFilter == 0 || task.quadrant == selectedQuadrantFilter
                    val matchesType = task.isNote == !activeTabIsTask
                    matchesQuery && matchesQuadrant && matchesType
                }
            }

            if (filteredTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.0f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (activeTabIsTask) Icons.Default.Inbox else Icons.Default.EditNote,
                            contentDescription = null,
                            tint = ChronoTextDim,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (activeTabIsTask) "Không có tác vụ nào phù hợp" else "Sổ ghi chú trống hoặc chưa khớp từ khóa",
                            color = ChronoTextDim,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1.0f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredTasks, key = { it.id }) { task ->
                        if (activeTabIsTask) {
                            TaskLogCard(
                                task = task,
                                onClick = { selectedEditingTask = task },
                                onComplete = {
                                    viewModel.completeTask(task.id)
                                },
                                onDelete = {
                                    viewModel.deleteTask(task.id)
                                }
                            )
                        } else {
                            NoteCard(
                                note = task,
                                onClick = { selectedEditingTask = task },
                                onDelete = { viewModel.deleteTask(task.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TaskLogCard(
    task: TaskEntity,
    onClick: () -> Unit,
    onComplete: () -> Unit,
    onDelete: () -> Unit
) {
    val qColor = when (task.quadrant) {
        1 -> QuadrantRed
        2 -> QuadrantGreen
        3 -> QuadrantOrange
        else -> QuadrantGrey
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ChronoSurface),
        border = BorderStroke(1.dp, qColor.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_item_card")
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon representing Priority Quadrant symbol
            val icon = when (task.quadrant) {
                1 -> Icons.Default.Warning
                2 -> Icons.Default.CalendarMonth
                3 -> Icons.Default.Notifications
                else -> Icons.Default.Block
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(qColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = qColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1.0f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = task.title,
                        color = ChronoText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (task.actualSeconds > 0) "${task.actualSeconds / 60}m" else "Chưa làm",
                        color = ChronoPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (task.description.isEmpty()) "Không có ghi chú..." else task.description,
                    color = ChronoTextDim,
                    fontSize = 12.sp,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = ChronoTextDim,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Ước lượng: ${task.estimatedMinutes} phút",
                        color = ChronoTextDim,
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Trạng thái: ${if (task.status == "COMPLETED") "Hoàn thành" else if (task.status == "DOING") "Đang chạy" else "Chờ"}",
                        color = if (task.status == "COMPLETED") QuadrantGreen else ChronoPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (task.status != "COMPLETED") {
                Spacer(modifier = Modifier.width(10.dp))
                IconButton(onClick = onComplete) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Hoàn thành",
                        tint = ChronoTextDim,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Xóa tác vụ",
                    tint = QuadrantRed.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// Full Edit Session Subscreen implementing wireframe 6's precise UI
@Composable
fun EditSessionScreen(
    task: TaskEntity,
    onDismiss: () -> Unit,
    onSave: (TaskEntity) -> Unit,
    onDelete: (Int) -> Unit
) {
    var title by remember { mutableStateOf(task.title) }
    var description by remember { mutableStateOf(task.description) }
    var estimatedMinutes by remember { mutableStateOf(task.estimatedMinutes.toString()) }
    var actualMinutes by remember { mutableStateOf((task.actualSeconds / 60).toString()) }
    var quadrant by remember { mutableStateOf(task.quadrant) }
    var status by remember { mutableStateOf(task.status) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChronoBackground)
            .padding(16.dp)
    ) {
        // Appbar for editing
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Trở lại", tint = ChronoText)
            }
            Text(
                text = if (task.isNote) "CHI TIẾT GHI CHÚ" else "CHI TIẾT TÁC VỤ",
                color = ChronoText,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = {}) {
                Icon(Icons.Default.Settings, contentDescription = "Sửa đổi", tint = ChronoText)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Large summary inputs matching wireframe text box
        Text(
            text = if (task.isNote) "TIÊU ĐỀ GHI CHÚ" else "TÊN TÁC VỤ",
            color = ChronoTextDim,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = ChronoText,
                unfocusedTextColor = ChronoText,
                focusedContainerColor = ChronoSurface,
                unfocusedContainerColor = ChronoSurface,
                focusedBorderColor = ChronoPrimary,
                unfocusedBorderColor = Color.Transparent
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = if (task.isNote) "NỘI DUNG CHÚ THÍCH" else "CHI TIẾT / GHI CHÚ",
            color = ChronoTextDim,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            shape = RoundedCornerShape(12.dp),
            minLines = 3,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = ChronoText,
                unfocusedTextColor = ChronoText,
                focusedContainerColor = ChronoSurface,
                unfocusedContainerColor = ChronoSurface,
                focusedBorderColor = ChronoPrimary,
                unfocusedBorderColor = Color.Transparent
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "CHARS: ${description.length}",
            color = ChronoTextDim,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.align(Alignment.End)
        )

        if (!task.isNote) {
            Spacer(modifier = Modifier.height(14.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1.0f)) {
                    Text(
                        text = "DỰ KIẾN (PHÚT)",
                        color = ChronoTextDim,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = estimatedMinutes,
                        onValueChange = { estimatedMinutes = it },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ChronoText,
                            unfocusedTextColor = ChronoText,
                            focusedContainerColor = ChronoSurface,
                            unfocusedContainerColor = ChronoSurface,
                            focusedBorderColor = ChronoPrimary,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1.0f)) {
                    Text(
                        text = "THỰC TẾ (PHÚT)",
                        color = ChronoTextDim,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = actualMinutes,
                        onValueChange = { actualMinutes = it },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ChronoText,
                            unfocusedTextColor = ChronoText,
                            focusedContainerColor = ChronoSurface,
                            unfocusedContainerColor = ChronoSurface,
                            focusedBorderColor = ChronoPrimary,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "TRẠNG THÁI TÁC VỤ",
                color = ChronoTextDim,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("WAITING", "COMPLETED").forEach { s ->
                    val isSelected = status == s
                    val label = if (s == "WAITING") "Đang Chờ / Chưa Xong" else "Đã Hoàn Thành"
                    val activeColor = if (s == "COMPLETED") QuadrantGreen else ChronoPrimary
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (isSelected) activeColor.copy(alpha = 0.2f) else ChronoSurface,
                                RoundedCornerShape(12.dp)
                            )
                            .border(
                                1.5.dp,
                                if (isSelected) activeColor else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { status = s }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) activeColor else ChronoText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Matrix priority selector cards under wireframe 6 layout
            Text(
                text = "PHÂN CẤP ƯU TIÊN (EISENHOWER)",
                color = ChronoTextDim,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            val priorities = listOf(
                PriorityData(1, "Q1", "Quan trọng, Khẩn cấp", QuadrantRed),
                PriorityData(2, "Q2", "Quan trọng, Không khẩn cấp", QuadrantGreen),
                PriorityData(3, "Q3", "Không quan trọng, Khẩn cấp", QuadrantOrange),
                PriorityData(4, "Q4", "Không quan trọng, Không khẩn cấp", QuadrantGrey)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1.0f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PriorityCardButton(priorities[0], quadrant == 1) { quadrant = 1 }
                    PriorityCardButton(priorities[2], quadrant == 3) { quadrant = 3 }
                }
                Column(modifier = Modifier.weight(1.0f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PriorityCardButton(priorities[1], quadrant == 2) { quadrant = 2 }
                    PriorityCardButton(priorities[3], quadrant == 4) { quadrant = 4 }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1.0f))

        // Complete save and delete actions
        Button(
            onClick = {
                val finalTitle = if (task.isNote) {
                    title.ifBlank { "Note ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())}" }
                } else {
                    title
                }
                val finalStatus = if (status == "DOING") "WAITING" else status
                val finalCompletedAt = if (finalStatus == "COMPLETED") (task.completedAt ?: System.currentTimeMillis()) else null
                val updated = task.copy(
                    title = finalTitle,
                    description = description,
                    estimatedMinutes = if (task.isNote) 0 else (estimatedMinutes.toIntOrNull() ?: 0),
                    actualSeconds = if (task.isNote) 0 else ((actualMinutes.toIntOrNull() ?: 0) * 60),
                    quadrant = if (task.isNote) 0 else quadrant,
                    status = finalStatus,
                    completedAt = finalCompletedAt,
                    updatedAt = System.currentTimeMillis()
                )
                onSave(updated)
            },
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ChronoPrimary, contentColor = ChronoOnPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("LƯU THAY ĐỔI", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = { onDelete(task.id) },
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, QuadrantRed.copy(alpha = 0.5f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = QuadrantRed),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(Icons.Default.Delete, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (task.isNote) "XÓA GHI CHÚ" else "XÓA TÁC VỤ", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun PriorityCardButton(
    data: PriorityData,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.5.dp, 
            if (isSelected) data.color else data.color.copy(alpha = 0.2f)
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) data.color.copy(alpha = 0.12f) else ChronoSurface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = data.label,
                    color = data.color,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                RadioButton(
                    selected = isSelected,
                    onClick = onClick,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = data.color,
                        unselectedColor = data.color.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = data.desc,
                color = ChronoTextDim,
                fontSize = 10.sp,
                maxLines = 1
            )
        }
    }
}

data class FilterChipData(val id: Int, val label: String)
data class PriorityData(val id: Int, val label: String, val desc: String, val color: Color)

@Composable
fun NoteCard(
    note: TaskEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val isNoPriority = note.quadrant == 0
    val qColor = when (note.quadrant) {
        1 -> QuadrantRed
        2 -> QuadrantGreen
        3 -> QuadrantOrange
        4 -> QuadrantGrey
        else -> AccentBlue
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ChronoSurface),
        border = BorderStroke(1.dp, qColor.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isNoPriority) {
                        Icon(
                            imageVector = Icons.Default.StickyNote2,
                            contentDescription = null,
                            tint = AccentBlue,
                            modifier = Modifier.size(14.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(qColor, RoundedCornerShape(50))
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isNoPriority) "Ghi chú thường" else when (note.quadrant) {
                            1 -> "Q1: Quan trọng, Khẩn cấp"
                            2 -> "Q2: Quan trọng, Không khẩn cấp"
                            3 -> "Q3: Không quan trọng, Khẩn cấp"
                            else -> "Q4: Không quan trọng, Không khẩn cấp"
                        },
                        color = qColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Xóa",
                        tint = QuadrantRed.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = note.title,
                color = ChronoText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            if (note.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = note.description,
                    color = ChronoTextDim,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ngày tạo: ${note.date}",
                    color = ChronoTextDim,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onClick() }
                ) {
                    Text(
                        text = "Chi tiết",
                        color = ChronoPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = ChronoPrimary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

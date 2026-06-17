package com.example.feature.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.database.TaskEntity
import com.example.feature.task.TaskViewModel
import com.example.ui.theme.*

@Composable
fun HomeScreen(
    viewModel: TaskViewModel,
    onNavigateToList: () -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToSync: () -> Unit,
    onNavigateToAi: () -> Unit,
    onOpenAddTask: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val tasks by viewModel.tasksForSelectedDate.collectAsState()
    val allTimeLogs by viewModel.allTimeLogs.collectAsState()
    val activeTaskId by viewModel.activeTaskId.collectAsState()
    val runningSeconds by viewModel.runningSeconds.collectAsState()
    val timerMode by viewModel.timerMode.collectAsState()

    var showModeSelector by remember { mutableStateOf(false) }

    // Aggregate today's elapsed time per quadrant from Room DB
    val quadrantTimes = remember(tasks, allTimeLogs) {
        val times = DoubleArray(4) // Q1, Q2, Q3, Q4 in minutes
        tasks.forEach { task ->
            if (task.quadrant in 1..4) {
                times[task.quadrant - 1] += (task.actualSeconds / 60.0)
            }
        }
        times
    }

    val totalTimeMin = quadrantTimes.sum().coerceAtLeast(1.0)
    val percentages = quadrantTimes.map { ((it / totalTimeMin) * 100).toInt() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChronoBackground)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Context selectors (Chế độ, Ngày)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Button(
                    onClick = { showModeSelector = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ChronoSurfaceElevated,
                        contentColor = ChronoText
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = Color.Yellow,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Chế độ: ${if (timerMode == "THAO_TAC_NHANH") "Thao tác nhanh" else "Tiến trình"}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }

                DropdownMenu(
                    expanded = showModeSelector,
                    onDismissRequest = { showModeSelector = false },
                    modifier = Modifier.background(ChronoSurfaceElevated)
                ) {
                    DropdownMenuItem(
                        text = { Text("Chế độ: Thao tác nhanh", color = ChronoText, fontSize = 13.sp) },
                        onClick = {
                            viewModel.setTimerMode("THAO_TAC_NHANH")
                            showModeSelector = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Chế độ: Tiến trình sâu", color = ChronoText, fontSize = 13.sp) },
                        onClick = {
                            viewModel.setTimerMode("TIENTRINH")
                            showModeSelector = false
                        }
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = ChronoSurfaceElevated),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.clickable { /* Date selector could go here */ }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = ChronoPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Hôm nay: 16/06/2026",
                        color = ChronoText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Matrix Categories
        val categories = listOf(
            MatrixCategory(1, "Quan trọng – Khẩn cấp", "Làm ngay", "DO", QuadrantRed, ContentRed),
            MatrixCategory(2, "Quan trọng – Không khẩn cấp", "Lên kế hoạch", "PLAN", QuadrantGreen, ContentGreen),
            MatrixCategory(3, "Không quan trọng – Khẩn cấp", "Ủy thác", "DELEGATE", QuadrantOrange, ContentOrange),
            MatrixCategory(4, "Không quan trọng – Không khẩn cấp", "Loại bỏ / Thư giãn", "DROP", QuadrantGrey, ContentGrey)
        )

        // 2x2 Eisenhower Matrix Grid Layout
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Upper-Left (Q1: DO)
            Box(modifier = Modifier.weight(1f)) {
                val cat = categories[0]
                val quadrantTasks = tasks.filter { it.quadrant == cat.id && !it.isNote }
                val isRunningHere = quadrantTasks.any { it.id == activeTaskId }
                val activeSeconds = if (isRunningHere) runningSeconds else 0L
                val historicalMinutes = quadrantTimes[cat.id - 1].toLong()
                val totalSeconds = (historicalMinutes * 60) + activeSeconds
                val displayClock = formatTimeClock(totalSeconds)
                val displayTaskTitle = if (isRunningHere) {
                    quadrantTasks.firstOrNull { it.id == activeTaskId }?.title ?: ""
                } else {
                    quadrantTasks.firstOrNull { it.status != "COMPLETED" }?.title ?: "Trống (Chạm để tạo)"
                }

                NewEisenhowerQuadrantCard(
                    category = cat,
                    displayClock = displayClock,
                    activeTaskTitle = displayTaskTitle,
                    isRunning = isRunningHere,
                    badgeText = "KHẨN CẤP & QUAN TRỌNG",
                    abbreviation = "DO",
                    onTogglePlay = { viewModel.startTimerByQuadrant(cat.id) }
                )
            }

            // Upper-Right (Q2: PLAN)
            Box(modifier = Modifier.weight(1f)) {
                val cat = categories[1]
                val quadrantTasks = tasks.filter { it.quadrant == cat.id && !it.isNote }
                val isRunningHere = quadrantTasks.any { it.id == activeTaskId }
                val activeSeconds = if (isRunningHere) runningSeconds else 0L
                val historicalMinutes = quadrantTimes[cat.id - 1].toLong()
                val totalSeconds = (historicalMinutes * 60) + activeSeconds
                val displayClock = formatTimeClock(totalSeconds)
                val displayTaskTitle = if (isRunningHere) {
                    quadrantTasks.firstOrNull { it.id == activeTaskId }?.title ?: ""
                } else {
                    quadrantTasks.firstOrNull { it.status != "COMPLETED" }?.title ?: "Trống (Chạm để tạo)"
                }

                NewEisenhowerQuadrantCard(
                    category = cat,
                    displayClock = displayClock,
                    activeTaskTitle = displayTaskTitle,
                    isRunning = isRunningHere,
                    badgeText = "QUAN TRỌNG & KHÔNG KHẨN CẤP",
                    abbreviation = "PLAN",
                    onTogglePlay = { viewModel.startTimerByQuadrant(cat.id) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Lower-Left (Q3: DELEGATE)
            Box(modifier = Modifier.weight(1f)) {
                val cat = categories[2]
                val quadrantTasks = tasks.filter { it.quadrant == cat.id && !it.isNote }
                val isRunningHere = quadrantTasks.any { it.id == activeTaskId }
                val activeSeconds = if (isRunningHere) runningSeconds else 0L
                val historicalMinutes = quadrantTimes[cat.id - 1].toLong()
                val totalSeconds = (historicalMinutes * 60) + activeSeconds
                val displayClock = formatTimeClock(totalSeconds)
                val displayTaskTitle = if (isRunningHere) {
                    quadrantTasks.firstOrNull { it.id == activeTaskId }?.title ?: ""
                } else {
                    quadrantTasks.firstOrNull { it.status != "COMPLETED" }?.title ?: "Trống (Chạm để tạo)"
                }

                NewEisenhowerQuadrantCard(
                    category = cat,
                    displayClock = displayClock,
                    activeTaskTitle = displayTaskTitle,
                    isRunning = isRunningHere,
                    badgeText = "KHÔNG QUAN TRỌNG & KHẨN CẤP",
                    abbreviation = "DELEGATE",
                    onTogglePlay = { viewModel.startTimerByQuadrant(cat.id) }
                )
            }

            // Lower-Right (Q4: DROP)
            Box(modifier = Modifier.weight(1f)) {
                val cat = categories[3]
                val quadrantTasks = tasks.filter { it.quadrant == cat.id && !it.isNote }
                val isRunningHere = quadrantTasks.any { it.id == activeTaskId }
                val activeSeconds = if (isRunningHere) runningSeconds else 0L
                val historicalMinutes = quadrantTimes[cat.id - 1].toLong()
                val totalSeconds = (historicalMinutes * 60) + activeSeconds
                val displayClock = formatTimeClock(totalSeconds)
                val displayTaskTitle = if (isRunningHere) {
                    quadrantTasks.firstOrNull { it.id == activeTaskId }?.title ?: ""
                } else {
                    quadrantTasks.firstOrNull { it.status != "COMPLETED" }?.title ?: "Trống (Chạm để tạo)"
                }

                NewEisenhowerQuadrantCard(
                    category = cat,
                    displayClock = displayClock,
                    activeTaskTitle = displayTaskTitle,
                    isRunning = isRunningHere,
                    badgeText = "KHÔNG QUAN TRỌNG & KHÔNG KHẨN CẤP",
                    abbreviation = "DROP",
                    onTogglePlay = { viewModel.startTimerByQuadrant(cat.id) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // AI Insight Banner Card (from Bold Typography theme specification)
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2F31)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ai_insight_banner")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFD0BCFF).copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFFD0BCFF),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "ĐỀ XUẤT HIỆU QUẢ AI",
                        color = Color(0xFFD0BCFF),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Hiệu suất đỉnh cao của bạn đạt được vào 09:00 - 11:00 sáng. Hãy lên lịch sâu cho các việc nhóm Q2!",
                        color = Color.White,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Daily aggregated summary (Tổng thời gian hôm nay)
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ChronoSurface),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("summary_section")
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Tổng thời gian hôm nay",
                    color = ChronoText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val summaryItems = listOf(
                        SummaryItem("Khẩn cấp", "Quan trọng", quadrantTimes[0], percentages[0], QuadrantRed),
                        SummaryItem("Quan trọng", "Không khẩn cấp", quadrantTimes[1], percentages[1], QuadrantGreen),
                        SummaryItem("Khẩn cấp", "Không quan trọng", quadrantTimes[2], percentages[2], QuadrantOrange),
                        SummaryItem("Không khẩn cấp", "Không quan trọng", quadrantTimes[3], percentages[3], QuadrantGrey)
                    )

                    summaryItems.forEachIndexed { idx, item ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 2.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(item.color, RoundedCornerShape(50))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = item.title,
                                    color = ChronoTextDim,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = item.subtitle,
                                color = ChronoTextDim,
                                fontSize = 9.sp,
                                maxLines = 1,
                                modifier = Modifier.padding(start = 10.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = formatMinutesToMinutesSeconds(item.minutes),
                                color = ChronoText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 10.dp)
                            )
                            Text(
                                text = "(${item.percent}%)",
                                color = ChronoTextDim,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(start = 10.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Quick Menu Icons row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("quick_access_tools"),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val quickActions = listOf(
                QuickActionItem("Danh sách", Icons.Default.FormatListBulleted, Color(0xFF2196F3), onNavigateToList),
                QuickActionItem("Báo cáo", Icons.Default.InsertChart, Color(0xFF9C27B0), onNavigateToStats),
                QuickActionItem("Thống kê", Icons.Default.TrendingUp, Color(0xFF00BFA5), onNavigateToStats),
                QuickActionItem("Đồng bộ", Icons.Default.CloudQueue, Color(0xFF03A9F4), onNavigateToSync),
                QuickActionItem("Cài đặt", Icons.Default.Settings, Color(0xFFFFB300), onOpenSettings)
            )

            quickActions.forEach { act ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { act.action() },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(act.color.copy(alpha = 0.15f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = act.icon,
                            contentDescription = act.label,
                            tint = act.color,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = act.label,
                        color = ChronoText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun EisenhowerCard(
    category: MatrixCategory,
    displayClock: String,
    isRunning: Boolean,
    statusLabel: String,
    onTogglePlay: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = category.color),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_card_${category.id}")
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            // Card Title Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Small header tag
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = category.desc.uppercase(),
                                color = category.contentColor.copy(alpha = 0.8f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            if (isRunning) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(category.contentColor, RoundedCornerShape(50))
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = category.shortLabel,
                            color = category.contentColor,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = category.name,
                            color = category.contentColor.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Sub-status block
            Box(
                modifier = Modifier
                    .background(category.contentColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = statusLabel,
                    color = category.contentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = displayClock,
                    color = category.contentColor,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )

                // Large action button
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(category.contentColor.copy(alpha = 0.2f), RoundedCornerShape(50))
                        .clickable { onTogglePlay() },
                    contentAlignment = Alignment.Center
                ) {
                    if (isRunning) {
                        PulseWaveIcon(category.contentColor)
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Bắt đầu",
                            tint = category.contentColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PulseWaveIcon(tintColor: Color) {
    Icon(
        imageVector = Icons.Default.Pause,
        contentDescription = "Tạm dừng",
        tint = tintColor,
        modifier = Modifier
            .size(28.dp)
            .animateContentSize()
    )
}

fun formatTimeClock(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return String.format("%02d:%02d:%02d", h, m, s)
}

fun formatMinutesToMinutesSeconds(minutesDouble: Double): String {
    val totalSeconds = (minutesDouble * 60).toLong()
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return String.format("%02d:%02d:%02d", h, m, s)
}

data class MatrixCategory(
    val id: Int,
    val name: String,
    val desc: String,
    val shortLabel: String,
    val color: Color,
    val contentColor: Color
)

data class SummaryItem(
    val title: String,
    val subtitle: String,
    val minutes: Double,
    val percent: Int,
    val color: Color
)

data class QuickActionItem(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color,
    val action: () -> Unit
)

@Composable
fun NewEisenhowerQuadrantCard(
    category: MatrixCategory,
    displayClock: String,
    activeTaskTitle: String,
    isRunning: Boolean,
    badgeText: String,
    abbreviation: String,
    onTogglePlay: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = category.color),
        modifier = Modifier
            .fillMaxWidth()
            .height(175.dp)
            .clickable { onTogglePlay() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row of the Quadrant card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = badgeText,
                    color = category.contentColor.copy(alpha = 0.75f),
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                if (isRunning) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(category.contentColor, RoundedCornerShape(50))
                    )
                }
            }

            // Big Bold Abbreviation title
            Text(
                text = abbreviation,
                color = category.contentColor,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp
            )

            // Bottom section
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = activeTaskTitle,
                    color = category.contentColor.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayClock,
                        color = category.contentColor,
                        fontSize = 16.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black
                    )

                    Icon(
                        imageVector = if (isRunning) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                        contentDescription = "Chạy/Tạm dừng",
                        tint = category.contentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

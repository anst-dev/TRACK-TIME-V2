package com.example.feature.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.database.TaskEntity
import com.example.feature.task.TaskViewModel
import com.example.ui.theme.*

@Composable
fun StatsScreen(viewModel: TaskViewModel) {
    val tasks by viewModel.tasksForSelectedDate.collectAsState()

    // Aggregate statistics
    val totalTasks = tasks.size
    val completedTasksCount = tasks.count { it.status == "COMPLETED" }
    val completionPercent = if (totalTasks > 0) ((completedTasksCount.toDouble() / totalTasks) * 100).toInt() else 0

    val qTimes = remember(tasks) {
        val list = DoubleArray(4) // Q1, Q2, Q3, Q4 actual minutes
        tasks.forEach {
            if (it.quadrant in 1..4) {
                list[it.quadrant - 1] += (it.actualSeconds / 60.0)
            }
        }
        list
    }

    val totalLoggedMin = qTimes.sum()
    val totalStr = formatMinutesToMinutesSeconds(totalLoggedMin)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChronoBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Screen custom AppBar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Báo cáo – 16/06/2026",
                color = ChronoText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Row {
                IconButton(onClick = {}) {
                    Icon(Icons.Default.DateRange, contentDescription = null, tint = ChronoPrimary)
                }
                IconButton(onClick = {}) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = ChronoPrimary)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // High contrast KPI Cards row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KpiCard(
                title = "Tổng thời gian",
                value = totalStr,
                modifier = Modifier.weight(1.0f)
            )
            KpiCard(
                title = "Số task",
                value = totalTasks.toString(),
                modifier = Modifier.weight(1.0f)
            )
            KpiCard(
                title = "Hoàn thành",
                value = "$completionPercent%",
                modifier = Modifier.weight(1.0f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Donut Chart Card layout
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ChronoSurface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Phân bổ theo loại",
                    color = ChronoText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Custom Donut Canvas
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        DonutChartCanvas(qTimes)
                        
                        // Centering aggregate summary text
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "TỔNG CỘNG",
                                color = ChronoTextDim,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                    text = "${totalLoggedMin.toInt()}m",
                                color = ChronoText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    // Legends list side-by-side
                    Column(
                        modifier = Modifier.weight(1.0f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val items = listOf(
                            LegendData("1. Khẩn cấp", formatMinutesToMinutesSeconds(qTimes[0]), getPercent(qTimes[0], totalLoggedMin), QuadrantRed),
                            LegendData("2. Quan trọng", formatMinutesToMinutesSeconds(qTimes[1]), getPercent(qTimes[1], totalLoggedMin), QuadrantGreen),
                            LegendData("3. Ủy thác", formatMinutesToMinutesSeconds(qTimes[2]), getPercent(qTimes[2], totalLoggedMin), QuadrantOrange),
                            LegendData("4. Thư giãn", formatMinutesToMinutesSeconds(qTimes[3]), getPercent(qTimes[3], totalLoggedMin), QuadrantGrey)
                        )

                        items.forEach { leg ->
                            Row(verticalAlignment = Alignment.Top) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(leg.color, RoundedCornerShape(3.dp))
                                        .padding(top = 2.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = leg.title,
                                        color = ChronoText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${leg.time} (${leg.percent}%)",
                                        color = ChronoTextDim,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ChronoSurface),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                color = ChronoTextDim,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                color = ChronoText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun DonutChartCanvas(qTimes: DoubleArray) {
    val total = qTimes.sum().coerceAtLeast(1.0)
    val sweepAngles = qTimes.map { ((it / total) * 360f).toFloat() }

    val colors = listOf(QuadrantRed, QuadrantGreen, QuadrantOrange, QuadrantGrey)

    Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeWidth = 14.dp.toPx()
        val sizeMin = size.minDimension
        val donutSize = Size(sizeMin - strokeWidth, sizeMin - strokeWidth)
        val offset = Offset(strokeWidth / 2f, strokeWidth / 2f)

        var startAngle = -90f

        sweepAngles.forEachIndexed { idx, sweep ->
            // Prevent drawing tiny zero lines to preserve cap borders
            if (sweep > 2f) {
                drawArc(
                    color = colors[idx],
                    startAngle = startAngle,
                    sweepAngle = sweep - 1.5f, // brief break separator
                    useCenter = false,
                    topLeft = offset,
                    size = donutSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
            startAngle += sweep
        }

        // Under-backing if all are 0
        if (total <= 1.0) {
            drawArc(
                color = Color.White.copy(alpha = 0.1f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = offset,
                size = donutSize,
                style = Stroke(width = strokeWidth)
            )
        }
    }
}

fun getPercent(part: Double, total: Double): Int {
    if (total <= 0.0) return 0
    return ((part / total) * 100).toInt()
}

data class LegendData(
    val title: String,
    val time: String,
    val percent: Int,
    val color: Color
)

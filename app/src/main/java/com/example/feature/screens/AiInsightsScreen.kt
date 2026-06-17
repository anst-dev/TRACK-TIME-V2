package com.example.feature.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.feature.task.AiAnalysisState
import com.example.feature.task.TaskViewModel
import com.example.ui.theme.*

@Composable
fun AiInsightsScreen(viewModel: TaskViewModel) {
    val analysisState by viewModel.aiAnalysisState.collectAsState()
    val checkedProposals by viewModel.checkedProposals.collectAsState()

    // Trigger AI analysis if it's initial
    LaunchedEffect(Unit) {
        if (analysisState is AiAnalysisState.Initial) {
            viewModel.runAiAnalysis()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChronoBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Custom AppBar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {}) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = ChronoPrimary)
            }
            Text(
                text = "AI phân tích — 16/hãn tích",
                color = ChronoText,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { viewModel.runAiAnalysis() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = ChronoPrimary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // AI Robot Comments Card
        when (val state = analysisState) {
            is AiAnalysisState.Initial, is AiAnalysisState.Loading -> {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = ChronoSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = ChronoPrimary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Hệ thống AI đang đọc biểu đồ log của bạn...",
                            color = ChronoTextDim,
                            fontSize = 13.sp
                        )
                    }
                }
            }
            is AiAnalysisState.Error -> {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = ChronoSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Đã xảy ra lỗi", color = QuadrantRed, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(state.message, color = ChronoTextDim, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.runAiAnalysis() },
                            colors = ButtonDefaults.buttonColors(containerColor = ChronoPrimary)
                        ) {
                            Text("Chạy lại phân tích", color = ChronoOnPrimary)
                        }
                    }
                }
            }
            is AiAnalysisState.Success -> {
                // Comments Banner with robot icon
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = ChronoSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(18.dp)) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(ChronoPrimary.copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Face,
                                contentDescription = null,
                                tint = ChronoPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1.0f)) {
                            Text(
                                text = "Nhận xét của AI",
                                color = ChronoText,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = state.analysis,
                                color = ChronoTextDim,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Trends list section
                Text(
                    text = "Xu hướng của bạn",
                    color = ChronoText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                state.trends.forEachIndexed { idx, trend ->
                    val color = when (idx) {
                        0 -> QuadrantGreen
                        1 -> QuadrantRed
                        else -> ChronoPrimary
                    }
                    val icon = when (idx) {
                        0 -> Icons.Default.Circle
                        1 -> Icons.Default.Cancel
                        else -> Icons.Default.EventNote
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = trend.title,
                                color = ChronoTextDim,
                                fontSize = 13.sp
                            )
                            Text(
                                text = trend.value,
                                color = ChronoText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Proposals checklist section
                Text(
                    text = "Đề xuất cho ngày mai",
                    color = ChronoText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))

                state.proposals.forEachIndexed { idx, prop ->
                    val isChecked = checkedProposals.contains(idx)
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isChecked) ChronoPrimary.copy(alpha = 0.08f) else ChronoSurface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable { viewModel.toggleProposalChecked(idx) }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isChecked) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = if (isChecked) QuadrantGreen else ChronoPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = prop,
                                color = if (isChecked) ChronoTextDim else ChronoText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1.0f)
                            )
                        }
                    }
                }
            }
        }
    }
}

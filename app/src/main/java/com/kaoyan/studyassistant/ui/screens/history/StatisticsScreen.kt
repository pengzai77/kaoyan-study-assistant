package com.kaoyan.studyassistant.ui.screens.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kaoyan.studyassistant.data.local.dao.DateDurationSummary
import com.kaoyan.studyassistant.data.local.dao.SubjectDurationSummary
import com.kaoyan.studyassistant.ui.components.EmptyState
import com.kaoyan.studyassistant.ui.components.KaoyanTopBar
import com.kaoyan.studyassistant.util.DateUtils

@Composable
fun StatisticsScreen(
    navController: NavController,
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val maxSubjectMillis = remember(uiState.subjectDurations) {
        uiState.subjectDurations.maxOfOrNull { it.totalMillis } ?: 0L
    }

    Scaffold(
        topBar = {
            KaoyanTopBar(
                title = "学习统计",
                onBack = { navController.popBackStack() }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item(key = "total_card") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "历史累计学习时长",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (uiState.totalMillis > 0) {
                                    DateUtils.formatDuration(uiState.totalMillis)
                                } else {
                                    "暂无记录"
                                },
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                item(key = "pie_title") {
                    Text(
                        text = "科目学习占比",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                item(key = "pie_chart") {
                    SubjectPieChartCard(durations = uiState.subjectDurations)
                }

                item(key = "subject_title") {
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "按科目统计",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (uiState.subjectDurations.isEmpty()) {
                    item(key = "empty_subjects") {
                        EmptyState(message = "暂无科目数据")
                    }
                } else {
                    items(
                        items = uiState.subjectDurations,
                        key = { item -> item.subjectName }
                    ) { item ->
                        SubjectStatRow(
                            item = item,
                            maxMillis = maxSubjectMillis
                        )
                    }
                }

                item(key = "trend_title") {
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "近30天每日学习时长",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                item(key = "trend_chart") {
                    DateTrendChart(durations = uiState.dateDurations)
                }
            }
        }
    }
}

@Composable
private fun SubjectPieChartCard(durations: List<SubjectDurationSummary>) {
    if (durations.isEmpty()) {
        EmptyState(message = "暂无学习数据")
        return
    }

    val totalMillis = remember(durations) { durations.sumOf { it.totalMillis } }
    if (totalMillis <= 0) {
        EmptyState(message = "暂无学习数据")
        return
    }

    val sortedDurations = remember(durations) {
        durations.sortedByDescending { it.totalMillis }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    var startAngle = -90f
                    sortedDurations.forEachIndexed { index, item ->
                        val sweepAngle = (item.totalMillis.toFloat() / totalMillis) * 360f
                        val color = SUBJECT_COLORS[item.subjectName]
                            ?: DEFAULT_SUBJECT_COLORS[index % DEFAULT_SUBJECT_COLORS.size]

                        drawArc(
                            color = color,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = true,
                            size = Size(size.width, size.height),
                            style = Fill
                        )
                        startAngle += sweepAngle
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sortedDurations.forEachIndexed { index, item ->
                    val color = SUBJECT_COLORS[item.subjectName]
                        ?: DEFAULT_SUBJECT_COLORS[index % DEFAULT_SUBJECT_COLORS.size]
                    val percentage = item.totalMillis.toDouble() / totalMillis * 100
                    val percentageStr = String.format("%.1f%%", percentage)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item.subjectName,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = percentageStr,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.width(60.dp)
                        )
                        Text(
                            text = DateUtils.formatDuration(item.totalMillis),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(80.dp),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubjectStatRow(item: SubjectDurationSummary, maxMillis: Long) {
    val progress = if (maxMillis > 0) item.totalMillis.toFloat() / maxMillis else 0f
    val barColor = SUBJECT_COLORS[item.subjectName] ?: MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.subjectName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = DateUtils.formatDuration(item.totalMillis),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = barColor
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(barColor.copy(alpha = 0.15f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .clip(RoundedCornerShape(3.dp))
                        .background(barColor)
                )
            }
        }
    }
}

@Composable
private fun DateTrendChart(durations: List<DateDurationSummary>) {
    if (durations.isEmpty()) {
        EmptyState(message = "近30天暂无学习数据")
        return
    }

    val maxMillis = remember(durations) {
        durations.maxOf { it.totalMillis }.coerceAtLeast(1)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                durations.forEach { item ->
                    val ratio = item.totalMillis.toFloat() / maxMillis
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(ratio.coerceAtLeast(0.02f))
                                .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = durations.firstOrNull()?.date?.substring(5) ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = durations.lastOrNull()?.date?.substring(5) ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private val SUBJECT_COLORS = mapOf(
    "政治" to Color(0xFFE53935),
    "英语" to Color(0xFF1E88E5),
    "数学" to Color(0xFF43A047),
    "专业课" to Color(0xFFFB8C00)
)

private val DEFAULT_SUBJECT_COLORS = listOf(
    Color(0xFF9C27B0),
    Color(0xFF00BCD4),
    Color(0xFF009688),
    Color(0xFFFFC107),
    Color(0xFF795548)
)

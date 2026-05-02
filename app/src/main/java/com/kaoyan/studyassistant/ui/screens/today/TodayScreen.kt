package com.kaoyan.studyassistant.ui.screens.today

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kaoyan.studyassistant.data.local.entity.StudySession
import com.kaoyan.studyassistant.ui.components.EmptyState
import com.kaoyan.studyassistant.ui.components.KaoyanTopBar
import com.kaoyan.studyassistant.util.DateUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 今日学习页面
 *
 * 新增能力：
 * - 点击日期文字区域弹出 DatePickerDialog，可选任意历史日期（不允许未来日期）
 * - 快捷按钮：今天 / 昨天
 * - "回到今天"按钮（当前日期不是今天时显示）
 * - 保留原有前一天 / 后一天切换按钮
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    navController: NavController,
    viewModel: TodayViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    var showDatePicker by remember { mutableStateOf(false) }

    // DatePickerState：限制最大可选日期为今天，默认选中当前日期
    val today = LocalDate.now()
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = runCatching {
            LocalDate.parse(uiState.selectedDate, formatter)
                .atStartOfDay(java.time.ZoneId.systemDefault())
                .toInstant().toEpochMilli()
        }.getOrElse { System.currentTimeMillis() },
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val selectedDay = java.time.Instant.ofEpochMilli(utcTimeMillis)
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                return !selectedDay.isAfter(today)
            }
        }
    )

    // 弹出日期选择器对话框
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val selected = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                        viewModel.selectDate(selected.format(formatter))
                    }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(
                state = datePickerState,
                title = {
                    Text(
                        text = "选择日期（不可选择未来日期）",
                        modifier = Modifier.padding(start = 24.dp, top = 16.dp),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            )
        }
    }

    Scaffold(
        topBar = {
            KaoyanTopBar(
                title = "今日学习",
                onBack = { navController.popBackStack() }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 日期切换器（含点击弹出日期选择器）
            DateSwitcher(
                currentDate = uiState.selectedDate,
                onPrevious = {
                    val date = LocalDate.parse(uiState.selectedDate, formatter).minusDays(1)
                    viewModel.selectDate(date.format(formatter))
                },
                onNext = {
                    val date = LocalDate.parse(uiState.selectedDate, formatter).plusDays(1)
                    if (!date.isAfter(today)) {
                        viewModel.selectDate(date.format(formatter))
                    }
                },
                canGoNext = uiState.selectedDate < DateUtils.today(),
                onPickDate = { showDatePicker = true },
                onGoToday = { viewModel.selectDate(DateUtils.today()) },
                onYesterday = {
                    viewModel.selectDate(
                        today.minusDays(1).format(formatter)
                    )
                }
            )

            // 总时长摘要
            if (uiState.totalMillis > 0) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "当日总学习时长",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = DateUtils.formatDuration(uiState.totalMillis),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.subjectSummaries.isEmpty()) {
                EmptyState(
                    message = "这天没有学习记录\n快去开始计时吧！",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = uiState.subjectSummaries,
                        key = { summary -> summary.subjectName }
                    ) { summary ->
                        SubjectSummaryCard(summary = summary)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 日期切换器（含日期选择入口）
// ─────────────────────────────────────────────────────────────

@Composable
private fun DateSwitcher(
    currentDate: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    canGoNext: Boolean,
    onPickDate: () -> Unit,
    onGoToday: () -> Unit,
    onYesterday: () -> Unit
) {
    val isToday = DateUtils.isToday(currentDate)
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val yesterday = LocalDate.now().minusDays(1).format(formatter)
    val isYesterday = currentDate == yesterday

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // 主行：左右切换 + 可点击日期区域
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevious) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "前一天")
            }

            // 点击日期文字区域弹出日期选择器
            Row(
                modifier = Modifier
                    .clickable(onClick = onPickDate)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "选择日期",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = DateUtils.formatFullDate(currentDate),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    val label = when {
                        isToday     -> "今天"
                        isYesterday -> "昨天"
                        else        -> null
                    }
                    if (label != null) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            IconButton(onClick = onNext, enabled = canGoNext) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "后一天",
                    tint = if (canGoNext) MaterialTheme.colorScheme.onSurface
                           else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
        }

        // 快捷按钮行：今天 / 昨天 / 回到今天
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            FilterChip(
                selected = isToday,
                onClick = onGoToday,
                label = { Text("今天", style = MaterialTheme.typography.labelSmall) },
                leadingIcon = if (isToday) {
                    { Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null
            )
            FilterChip(
                selected = isYesterday,
                onClick = onYesterday,
                label = { Text("昨天", style = MaterialTheme.typography.labelSmall) }
            )
            // 非今天时显示"回到今天"
            if (!isToday) {
                AssistChip(
                    onClick = onGoToday,
                    label = { Text("回到今天", style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = {
                        Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 科目学习摘要卡片
// ─────────────────────────────────────────────────────────────

@Composable
private fun SubjectSummaryCard(summary: SubjectStudySummary) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        onClick = { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = summary.subjectName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "共 ${summary.sessionCount} 次 · ${DateUtils.formatDuration(summary.totalMillis)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = DateUtils.formatDuration(summary.totalMillis),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (summary.latestNote.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "最近备注：${summary.latestNote}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }

            // 展开显示详细记录
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(8.dp))
                summary.sessions.forEach { session ->
                    SessionDetailItem(session = session)
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 单条学习记录详情
// ─────────────────────────────────────────────────────────────

@Composable
private fun SessionDetailItem(session: StudySession) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${DateUtils.formatTime(session.startTime)} - ${DateUtils.formatTime(session.endTime)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (session.note.isNotBlank()) {
                Text(
                    text = session.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Text(
            text = DateUtils.formatDuration(session.durationMillis),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

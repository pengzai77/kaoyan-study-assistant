package com.kaoyan.studyassistant.ui.screens.history

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kaoyan.studyassistant.data.local.entity.StudySession
import com.kaoyan.studyassistant.ui.components.EmptyState
import com.kaoyan.studyassistant.ui.components.KaoyanTopBar
import com.kaoyan.studyassistant.util.DateUtils
import java.util.Calendar

private data class HistoryDateSection(
    val date: String,
    val sessions: List<StudySession>,
    val totalMillis: Long
)

/**
 * 历史记录页面
 *
 * 新增功能：
 * - 顶部快捷筛选（7天/30天/90天/全部）
 * - 自定义开始/结束日期选择（DatePickerDialog）
 * - 日期错误提示
 */
@Composable
fun HistoryScreen(
    navController: NavController,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val groupedSections = remember(uiState.sessions) {
        uiState.sessions
            .groupBy { it.date }
            .toSortedMap(compareByDescending { it })
            .map { (date, sessions) ->
                HistoryDateSection(
                    date = date,
                    sessions = sessions,
                    totalMillis = sessions.sumOf { it.durationMillis }
                )
            }
    }

    Scaffold(
        topBar = {
            KaoyanTopBar(
                title = "历史记录",
                onBack = { navController.popBackStack() }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ---- 快捷筛选 Chip 行 ----
            LazyRow(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = HistoryQuickRange.entries,
                    key = { range -> range.name }
                ) { range ->
                    FilterChip(
                        selected = uiState.quickRange == range,
                        onClick = { viewModel.applyQuickRange(range) },
                        label = { Text(range.label, style = MaterialTheme.typography.bodySmall) }
                    )
                }
            }

            // ---- 自定义日期范围行 ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 开始日期按钮
                OutlinedButton(
                    onClick = {
                        val cal = DateUtils.parseDate(uiState.filterStartDate) ?: Calendar.getInstance()
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                val newStart = "%04d-%02d-%02d".format(y, m + 1, d)
                                viewModel.updateDateFilter(newStart, uiState.filterEndDate)
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(DateUtils.formatDisplayDate(uiState.filterStartDate), style = MaterialTheme.typography.bodySmall)
                }

                Text("至", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                // 结束日期按钮
                OutlinedButton(
                    onClick = {
                        val cal = DateUtils.parseDate(uiState.filterEndDate) ?: Calendar.getInstance()
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                val newEnd = "%04d-%02d-%02d".format(y, m + 1, d)
                                viewModel.updateDateFilter(uiState.filterStartDate, newEnd)
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(DateUtils.formatDisplayDate(uiState.filterEndDate), style = MaterialTheme.typography.bodySmall)
                }

                // 重置按钮
                IconButton(onClick = viewModel::resetFilter, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = "重置", tint = MaterialTheme.colorScheme.primary)
                }
            }

            // ---- 日期错误提示 ----
            if (uiState.dateRangeError != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null,
                            tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = uiState.dateRangeError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // ---- 记录数量提示 ----
            Text(
                text = "共 ${uiState.sessions.size} 条记录",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // ---- 内容区 ----
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.sessions.isEmpty()) {
                EmptyState(
                    message = "该时间段内暂无学习记录",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    groupedSections.forEach { section ->
                        item(key = "header_${section.date}") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = DateUtils.formatFullDate(section.date),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "共 ${DateUtils.formatDuration(section.totalMillis)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        items(section.sessions, key = { it.id }) { session ->
                            SessionHistoryCard(
                                session = session,
                                onDelete = { viewModel.deleteSession(session) },
                                onUpdateNote = { updatedNote -> viewModel.updateSessionNote(session, updatedNote) }
                            )
                        }
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

/** 单条历史记录卡片 */
@Composable
private fun SessionHistoryCard(
    session: StudySession,
    onDelete: () -> Unit,
    onUpdateNote: (String) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editNote by remember(session.id, session.note) { mutableStateOf(session.note) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 12.dp)
                .fillMaxWidth()
        ) {
            // 顶部行：科目名 + 时长 + 删除按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = session.subjectName.ifEmpty { "未分类" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = DateUtils.formatDuration(session.durationMillis),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            // 时间段
            Text(
                text = "${DateUtils.formatTime(session.startTime)} - ${DateUtils.formatTime(session.endTime)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(
                modifier = Modifier.padding(end = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "备注",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = session.note.ifBlank { "暂无备注" },
                style = MaterialTheme.typography.bodySmall,
                color = if (session.note.isBlank()) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 8.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { showNoteDialog = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("查看备注", style = MaterialTheme.typography.bodySmall)
                }
                OutlinedButton(
                    onClick = {
                        editNote = session.note
                        showEditDialog = true
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (session.note.isBlank()) "添加备注" else "修改备注", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除记录") },
            text = { Text("确定要删除这条学习记录吗？") },
            confirmButton = {
                TextButton(
                    onClick = { onDelete(); showDeleteDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            }
        )
    }

    if (showNoteDialog) {
        AlertDialog(
            onDismissRequest = { showNoteDialog = false },
            title = { Text("备注详情") },
            text = {
                Text(
                    text = session.note.ifBlank { "这条学习记录还没有备注。" },
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { showNoteDialog = false }) { Text("关闭") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showNoteDialog = false
                        editNote = session.note
                        showEditDialog = true
                    }
                ) { Text("去修改") }
            }
        )
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text(if (session.note.isBlank()) "添加备注" else "修改备注") },
            text = {
                OutlinedTextField(
                    value = editNote,
                    onValueChange = { editNote = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("备注") },
                    placeholder = { Text("记录这次学习学了什么、卡在哪、下次怎么接着学") },
                    minLines = 4,
                    maxLines = 8,
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateNote(editNote)
                        showEditDialog = false
                    }
                ) { Text("保存修改") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("取消") }
            }
        )
    }
}

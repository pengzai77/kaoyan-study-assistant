package com.kaoyan.studyassistant.ui.screens.goal

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kaoyan.studyassistant.ui.components.KaoyanTopBar
import com.kaoyan.studyassistant.util.DateUtils
import java.util.Calendar

/**
 * 考研目标页面
 *
 * 功能：
 * - 设置目标院校、目标专业
 * - 设置考试日期（DatePickerDialog）
 * - 设置每周学习目标（分钟）
 * - 显示倒计时天数
 * - 显示本周学习进度
 */
@Composable
fun GoalScreen(
    navController: NavController,
    viewModel: GoalViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(uiState.savedSuccess) {
        if (uiState.savedSuccess) {
            snackbarHostState.showSnackbar("目标已保存 ✓")
            viewModel.clearSuccess()
        }
    }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            KaoyanTopBar(
                title = "考研目标",
                onBack = { navController.popBackStack() },
                actions = {
                    if (uiState.isEditing) {
                        TextButton(onClick = viewModel::save) {
                            Text("保存", fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        IconButton(onClick = viewModel::startEditing) {
                            Icon(Icons.Default.Edit, contentDescription = "编辑")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ---- 倒计时卡片 ----
            CountdownCard(
                daysToExam = uiState.daysToExam,
                examDate = uiState.examDate,
                targetSchool = uiState.targetSchool,
                targetMajor = uiState.targetMajor
            )

            // ---- 本周进度卡片 ----
            WeeklyProgressCard(
                studiedMinutes = uiState.weeklyStudiedMinutes,
                goalMinutes = uiState.weeklyGoalMinutes.toLong(),
                progress = uiState.weeklyProgress
            )

            // ---- 目标设置区 ----
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "目标设置",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // 目标院校
                    GoalTextField(
                        label = "目标院校",
                        value = uiState.targetSchool,
                        placeholder = "如：北京大学",
                        enabled = uiState.isEditing,
                        onValueChange = viewModel::updateTargetSchool,
                        leadingIcon = Icons.Default.School
                    )

                    // 目标专业
                    GoalTextField(
                        label = "目标专业",
                        value = uiState.targetMajor,
                        placeholder = "如：计算机科学与技术",
                        enabled = uiState.isEditing,
                        onValueChange = viewModel::updateTargetMajor,
                        leadingIcon = Icons.Default.AutoStories
                    )

                    // 考试日期
                    OutlinedTextField(
                        value = if (uiState.examDate.isBlank()) "" else DateUtils.formatFullDate(uiState.examDate),
                        onValueChange = {},
                        label = { Text("考试日期") },
                        placeholder = { Text("点击选择日期") },
                        leadingIcon = {
                            Icon(Icons.Default.Event, contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingIcon = {
                            if (uiState.isEditing) {
                                IconButton(onClick = {
                                    val cal = if (uiState.examDate.isNotBlank())
                                        DateUtils.parseDate(uiState.examDate) ?: Calendar.getInstance()
                                    else Calendar.getInstance()
                                    DatePickerDialog(
                                        context,
                                        { _, y, m, d ->
                                            viewModel.updateExamDate("%04d-%02d-%02d".format(y, m + 1, d))
                                        },
                                        cal.get(Calendar.YEAR),
                                        cal.get(Calendar.MONTH),
                                        cal.get(Calendar.DAY_OF_MONTH)
                                    ).apply {
                                        // 最早可选今天
                                        datePicker.minDate = System.currentTimeMillis()
                                    }.show()
                                }) {
                                    Icon(Icons.Default.DateRange, contentDescription = "选择日期")
                                }
                            }
                        },
                        readOnly = true,
                        enabled = uiState.isEditing,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // 每周目标（分钟）
                    var weeklyGoalText by remember(uiState.weeklyGoalMinutes) {
                        mutableStateOf(if (uiState.weeklyGoalMinutes > 0) uiState.weeklyGoalMinutes.toString() else "")
                    }
                    OutlinedTextField(
                        value = weeklyGoalText,
                        onValueChange = { v ->
                            weeklyGoalText = v.filter { it.isDigit() }.take(4)
                            viewModel.updateWeeklyGoal(weeklyGoalText.toIntOrNull() ?: 0)
                        },
                        label = { Text("每周学习目标（分钟）") },
                        placeholder = { Text("如：1200（即20小时）") },
                        leadingIcon = {
                            Icon(Icons.Default.Timer, contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        enabled = uiState.isEditing,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }
            }

            // ---- 编辑模式底部保存按钮 ----
            if (uiState.isEditing) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = viewModel::cancelEditing,
                        modifier = Modifier.weight(1f)
                    ) { Text("取消") }
                    Button(
                        onClick = viewModel::save,
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isSaving
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("保存目标")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/** 倒计时卡片 */
@Composable
private fun CountdownCard(
    daysToExam: Int,
    examDate: String,
    targetSchool: String,
    targetMajor: String
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (targetSchool.isNotBlank() || targetMajor.isNotBlank()) {
                Text(
                    text = listOf(targetSchool, targetMajor).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (daysToExam > 0) {
                Text(
                    text = "距离考研",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    text = "$daysToExam",
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "天",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                if (examDate.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = DateUtils.formatFullDate(examDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    )
                }
            } else {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (daysToExam == 0) "今天就是考试日！加油！"
                    else if (examDate.isBlank()) "请设置考试日期"
                    else "考试日期已过，继续加油！",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

/** 本周进度卡片 */
@Composable
private fun WeeklyProgressCard(
    studiedMinutes: Long,
    goalMinutes: Long,
    progress: Int
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "本周学习进度",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "$progress%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (progress >= 100) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (progress / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "已学习：${formatMinutes(studiedMinutes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (goalMinutes > 0) "目标：${formatMinutes(goalMinutes)}" else "未设置目标",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** 目标文本输入框 */
@Composable
private fun GoalTextField(
    label: String,
    value: String,
    placeholder: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodySmall) },
        leadingIcon = {
            Icon(leadingIcon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        singleLine = true
    )
}

private fun formatMinutes(minutes: Long): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h > 0 && m > 0 -> "${h}小时${m}分钟"
        h > 0 -> "${h}小时"
        else -> "${m}分钟"
    }
}

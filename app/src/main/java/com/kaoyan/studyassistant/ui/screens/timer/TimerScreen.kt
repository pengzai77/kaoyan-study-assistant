package com.kaoyan.studyassistant.ui.screens.timer
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kaoyan.studyassistant.data.datastore.AppPreferences
import com.kaoyan.studyassistant.data.local.entity.Subject
import com.kaoyan.studyassistant.service.PomodoroPhase as ServicePomodoroPhase
import com.kaoyan.studyassistant.service.TimerRunState
import com.kaoyan.studyassistant.ui.components.AnimatedPomodoroDisplay
import com.kaoyan.studyassistant.ui.components.AnimatedTimerDisplay
import com.kaoyan.studyassistant.ui.components.KaoyanTopBar
import com.kaoyan.studyassistant.ui.components.rememberParsedColor
import com.kaoyan.studyassistant.util.DateUtils
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
// ─────────────────────────────────────────────
// 枚举：计时模式 / 番茄钟阶段（本地 UI 枚举）
// ─────────────────────────────────────────────
private enum class TimerMode { STOPWATCH, POMODORO }
private enum class PomodoroPhase { FOCUS, BREAK }
// 番茄钟预设数据类（UI 层私有，避免与 TimerViewModel.PomodoroPreset 冲突）
private data class UiPomodoroPreset(
    val focusMinutes: Int,
    val breakMinutes: Int,
    val label: String
)
private val UI_POMODORO_PRESETS = listOf(
    UiPomodoroPreset(25, 5, "25/5 分钟"),
    UiPomodoroPreset(50, 10, "50/10 分钟"),
    UiPomodoroPreset(90, 20, "90/20 分钟")
)
// ─────────────────────────────────────────────
// 主页面
// ─────────────────────────────────────────────
@Composable
fun TimerScreen(
    navController: NavController,
    viewModel: TimerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val appPreferences = viewModel.appPreferences
    val userSettings by appPreferences.userSettings.collectAsStateWithLifecycle(
        initialValue = AppPreferences.UserSettings()
    )
    // ── 模式状态（UI 层保留，不影响 Service）──
    var timerMode by rememberSaveable { mutableStateOf(TimerMode.STOPWATCH.name) }
    // ── 番茄钟输入框状态（仅 UI 输入，不影响 Service）──
    var pomodoroFocusInput by rememberSaveable { mutableStateOf("25") }
    var pomodoroBreakInput by rememberSaveable { mutableStateOf("5") }
    // ── 从 ViewModel 读取番茄钟运行状态（Service 驱动）──
    val pomodoroFocusMinutes = uiState.pomodoroFocusMinutes
    val pomodoroBreakMinutes = uiState.pomodoroBreakMinutes
    val pomodoroRunning = uiState.pomodoroRunState == TimerRunState.RUNNING
    val pomodoroPaused = uiState.pomodoroRunState == TimerRunState.PAUSED
    val pomodoroCompletedFocusCount = uiState.pomodoroCompletedCount
    // 将 service.PomodoroPhase 转换为本地 UI 枚举
    val currentPomodoroPhase = when (uiState.pomodoroPhase) {
        ServicePomodoroPhase.FOCUS -> PomodoroPhase.FOCUS
        ServicePomodoroPhase.BREAK -> PomodoroPhase.BREAK
    }
    val currentTimerMode = TimerMode.valueOf(timerMode)
    val pomodoroBusy = pomodoroRunning || pomodoroPaused
    val stopwatchBusy = uiState.timerState != TimerRunState.IDLE
    val modeSwitchEnabled = !pomodoroBusy && !stopwatchBusy
    // ── 错误 / 成功 Snackbar ──
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }
    LaunchedEffect(uiState.savedSuccess) {
        if (uiState.savedSuccess) {
            snackbarHostState.showSnackbar("学习记录已保存 ✓")
            viewModel.clearSuccess()
        }
    }
    // ── 番茄钟专注完成通知（Service 已自动切换阶段，UI 只需显示 Snackbar）──
    LaunchedEffect(uiState.pomodoroPhase, pomodoroRunning) {
        // 当阶段切换到 BREAK 且正在运行时，说明刚完成了专注
        // Service 会通过 pomodoroPendingArchive 触发保存弹窗，这里只显示提示
    }
    Scaffold(
        topBar = {
            KaoyanTopBar(
                title = "学习计时",
                onBack = { navController.popBackStack() }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ModeSelector(
                    selectedMode = currentTimerMode,
                    onSelectMode = { timerMode = it.name },
                    enabled = modeSwitchEnabled
                )
            }
            item {
                SubjectSelector(
                    subjects = uiState.subjects,
                    selectedSubject = uiState.selectedSubject,
                    onSelect = viewModel::selectSubject,
                    enabled = uiState.timerState == TimerRunState.IDLE && !pomodoroBusy
                )
            }
            item {
                when (currentTimerMode) {
                    TimerMode.STOPWATCH -> {
                        StopwatchSection(
                            elapsedMillisFlow = viewModel.stopwatchElapsedMillis,
                            timerState = uiState.timerState,
                            showSeconds = userSettings.showSeconds,
                            onStart = viewModel::startTimer,
                            onPause = viewModel::pauseTimer,
                            onResume = viewModel::resumeTimer,
                            onStop = viewModel::stopTimer
                        )
                    }
                    TimerMode.POMODORO -> {
                        PomodoroSection(
                            focusMinutes = pomodoroFocusMinutes,
                            breakMinutes = pomodoroBreakMinutes,
                            focusInput = pomodoroFocusInput,
                            breakInput = pomodoroBreakInput,
                            phase = currentPomodoroPhase,
                            remainingMillisFlow = viewModel.pomodoroRemainingMillis,
                            targetMillisFlow = viewModel.pomodoroTargetMillis,
                            isRunning = pomodoroRunning,
                            isPaused = pomodoroPaused,
                            completedFocusCount = pomodoroCompletedFocusCount,
                            showSeconds = userSettings.showSeconds,
                            onPresetSelect = { focus, rest ->
                                if (!pomodoroBusy) {
                                    pomodoroFocusInput = focus.toString()
                                    pomodoroBreakInput = rest.toString()
                                    val idx = UI_POMODORO_PRESETS.indexOfFirst {
                                        it.focusMinutes == focus && it.breakMinutes == rest
                                    }
                                    if (idx >= 0) {
                                        viewModel.selectPreset(idx)
                                    } else {
                                        viewModel.applyCustomPomodoro(focus, rest)
                                    }
                                } else {
                                    scope.launch { snackbarHostState.showSnackbar("请先重置番茄钟再修改配置") }
                                }
                            },
                            onFocusInputChange = { pomodoroFocusInput = it },
                            onBreakInputChange = { pomodoroBreakInput = it },
                            onApplyCustom = {
                                if (!pomodoroBusy) {
                                    val focus = pomodoroFocusInput.toIntOrNull()
                                    val rest = pomodoroBreakInput.toIntOrNull()
                                    when {
                                        focus == null || rest == null -> scope.launch {
                                            snackbarHostState.showSnackbar("请输入有效的专注和休息分钟数")
                                        }
                                        focus !in 1..300 -> scope.launch {
                                            snackbarHostState.showSnackbar("专注时长请设置在 1～300 分钟之间")
                                        }
                                        rest !in 1..180 -> scope.launch {
                                            snackbarHostState.showSnackbar("休息时长请设置在 1～180 分钟之间")
                                        }
                                        else -> {
                                            val err = viewModel.applyCustomPomodoro(focus, rest)
                                            if (err != null) {
                                                scope.launch { snackbarHostState.showSnackbar(err) }
                                            } else {
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("已应用：专注 $focus 分钟，休息 $rest 分钟")
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    scope.launch { snackbarHostState.showSnackbar("请先重置番茄钟再修改配置") }
                                }
                            },
                            onStart = {
                                if (pomodoroPaused) viewModel.resumePomodoro()
                                else viewModel.startPomodoro()
                            },
                            onPause = viewModel::pausePomodoro,
                            onReset = viewModel::resetPomodoro
                        )
                    }
                }
            }
            item {
                TodayStatCard(totalMillis = uiState.todayTotalMillis)
            }
        }
    }
    // 保存对话框
    if (uiState.showSaveDialog) {
        SaveSessionDialog(
            title = uiState.saveDialogTitle,
            elapsedMillis = uiState.saveDialogDurationMillis,
            subjects = uiState.subjects,
            selectedSubject = uiState.selectedSubject,
            note = uiState.note,
            isSaving = uiState.isSaving,
            onSubjectSelect = viewModel::selectSubject,
            onNoteChange = viewModel::updateNote,
            onSave = viewModel::saveSession,
            onDiscard = viewModel::discardSession
        )
    }
}

// ─────────────────────────────────────────────
// ─────────────────────────────────────────────
// 模式切换区
// ─────────────────────────────────────────────
@Composable
private fun ModeSelector(
    selectedMode: TimerMode,
    onSelectMode: (TimerMode) -> Unit,
    enabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "计时模式",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(
                    selected = selectedMode == TimerMode.STOPWATCH,
                    onClick = { if (enabled) onSelectMode(TimerMode.STOPWATCH) },
                    enabled = enabled,
                    label = { Text("正计时") },
                    leadingIcon = {
                        Icon(Icons.Default.Timelapse, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                )
                FilterChip(
                    selected = selectedMode == TimerMode.POMODORO,
                    onClick = { if (enabled) onSelectMode(TimerMode.POMODORO) },
                    enabled = enabled,
                    label = { Text("番茄钟") },
                    leadingIcon = {
                        Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                )
            }
            if (!enabled) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "当前有计时任务进行中，结束后才能切换模式",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// 科目选择区
// ─────────────────────────────────────────────
@Composable
private fun SubjectSelector(
    subjects: List<Subject>,
    selectedSubject: Subject?,
    onSelect: (Subject) -> Unit,
    enabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "选择科目",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(
                    items = subjects,
                    key = { subject -> subject.id }
                ) { subject ->
                    val isSelected = subject.id == selectedSubject?.id
                    val subjectColor = rememberParsedColor(subject.color)
                    FilterChip(
                        selected = isSelected,
                        onClick = { if (enabled) onSelect(subject) },
                        label = { Text(subject.name) },
                        enabled = enabled,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = subjectColor.copy(alpha = 0.2f),
                            selectedLabelColor = subjectColor
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = enabled,
                            selected = isSelected,
                            selectedBorderColor = subjectColor,
                            borderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// 普通正计时区域（封装为独立 Section）
// ─────────────────────────────────────────────
@Composable
private fun StopwatchSection(
    elapsedMillisFlow: StateFlow<Long>,
    timerState: TimerRunState,
    showSeconds: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    val elapsedMillis by elapsedMillisFlow.collectAsStateWithLifecycle()
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(999.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.Timelapse,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "正计时",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                StopwatchDisplay(
                    elapsedMillis = elapsedMillis,
                    timerState = timerState,
                    showSeconds = showSeconds
                )
            }
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                StopwatchControls(
                    timerState = timerState,
                    onStart = onStart,
                    onPause = onPause,
                    onResume = onResume,
                    onStop = onStop
                )
            }
        }
    }
}

@Composable
private fun StopwatchDisplay(elapsedMillis: Long, timerState: TimerRunState, showSeconds: Boolean = true) {
    // 使用带动画的计时器显示组件
    // - 数字变化时有翻页动画
    // - 状态切换时颜色平滑过渡
    // - 运行中有轻微脉冲效果
    AnimatedTimerDisplay(
        elapsedMillis = elapsedMillis,
        timerState = timerState,
        showSeconds = showSeconds,
        size = 200.dp,
        fontSize = 38
    )
}

@Composable
private fun StopwatchControls(
    timerState: TimerRunState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (timerState) {
            TimerRunState.IDLE -> {
                Button(
                    onClick = onStart,
                    modifier = Modifier.size(72.dp),
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "开始", modifier = Modifier.size(32.dp))
                }
            }
            TimerRunState.RUNNING -> {
                OutlinedButton(
                    onClick = onPause,
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.Pause, contentDescription = "暂停", modifier = Modifier.size(28.dp))
                }
                Button(
                    onClick = onStop,
                    modifier = Modifier.size(72.dp),
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "结束", modifier = Modifier.size(32.dp))
                }
            }
            TimerRunState.PAUSED -> {
                Button(
                    onClick = onResume,
                    modifier = Modifier.size(72.dp),
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "继续", modifier = Modifier.size(32.dp))
                }
                OutlinedButton(
                    onClick = onStop,
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "结束", modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// 番茄钟完整区域
// ─────────────────────────────────────────────
@Composable
private fun PomodoroSection(
    focusMinutes: Int,
    breakMinutes: Int,
    focusInput: String,
    breakInput: String,
    phase: PomodoroPhase,
    remainingMillisFlow: StateFlow<Long>,
    targetMillisFlow: StateFlow<Long>,
    isRunning: Boolean,
    isPaused: Boolean,
    completedFocusCount: Int,
    showSeconds: Boolean,
    onPresetSelect: (Int, Int) -> Unit,
    onFocusInputChange: (String) -> Unit,
    onBreakInputChange: (String) -> Unit,
    onApplyCustom: () -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit
) {
    val remainingMillis by remainingMillisFlow.collectAsStateWithLifecycle()
    val targetMillis by targetMillisFlow.collectAsStateWithLifecycle()
    val pomodoroBusy = isRunning || isPaused
    val phaseColor = if (phase == PomodoroPhase.FOCUS) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.tertiary
    }
    val phaseLabel = if (phase == PomodoroPhase.FOCUS) "专注中" else "休息中"
    val progress = if (targetMillis > 0L) {
        1f - (remainingMillis.toFloat() / targetMillis.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── A. 预设选择区 ──
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "预设时长",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    UI_POMODORO_PRESETS.forEach { preset ->
                        val selected = preset.focusMinutes == focusMinutes && preset.breakMinutes == breakMinutes
                        FilterChip(
                            selected = selected,
                            onClick = { onPresetSelect(preset.focusMinutes, preset.breakMinutes) },
                            enabled = !pomodoroBusy,
                            label = { Text(preset.label, style = MaterialTheme.typography.bodySmall) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // ── B. 自定义输入区 ──
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "自定义时长",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "专注 1～300 分钟 / 休息 1～180 分钟",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = focusInput,
                        onValueChange = onFocusInputChange,
                        modifier = Modifier.weight(1f),
                        enabled = !pomodoroBusy,
                        singleLine = true,
                        label = { Text("专注") },
                        suffix = { Text("分钟") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = breakInput,
                        onValueChange = onBreakInputChange,
                        modifier = Modifier.weight(1f),
                        enabled = !pomodoroBusy,
                        singleLine = true,
                        label = { Text("休息") },
                        suffix = { Text("分钟") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onApplyCustom,
                    enabled = !pomodoroBusy,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("应用自定义倒计时")
                }
            }
        }

        // ── C. 当前状态显示区 + 倒计时主显示区 ──
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 倒计时大数字（带翻页动画）+ 阶段标签（带淡入淡出）
                AnimatedPomodoroDisplay(
                    remainingMillis = remainingMillis,
                    targetMillis = targetMillis,
                    phaseLabel = phaseLabel,
                    phaseColor = phaseColor,
                    showSeconds = showSeconds
                )

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = phaseColor,
                    trackColor = phaseColor.copy(alpha = 0.15f)
                )

                // 当前配置信息
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "专注 $focusMinutes 分钟 / 休息 $breakMinutes 分钟",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "已完成 $completedFocusCount 番茄",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // ── D. 操作按钮区 ──
                PomodoroControls(
                    isRunning = isRunning,
                    isPaused = isPaused,
                    onStart = onStart,
                    onPause = onPause,
                    onReset = onReset
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// 番茄钟操作按钮
// ─────────────────────────────────────────────
@Composable
private fun PomodoroControls(
    isRunning: Boolean,
    isPaused: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when {
            isRunning -> {
                // 运行中：暂停 + 重置
                OutlinedButton(
                    onClick = onPause,
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.Pause, contentDescription = "暂停", modifier = Modifier.size(28.dp))
                }
                Button(
                    onClick = onReset,
                    modifier = Modifier.size(72.dp),
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "重置", modifier = Modifier.size(30.dp))
                }
            }
            isPaused -> {
                // 已暂停：继续 + 重置
                Button(
                    onClick = onStart,
                    modifier = Modifier.size(72.dp),
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "继续", modifier = Modifier.size(32.dp))
                }
                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "重置", modifier = Modifier.size(28.dp))
                }
            }
            else -> {
                // 空闲：开始 + 重置
                Button(
                    onClick = onStart,
                    modifier = Modifier.size(72.dp),
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "开始", modifier = Modifier.size(32.dp))
                }
                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "重置", modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// 今日统计卡片
// ─────────────────────────────────────────────
@Composable
private fun TodayStatCard(totalMillis: Long) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.BarChart,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "今日累计学习",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = if (totalMillis > 0) DateUtils.formatDuration(totalMillis) else "0 分钟",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// ─────────────────────────────────────────────
// 保存记录对话框
// ─────────────────────────────────────────────
@Composable
private fun SaveSessionDialog(
    title: String,
    elapsedMillis: Long,
    subjects: List<Subject>,
    selectedSubject: Subject?,
    note: String,
    isSaving: Boolean,
    onSubjectSelect: (Subject) -> Unit,
    onNoteChange: (String) -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit
) {
    val onCancel: () -> Unit = {
        if (!isSaving) {
            onDiscard()
        }
    }
    AlertDialog(
        onDismissRequest = {
            if (!isSaving) {
                onSave()
            }
        },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "本次学习时长：${DateUtils.formatDuration(elapsedMillis)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text("选择科目：", style = MaterialTheme.typography.bodySmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(
                        items = subjects,
                        key = { subject -> subject.id }
                    ) { subject ->
                        val isSelected = subject.id == selectedSubject?.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSubjectSelect(subject) },
                            label = { Text(subject.name, style = MaterialTheme.typography.bodySmall) }
                        )
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = onNoteChange,
                    label = { Text("备注（可选）") },
                    placeholder = { Text("记录一下今天学了什么...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = onSave, enabled = !isSaving) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("保存")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("丢弃") }
        }
    )
}

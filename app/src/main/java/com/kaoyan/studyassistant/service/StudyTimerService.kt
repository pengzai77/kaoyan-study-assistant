package com.kaoyan.studyassistant.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.kaoyan.studyassistant.MainActivity
import com.kaoyan.studyassistant.R
import com.kaoyan.studyassistant.data.datastore.AppPreferences
import com.kaoyan.studyassistant.util.DateUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * 学习计时前台服务（v5 番茄钟后台计时版）
 *
 * 支持两种计时模式（互斥，不能同时运行）：
 * 1. 普通正计时：ACTION_START / ACTION_PAUSE / ACTION_RESUME / ACTION_STOP
 * 2. 番茄钟模式：ACTION_POMO_START / ACTION_POMO_PAUSE / ACTION_POMO_RESUME / ACTION_POMO_RESET
 *
 * 番茄钟专注完成后，通过 TimerServiceState.pomodoroPendingArchive 通知 ViewModel 弹出保存弹窗。
 */
@AndroidEntryPoint
class StudyTimerService : Service() {

    @Inject
    lateinit var appPreferences: AppPreferences

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var tickJob: Job? = null
    private var pomodoroTickJob: Job? = null
    private var pomodoroDeadlineElapsedRealtime: Long = 0L

    @Volatile private var showSeconds: Boolean = true
    @Volatile private var enableVibration: Boolean = false

    companion object {
        const val CHANNEL_ID = "study_timer_channel"
        const val NOTIFICATION_ID = 1001

        // 普通计时 Actions
        const val ACTION_START = "action_start"
        const val ACTION_PAUSE = "action_pause"
        const val ACTION_RESUME = "action_resume"
        const val ACTION_STOP = "action_stop"

        // 番茄钟 Actions
        const val ACTION_POMO_START = "action_pomo_start"
        const val ACTION_POMO_PAUSE = "action_pomo_pause"
        const val ACTION_POMO_RESUME = "action_pomo_resume"
        const val ACTION_POMO_RESET = "action_pomo_reset"

        const val EXTRA_SUBJECT_ID = "extra_subject_id"
        const val EXTRA_SUBJECT_NAME = "extra_subject_name"
        const val EXTRA_POMO_FOCUS_MIN = "extra_pomo_focus_min"
        const val EXTRA_POMO_BREAK_MIN = "extra_pomo_break_min"

        // ── 普通计时 Intent 构建 ──
        fun buildStartIntent(context: Context, subjectId: Long, subjectName: String): Intent =
            Intent(context, StudyTimerService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_SUBJECT_ID, subjectId)
                putExtra(EXTRA_SUBJECT_NAME, subjectName)
            }
        fun buildPauseIntent(context: Context): Intent =
            Intent(context, StudyTimerService::class.java).apply { action = ACTION_PAUSE }
        fun buildResumeIntent(context: Context): Intent =
            Intent(context, StudyTimerService::class.java).apply { action = ACTION_RESUME }
        fun buildStopIntent(context: Context): Intent =
            Intent(context, StudyTimerService::class.java).apply { action = ACTION_STOP }

        // ── 番茄钟 Intent 构建 ──
        fun buildPomoStartIntent(context: Context, focusMin: Int, breakMin: Int): Intent =
            Intent(context, StudyTimerService::class.java).apply {
                action = ACTION_POMO_START
                putExtra(EXTRA_POMO_FOCUS_MIN, focusMin)
                putExtra(EXTRA_POMO_BREAK_MIN, breakMin)
            }
        fun buildPomoPauseIntent(context: Context): Intent =
            Intent(context, StudyTimerService::class.java).apply { action = ACTION_POMO_PAUSE }
        fun buildPomoResumeIntent(context: Context): Intent =
            Intent(context, StudyTimerService::class.java).apply { action = ACTION_POMO_RESUME }
        fun buildPomoResetIntent(context: Context): Intent =
            Intent(context, StudyTimerService::class.java).apply { action = ACTION_POMO_RESET }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        serviceScope.launch {
            appPreferences.userSettings.collect { settings ->
                showSeconds = settings.showSeconds
                enableVibration = settings.enableVibration
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            // ── 普通计时 ──
            ACTION_START -> {
                val subjectId = intent.getLongExtra(EXTRA_SUBJECT_ID, 0L)
                val subjectName = intent.getStringExtra(EXTRA_SUBJECT_NAME) ?: ""
                handleStart(subjectId, subjectName)
            }
            ACTION_PAUSE -> handlePause()
            ACTION_RESUME -> handleResume()
            ACTION_STOP -> handleStop()

            // ── 番茄钟 ──
            ACTION_POMO_START -> {
                val focusMin = intent.getIntExtra(EXTRA_POMO_FOCUS_MIN, 25)
                val breakMin = intent.getIntExtra(EXTRA_POMO_BREAK_MIN, 5)
                handlePomoStart(focusMin, breakMin)
            }
            ACTION_POMO_PAUSE -> handlePomoPause()
            ACTION_POMO_RESUME -> handlePomoResume()
            ACTION_POMO_RESET -> handlePomoReset()

            else -> {
                // 服务被系统重启（START_STICKY），尝试从 DataStore 恢复
                serviceScope.launch { tryRestoreFromDataStore() }
            }
        }
        return START_STICKY
    }

    // ════════════════════════════════════════════
    // 普通计时逻辑
    // ════════════════════════════════════════════

    private fun handleStart(subjectId: Long, subjectName: String) {
        // 如果番茄钟正在运行，先停止
        if (TimerServiceState.pomodoroRunState.value == TimerRunState.RUNNING ||
            TimerServiceState.pomodoroRunState.value == TimerRunState.PAUSED) {
            handlePomoReset()
        }
        val now = System.currentTimeMillis()
        TimerServiceState.updateSubject(subjectId, subjectName)
        TimerServiceState.updateStartTime(now)
        TimerServiceState.updateOriginalStartTime(now)
        TimerServiceState.updatePausedElapsed(0L)
        TimerServiceState.updateRunState(TimerRunState.RUNNING)
        TimerServiceState.updateElapsedMillis(0L)
        serviceScope.launch {
            appPreferences.saveTimerState(
                AppPreferences.TimerState(
                    isRunning = true,
                    isPaused = false,
                    startTime = now,
                    pausedElapsed = 0L,
                    subjectId = subjectId,
                    subjectName = subjectName,
                    originalStartTime = now
                )
            )
        }
        startForegroundWithNotification(subjectName, 0L)
        startTicking()
    }

    private fun handlePause() {
        if (TimerServiceState.runState.value != TimerRunState.RUNNING) return
        tickJob?.cancel()
        val elapsed = computeElapsed()
        TimerServiceState.updatePausedElapsed(elapsed)
        TimerServiceState.updateRunState(TimerRunState.PAUSED)
        TimerServiceState.updateElapsedMillis(elapsed)
        val subjectName = TimerServiceState.subjectName.value
        updateNotification(subjectName, elapsed, paused = true)
        serviceScope.launch {
            val current = appPreferences.timerState.first()
            appPreferences.saveTimerState(
                current.copy(isPaused = true, pausedElapsed = elapsed)
            )
        }
    }

    private fun handleResume() {
        if (TimerServiceState.runState.value != TimerRunState.PAUSED) return
        val now = System.currentTimeMillis()
        TimerServiceState.updateStartTime(now)
        TimerServiceState.updateRunState(TimerRunState.RUNNING)
        val subjectName = TimerServiceState.subjectName.value
        val elapsed = TimerServiceState.pausedElapsed.value
        startForegroundWithNotification(subjectName, elapsed)
        startTicking()
        serviceScope.launch {
            val current = appPreferences.timerState.first()
            appPreferences.saveTimerState(
                current.copy(isRunning = true, isPaused = false, startTime = now)
            )
        }
    }

    private fun handleStop() {
        tickJob?.cancel()
        if (enableVibration) vibrate()
        TimerServiceState.reset()
        serviceScope.launch { appPreferences.clearTimerState() }
        // 检查番茄钟是否也停止了，如果都停止则停止前台服务
        if (TimerServiceState.pomodoroRunState.value == TimerRunState.IDLE) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun startTicking() {
        tickJob?.cancel()
        tickJob = serviceScope.launch {
            var tickCount = 0
            while (isActive) {
                val elapsed = computeElapsed()
                TimerServiceState.updateElapsedMillis(elapsed)
                // 通知每 5 秒刷新一次，减少系统开销
                if (tickCount % 5 == 0) {
                    updateNotification(TimerServiceState.subjectName.value, elapsed)
                }
                tickCount++
                delay(1000L)
            }
        }
    }

    private fun computeElapsed(): Long {
        val running = System.currentTimeMillis() - TimerServiceState.startTimeMs.value
        return running.coerceAtLeast(0L) + TimerServiceState.pausedElapsed.value
    }

    // ════════════════════════════════════════════
    // 番茄钟逻辑
    // ════════════════════════════════════════════

    private fun handlePomoStart(focusMin: Int, breakMin: Int) {
        // 如果普通计时正在运行，先停止
        if (TimerServiceState.runState.value == TimerRunState.RUNNING ||
            TimerServiceState.runState.value == TimerRunState.PAUSED) {
            handleStop()
        }
        val focusMs = focusMin * 60_000L
        val now = System.currentTimeMillis()

        TimerServiceState.updatePomodoroFocusMinutes(focusMin)
        TimerServiceState.updatePomodoroBreakMinutes(breakMin)
        TimerServiceState.updatePomodoroPhase(PomodoroPhase.FOCUS)
        TimerServiceState.updatePomodoroTargetMillis(focusMs)
        TimerServiceState.updatePomodoroRemainingMillis(focusMs)
        TimerServiceState.updatePomodoroRunState(TimerRunState.RUNNING)
        TimerServiceState.updatePomodoroFocusStartedAt(now)
        TimerServiceState.updatePomodoroPendingArchive(null)

        val endAt = now + focusMs
        pomodoroDeadlineElapsedRealtime = SystemClock.elapsedRealtime() + focusMs
        serviceScope.launch {
            appPreferences.savePomodoroState(
                AppPreferences.PomodoroState(
                    isRunning = true, isPaused = false,
                    phase = "FOCUS",
                    focusMinutes = focusMin, breakMinutes = breakMin,
                    completedCount = TimerServiceState.pomodoroCompletedCount.value,
                    endAtMillis = endAt,
                    remainingMillis = focusMs,
                    focusStartedAt = now
                )
            )
        }
        startPomoForeground()
        startPomoTicking()
    }

    private fun handlePomoPause() {
        if (TimerServiceState.pomodoroRunState.value != TimerRunState.RUNNING) return
        pomodoroTickJob?.cancel()
        val remaining = TimerServiceState.pomodoroRemainingMillis.value
        TimerServiceState.updatePomodoroRunState(TimerRunState.PAUSED)
        updatePomoNotification()
        serviceScope.launch {
            val current = appPreferences.pomodoroState.first()
            appPreferences.savePomodoroState(
                current.copy(isPaused = true, isRunning = false, remainingMillis = remaining)
            )
        }
    }

    private fun handlePomoResume() {
        if (TimerServiceState.pomodoroRunState.value != TimerRunState.PAUSED) return
        val remaining = TimerServiceState.pomodoroRemainingMillis.value
        val newEndAt = System.currentTimeMillis() + remaining
        pomodoroDeadlineElapsedRealtime = SystemClock.elapsedRealtime() + remaining
        TimerServiceState.updatePomodoroRunState(TimerRunState.RUNNING)
        updatePomoNotification()
        serviceScope.launch {
            val current = appPreferences.pomodoroState.first()
            appPreferences.savePomodoroState(
                current.copy(isPaused = false, isRunning = true, endAtMillis = newEndAt, remainingMillis = remaining)
            )
        }
        startPomoTicking()
    }

    private fun handlePomoReset() {
        pomodoroTickJob?.cancel()
        pomodoroDeadlineElapsedRealtime = 0L
        TimerServiceState.resetPomodoro()
        serviceScope.launch { appPreferences.clearPomodoroState() }
        // 如果普通计时也没在运行，停止前台服务
        if (TimerServiceState.runState.value == TimerRunState.IDLE) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun startPomoTicking() {
        pomodoroTickJob?.cancel()
        if (pomodoroDeadlineElapsedRealtime <= 0L) {
            val fallbackRemaining = TimerServiceState.pomodoroRemainingMillis.value.coerceAtLeast(0L)
            pomodoroDeadlineElapsedRealtime = SystemClock.elapsedRealtime() + fallbackRemaining
        }
        pomodoroTickJob = serviceScope.launch {
            var tickCount = 0
            while (isActive) {
                val remaining = (pomodoroDeadlineElapsedRealtime - SystemClock.elapsedRealtime())
                    .coerceAtLeast(0L)
                TimerServiceState.updatePomodoroRemainingMillis(remaining)
                if (remaining <= 0L) {
                    onPomoPhaseComplete()
                    break
                }
                if (tickCount % 5 == 0) {
                    updatePomoNotification()
                }
                if (tickCount % 30 == 0) {
                    persistPomoRemaining(remaining)
                }
                tickCount++
                delay(1000L)
            }
        }
    }

    private fun onPomoPhaseComplete() {
        if (enableVibration) vibrate()
        val phase = TimerServiceState.pomodoroPhase.value
        if (phase == PomodoroPhase.FOCUS) {
            // 专注完成 → 生成待归档快照，切换到休息阶段
            val focusStartedAt = TimerServiceState.pomodoroFocusStartedAt.value
            val now = System.currentTimeMillis()
            val focusMs = TimerServiceState.pomodoroFocusMinutes.value * 60_000L
            TimerServiceState.updatePomodoroPendingArchive(
                PomodoroArchiveSnapshot(
                    elapsedMillis = focusMs,
                    archiveStartTime = focusStartedAt,
                    archiveEndTime = now
                )
            )
            val newCount = TimerServiceState.pomodoroCompletedCount.value + 1
            TimerServiceState.updatePomodoroCompletedCount(newCount)

            // 切换到休息阶段
            val breakMs = TimerServiceState.pomodoroBreakMinutes.value * 60_000L
            TimerServiceState.updatePomodoroPhase(PomodoroPhase.BREAK)
            TimerServiceState.updatePomodoroTargetMillis(breakMs)
            TimerServiceState.updatePomodoroRemainingMillis(breakMs)
            TimerServiceState.updatePomodoroRunState(TimerRunState.RUNNING)

            val newEndAt = System.currentTimeMillis() + breakMs
            pomodoroDeadlineElapsedRealtime = SystemClock.elapsedRealtime() + breakMs
            serviceScope.launch {
                appPreferences.savePomodoroState(
                    AppPreferences.PomodoroState(
                        isRunning = true, isPaused = false,
                        phase = "BREAK",
                        focusMinutes = TimerServiceState.pomodoroFocusMinutes.value,
                        breakMinutes = TimerServiceState.pomodoroBreakMinutes.value,
                        completedCount = newCount,
                        endAtMillis = newEndAt,
                        remainingMillis = breakMs,
                        focusStartedAt = 0L
                    )
                )
            }
            updatePomoNotification()
            startPomoTicking()
        } else {
            // 休息完成 → 重置回专注阶段（等待用户手动开始下一轮）
            val focusMs = TimerServiceState.pomodoroFocusMinutes.value * 60_000L
            TimerServiceState.updatePomodoroPhase(PomodoroPhase.FOCUS)
            TimerServiceState.updatePomodoroTargetMillis(focusMs)
            TimerServiceState.updatePomodoroRemainingMillis(focusMs)
            TimerServiceState.updatePomodoroRunState(TimerRunState.IDLE)
            pomodoroDeadlineElapsedRealtime = 0L
            updatePomoNotification()
            serviceScope.launch { appPreferences.clearPomodoroState() }
        }
    }

    private fun persistPomoRemaining(remaining: Long) {
        serviceScope.launch {
            try {
                val current = appPreferences.pomodoroState.first()
                if (current.isRunning) {
                    appPreferences.savePomodoroState(current.copy(remainingMillis = remaining))
                }
            } catch (_: Exception) {}
        }
    }

    // ════════════════════════════════════════════
    // DataStore 恢复（进程重启）
    // ════════════════════════════════════════════

    private suspend fun tryRestoreFromDataStore() {
        // 优先恢复普通计时
        val saved = appPreferences.timerState.first()
        if (saved.isRunning) {
            if (saved.isPaused) {
                TimerServiceState.updateSubject(saved.subjectId, saved.subjectName)
                TimerServiceState.updatePausedElapsed(saved.pausedElapsed)
                TimerServiceState.updateOriginalStartTime(saved.originalStartTime)
                TimerServiceState.updateRunState(TimerRunState.PAUSED)
                TimerServiceState.updateElapsedMillis(saved.pausedElapsed)
                startForegroundWithNotification(saved.subjectName, saved.pausedElapsed, paused = true)
            } else {
                val restoredElapsed = (System.currentTimeMillis() - saved.startTime)
                    .coerceAtLeast(0L) + saved.pausedElapsed
                TimerServiceState.updateSubject(saved.subjectId, saved.subjectName)
                TimerServiceState.updateStartTime(saved.startTime)
                TimerServiceState.updateOriginalStartTime(saved.originalStartTime)
                TimerServiceState.updatePausedElapsed(saved.pausedElapsed)
                TimerServiceState.updateRunState(TimerRunState.RUNNING)
                TimerServiceState.updateElapsedMillis(restoredElapsed)
                startForegroundWithNotification(saved.subjectName, restoredElapsed)
                startTicking()
            }
            return
        }

        // 恢复番茄钟
        val pomo = appPreferences.pomodoroState.first()
        if (pomo.isRunning || pomo.isPaused) {
            val phase = if (pomo.phase == "FOCUS") PomodoroPhase.FOCUS else PomodoroPhase.BREAK
            val targetMs = if (phase == PomodoroPhase.FOCUS) pomo.focusMinutes * 60_000L
                           else pomo.breakMinutes * 60_000L
            TimerServiceState.updatePomodoroFocusMinutes(pomo.focusMinutes)
            TimerServiceState.updatePomodoroBreakMinutes(pomo.breakMinutes)
            TimerServiceState.updatePomodoroPhase(phase)
            TimerServiceState.updatePomodoroTargetMillis(targetMs)
            TimerServiceState.updatePomodoroCompletedCount(pomo.completedCount)
            TimerServiceState.updatePomodoroFocusStartedAt(pomo.focusStartedAt)

            if (pomo.isPaused) {
                TimerServiceState.updatePomodoroRemainingMillis(pomo.remainingMillis)
                TimerServiceState.updatePomodoroRunState(TimerRunState.PAUSED)
            } else {
                // 计算实际剩余时间
                val remaining = (pomo.endAtMillis - System.currentTimeMillis()).coerceAtLeast(0L)
                pomodoroDeadlineElapsedRealtime = SystemClock.elapsedRealtime() + remaining
                TimerServiceState.updatePomodoroRemainingMillis(remaining)
                if (remaining > 0L) {
                    TimerServiceState.updatePomodoroRunState(TimerRunState.RUNNING)
                    startPomoTicking()
                } else {
                    // 已经过期，触发阶段完成
                    TimerServiceState.updatePomodoroRemainingMillis(0L)
                    TimerServiceState.updatePomodoroRunState(TimerRunState.RUNNING)
                    onPomoPhaseComplete()
                }
            }
            startPomoForeground()
        } else {
            stopSelf()
        }
    }

    // ════════════════════════════════════════════
    // 震动
    // ════════════════════════════════════════════

    private fun vibrate() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vm = getSystemService(VibratorManager::class.java)
                vm?.defaultVibrator?.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 300, 100, 300, 100, 500), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                val v = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                v?.vibrate(longArrayOf(0, 300, 100, 300, 100, 500), -1)
            }
        } catch (_: Exception) {}
    }

    // ════════════════════════════════════════════
    // 通知
    // ════════════════════════════════════════════

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "学习计时", NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "显示当前学习计时状态"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildOpenAppIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildActionPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, StudyTimerService::class.java).apply { this.action = action }
        return PendingIntent.getService(
            this, action.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** 普通计时通知 */
    private fun startForegroundWithNotification(subjectName: String, elapsed: Long, paused: Boolean = false) {
        startForeground(NOTIFICATION_ID, buildNormalNotification(subjectName, elapsed, paused))
    }

    private fun updateNotification(subjectName: String, elapsed: Long, paused: Boolean = false) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNormalNotification(subjectName, elapsed, paused))
    }

    private fun buildNormalNotification(subjectName: String, elapsed: Long, paused: Boolean): android.app.Notification {
        val timeText = if (showSeconds) DateUtils.formatTimerDisplay(elapsed)
                       else DateUtils.formatTimerDisplayNoSeconds(elapsed)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setContentTitle(if (paused) "⏸ 学习已暂停" else "📚 正在学习：$subjectName")
            .setContentText(if (paused) "已学习 $timeText，点击继续" else "已学习 $timeText")
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(buildOpenAppIntent())
            .addAction(
                android.R.drawable.ic_media_pause,
                if (paused) "继续" else "暂停",
                if (paused) buildActionPendingIntent(ACTION_RESUME)
                else buildActionPendingIntent(ACTION_PAUSE)
            )
            .addAction(
                android.R.drawable.ic_media_next,
                "结束",
                buildActionPendingIntent(ACTION_STOP)
            )
            .build()
    }

    /** 番茄钟通知 */
    private fun startPomoForeground() {
        startForeground(NOTIFICATION_ID, buildPomoNotification())
    }

    private fun updatePomoNotification() {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildPomoNotification())
    }

    private fun buildPomoNotification(): android.app.Notification {
        val phase = TimerServiceState.pomodoroPhase.value
        val remaining = TimerServiceState.pomodoroRemainingMillis.value
        val runState = TimerServiceState.pomodoroRunState.value
        val count = TimerServiceState.pomodoroCompletedCount.value

        val phaseText = if (phase == PomodoroPhase.FOCUS) "🍅 专注中" else "☕ 休息中"
        val remainText = formatPomoTime(remaining)
        val pauseText = if (runState == TimerRunState.PAUSED) "（已暂停）" else ""
        val countText = if (count > 0) "  已完成 $count 轮" else ""

        val (pauseOrResumeLabel, pauseOrResumeAction) = if (runState == TimerRunState.PAUSED)
            "继续" to ACTION_POMO_RESUME
        else
            "暂停" to ACTION_POMO_PAUSE

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setContentTitle("$phaseText$pauseText$countText")
            .setContentText("剩余 $remainText")
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(buildOpenAppIntent())
            .addAction(
                android.R.drawable.ic_media_pause,
                pauseOrResumeLabel,
                buildActionPendingIntent(pauseOrResumeAction)
            )
            .addAction(
                android.R.drawable.ic_delete,
                "重置",
                buildActionPendingIntent(ACTION_POMO_RESET)
            )
            .build()
    }

    private fun formatPomoTime(millis: Long): String {
        val totalSec = (millis / 1000L).coerceAtLeast(0L)
        val min = totalSec / 60
        val sec = totalSec % 60
        return "%02d:%02d".format(min, sec)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}

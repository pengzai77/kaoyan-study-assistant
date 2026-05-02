package com.kaoyan.studyassistant.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.kaoyan.studyassistant.data.local.dao.DateDurationSummary
import com.kaoyan.studyassistant.ui.components.EmptyState
import com.kaoyan.studyassistant.ui.navigation.NavRoutes
import com.kaoyan.studyassistant.util.DateUtils

/**
 * 首页 Dashboard 页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "考研学习助手",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = DateUtils.formatFullDate(DateUtils.today()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(NavRoutes.SETTINGS) }) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ① 考研倒计时横幅（最顶端）
            ExamCountdownBanner(
                countdown = uiState.countdown,
                onSetGoal = { navController.navigate(NavRoutes.GOAL) }
            )

            // ② 今日概览卡片
            TodaySummaryCard(
                totalMillis = uiState.todayTotalMillis,
                subjectCount = uiState.todaySubjectCount
            )

            // ③ 快捷入口（恢复6个，移除考研目标和备份恢复）
            QuickAccessGrid(navController = navController)

            // ④ 近7天学习趋势
            WeekTrendCard(durations = uiState.last7DaysDurations)

            // ⑥ 最近复盘摘要（受 showLatestReview 设置控制）
            if (uiState.showLatestReview) {
                LatestReviewCard(
                    review = uiState.latestReview,
                    onViewAll = { navController.navigate(NavRoutes.REVIEW_LIST) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 考研倒计时横幅
// ─────────────────────────────────────────────────────────────

/**
 * 首页顶部考研倒计时横幅
 *
 * 三种状态：
 * 1. 未设置目标（examDate 为空）→ 提示"去设置"
 * 2. 考试日期已过（daysLeft < 0）→ 提示更新目标
 * 3. 正常倒计时（daysLeft >= 0）→ 大字展示天数
 */
@Composable
private fun ExamCountdownBanner(
    countdown: CountdownState,
    onSetGoal: () -> Unit
) {
    val notSet = countdown.examDate.isBlank() || countdown.daysLeft == Int.MIN_VALUE
    val isPast = !notSet && countdown.daysLeft < 0
    val isToday = !notSet && countdown.daysLeft == 0

    val containerColor = when {
        notSet -> MaterialTheme.colorScheme.surfaceVariant
        isPast -> MaterialTheme.colorScheme.errorContainer
        else   -> MaterialTheme.colorScheme.primaryContainer
    }
    val contentColor = when {
        notSet -> MaterialTheme.colorScheme.onSurfaceVariant
        isPast -> MaterialTheme.colorScheme.onErrorContainer
        else   -> MaterialTheme.colorScheme.onPrimaryContainer
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        when {
            notSet -> {
                // 未设置目标
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "还未设置考研目标",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = contentColor
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "设置考试日期后，这里将显示倒计时",
                            style = MaterialTheme.typography.bodySmall,
                            color = contentColor.copy(alpha = 0.7f)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    FilledTonalButton(
                        onClick = onSetGoal,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("去设置", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            isPast -> {
                // 考试日期已过
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "考试日期已过",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = contentColor
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "请前往考研目标页面更新考试日期",
                            style = MaterialTheme.typography.bodySmall,
                            color = contentColor.copy(alpha = 0.7f)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    FilledTonalButton(
                        onClick = onSetGoal,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("去更新", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            else -> {
                // 正常倒计时
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isToday) "今天就是考试日！" else "距离考研还有",
                            style = MaterialTheme.typography.bodyMedium,
                            color = contentColor.copy(alpha = 0.8f)
                        )
                        if (!isToday) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "${countdown.daysLeft}",
                                    fontSize = 48.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = contentColor,
                                    lineHeight = 52.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "天",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = contentColor,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                        } else {
                            Text(
                                text = "加油！",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = contentColor
                            )
                        }
                        Text(
                            text = "考试日期：${DateUtils.formatFullDate(countdown.examDate)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = contentColor.copy(alpha = 0.6f)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = contentColor.copy(alpha = 0.25f),
                        modifier = Modifier.size(64.dp)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 今日概览卡片
// ─────────────────────────────────────────────────────────────

@Composable
private fun TodaySummaryCard(totalMillis: Long, subjectCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "今日学习",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (totalMillis > 0) DateUtils.formatDuration(totalMillis) else "今天还没开始学习",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (subjectCount > 0) "已学习 $subjectCount 个科目" else "快去开始第一个计时吧 💪",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 快捷入口（6个，聚焦学习功能）
// ─────────────────────────────────────────────────────────────

@Composable
private fun QuickAccessGrid(navController: NavController) {
    val items = listOf(
        Triple("开始计时", Icons.Default.PlayCircle, NavRoutes.TIMER),
        Triple("今日学习", Icons.Default.Today, NavRoutes.TODAY),
        Triple("每日复盘", Icons.Default.EditNote, NavRoutes.REVIEW_LIST),
        Triple("学习统计", Icons.Default.BarChart, NavRoutes.STATISTICS),
        Triple("智能总结", Icons.Default.AutoAwesome, NavRoutes.SUMMARY),
        Triple("科目管理", Icons.Default.Category, NavRoutes.SUBJECT_MANAGE)
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "快捷入口",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        // 第一行：3个
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.take(3).forEach { (label, icon, route) ->
                QuickAccessItem(
                    label = label,
                    icon = icon,
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate(route) }
                )
            }
        }
        // 第二行：3个
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.drop(3).forEach { (label, icon, route) ->
                QuickAccessItem(
                    label = label,
                    icon = icon,
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate(route) }
                )
            }
        }
    }
}

@Composable
private fun QuickAccessItem(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 近7天趋势卡片
// ─────────────────────────────────────────────────────────────

@Composable
private fun WeekTrendCard(durations: List<DateDurationSummary>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "近7天学习趋势",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (durations.isEmpty()) {
                Text(
                    text = "暂无学习记录",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val maxMillis = durations.maxOf { it.totalMillis }.coerceAtLeast(1)
                val last7Days = (6 downTo 0).map { DateUtils.daysAgo(it) }
                val durationMap = durations.associate { it.date to it.totalMillis }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    last7Days.forEach { date ->
                        val millis = durationMap[date] ?: 0L
                        val ratio = millis.toFloat() / maxMillis
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(20.dp)
                                    .height(60.dp),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(20.dp)
                                        .height((60 * ratio).dp.coerceAtLeast(2.dp))
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(
                                            if (millis > 0) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                        )
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = date.substring(8),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 最近复盘摘要卡片
// ─────────────────────────────────────────────────────────────

@Composable
private fun LatestReviewCard(
    review: com.kaoyan.studyassistant.data.local.entity.DailyReview?,
    onViewAll: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "最近复盘",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = onViewAll) {
                    Text("查看全部")
                }
            }

            if (review == null) {
                Text(
                    text = "还没有复盘记录，今天来写一篇吧",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = DateUtils.formatFullDate(review.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                if (review.completedContent.isNotBlank()) {
                    ReviewPreviewBlock(
                        label = "完成内容",
                        content = review.completedContent,
                        accentColor = Color(0xFF2E7D32),
                        icon = Icons.Default.CheckCircle,
                        maxLines = 2
                    )
                }
                if (review.problems.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ReviewPreviewBlock(
                        label = "遇到问题",
                        content = review.problems,
                        accentColor = Color(0xFFF57C00),
                        icon = Icons.Default.ReportProblem,
                        maxLines = 2
                    )
                }
                if (review.tomorrowPlan.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ReviewPreviewBlock(
                        label = "明日计划",
                        content = review.tomorrowPlan,
                        accentColor = Color(0xFF1565C0),
                        icon = Icons.Default.EventNote,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewPreviewBlock(
    label: String,
    content: String,
    accentColor: Color,
    icon: ImageVector,
    maxLines: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(accentColor.copy(alpha = 0.08f))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(accentColor.copy(alpha = 0.14f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = content,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = maxLines
        )
    }
}

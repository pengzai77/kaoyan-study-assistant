package com.kaoyan.studyassistant.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.kaoyan.studyassistant.ui.screens.backup.BackupScreen
import com.kaoyan.studyassistant.ui.screens.dashboard.DashboardScreen
import com.kaoyan.studyassistant.ui.screens.goal.GoalScreen
import com.kaoyan.studyassistant.ui.screens.history.HistoryScreen
import com.kaoyan.studyassistant.ui.screens.history.StatisticsScreen
import com.kaoyan.studyassistant.ui.screens.review.ReviewEditScreen
import com.kaoyan.studyassistant.ui.screens.review.ReviewListScreen
import com.kaoyan.studyassistant.ui.screens.settings.SettingsScreen
import com.kaoyan.studyassistant.ui.screens.subject.SubjectManageScreen
import com.kaoyan.studyassistant.ui.screens.summary.SummaryScreen
import com.kaoyan.studyassistant.ui.screens.timer.TimerScreen
import com.kaoyan.studyassistant.ui.screens.today.TodayScreen

/**
 * 应用主导航图
 *
 * ── 微信风格页面切换动画 ──────────────────────────────────────────────────────
 *
 * 核心特征：
 *   1. 纯滑动，无淡入淡出（fadeIn/fadeOut）
 *   2. 新页面从屏幕右侧 100% 完整滑入
 *   3. 旧页面同步向左移动（约 30% 屏宽），形成"推出"效果
 *   4. 两个页面像绑在一起移动，联动感强
 *   5. 返回时方向相反：当前页面向右滑出，下层页面从左侧滑回
 *
 * 参数设计：
 *   - 时长：350ms（比标准 300ms 稍慢，更从容，符合"慢一点也没关系"的要求）
 *   - 曲线：cubic-bezier(0.25, 0.1, 0.25, 1) 微信实测曲线，开始稍快，结束平滑
 *   - 新页面位移：100% 屏宽（从完全不可见到完全可见）
 *   - 旧页面位移：-30% 屏宽（向左推出，但不完全消失，保留视觉连续性）
 *
 * 效果对比：
 *   ❌ 淡入淡出：页面"闪现"，缺少空间感
 *   ✅ 纯滑动：页面"推进/推出"，空间关系清晰，符合物理直觉
 * ────────────────────────────────────────────────────────────────────────────
 */
@Composable
fun AppNavGraph(navController: NavHostController) {

    // 缩短页面切换时长，减少“拖泥带水”的体感
    val DURATION = 240

    // 微信实测曲线：cubic-bezier(0.25, 0.1, 0.25, 1)
    // 特点：开始稍快（0.25），中段平稳（0.1），结束平滑（0.25, 1）
    val wechatEasing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)

    // ── 向前导航（push）：新页面从右侧完整滑入（100% 屏宽）──────────────────
    val enterTransition: AnimatedContentTransitionScope<*>.() -> EnterTransition = {
        slideInHorizontally(
            initialOffsetX = { fullWidth -> fullWidth },  // 从屏幕右侧外 100% 开始
            animationSpec = tween(durationMillis = DURATION, easing = wechatEasing)
        )
    }

    // ── 向前导航（push）：旧页面向左推出（-30% 屏宽）────────────────────────
    val exitTransition: AnimatedContentTransitionScope<*>.() -> ExitTransition = {
        slideOutHorizontally(
            targetOffsetX = { fullWidth -> -(fullWidth * 0.18f).toInt() },  // 旧页面轻推，减少整体位移成本
            animationSpec = tween(durationMillis = DURATION, easing = wechatEasing)
        )
    }

    // ── 返回（pop）：下层页面从左侧滑回（从 -30% 回到 0）────────────────────
    val popEnterTransition: AnimatedContentTransitionScope<*>.() -> EnterTransition = {
        slideInHorizontally(
            initialOffsetX = { fullWidth -> -(fullWidth * 0.18f).toInt() },  // 从左侧轻推回位
            animationSpec = tween(durationMillis = DURATION, easing = wechatEasing)
        )
    }

    // ── 返回（pop）：当前页面向右滑出（100% 屏宽）──────────────────────────
    val popExitTransition: AnimatedContentTransitionScope<*>.() -> ExitTransition = {
        slideOutHorizontally(
            targetOffsetX = { fullWidth -> fullWidth },  // 向右完全滑出屏幕
            animationSpec = tween(durationMillis = DURATION, easing = wechatEasing)
        )
    }

    NavHost(
        navController = navController,
        startDestination = NavRoutes.DASHBOARD,
        enterTransition = enterTransition,
        exitTransition = exitTransition,
        popEnterTransition = popEnterTransition,
        popExitTransition = popExitTransition
    ) {
        // 首页 Dashboard
        composable(NavRoutes.DASHBOARD) {
            DashboardScreen(navController = navController)
        }
        // 计时器
        composable(NavRoutes.TIMER) {
            TimerScreen(navController = navController)
        }
        // 今日学习
        composable(NavRoutes.TODAY) {
            TodayScreen(navController = navController)
        }
        // 历史记录
        composable(NavRoutes.HISTORY) {
            HistoryScreen(navController = navController)
        }
        // 学习统计
        composable(NavRoutes.STATISTICS) {
            StatisticsScreen(navController = navController)
        }
        // 复盘列表
        composable(NavRoutes.REVIEW_LIST) {
            ReviewListScreen(navController = navController)
        }
        // 复盘编辑（reviewId=0 表示新建）
        composable(
            route = NavRoutes.REVIEW_EDIT,
            arguments = listOf(
                navArgument("reviewId") { type = NavType.LongType; defaultValue = 0L }
            )
        ) { backStackEntry ->
            val reviewId = backStackEntry.arguments?.getLong("reviewId") ?: 0L
            ReviewEditScreen(navController = navController, reviewId = reviewId)
        }
        // 智能总结
        composable(NavRoutes.SUMMARY) {
            SummaryScreen(
                viewModel = hiltViewModel(),
                onNavigateBack = { navController.popBackStack() }
            )
        }
        // 科目管理
        composable(NavRoutes.SUBJECT_MANAGE) {
            SubjectManageScreen(navController = navController)
        }
        // 设置
        composable(NavRoutes.SETTINGS) {
            SettingsScreen(navController = navController)
        }
        // 备份与恢复
        composable(NavRoutes.BACKUP) {
            BackupScreen(navController = navController)
        }
        // 考研目标
        composable(NavRoutes.GOAL) {
            GoalScreen(navController = navController)
        }
    }
}

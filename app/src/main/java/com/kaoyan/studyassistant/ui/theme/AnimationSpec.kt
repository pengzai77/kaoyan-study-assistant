package com.kaoyan.studyassistant.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * 全局动画规范（柔顺版）
 *
 * ── 设计原则 ─────────────────────────────────────────────────────────────────
 *
 * 柔顺感来自三个要素：
 *   1. 合适的时长：不快不慢，让眼睛"跟得上"又不觉得等待
 *   2. 正确的曲线：ease-in-out 两端缓慢，符合自然运动规律
 *   3. 动画同步：同一场景中所有属性（位移、透明度）使用相同时长和曲线
 *
 * ── 曲线选择说明 ──────────────────────────────────────────────────────────────
 *
 *   EaseInOut  (0.42, 0, 0.58, 1)  → 两端慢中间快，最自然，用于大多数场景
 *   EaseOut    (0.0,  0, 0.58, 1)  → 开始快结束慢，适合"进入"动画（元素出现）
 *   EaseIn     (0.42, 0, 1.0,  1)  → 开始慢结束快，适合"退出"动画（元素消失）
 *
 * ── 时长选择说明 ──────────────────────────────────────────────────────────────
 *
 *   SHORT  = 150ms  微交互：颜色变化、开关切换、涟漪效果
 *   MEDIUM = 250ms  组件级：卡片展开、内容切换、状态变化
 *   NAV    = 320ms  页面级：导航过渡（进入/退出）
 *   LONG   = 400ms  强调级：首次出现、重要状态转换
 * ─────────────────────────────────────────────────────────────────────────────
 */
object KaoyanAnimSpec {

    // ── 缓动曲线 ──────────────────────────────────────────────────────────────

    /** ease-in-out：两端缓慢，中间加速，最接近自然运动，适合大多数动画 */
    val EaseInOut: Easing = CubicBezierEasing(0.42f, 0.0f, 0.58f, 1.0f)

    /** ease-out：开始快结束慢，适合元素"进入"场景（减速停下） */
    val EaseOut: Easing = CubicBezierEasing(0.0f, 0.0f, 0.58f, 1.0f)

    /** ease-in：开始慢结束快，适合元素"退出"场景（加速离开） */
    val EaseIn: Easing = CubicBezierEasing(0.42f, 0.0f, 1.0f, 1.0f)

    // 保留旧名称兼容性
    val EmphasizedEasing: Easing = EaseInOut
    val StandardEasing: Easing   = EaseOut
    val DecelerateEasing: Easing = EaseOut
    val AccelerateEasing: Easing = EaseIn

    // ── 时长常量 ──────────────────────────────────────────────────────────────

    const val SHORT  = 150   // 微交互：颜色、开关、涟漪
    const val MEDIUM = 250   // 组件级：卡片、内容切换
    const val NAV    = 320   // 页面级：导航过渡（柔顺最佳值）
    const val LONG   = 400   // 强调级：首次出现、重要状态

    // 保留旧名称兼容性
    const val LONG_COMPAT = NAV

    // ── 弹簧动画规格 ──────────────────────────────────────────────────────────

    /**
     * 柔顺弹簧：无弹跳，阻尼充足，适合列表项、卡片出现
     * DampingRatioNoBouncy = 1.0f，完全不弹，平滑停止
     */
    fun <T> smoothSpring() = spring<T>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness    = Spring.StiffnessMediumLow
    )

    /**
     * 轻弹弹簧：极轻微弹性，增加活泼感但不突兀
     * 适合按钮按下、图标缩放等微交互
     */
    fun <T> gentleSpring() = spring<T>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness    = Spring.StiffnessMedium
    )

    /**
     * 无弹性弹簧：完全平滑，适合颜色、透明度等不适合弹性的属性
     */
    fun <T> noBouncySpring() = spring<T>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness    = Spring.StiffnessMedium
    )

    // ── 补间动画规格（工厂方法）──────────────────────────────────────────────

    /** 导航过渡：320ms + ease-in-out，页面进入/退出 */
    fun <T> navSpec() = tween<T>(
        durationMillis = NAV,
        easing         = EaseInOut
    )

    /** 淡入淡出：250ms + ease-in-out，内容切换 */
    fun <T> fadeSpec() = tween<T>(
        durationMillis = MEDIUM,
        easing         = EaseInOut
    )

    /** 快速淡入淡出：150ms + ease-in-out，状态指示器 */
    fun <T> quickFadeSpec() = tween<T>(
        durationMillis = SHORT,
        easing         = EaseInOut
    )

    /** 滑入：250ms + ease-out，元素从外部进入 */
    fun <T> slideInSpec() = tween<T>(
        durationMillis = MEDIUM,
        easing         = EaseOut
    )

    /** 滑出：200ms + ease-in，元素退出屏幕 */
    fun <T> slideOutSpec() = tween<T>(
        durationMillis = (MEDIUM * 0.8f).toInt(),
        easing         = EaseIn
    )
}

package com.kaoyan.studyassistant.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaoyan.studyassistant.ui.theme.KaoyanAnimSpec

// ─────────────────────────────────────────────────────────────────────────────
// 顶部栏
// ─────────────────────────────────────────────────────────────────────────────

/**
 * 通用顶部栏（带返回按钮）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KaoyanTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回"
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// 空状态
// ─────────────────────────────────────────────────────────────────────────────

/**
 * 空状态占位组件（带淡入动画）
 */
@Composable
fun EmptyState(
    message: String,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(animationSpec = tween(KaoyanAnimSpec.MEDIUM, easing = KaoyanAnimSpec.EmphasizedEasing)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "📭", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 信息卡片
// ─────────────────────────────────────────────────────────────────────────────

/**
 * 信息卡片
 */
@Composable
fun InfoCard(
    title: String,
    value: String,
    subtitle: String = "",
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            // 数值变化时带数字滚动动画
            AnimatedContent(
                targetState = value,
                transitionSpec = {
                    fadeIn(tween(KaoyanAnimSpec.SHORT)) togetherWith
                        fadeOut(tween(KaoyanAnimSpec.SHORT))
                },
                label = "info_card_value"
            ) { v ->
                Text(
                    text = v,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            if (subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 科目标签
// ─────────────────────────────────────────────────────────────────────────────

/**
 * 科目颜色标签
 */
@Composable
fun SubjectTag(
    name: String,
    color: String,
    modifier: Modifier = Modifier
) {
    val tagColor = rememberParsedColor(color)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(tagColor.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(tagColor)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.labelLarge,
                color = tagColor
            )
        }
    }
}

@Composable
fun rememberParsedColor(
    colorHex: String,
    fallback: Color = MaterialTheme.colorScheme.primary
): Color {
    return remember(colorHex, fallback) {
        runCatching { Color(android.graphics.Color.parseColor(colorHex)) }
            .getOrElse { fallback }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 进度条（带动画）
// ─────────────────────────────────────────────────────────────────────────────

/**
 * 带动画的进度条（用于科目时长占比）
 * - progress 变化时平滑过渡，不突变
 */
@Composable
fun SimpleProgressBar(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(
            durationMillis = KaoyanAnimSpec.MEDIUM,
            easing = KaoyanAnimSpec.EmphasizedEasing
        ),
        label = "progress_bar"
    )

    Box(
        modifier = modifier
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(color.copy(alpha = 0.15f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedProgress)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 分隔线
// ─────────────────────────────────────────────────────────────────────────────

/**
 * 分隔线
 */
@Composable
fun SectionDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.padding(vertical = 8.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// 骨架屏（Shimmer Loading）
// ─────────────────────────────────────────────────────────────────────────────

/**
 * 单个骨架屏条目（闪光效果）
 *
 * 用于列表/卡片加载中状态，比纯 CircularProgressIndicator 更专业
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(8.dp)
) {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 200f, 0f),
        end = Offset(translateAnim, 0f)
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(brush)
    )
}

/**
 * 卡片骨架屏（用于列表加载中）
 */
@Composable
fun ShimmerCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(14.dp),
                shape = RoundedCornerShape(4.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp),
                shape = RoundedCornerShape(4.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(12.dp),
                shape = RoundedCornerShape(4.dp)
            )
        }
    }
}

/**
 * 列表骨架屏（多个卡片）
 */
@Composable
fun ShimmerList(
    count: Int = 4,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(count) {
            ShimmerCard()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 带动画的数字文本
// ─────────────────────────────────────────────────────────────────────────────

/**
 * 数字变化时带淡入淡出动画的 Text
 *
 * 适用于计时器、统计数字等需要平滑更新的场景
 */
@Composable
fun AnimatedCounterText(
    value: String,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = Color.Unspecified,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = value,
        transitionSpec = {
            fadeIn(
                animationSpec = tween(KaoyanAnimSpec.SHORT, easing = KaoyanAnimSpec.EmphasizedEasing)
            ) togetherWith fadeOut(
                animationSpec = tween(KaoyanAnimSpec.SHORT, easing = KaoyanAnimSpec.AccelerateEasing)
            )
        },
        label = "animated_counter",
        modifier = modifier
    ) { v ->
        Text(text = v, style = style, color = color)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 带动画的颜色切换 Surface
// ─────────────────────────────────────────────────────────────────────────────

/**
 * 颜色平滑过渡的 Surface
 * 适用于状态切换时（如计时器运行/暂停）背景色平滑变化
 */
@Composable
fun AnimatedColorSurface(
    color: Color,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = MaterialTheme.shapes.medium,
    content: @Composable () -> Unit
) {
    val animatedColor by animateColorAsState(
        targetValue = color,
        animationSpec = tween(
            durationMillis = KaoyanAnimSpec.MEDIUM,
            easing = KaoyanAnimSpec.EmphasizedEasing
        ),
        label = "surface_color"
    )
    Surface(
        modifier = modifier,
        color = animatedColor,
        shape = shape,
        content = content
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// 可展开卡片
// ─────────────────────────────────────────────────────────────────────────────

/**
 * 带展开/收起动画的卡片容器
 * 内容高度变化时平滑过渡
 */
@Composable
fun ExpandableCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                    stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                )
            )
    ) {
        Column(content = content)
    }
}

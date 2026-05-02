package com.kaoyan.studyassistant.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaoyan.studyassistant.service.TimerRunState
import com.kaoyan.studyassistant.util.DateUtils

/**
 * 轻量版计时展示组件。
 *
 * 旧实现把高频变化的时间数字做成 AnimatedContent 翻页，
 * 在计时页这种每秒/半秒刷新的场景里很容易放大掉帧感。
 * 这里保留状态颜色的轻动画，但把数字渲染改为普通 Text，
 * 让重组范围和绘制成本都更稳定。
 */
@Composable
fun AnimatedTimerDisplay(
    elapsedMillis: Long,
    timerState: TimerRunState,
    showSeconds: Boolean = true,
    size: Dp = 200.dp,
    fontSize: Int = 38,
    modifier: Modifier = Modifier
) {
    val targetColor = when (timerState) {
        TimerRunState.RUNNING -> MaterialTheme.colorScheme.primary
        TimerRunState.PAUSED -> MaterialTheme.colorScheme.tertiary
        TimerRunState.IDLE -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val displayColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 180),
        label = "timer_color"
    )

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            TimerText(
                timeMillis = elapsedMillis,
                showSeconds = showSeconds,
                color = displayColor,
                fontSize = fontSize
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = when (timerState) {
                    TimerRunState.RUNNING -> "学习中..."
                    TimerRunState.PAUSED -> "已暂停"
                    TimerRunState.IDLE -> "准备开始"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TimerText(
    timeMillis: Long,
    showSeconds: Boolean,
    color: Color,
    fontSize: Int
) {
    val timeString = if (showSeconds) {
        DateUtils.formatTimerDisplay(timeMillis)
    } else {
        val totalSeconds = timeMillis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        if (hours > 0) "%d:%02d".format(hours, minutes) else "%d分钟".format(minutes)
    }

    Text(
        text = timeString,
        fontSize = fontSize.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        textAlign = TextAlign.Center
    )
}

/**
 * 轻量版番茄钟展示组件。
 * 去掉高频 AnimatedContent/进度动画，减少倒计时过程中不必要的额外开销。
 */
@Composable
fun AnimatedPomodoroDisplay(
    remainingMillis: Long,
    targetMillis: Long,
    phaseLabel: String,
    phaseColor: Color,
    showSeconds: Boolean = true,
    modifier: Modifier = Modifier
) {
    val displayText = if (showSeconds) {
        DateUtils.formatTimerDisplay(remainingMillis)
    } else {
        DateUtils.formatTimerDisplayNoSeconds(remainingMillis)
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(phaseColor.copy(alpha = 0.15f))
        ) {
            Text(
                text = phaseLabel,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 7.dp),
                color = phaseColor,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = displayText,
            fontSize = 52.sp,
            fontWeight = FontWeight.Bold,
            color = phaseColor,
            textAlign = TextAlign.Center
        )

        if (targetMillis > 0L) {
            Text(
                text = "进度 ${(100f - (remainingMillis.coerceAtLeast(0L).toFloat() / targetMillis.toFloat() * 100f)).coerceIn(0f, 100f).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

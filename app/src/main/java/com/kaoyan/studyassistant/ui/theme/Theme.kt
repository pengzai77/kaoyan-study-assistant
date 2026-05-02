package com.kaoyan.studyassistant.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// 主色调：沉稳蓝色，适合学习工具
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1A6BB5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E4FF),
    onPrimaryContainer = Color(0xFF001C3D),
    secondary = Color(0xFF4A90D9),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCEEFF),
    onSecondaryContainer = Color(0xFF001D36),
    tertiary = Color(0xFF43A047),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC8E6C9),
    onTertiaryContainer = Color(0xFF002107),
    background = Color(0xFFF8F9FB),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFEEF2F7),
    onSurfaceVariant = Color(0xFF42474E),
    outline = Color(0xFFCDD5E0),
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF9ECAFF),
    onPrimary = Color(0xFF003062),
    primaryContainer = Color(0xFF00458A),
    onPrimaryContainer = Color(0xFFD6E4FF),
    secondary = Color(0xFF90CAF9),
    onSecondary = Color(0xFF003355),
    secondaryContainer = Color(0xFF004A77),
    onSecondaryContainer = Color(0xFFCDE5FF),
    tertiary = Color(0xFF81C784),
    onTertiary = Color(0xFF003910),
    tertiaryContainer = Color(0xFF005319),
    onTertiaryContainer = Color(0xFFC8E6C9),
    background = Color(0xFF1A1C1E),
    onBackground = Color(0xFFE2E2E6),
    surface = Color(0xFF1A1C1E),
    onSurface = Color(0xFFE2E2E6),
    surfaceVariant = Color(0xFF252830),
    onSurfaceVariant = Color(0xFFC2C7CF),
    outline = Color(0xFF3A3F47),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

@Composable
fun KaoyanStudyAssistantTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // 不再直接设置 statusBarColor（已废弃，targetSdk 35 edge-to-edge 场景下无效）
            // MainActivity 已调用 enableEdgeToEdge()，状态栏颜色由系统透明处理
            // 仅控制状态栏图标明暗（浅色主题用深色图标，深色主题用浅色图标）
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

package com.kaoyan.studyassistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.kaoyan.studyassistant.data.datastore.AppPreferences
import com.kaoyan.studyassistant.ui.navigation.AppNavGraph
import com.kaoyan.studyassistant.ui.startup.StartupViewModel
import com.kaoyan.studyassistant.ui.theme.KaoyanStudyAssistantTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * 主 Activity
 *
 * 改造点（v3）：
 * 1. 接入 StartupViewModel，启动时探测备份并弹出恢复提示（非模态）
 * 2. 读取 DataStore darkMode 设置，动态切换深色/浅色主题
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var appPreferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MainContent(appPreferences = appPreferences)
        }
    }
}

@Composable
private fun MainContent(appPreferences: AppPreferences) {
    // 读取 darkMode 设置
    val userSettings by appPreferences.userSettings.collectAsStateWithLifecycle(
        initialValue = AppPreferences.UserSettings()
    )

    val systemDark = isSystemInDarkTheme()
    val isDark = when (userSettings.darkMode) {
        1 -> false  // 强制浅色
        2 -> true   // 强制深色
        else -> systemDark  // 跟随系统
    }

    KaoyanStudyAssistantTheme(darkTheme = isDark) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val navController = rememberNavController()
            val snackbarHostState = remember { SnackbarHostState() }

            // 启动探测 ViewModel（Activity 级别，只初始化一次）
            val startupViewModel: StartupViewModel = hiltViewModel()
            val startupState by startupViewModel.state.collectAsStateWithLifecycle()

            // 主导航图
            AppNavGraph(navController = navController)

            // Snackbar 宿主（浮于导航图之上）
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(snackbarData = data)
            }

            // 恢复结果 Snackbar
            LaunchedEffect(startupState.restoreResultMessage) {
                startupState.restoreResultMessage?.let { msg ->
                    snackbarHostState.showSnackbar(msg)
                    startupViewModel.clearResultMessage()
                }
            }

            // 恢复中 Loading 对话框
            if (startupState.isRestoring) {
                AlertDialog(
                    onDismissRequest = {},
                    title = { Text("正在恢复数据…") },
                    text = { CircularProgressIndicator() },
                    confirmButton = {}
                )
            }

            // 启动恢复提示对话框
            if (startupState.showRestorePrompt) {
                AlertDialog(
                    onDismissRequest = { startupViewModel.dismissRestore() },
                    title = { Text("检测到备份数据") },
                    text = {
                        Text(
                            "发现上次的学习数据备份，是否恢复？\n\n" +
                                    startupState.backupSummary
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = { startupViewModel.confirmRestore() }) {
                            Text("立即恢复")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { startupViewModel.dismissRestore() }) {
                            Text("跳过")
                        }
                    }
                )
            }
        }
    }
}

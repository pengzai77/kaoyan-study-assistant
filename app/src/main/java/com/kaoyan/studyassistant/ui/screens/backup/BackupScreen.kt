package com.kaoyan.studyassistant.ui.screens.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kaoyan.studyassistant.ui.components.KaoyanTopBar
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 备份与恢复页面
 *
 * 功能：
 * 1. 立即备份（写入 App 私有目录）
 * 2. 导出备份（SAF，用户选择目录）
 * 3. 导入备份（SAF，用户选择文件）
 * 4. 查看自动备份历史
 * 5. 从最新自动备份恢复
 */
@Composable
fun BackupScreen(
    navController: NavController,
    viewModel: BackupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // 导出备份：用户选择目标文件 URI
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        uri?.let { viewModel.backupToUri(it) }
    }

    // 导入备份：用户选择备份文件 URI
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.previewRestore(it) }
    }

    // 显示消息
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    // 恢复确认对话框
    if (uiState.pendingRestorePayload != null) {
        AlertDialog(
            onDismissRequest = viewModel::cancelRestore,
            icon = { Icon(Icons.Default.Restore, contentDescription = null) },
            title = { Text("确认恢复数据") },
            text = {
                Column {
                    Text("即将从备份文件恢复以下数据：")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.pendingRestorePayload!!.summary(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "恢复采用合并策略，不会删除现有数据。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Button(onClick = viewModel::confirmRestore) { Text("确认恢复") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelRestore) { Text("取消") }
            }
        )
    }

    Scaffold(
        topBar = {
            KaoyanTopBar(
                title = "数据备份与恢复",
                onBack = { navController.popBackStack() }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("处理中...", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ---- 备份操作 ----
                item {
                    BackupSection(
                        onBackupLocal = viewModel::backup,
                        onExportSaf = {
                            val fileName = "kaoyan_backup_${
                                SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                            }.kaoyan_backup"
                            exportLauncher.launch(fileName)
                        }
                    )
                }

                // ---- 恢复操作 ----
                item {
                    RestoreSection(
                        hasAutoBackup = uiState.autoBackupFiles.isNotEmpty(),
                        onImportSaf = { importLauncher.launch(arrayOf("*/*")) },
                        onRestoreLatest = viewModel::restoreFromLatestAutoBackup
                    )
                }

                // ---- 自动备份历史 ----
                if (uiState.autoBackupFiles.isNotEmpty()) {
                    item {
                        Text(
                            text = "本地备份历史（最近 5 份）",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                        )
                    }
                    items(uiState.autoBackupFiles) { file ->
                        AutoBackupFileItem(file = file)
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

/** 备份操作区 */
@Composable
private fun BackupSection(
    onBackupLocal: () -> Unit,
    onExportSaf: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "备份数据",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            // 本地备份
            OutlinedButton(
                onClick = onBackupLocal,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("立即备份（保存到本地）")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // SAF 导出
            OutlinedButton(
                onClick = onExportSaf,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("导出备份文件（选择位置）")
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "备份文件包含科目、学习记录、复盘和目标设置，格式为 .kaoyan_backup（JSON）",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** 恢复操作区 */
@Composable
private fun RestoreSection(
    hasAutoBackup: Boolean,
    onImportSaf: () -> Unit,
    onRestoreLatest: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "恢复数据",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            // 从文件导入
            OutlinedButton(
                onClick = onImportSaf,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("从文件恢复（选择备份文件）")
            }

            if (hasAutoBackup) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onRestoreLatest,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("从最新自动备份恢复")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "恢复采用合并策略，不会删除现有数据，重复数据将被跳过或更新。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** 自动备份文件列表项 */
@Composable
private fun AutoBackupFileItem(file: File) {
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = sdf.format(Date(file.lastModified())),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = formatFileSize(file.length()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "${bytes}B"
        bytes < 1024 * 1024 -> "${bytes / 1024}KB"
        else -> "${"%.1f".format(bytes / 1024.0 / 1024.0)}MB"
    }
}

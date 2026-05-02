package com.kaoyan.studyassistant.ui.screens.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kaoyan.studyassistant.BuildConfig
import com.kaoyan.studyassistant.R
import com.kaoyan.studyassistant.domain.summarizer.AiModelOption
import com.kaoyan.studyassistant.domain.summarizer.AiProvider
import com.kaoyan.studyassistant.ui.components.KaoyanTopBar
import com.kaoyan.studyassistant.ui.navigation.NavRoutes

@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
    val backupSettings by viewModel.backupSettings.collectAsStateWithLifecycle()
    val aiSettings by viewModel.aiSettings.collectAsStateWithLifecycle()
    val aiConnectionState by viewModel.aiConnectionState.collectAsStateWithLifecycle()
    val modelCatalogState by viewModel.modelCatalogState.collectAsStateWithLifecycle()
    var showRewardDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            KaoyanTopBar(
                title = "设置",
                onBack = { navController.popBackStack() },
                actions = {
                    TextButton(onClick = { showRewardDialog = true }) {
                        Icon(Icons.Default.VolunteerActivism, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("打赏作者")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingGroup(title = "数据与目标") {
                SettingItem(
                    icon = Icons.Default.EmojiEvents,
                    title = "考研目标",
                    subtitle = "设置目标院校、专业和考试日期",
                    onClick = { navController.navigate(NavRoutes.GOAL) }
                )
            }

            SettingGroup(title = "功能") {
                SettingItem(
                    icon = Icons.Default.Category,
                    title = "科目管理",
                    subtitle = "新增、编辑、删除学习科目",
                    onClick = { navController.navigate(NavRoutes.SUBJECT_MANAGE) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                SettingItem(
                    icon = Icons.Default.BarChart,
                    title = "学习统计",
                    subtitle = "查看累计学习数据与趋势图",
                    onClick = { navController.navigate(NavRoutes.STATISTICS) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                SettingItem(
                    icon = Icons.Default.History,
                    title = "历史记录",
                    subtitle = "查看所有学习记录",
                    onClick = { navController.navigate(NavRoutes.HISTORY) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                SettingItem(
                    icon = Icons.Default.AutoAwesome,
                    title = "智能总结",
                    subtitle = "查看总结结果，或进入对话模式继续追问",
                    onClick = { navController.navigate(NavRoutes.SUMMARY) }
                )
            }

            SettingGroup(title = "AI 增强总结") {
                SwitchSettingItem(
                    icon = Icons.Default.AutoAwesome,
                    title = "启用 AI 增强",
                    subtitle = "先跑本地规则总结，再调用兼容接口做增强分析",
                    checked = aiSettings.enabled,
                    onCheckedChange = viewModel::setAiEnabled
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ProviderSettingItem(
                    selectedProvider = aiSettings.provider,
                    onProviderSelected = viewModel::setAiProvider
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                TextFieldSettingItem(
                    icon = Icons.Default.Key,
                    title = "API Key（${aiSettings.provider.displayName}）",
                    value = aiSettings.apiKey,
                    onValueChange = viewModel::setAiApiKey,
                    placeholder = "请输入 ${aiSettings.provider.displayName} API Key",
                    isSecret = true,
                    keyboardType = KeyboardType.Password
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                TextFieldSettingItem(
                    icon = Icons.Default.Link,
                    title = "接口地址（Base URL）",
                    value = aiSettings.baseUrl,
                    onValueChange = viewModel::setAiBaseUrl,
                    placeholder = aiSettings.provider.defaultBaseUrl.ifBlank { "https://example.com/v1" },
                    keyboardType = KeyboardType.Uri,
                    helperText = when (aiSettings.provider) {
                        AiProvider.DEEPSEEK -> "DeepSeek 填 https://api.deepseek.com 即可，应用会自动请求 /chat/completions。"
                        else -> "填写 OpenAI 兼容服务的基础地址，通常不需要写 /chat/completions。"
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ActionRow(
                    primaryLabel = if (modelCatalogState.isLoading) "获取中..." else "刷新模型",
                    onPrimaryClick = viewModel::refreshModelCatalog,
                    primaryEnabled = !modelCatalogState.isLoading,
                    secondaryLabel = if (aiConnectionState.isTesting) "测试中..." else "测试连接",
                    onSecondaryClick = viewModel::testAiConnection,
                    secondaryEnabled = !aiConnectionState.isTesting
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ModelPickerSettingItem(
                    selectedModel = aiSettings.model,
                    modelHistory = aiSettings.modelHistory,
                    options = modelCatalogState.options.filter { it.provider == aiSettings.provider },
                    onSelect = viewModel::selectAiModel,
                    onManualModelChange = viewModel::setAiModel,
                    isLoading = modelCatalogState.isLoading,
                    placeholder = "输入模型名，例如 ${aiSettings.provider.defaultModel.ifBlank { "deepseek-chat" }}"
                )
                StatusMessageItem(
                    helperText = "未填写 Key 或连接失败时，会自动回退到本地规则总结",
                    successMessage = listOfNotNull(
                        modelCatalogState.message,
                        aiConnectionState.successMessage
                    ).joinToString("\n").ifBlank { null },
                    errorMessage = listOfNotNull(
                        modelCatalogState.errorMessage,
                        aiConnectionState.errorMessage
                    ).joinToString("\n").ifBlank { null }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                SwitchSettingItem(
                    icon = Icons.Default.Tune,
                    title = "严格 JSON 输出",
                    subtitle = "总结模式要求模型返回可解析 JSON；对话模式仍走自然语言回答",
                    checked = aiSettings.jsonMode,
                    onCheckedChange = viewModel::setAiJsonMode
                )
            }

            SettingGroup(title = "显示") {
                SwitchSettingItem(
                    icon = Icons.Default.Timer,
                    title = "计时器显示秒数",
                    subtitle = "打开后显示 HH:MM:SS，关闭后显示 HH:MM",
                    checked = userSettings.showSeconds,
                    onCheckedChange = viewModel::setShowSeconds
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                SwitchSettingItem(
                    icon = Icons.Default.RateReview,
                    title = "首页显示最近复盘",
                    subtitle = "在首页卡片中展示最近一条复盘摘要",
                    checked = userSettings.showLatestReview,
                    onCheckedChange = viewModel::setShowLatestReview
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                SwitchSettingItem(
                    icon = Icons.Default.Vibration,
                    title = "计时结束震动提醒",
                    subtitle = "结束学习计时时给出震动提示",
                    checked = userSettings.enableVibration,
                    onCheckedChange = viewModel::setEnableVibration
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                InlineChoiceSettingItem(
                    icon = Icons.Default.Tune,
                    title = "深色模式",
                    subtitle = "跟随系统 / 浅色 / 深色",
                    options = listOf(0 to "跟随系统", 1 to "浅色", 2 to "深色"),
                    selectedValue = userSettings.darkMode,
                    onSelect = viewModel::setDarkMode
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                InlineChoiceSettingItem(
                    icon = Icons.Default.History,
                    title = "历史记录默认范围",
                    subtitle = "打开历史页时默认显示的时间范围",
                    options = listOf(7 to "7天", 30 to "30天", 90 to "90天", 0 to "全部"),
                    selectedValue = userSettings.historyDefaultRange,
                    onSelect = viewModel::setHistoryDefaultRange
                )
            }

            SettingGroup(title = "数据备份") {
                SwitchSettingItem(
                    icon = Icons.Default.Backup,
                    title = "自动备份",
                    subtitle = "保存记录后自动生成本地备份",
                    checked = backupSettings.autoBackupEnabled,
                    onCheckedChange = viewModel::setAutoBackupEnabled
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                SettingItem(
                    icon = Icons.Default.SaveAlt,
                    title = "立即备份",
                    subtitle = backupSettings.lastBackupTime.takeIf { it > 0L }?.let {
                        "上次备份：${formatBackupTime(it)}"
                    } ?: "尚未备份",
                    onClick = { navController.navigate(NavRoutes.BACKUP) }
                )
            }

            SettingGroup(title = "关于") {
                SettingItem(
                    icon = Icons.Default.Info,
                    title = "版本信息",
                    subtitle = "考研学习助手 v${BuildConfig.VERSION_NAME}",
                    onClick = {}
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                SettingItem(
                    icon = Icons.Default.Security,
                    title = "隐私说明",
                    subtitle = "API Key、模型缓存和对话上下文都不会进入备份文件",
                    onClick = {}
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showRewardDialog) {
        RewardAuthorDialog(onDismiss = { showRewardDialog = false })
    }
}

private fun formatBackupTime(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}

@Composable
private fun SettingGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column { content() }
        }
    }
}

@Composable
private fun SettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(onClick = onClick, color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun SwitchSettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ProviderSettingItem(
    selectedProvider: AiProvider,
    onProviderSelected: (AiProvider) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
            text = "供应商",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Text(
            text = "模型列表和默认接口地址会和当前供应商保持一致。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(AiProvider.entries) { provider ->
                FilterChip(
                    selected = selectedProvider == provider,
                    onClick = { onProviderSelected(provider) },
                    label = { Text(provider.displayName) }
                )
            }
        }
    }
}

@Composable
private fun TextFieldSettingItem(
    icon: ImageVector,
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isSecret: Boolean = false,
    keyboardType: KeyboardType,
    helperText: String? = null
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        }
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text(placeholder) },
            visualTransformation = if (isSecret && !passwordVisible) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            trailingIcon = if (isSecret) {
                {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "隐藏 API Key" else "显示 API Key"
                        )
                    }
                }
            } else {
                null
            },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
        )
        helperText?.let {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ActionRow(
    primaryLabel: String,
    onPrimaryClick: () -> Unit,
    primaryEnabled: Boolean,
    secondaryLabel: String,
    onSecondaryClick: () -> Unit,
    secondaryEnabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onPrimaryClick,
            enabled = primaryEnabled,
            modifier = Modifier.weight(1f)
        ) { Text(primaryLabel) }
        Button(
            onClick = onSecondaryClick,
            enabled = secondaryEnabled,
            modifier = Modifier.weight(1f)
        ) { Text(secondaryLabel) }
    }
}

@Composable
private fun ModelPickerSettingItem(
    selectedModel: String,
    modelHistory: List<String>,
    options: List<AiModelOption>,
    onSelect: (AiModelOption) -> Unit,
    onManualModelChange: (String) -> Unit,
    isLoading: Boolean,
    placeholder: String
) {
    val suggestions = buildModelSuggestions(options, modelHistory)

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "模型", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        Text(
            text = "可直接输入模型名，也可以从下方建议和最近使用中选择。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
        )

        OutlinedTextField(
            value = selectedModel,
            onValueChange = onManualModelChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isLoading,
            placeholder = { Text(if (isLoading) "正在获取模型列表..." else placeholder) }
        )

        if (suggestions.isNotEmpty()) {
            Text(
                text = "建议",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(suggestions, key = { "${it.modelId}_${it.sourceLabel}" }) { suggestion ->
                    FilterChip(
                        selected = selectedModel.equals(suggestion.modelId, ignoreCase = true),
                        onClick = {
                            suggestion.option?.let(onSelect) ?: onManualModelChange(suggestion.modelId)
                        },
                        label = { Text(suggestion.label) }
                    )
                }
            }
        }
    }
}

private data class ModelSuggestion(
    val modelId: String,
    val label: String,
    val sourceLabel: String,
    val option: AiModelOption? = null
)

private fun buildModelSuggestions(
    options: List<AiModelOption>,
    modelHistory: List<String>
): List<ModelSuggestion> {
    val remoteOrPreset = options
        .sortedWith(
            compareByDescending<AiModelOption> { it.isRecommended }
                .thenByDescending { it.isLatest }
                .thenBy { it.id }
        )
        .map { option ->
            ModelSuggestion(
                modelId = option.id,
                label = compactModelLabel(option),
                sourceLabel = "option",
                option = option
            )
        }

    val knownIds = remoteOrPreset.map { it.modelId.lowercase() }.toSet()
    val history = modelHistory
        .filterNot { it.lowercase() in knownIds }
        .map { model ->
            ModelSuggestion(
                modelId = model,
                label = "$model · 最近",
                sourceLabel = "history"
            )
        }

    return (remoteOrPreset + history).take(10)
}

private fun compactModelLabel(option: AiModelOption): String {
    val tags = buildList {
        if (option.isRecommended) add("推荐")
        if (option.isLatest) add("最新")
        if (option.isPreview) add("推理")
    }
    return if (tags.isEmpty()) option.label else "${option.label} · ${tags.joinToString("/")}"
}

@Composable
private fun InlineChoiceSettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    options: List<Pair<Int, String>>,
    selectedValue: Int,
    onSelect: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { (value, label) ->
                FilterChip(
                    selected = selectedValue == value,
                    onClick = { onSelect(value) },
                    label = { Text(label, style = MaterialTheme.typography.bodySmall) }
                )
            }
        }
    }
}

@Composable
private fun StatusMessageItem(
    helperText: String,
    successMessage: String?,
    errorMessage: String?
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        Text(text = helperText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        successMessage?.let {
            Spacer(modifier = Modifier.height(8.dp))
            StatusBanner(
                text = it,
                icon = Icons.Default.CheckCircle,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        errorMessage?.let {
            Spacer(modifier = Modifier.height(8.dp))
            StatusBanner(
                text = it,
                icon = Icons.Default.Error,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun StatusBanner(
    text: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor
            )
        }
    }
}

@Composable
private fun RewardAuthorDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("我知道了")
            }
        },
        title = { Text("打赏作者") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.alipay_reward_code),
                    contentDescription = "支付宝收款码",
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.FillWidth
                )
                Text(
                    text = "感谢你的喜欢和支持，这会让我更有动力继续认真打磨它。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

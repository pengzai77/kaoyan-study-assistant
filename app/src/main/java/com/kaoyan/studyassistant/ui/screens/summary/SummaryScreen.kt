package com.kaoyan.studyassistant.ui.screens.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaoyan.studyassistant.domain.summarizer.SummaryResult
import com.kaoyan.studyassistant.domain.summarizer.SummarySource
import com.kaoyan.studyassistant.ui.components.KaoyanTopBar
import com.kaoyan.studyassistant.util.DateUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * 总结主界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    viewModel: SummaryViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var chatInput by rememberSaveable { mutableStateOf("") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            KaoyanTopBar(
                title = "智能总结",
                onBack = onNavigateBack,
                actions = {
                    IconButton(
                        onClick = viewModel::generateSummary,
                        enabled = !uiState.isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "重新总结"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            RangeSelector(
                selectedRange = uiState.range,
                onRangeSelected = viewModel::setRange,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            TabRow(selectedTabIndex = uiState.selectedTab.ordinal) {
                SummaryTab.entries.forEach { tab ->
                    Tab(
                        selected = uiState.selectedTab == tab,
                        onClick = { viewModel.setTab(tab) },
                        text = { Text(tab.label()) }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (uiState.selectedTab == SummaryTab.SUMMARY) {
                    SummaryTabPane(
                        isLoading = uiState.isLoading,
                        summaryResult = uiState.result,
                        errorMessage = uiState.errorMessage,
                        selectedRange = uiState.range,
                        lastGeneratedRange = uiState.lastGeneratedRange,
                        generatedAtMillis = uiState.generatedAtMillis,
                        dataFootprint = uiState.dataFootprint,
                        problemEvidence = uiState.problemEvidence,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    SummaryChatPane(
                        messages = uiState.chatMessages,
                        inputText = chatInput,
                        isSending = uiState.isChatLoading,
                        errorMessage = uiState.chatErrorMessage,
                        contextNotice = buildChatContextNotice(uiState),
                        quickQuestions = uiState.quickQuestions,
                        onQuickQuestionClick = { question ->
                            chatInput = ""
                            viewModel.askQuestion(question)
                        },
                        onInputChanged = { chatInput = it },
                        onSend = {
                            val question = chatInput.trim()
                            if (question.isNotBlank()) {
                                viewModel.askQuestion(question)
                                chatInput = ""
                            }
                        },
                        onDeleteMessage = viewModel::deleteChatMessage,
                        onClearConversation = viewModel::clearChatConversation,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryTabPane(
    isLoading: Boolean,
    summaryResult: SummaryResult?,
    errorMessage: String?,
    selectedRange: SummaryRange,
    lastGeneratedRange: SummaryRange?,
    generatedAtMillis: Long?,
    dataFootprint: SummaryDataFootprint,
    problemEvidence: Map<String, List<String>>,
    modifier: Modifier = Modifier
) {
    when {
        isLoading && summaryResult == null -> LoadingPane(modifier)
        summaryResult == null -> EmptySummaryPane(
            selectedRange = selectedRange,
            lastGeneratedRange = lastGeneratedRange,
            errorMessage = errorMessage,
            modifier = modifier
        )
        else -> SummaryContent(
            summaryResult = summaryResult,
            selectedRange = selectedRange,
            lastGeneratedRange = lastGeneratedRange,
            generatedAtMillis = generatedAtMillis,
            dataFootprint = dataFootprint,
            problemEvidence = problemEvidence,
            modifier = modifier
        )
    }
}

@Composable
private fun LoadingPane(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptySummaryPane(
    selectedRange: SummaryRange,
    lastGeneratedRange: SummaryRange?,
    errorMessage: String?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "尚未生成总结",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (lastGeneratedRange == null) {
                        "点击右上角刷新，为${selectedRange.label()}生成总结。"
                    } else {
                        "上次生成范围是${lastGeneratedRange.label()}，点击右上角可重新生成${selectedRange.label()}总结。"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                errorMessage?.takeIf { it.isNotBlank() }?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun RangeSelector(
    selectedRange: SummaryRange,
    onRangeSelected: (SummaryRange) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SummaryRange.entries.forEach { range ->
            val selected = selectedRange == range
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clickable { onRangeSelected(range) },
                color = if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
                },
                shape = RoundedCornerShape(14.dp),
                tonalElevation = if (selected) 2.dp else 0.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = range.label(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryContent(
    summaryResult: SummaryResult,
    selectedRange: SummaryRange,
    lastGeneratedRange: SummaryRange?,
    generatedAtMillis: Long?,
    dataFootprint: SummaryDataFootprint,
    problemEvidence: Map<String, List<String>>,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (lastGeneratedRange != null && lastGeneratedRange != selectedRange) {
            item {
                NoticeCard(
                    text = "你当前选择的是${selectedRange.label()}，但页面显示的还是上次生成的${lastGeneratedRange.label()}总结。想切换范围的话，点右上角刷新就行。"
                )
            }
        }

        item {
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "智能总结",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        SourceTag(summaryResult.source)
                    }
                    Text(
                        text = summaryResult.naturalLanguageSummary.ifBlank { "暂无总结内容" },
                        style = MaterialTheme.typography.bodyLarge
                    )
                    summaryResult.warningMessage?.takeIf { it.isNotBlank() }?.let { warning ->
                        Text(
                            text = warning,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        item {
            SummaryMetaCard(
                generatedAtMillis = generatedAtMillis,
                selectedRange = lastGeneratedRange ?: selectedRange,
                dataFootprint = dataFootprint
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatItem(
                    title = "总时长",
                    value = formatMillis(summaryResult.totalStudyMillis),
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    title = "覆盖天数",
                    value = "${summaryResult.coverDays} 天",
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    title = "问题数",
                    value = summaryResult.commonProblems.size.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (summaryResult.dailyDurations.isNotEmpty()) {
            item {
                TrendCard(dailyDurations = summaryResult.dailyDurations)
            }
        }

        if (summaryResult.strengths.isNotEmpty()) {
            item {
                InsightListCard(
                    title = "这段时间做得好的地方",
                    items = summaryResult.strengths,
                    accentColor = MaterialTheme.colorScheme.tertiary,
                    emptyHint = "暂无明显优势"
                )
            }
        }

        if (summaryResult.commonProblems.isNotEmpty()) {
            item {
                ProblemInsightCard(
                    title = "重点问题",
                    items = summaryResult.commonProblems,
                    evidence = problemEvidence
                )
            }
        }

        if (summaryResult.suggestions.isNotEmpty()) {
            item {
                InsightListCard(
                    title = "行动建议",
                    items = summaryResult.suggestions,
                    accentColor = MaterialTheme.colorScheme.primary,
                    emptyHint = "暂无行动建议"
                )
            }
        }

        if (summaryResult.topTomorrowGoals.isNotEmpty()) {
            item {
                InsightListCard(
                    title = "明日重点",
                    items = summaryResult.topTomorrowGoals,
                    accentColor = MaterialTheme.colorScheme.secondary,
                    emptyHint = "暂无明日重点",
                    headerAction = {
                        TextButton(
                            onClick = {
                                clipboardManager.setText(
                                    AnnotatedString(summaryResult.topTomorrowGoals.joinToString(separator = "\n") { "• $it" })
                                )
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("复制")
                        }
                    }
                )
            }
        }

        if (summaryResult.topSubjects.isNotEmpty()) {
            item {
                SubjectFocusCard(subjects = summaryResult.topSubjects)
            }
        }
    }
}

@Composable
private fun SummaryMetaCard(
    generatedAtMillis: Long?,
    selectedRange: SummaryRange,
    dataFootprint: SummaryDataFootprint
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "本次总结信息",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            SummaryMetaRow(label = "总结范围", value = selectedRange.label())
            SummaryMetaRow(
                label = "生成时间",
                value = generatedAtMillis?.let(::formatGeneratedTime) ?: "本次会话内尚未生成"
            )
            SummaryMetaRow(
                label = "参考数据",
                value = buildString {
                    append("学习记录 ${dataFootprint.sessionCount} 条")
                    append(" · 复盘 ${dataFootprint.reviewCount} 条")
                    if (dataFootprint.noteCount > 0) {
                        append(" · 含备注 ${dataFootprint.noteCount} 条")
                    }
                }
            )
            Text(
                text = dataFootprint.confidenceHint(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SummaryMetaRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun NoticeCard(text: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.82f)
        )
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun TrendCard(dailyDurations: Map<String, Long>) {
    val sortedData = dailyDurations.toList().sortedBy { it.first }
    val maxValue = sortedData.maxOfOrNull { it.second }?.coerceAtLeast(1L) ?: 1L
    val trendSummary = buildTrendSummary(sortedData)

    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "学习趋势",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            MiniBarChart(data = sortedData, maxValue = maxValue)
            Text(
                text = trendSummary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MiniBarChart(
    data: List<Pair<String, Long>>,
    maxValue: Long
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(168.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        data.takeLast(7).forEach { (date, value) ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = durationLabel(value),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((112f * (value.toFloat() / maxValue.toFloat())).coerceAtLeast(10f).dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.86f),
                                shape = RoundedCornerShape(12.dp)
                            )
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = DateUtils.formatDisplayDate(date),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun InsightListCard(
    title: String,
    items: List<String>,
    accentColor: Color,
    emptyHint: String,
    headerAction: @Composable (() -> Unit)? = null
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                headerAction?.invoke()
            }

            if (items.isEmpty()) {
                Text(
                    text = emptyHint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                items.forEachIndexed { index, item ->
                    InsightRow(
                        index = index + 1,
                        text = item,
                        accentColor = accentColor
                    )
                }
            }
        }
    }
}

@Composable
private fun ProblemInsightCard(
    title: String,
    items: List<String>,
    evidence: Map<String, List<String>>
) {
    val expandedItems = remember { mutableStateListOf<String>() }

    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            items.forEachIndexed { index, item ->
                val relatedEvidence = evidence[item].orEmpty()
                val isExpanded = item in expandedItems

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                NumberBadge(index = index + 1, accentColor = MaterialTheme.colorScheme.error)
                                Text(
                                    text = item,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (relatedEvidence.isNotEmpty()) {
                                TextButton(
                                    onClick = {
                                        if (isExpanded) {
                                            expandedItems.remove(item)
                                        } else {
                                            expandedItems.add(item)
                                        }
                                    }
                                ) {
                                    Text(if (isExpanded) "收起依据" else "查看依据")
                                }
                            }
                        }

                        if (isExpanded && relatedEvidence.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            relatedEvidence.forEach { snippet ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 6.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.background
                                ) {
                                    Text(
                                        text = snippet,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
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
    }
}

@Composable
private fun InsightRow(
    index: Int,
    text: String,
    accentColor: Color
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            NumberBadge(index = index, accentColor = accentColor)
            Text(
                text = text,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun NumberBadge(index: Int, accentColor: Color) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .background(accentColor.copy(alpha = 0.16f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = index.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = accentColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SubjectFocusCard(subjects: List<Pair<String, Long>>) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "高投入科目",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            subjects.forEachIndexed { index, (name, duration) ->
                InsightRow(
                    index = index + 1,
                    text = "$name · ${DateUtils.formatDuration(duration)}",
                    accentColor = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 92.dp)
                .padding(vertical = 14.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SourceTag(source: SummarySource) {
    val (label, color, textColor) = when (source) {
        SummarySource.AI -> Triple(
            "AI增强",
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        SummarySource.HYBRID_FALLBACK -> Triple(
            "回退结果",
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
        SummarySource.RULE -> Triple(
            "本地规则",
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Surface(
        color = color,
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun buildChatContextNotice(uiState: SummaryUiState): String? {
    return when {
        uiState.result == null -> "当前还没有生成总结，也可以继续提问；回答会尽量参考最近的学习和复盘记录。"
        uiState.lastGeneratedRange != null && uiState.lastGeneratedRange != uiState.range ->
            "当前对话仍基于上次生成的${uiState.lastGeneratedRange.label()}总结。若想切换到${uiState.range.label()}，请先点右上角刷新。"
        else -> null
    }
}

private fun SummaryRange.label(): String = when (this) {
    SummaryRange.LAST_7_DAYS -> "近7天"
    SummaryRange.LAST_30_DAYS -> "近30天"
    SummaryRange.ALL -> "全部"
}

private fun SummaryTab.label(): String = when (this) {
    SummaryTab.SUMMARY -> "总结"
    SummaryTab.CHAT -> "对话"
}

private fun formatMillis(millis: Long): String {
    if (millis <= 0L) return "0h"
    val totalMinutes = millis / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (minutes == 0L) {
        "${hours}h"
    } else {
        "${hours}h ${minutes}m"
    }
}

private fun formatGeneratedTime(timestamp: Long): String {
    return try {
        SimpleDateFormat("MM月dd日 HH:mm", Locale.getDefault()).format(Date(timestamp))
    } catch (_: Exception) {
        "刚刚"
    }
}

private fun durationLabel(millis: Long): String {
    val totalMinutes = millis / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours <= 0) "${minutes}m" else "${hours}h"
}

private fun buildTrendSummary(data: List<Pair<String, Long>>): String {
    if (data.size < 2) return "当前样本较少，后面再多积累几天，会更容易看出节奏变化。"
    val lastThree = data.takeLast(3).sumOf { it.second }
    val previousThree = data.dropLast(3).takeLast(3).sumOf { it.second }
    if (previousThree <= 0L && lastThree > 0L) return "最近几天开始有明显投入，节奏正在重新拉起来。"
    if (previousThree <= 0L) return "这段时间记录还比较少，趋势判断暂时不稳定。"
    val ratio = lastThree.toDouble() / previousThree.toDouble()
    return when {
        ratio >= 1.2 -> "最近 3 天比前一段更投入，整体有回升趋势。"
        ratio <= 0.8 -> "最近 3 天比前一段有所下滑，适合把明日重点压缩到更具体的 1~3 件事。"
        else -> "最近几天整体比较平稳，接下来更适合追求连续性，而不是突然猛学。"
    }
}

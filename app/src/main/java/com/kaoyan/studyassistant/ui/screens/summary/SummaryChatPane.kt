package com.kaoyan.studyassistant.ui.screens.summary

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kaoyan.studyassistant.domain.summarizer.SummaryChatMessage
import com.kaoyan.studyassistant.domain.summarizer.SummaryChatRole

/**
 * 聊天面板
 *
 * 键盘适配策略：
 * - 整体使用 Column（fillMaxSize） + imePadding() 包裹，让整个面板随键盘上移
 * - LazyColumn（消息列表）占据剩余空间（weight(1f)），内容可滚动
 * - 底部输入栏固定在 Column 末尾，不参与滚动
 * - navigationBarsPadding() 应用在输入栏容器上，保证底部安全区
 * - 不使用嵌套 Scaffold，避免 innerPadding 与 imePadding 叠加导致间距失控
 * - 不使用条件切换 imePadding/navigationBarsPadding，统一由 imePadding() 处理
 *   （键盘收起时 ime inset = 0，imePadding 退化为 0；navigationBarsPadding 始终保证底部安全区）
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SummaryChatPane(
    messages: List<SummaryChatMessage>,
    inputText: String,
    isSending: Boolean,
    errorMessage: String?,
    contextNotice: String?,
    quickQuestions: List<String>,
    onQuickQuestionClick: (String) -> Unit,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit,
    onDeleteMessage: (Long) -> Unit,
    onClearConversation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    var lastMessageCount by remember { mutableIntStateOf(0) }
    var pendingDeleteMessage by remember { mutableStateOf<SummaryChatMessage?>(null) }
    var showClearDialog by rememberSaveable { mutableStateOf(false) }

    // 新消息到达时自动滚动到底部
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty() && messages.size != lastMessageCount) {
            val targetIndex = messages.lastIndex
            listState.scrollToItem(targetIndex)
        }
        lastMessageCount = messages.size
    }

    // 核心布局：Column + imePadding()
    // imePadding() 会在键盘弹出时为整个 Column 添加底部 padding，
    // 使 Column 整体上移，输入框自然跟随键盘顶部对齐。
    // 键盘收起时 ime inset = 0，imePadding() 不产生额外间距。
    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()   // ← 核心：让整个面板随键盘上移
    ) {
        // ── 消息列表区域（占满剩余空间，可滚动）──────────────────────────────
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),    // 占满剩余高度，输入栏始终固定在底部
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = 8.dp   // 与输入栏之间留出自然间距
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!contextNotice.isNullOrBlank()) {
                item(key = "context_notice") {
                    ChatNoticeCard(text = contextNotice)
                }
            }

            if (messages.isNotEmpty()) {
                item(key = "clear_action") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showClearDialog = true }) {
                            Text("清空对话")
                        }
                    }
                }
            }

            if (messages.isEmpty() && errorMessage.isNullOrBlank() && !isSending) {
                item(key = "empty_hint") {
                    EmptyChatHint(
                        quickQuestions = quickQuestions,
                        onQuickQuestionClick = onQuickQuestionClick
                    )
                }
            }

            items(
                items = messages,
                key = { message -> message.id }
            ) { message ->
                ChatMessageBubble(
                    message = message,
                    onLongPressDelete = { pendingDeleteMessage = message }
                )
            }

            if (isSending) {
                item(key = "typing_indicator") {
                    TypingIndicatorBubble()
                }
            }

            errorMessage?.takeIf { it.isNotBlank() }?.let { message ->
                item(key = "chat_error") {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = message,
                            modifier = Modifier.padding(14.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        // ── 底部输入栏（固定在 Column 末尾，不随列表滚动）────────────────────
        // 使用 Surface 提供视觉分隔层，避免消息内容与输入框混在一起
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp,     // 轻微阴影，与消息列表形成层次感
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.navigationBarsPadding()) {  // ← 底部安全区
                // 分隔线（可选，增强视觉层次）
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                    thickness = 0.5.dp
                )
                ChatInputBar(
                    value = inputText,
                    onValueChange = onInputChanged,
                    onSend = onSend,
                    enabled = !isSending,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 16.dp,
                            vertical = 10.dp    // 上下留出舒适间距
                        )
                )
            }
        }
    }

    // ── 删除消息确认对话框 ────────────────────────────────────────────────────
    pendingDeleteMessage?.let { targetMessage ->
        AlertDialog(
            onDismissRequest = { pendingDeleteMessage = null },
            title = { Text("删除这条消息？") },
            text = {
                Text(
                    text = targetMessage.content.take(80).ifBlank { "这条消息没有可预览内容。" }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteMessage(targetMessage.id)
                        pendingDeleteMessage = null
                    }
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteMessage = null }) {
                    Text("取消")
                }
            }
        )
    }

    // ── 清空对话确认对话框 ────────────────────────────────────────────────────
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清空全部对话？") },
            text = { Text("清空后无法恢复，这次会话中的所有聊天记录都会被删除。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearConversation()
                        showClearDialog = false
                    }
                ) {
                    Text("清空")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 子组件
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ChatNoticeCard(text: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        )
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyChatHint(
    quickQuestions: List<String>,
    onQuickQuestionClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
            )
        ) {
            Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
                Text(
                    text = "还没有聊天记录，可以直接点下面的问题继续问。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        quickQuestions.take(4).forEach { question ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onQuickQuestionClick(question) },
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f)
            ) {
                Text(
                    text = question,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun TypingIndicatorBubble() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
                Text(
                    text = "正在回复…",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatMessageBubble(
    message: SummaryChatMessage,
    onLongPressDelete: () -> Unit
) {
    val isUser = message.role == SummaryChatRole.USER
    val bubbleColor = if (isUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 360.dp)
                .combinedClickable(
                    onClick = {},
                    onLongClick = onLongPressDelete
                ),
            colors = CardDefaults.cardColors(containerColor = bubbleColor)
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
        }
    }
}

/**
 * 底部输入栏
 *
 * - OutlinedTextField 高度限制：min=52dp, max=120dp，超出后内部滚动
 * - 发送按钮与输入框底部对齐（verticalAlignment = Alignment.Bottom）
 * - 发送按钮在有内容且可用时高亮，否则置灰
 */
@Composable
private fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val canSend = enabled && value.isNotBlank()

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Bottom   // 多行时按钮与输入框底部对齐
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 52.dp, max = 120.dp),   // 超过 120dp 后内部滚动
            enabled = enabled,
            placeholder = {
                Text(
                    text = "继续问点什么…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            },
            maxLines = 4,
            shape = MaterialTheme.shapes.medium,    // 圆角与整体风格一致
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        )

        // 发送按钮：使用 Surface + IconButton，视觉上更圆润统一
        Surface(
            color = if (canSend) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.size(52.dp)     // 与输入框最小高度保持一致
        ) {
            IconButton(
                onClick = onSend,
                enabled = canSend,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "发送",
                    tint = if (canSend) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    }
                )
            }
        }
    }
}

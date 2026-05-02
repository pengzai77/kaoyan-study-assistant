package com.kaoyan.studyassistant.ui.screens.subject

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kaoyan.studyassistant.data.local.entity.Subject
import com.kaoyan.studyassistant.ui.components.EmptyState
import com.kaoyan.studyassistant.ui.components.KaoyanTopBar
import com.kaoyan.studyassistant.ui.components.rememberParsedColor

/**
 * 科目管理页面
 */
@Composable
fun SubjectManageScreen(
    navController: NavController,
    viewModel: SubjectViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            KaoyanTopBar(
                title = "科目管理",
                onBack = { navController.popBackStack() }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::showAddDialog,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加科目")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (uiState.subjects.isEmpty()) {
            EmptyState(
                message = "暂无科目，点击右下角添加",
                modifier = Modifier.fillMaxSize().padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        text = "默认科目不可删除，可以修改名称和颜色",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                items(uiState.subjects, key = { it.id }) { subject ->
                    SubjectItem(
                        subject = subject,
                        onEdit = { viewModel.showEditDialog(subject) },
                        onDelete = { viewModel.deleteSubject(subject) }
                    )
                }
            }
        }
    }

    // 新增/编辑对话框
    if (uiState.showAddDialog) {
        SubjectDialog(
            editingSubject = uiState.editingSubject,
            onConfirm = { name, color ->
                if (uiState.editingSubject != null) {
                    viewModel.updateSubject(uiState.editingSubject!!, name, color)
                } else {
                    viewModel.addSubject(name, color)
                }
            },
            onDismiss = viewModel::hideDialog
        )
    }
}

/** 科目列表项 */
@Composable
private fun SubjectItem(
    subject: Subject,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val subjectColor = rememberParsedColor(subject.color)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(16.dp).clip(CircleShape).background(subjectColor)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = subject.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            if (subject.isDefault) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "默认科目",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "编辑", modifier = Modifier.size(18.dp))
            }
            if (!subject.isDefault) {
                IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除科目") },
            text = { Text("确定要删除科目「${subject.name}」吗？相关学习记录不会被删除。") },
            confirmButton = {
                TextButton(
                    onClick = { onDelete(); showDeleteDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            }
        )
    }
}

/** 颜色选项 */
private val colorOptions = listOf(
    "#E53935", "#D81B60", "#8E24AA", "#3949AB",
    "#1E88E5", "#039BE5", "#00ACC1", "#00897B",
    "#43A047", "#7CB342", "#FB8C00", "#F4511E",
    "#6D4C41", "#546E7A", "#4A90D9", "#757575"
)

/** 科目新增/编辑对话框 */
@Composable
private fun SubjectDialog(
    editingSubject: Subject?,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(editingSubject?.name ?: "") }
    var selectedColor by remember { mutableStateOf(editingSubject?.color ?: "#4A90D9") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editingSubject != null) "编辑科目" else "添加科目") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("科目名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                Text("选择颜色：", style = MaterialTheme.typography.bodySmall)
                // 颜色选择网格
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    colorOptions.chunked(8).forEach { rowColors ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            rowColors.forEach { colorHex ->
                                val color = rememberParsedColor(colorHex, Color.Gray)
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (selectedColor == colorHex)
                                                color
                                            else color.copy(alpha = 0.7f)
                                        )
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { selectedColor = colorHex },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selectedColor == colorHex) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(Color.White.copy(alpha = 0.8f))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name, selectedColor) },
                enabled = name.isNotBlank()
            ) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

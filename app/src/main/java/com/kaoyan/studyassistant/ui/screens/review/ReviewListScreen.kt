package com.kaoyan.studyassistant.ui.screens.review

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kaoyan.studyassistant.data.local.entity.DailyReview
import com.kaoyan.studyassistant.ui.components.EmptyState
import com.kaoyan.studyassistant.ui.components.KaoyanTopBar
import com.kaoyan.studyassistant.ui.components.ShimmerList
import com.kaoyan.studyassistant.ui.navigation.NavRoutes
import com.kaoyan.studyassistant.ui.theme.KaoyanAnimSpec
import com.kaoyan.studyassistant.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewListScreen(
    navController: NavController,
    viewModel: ReviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            KaoyanTopBar(
                title = "每日复盘",
                onBack = { navController.popBackStack() }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(NavRoutes.reviewEdit(0L)) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "新增复盘")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        AnimatedContent(
            targetState = when {
                uiState.isLoading -> "loading"
                uiState.reviews.isEmpty() -> "empty"
                else -> "list"
            },
            transitionSpec = {
                fadeIn(tween(KaoyanAnimSpec.MEDIUM, easing = KaoyanAnimSpec.EmphasizedEasing)) togetherWith
                    fadeOut(tween(KaoyanAnimSpec.SHORT, easing = KaoyanAnimSpec.AccelerateEasing))
            },
            label = "review_list_state",
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) { state ->
            when (state) {
                "loading" -> {
                    ShimmerList(count = 5)
                }

                "empty" -> {
                    EmptyState(
                        message = "还没有复盘记录\n点击右下角 + 开始今日复盘",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.reviews, key = { it.id }) { review ->
                            ReviewListCard(
                                review = review,
                                onEdit = { navController.navigate(NavRoutes.reviewEdit(review.id)) },
                                onDelete = { viewModel.deleteReview(review) },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewListCard(
    review: DailyReview,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = DateUtils.formatFullDate(review.date),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "编辑", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "删除",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (review.completedContent.isNotBlank()) {
                ReviewSection(
                    label = "完成内容",
                    content = review.completedContent,
                    maxLines = 2,
                    accentColor = Color(0xFF2E7D32),
                    icon = Icons.Default.CheckCircle
                )
            }
            if (review.problems.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                ReviewSection(
                    label = "遇到问题",
                    content = review.problems,
                    maxLines = 2,
                    accentColor = Color(0xFFF57C00),
                    icon = Icons.Default.ReportProblem
                )
            }
            if (review.tomorrowPlan.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                ReviewSection(
                    label = "明日计划",
                    content = review.tomorrowPlan,
                    maxLines = 2,
                    accentColor = Color(0xFF1565C0),
                    icon = Icons.Default.EventNote
                )
            }
        }
    }

    AnimatedVisibility(
        visible = showDeleteDialog,
        enter = fadeIn(tween(KaoyanAnimSpec.SHORT)),
        exit = fadeOut(tween(KaoyanAnimSpec.SHORT))
    ) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除复盘") },
            text = { Text("确定要删除 ${DateUtils.formatFullDate(review.date)} 的复盘吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun ReviewSection(
    label: String,
    content: String,
    maxLines: Int = Int.MAX_VALUE,
    accentColor: Color,
    icon: ImageVector
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = accentColor.copy(alpha = 0.08f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(
                        color = accentColor.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(999.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = maxLines
        )
    }
}

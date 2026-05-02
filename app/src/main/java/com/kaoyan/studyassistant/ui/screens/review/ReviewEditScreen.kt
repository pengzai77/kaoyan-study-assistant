package com.kaoyan.studyassistant.ui.screens.review

import android.app.DatePickerDialog
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kaoyan.studyassistant.ui.components.KaoyanTopBar
import com.kaoyan.studyassistant.util.DateUtils
import java.util.Calendar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ReviewEditScreen(
    navController: NavController,
    reviewId: Long,
    viewModel: ReviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val editState by viewModel.editState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(reviewId) {
        viewModel.initEdit(reviewId)
    }

    LaunchedEffect(uiState.savedSuccess) {
        if (uiState.savedSuccess) {
            snackbarHostState.showSnackbar("复盘已保存")
            viewModel.clearSuccess()
            navController.popBackStack()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            KaoyanTopBar(
                title = if (editState.isNew) "新建复盘" else "编辑复盘",
                onBack = { navController.popBackStack() },
                actions = {
                    TextButton(onClick = viewModel::saveReview) {
                        Text("保存", fontWeight = FontWeight.SemiBold)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
        ) {
            item(key = "date_card") {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val cal = DateUtils.parseDate(editState.date) ?: Calendar.getInstance()
                            val picker = DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val newDate = "%04d-%02d-%02d".format(year, month + 1, dayOfMonth)
                                    viewModel.updateDate(newDate)
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            )
                            picker.datePicker.maxDate = System.currentTimeMillis()
                            picker.show()
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "复盘日期",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = DateUtils.formatFullDate(editState.date),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "选择日期",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            item(key = "completed") {
                ReviewTextField(
                    label = "今天完成内容",
                    placeholder = "今天完成了哪些学习任务？",
                    value = editState.completedContent,
                    onValueChange = viewModel::updateCompletedContent,
                    minLines = 3
                )
            }

            item(key = "problems") {
                ReviewTextField(
                    label = "今天遇到的问题",
                    placeholder = "学习中遇到了哪些困难或卡点？",
                    value = editState.problems,
                    onValueChange = viewModel::updateProblems,
                    minLines = 3
                )
            }

            item(key = "tomorrow") {
                ReviewTextField(
                    label = "明日计划",
                    placeholder = "明天准备优先完成哪些任务？",
                    value = editState.tomorrowPlan,
                    onValueChange = viewModel::updateTomorrowPlan,
                    minLines = 3
                )
            }

            item(key = "extra_notes") {
                ReviewTextField(
                    label = "补充记录",
                    placeholder = "还有什么值得记下来的想法？",
                    value = editState.extraNotes,
                    onValueChange = viewModel::updateExtraNotes,
                    minLines = 2
                )
            }

            item(key = "save_button") {
                Button(
                    onClick = viewModel::saveReview,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("保存复盘", modifier = Modifier.padding(vertical = 4.dp))
                }
            }

            item(key = "bottom_space") {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReviewTextField(
    label: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    minLines: Int = 3
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()

    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .bringIntoViewRequester(bringIntoViewRequester)
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        scope.launch {
                            delay(250)
                            bringIntoViewRequester.bringIntoView()
                        }
                    }
                },
            minLines = minLines
        )
    }
}

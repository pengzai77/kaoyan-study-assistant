package com.kaoyan.studyassistant.ui.screens.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaoyan.studyassistant.data.backup.AutoBackupCoordinator
import com.kaoyan.studyassistant.data.local.entity.DailyReview
import com.kaoyan.studyassistant.data.repository.DailyReviewRepository
import com.kaoyan.studyassistant.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReviewUiState(
    val reviews: List<DailyReview> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val savedSuccess: Boolean = false
)

data class ReviewEditState(
    val id: Long = 0L,
    val date: String = DateUtils.today(),
    val completedContent: String = "",
    val problems: String = "",
    val tomorrowPlan: String = "",
    val extraNotes: String = "",
    val isNew: Boolean = true
)

/**
 * 每日复盘 ViewModel
 */
@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val reviewRepository: DailyReviewRepository,
    private val autoBackupCoordinator: AutoBackupCoordinator
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    private val _editState = MutableStateFlow(ReviewEditState())
    val editState: StateFlow<ReviewEditState> = _editState.asStateFlow()

    init {
        viewModelScope.launch {
            reviewRepository.getAllReviews().collect { reviews ->
                _uiState.value = _uiState.value.copy(reviews = reviews, isLoading = false)
            }
        }
    }

    /** 初始化编辑状态（新建或编辑） */
    fun initEdit(reviewId: Long?) {
        viewModelScope.launch {
            if (reviewId == null || reviewId == 0L) {
                // 新建：检查今天是否已有复盘
                val existing = reviewRepository.getReviewByDate(DateUtils.today())
                if (existing != null) {
                    _editState.value = ReviewEditState(
                        id = existing.id,
                        date = existing.date,
                        completedContent = existing.completedContent,
                        problems = existing.problems,
                        tomorrowPlan = existing.tomorrowPlan,
                        extraNotes = existing.extraNotes,
                        isNew = false
                    )
                } else {
                    _editState.value = ReviewEditState()
                }
            } else {
                // 编辑已有复盘
                val review = reviewRepository.getReviewById(reviewId)
                if (review != null) {
                    _editState.value = ReviewEditState(
                        id = review.id,
                        date = review.date,
                        completedContent = review.completedContent,
                        problems = review.problems,
                        tomorrowPlan = review.tomorrowPlan,
                        extraNotes = review.extraNotes,
                        isNew = false
                    )
                }
            }
        }
    }

    fun updateCompletedContent(v: String) { _editState.value = _editState.value.copy(completedContent = v) }
    fun updateProblems(v: String) { _editState.value = _editState.value.copy(problems = v) }
    fun updateTomorrowPlan(v: String) { _editState.value = _editState.value.copy(tomorrowPlan = v) }
    fun updateExtraNotes(v: String) { _editState.value = _editState.value.copy(extraNotes = v) }
    fun updateDate(v: String) { _editState.value = _editState.value.copy(date = v) }

    /** 保存复盘 */
    fun saveReview() {
        val edit = _editState.value
        if (edit.completedContent.isBlank() && edit.problems.isBlank() && edit.tomorrowPlan.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "请至少填写一项内容")
            return
        }
        viewModelScope.launch {
            try {
                val review = DailyReview(
                    id = edit.id,
                    date = edit.date,
                    completedContent = edit.completedContent,
                    problems = edit.problems,
                    tomorrowPlan = edit.tomorrowPlan,
                    extraNotes = edit.extraNotes
                )
                reviewRepository.saveReview(review)
                autoBackupCoordinator.triggerIfEnabled()
                _uiState.value = _uiState.value.copy(savedSuccess = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "保存失败：${e.message}")
            }
        }
    }

    /** 删除复盘 */
    fun deleteReview(review: DailyReview) {
        viewModelScope.launch {
            reviewRepository.deleteReview(review)
            autoBackupCoordinator.triggerIfEnabled()
        }
    }

    fun clearError() { _uiState.value = _uiState.value.copy(errorMessage = null) }
    fun clearSuccess() { _uiState.value = _uiState.value.copy(savedSuccess = false) }
}

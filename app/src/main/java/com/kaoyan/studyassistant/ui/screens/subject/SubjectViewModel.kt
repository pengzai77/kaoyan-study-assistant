package com.kaoyan.studyassistant.ui.screens.subject

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaoyan.studyassistant.data.backup.AutoBackupCoordinator
import com.kaoyan.studyassistant.data.local.entity.Subject
import com.kaoyan.studyassistant.data.repository.SubjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubjectUiState(
    val subjects: List<Subject> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val showAddDialog: Boolean = false,
    val editingSubject: Subject? = null
)

@HiltViewModel
class SubjectViewModel @Inject constructor(
    private val subjectRepository: SubjectRepository,
    private val autoBackupCoordinator: AutoBackupCoordinator
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubjectUiState())
    val uiState: StateFlow<SubjectUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            subjectRepository.getAllSubjects().collect { subjects ->
                _uiState.value = _uiState.value.copy(subjects = subjects, isLoading = false)
            }
        }
    }

    fun showAddDialog() { _uiState.value = _uiState.value.copy(showAddDialog = true, editingSubject = null) }
    fun showEditDialog(subject: Subject) { _uiState.value = _uiState.value.copy(showAddDialog = true, editingSubject = subject) }
    fun hideDialog() { _uiState.value = _uiState.value.copy(showAddDialog = false, editingSubject = null) }

    fun addSubject(name: String, color: String) {
        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "科目名称不能为空")
            return
        }
        viewModelScope.launch {
            subjectRepository.addSubject(name.trim(), color)
            autoBackupCoordinator.triggerIfEnabled()
            hideDialog()
        }
    }

    fun updateSubject(subject: Subject, newName: String, newColor: String) {
        if (newName.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "科目名称不能为空")
            return
        }
        viewModelScope.launch {
            subjectRepository.updateSubject(subject.copy(name = newName.trim(), color = newColor))
            autoBackupCoordinator.triggerIfEnabled()
            hideDialog()
        }
    }

    fun deleteSubject(subject: Subject) {
        if (subject.isDefault) {
            _uiState.value = _uiState.value.copy(errorMessage = "默认科目不可删除")
            return
        }
        viewModelScope.launch {
            subjectRepository.deleteSubject(subject)
            autoBackupCoordinator.triggerIfEnabled()
        }
    }

    fun clearError() { _uiState.value = _uiState.value.copy(errorMessage = null) }
}

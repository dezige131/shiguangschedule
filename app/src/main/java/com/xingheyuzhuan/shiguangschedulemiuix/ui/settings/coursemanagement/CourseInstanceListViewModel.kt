package com.xingheyuzhuan.shiguangschedulemiuix.ui.settings.coursemanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xingheyuzhuan.shiguangschedulemiuix.data.db.main.CourseWithWeeks
import com.xingheyuzhuan.shiguangschedulemiuix.data.model.DualColor
import com.xingheyuzhuan.shiguangschedulemiuix.data.repository.AppSettingsRepository
import com.xingheyuzhuan.shiguangschedulemiuix.data.repository.CourseTableRepository
import com.xingheyuzhuan.shiguangschedulemiuix.data.repository.StyleSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CourseInstanceListViewModel @Inject constructor(
    // 🚨 移除了 savedStateHandle
    private val appSettingsRepository: AppSettingsRepository,
    private val courseTableRepository: CourseTableRepository,
    private val styleSettingsRepository: StyleSettingsRepository
) : ViewModel() {

    // 🌟 新增：用于接收 UI 层传入的 courseName
    private val _selectedCourseName = MutableStateFlow("")

    // 🌟 新增：暴露给 UI 调用的加载方法
    fun loadCourseName(name: String) {
        if (_selectedCourseName.value != name) {
            _selectedCourseName.value = name
        }
    }

    private val currentTableIdFlow = appSettingsRepository.getAppSettings()
        .map { it.currentCourseTableId ?: "" }

    /**
     * 课程实例列表流：现在需要结合 tableId 和 外部传入的 selectedCourseName 一起过滤
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val courseInstances: StateFlow<List<CourseWithWeeks>> = combine(
        currentTableIdFlow,
        _selectedCourseName
    ) { tableId, courseName ->
        Pair(tableId, courseName)
    }.flatMapLatest { (tableId, courseName) ->
        // 如果 tableId 为空，或者还没接收到传进来的 courseName，直接返回空列表
        if (tableId.isEmpty() || courseName.isEmpty()) {
            flowOf(emptyList())
        } else {
            courseTableRepository.getCoursesWithWeeksByTableId(tableId)
                .map { allCourses ->
                    // 过滤出与传入名称一致的课程实例
                    allCourses.filter { it.course.name == courseName }
                }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _isSelectionMode = MutableStateFlow(false)
    private val _selectedCourseIds = MutableStateFlow(emptySet<String>())

    val uiState: StateFlow<CourseInstanceUiState> = combine(
        _isSelectionMode,
        _selectedCourseIds,
        styleSettingsRepository.styleFlow
    ) { isSelection, selectedIds, currentStyle ->
        CourseInstanceUiState(
            isSelectionMode = isSelection,
            selectedCourseIds = selectedIds,
            courseColorMaps = currentStyle.courseColorMaps
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CourseInstanceUiState()
    )

    fun toggleSelectionMode() {
        _isSelectionMode.update { !it }
        if (!_isSelectionMode.value) _selectedCourseIds.value = emptySet()
    }

    fun toggleCourseSelection(courseId: String) {
        _selectedCourseIds.update { currentIds ->
            if (currentIds.contains(courseId)) currentIds - courseId else currentIds + courseId
        }
        if (_selectedCourseIds.value.isNotEmpty() && !_isSelectionMode.value) {
            _isSelectionMode.value = true
        }
    }

    fun toggleSelectAll() {
        val allIds = courseInstances.value.map { it.course.id }.toSet()
        if (_selectedCourseIds.value.size == allIds.size && allIds.isNotEmpty()) {
            _selectedCourseIds.value = emptySet()
        } else {
            _selectedCourseIds.value = allIds
            _isSelectionMode.value = true
        }
    }

    fun deleteSelectedCourses() {
        val idsToDelete = _selectedCourseIds.value.toList()
        if (idsToDelete.isNotEmpty()) {
            viewModelScope.launch {
                courseTableRepository.deleteCoursesByIds(idsToDelete)
                _selectedCourseIds.value = emptySet()
                _isSelectionMode.value = false
            }
        }
    }

    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()
    val selectedCourseIds: StateFlow<Set<String>> = _selectedCourseIds.asStateFlow()
}

data class CourseInstanceUiState(
    val isSelectionMode: Boolean = false,
    val selectedCourseIds: Set<String> = emptySet(),
    val courseColorMaps: List<DualColor> = emptyList()
)
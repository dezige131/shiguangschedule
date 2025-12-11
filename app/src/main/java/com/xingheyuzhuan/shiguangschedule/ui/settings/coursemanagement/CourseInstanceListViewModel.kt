package com.xingheyuzhuan.shiguangschedule.ui.settings.coursemanagement

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.xingheyuzhuan.shiguangschedule.MyApplication
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWithWeeks
import com.xingheyuzhuan.shiguangschedule.data.repository.AppSettingsRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseTableRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// 导航参数的 Key，用于从 SavedStateHandle 中获取课程名称
const val COURSE_NAME_ARG = "courseName"

/**
 * 【二级页面】课程实例列表 ViewModel (Detail View)
 * 负责根据传入的课程名称，过滤出该名称下所有课程实例。
 */
class CourseInstanceListViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val appSettingsRepository: AppSettingsRepository,
    private val courseTableRepository: CourseTableRepository
) : ViewModel() {

    // 1. 从 SavedStateHandle 中获取导航参数：被选中的课程名称
    private val selectedCourseName: String = savedStateHandle[COURSE_NAME_ARG] ?: ""

    // 2. 获取当前激活的课表ID
    private val currentTableIdFlow = appSettingsRepository.getAppSettings()
        .map { it.currentCourseTableId ?: "" }

    /**
     * 暴露给 UI 的该课程名称下的所有实例列表。
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val courseInstances: StateFlow<List<CourseWithWeeks>> = currentTableIdFlow
        .flatMapLatest { tableId ->
            if (tableId.isEmpty() || selectedCourseName.isEmpty()) {
                flowOf(emptyList())
            } else {
                courseTableRepository.getCoursesWithWeeksByTableId(tableId)
            }
        }
        .map { allCourses ->
            // 核心逻辑：过滤出与 selectedCourseName 匹配的课程实例
            allCourses.filter { it.course.name == selectedCourseName }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )


    // 多选模式状态
    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    // 被选中课程ID集合
    private val _selectedCourseIds = MutableStateFlow(emptySet<String>())
    val selectedCourseIds: StateFlow<Set<String>> = _selectedCourseIds.asStateFlow()

    /**
     * 切换选择模式。退出时清空所有选择。
     */
    fun toggleSelectionMode() {
        // UI 上的 MenuOpen 按钮会调用此函数：
        // 1. 如果是 !isSelectionMode (正常模式)，它会进入选择模式。
        // 2. 如果是 isSelectionMode (选择模式)，它会退出选择模式并清空选择。
        _isSelectionMode.update { !it }
        if (!_isSelectionMode.value) {
            _selectedCourseIds.value = emptySet()
        }
    }

    /**
     * 切换单个课程的选择状态。
     * @param courseId 课程的唯一ID。
     */
    fun toggleCourseSelection(courseId: String) {
        _selectedCourseIds.update { currentIds ->
            if (currentIds.contains(courseId)) {
                currentIds - courseId
            } else {
                currentIds + courseId
            }
        }
        if (_selectedCourseIds.value.isNotEmpty() && !_isSelectionMode.value) {
            _isSelectionMode.value = true
        }
    }

    /**
     * 全选/全不选当前列表中的所有课程实例。
     */
    fun toggleSelectAll() {
        val allIds = courseInstances.value.map { it.course.id }.toSet()
        if (_selectedCourseIds.value.size == allIds.size && allIds.isNotEmpty()) {
            // 当前已全选，执行全不选
            _selectedCourseIds.value = emptySet()
        } else {
            // 执行全选
            _selectedCourseIds.value = allIds
            _isSelectionMode.value = true
        }
    }

    /**
     * 批量删除选中的课程实例（基于课程 ID 删除）。
     */
    fun deleteSelectedCourses() {
        val idsToDelete = _selectedCourseIds.value.toList()
        if (idsToDelete.isNotEmpty()) {
            viewModelScope.launch {
                courseTableRepository.deleteCoursesByIds(idsToDelete)
                // 删除完成后，清空选择状态并退出选择模式
                _selectedCourseIds.value = emptySet()
                _isSelectionMode.value = false
            }
        }
    }


    /**
     * ViewModel 的工厂类，用于依赖注入。
     */
    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as MyApplication

                if (modelClass.isAssignableFrom(CourseInstanceListViewModel::class.java)) {
                    val savedStateHandle = extras.createSavedStateHandle()

                    return CourseInstanceListViewModel(
                        savedStateHandle,
                        application.appSettingsRepository,
                        application.courseTableRepository
                    ) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }
}
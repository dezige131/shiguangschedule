package com.xingheyuzhuan.shiguangschedulemiuix.ui.settings.course

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xingheyuzhuan.shiguangschedulemiuix.data.db.main.Course
import com.xingheyuzhuan.shiguangschedulemiuix.data.db.main.TimeSlot
import com.xingheyuzhuan.shiguangschedulemiuix.data.model.DualColor
import com.xingheyuzhuan.shiguangschedulemiuix.data.repository.AppSettingsRepository
import com.xingheyuzhuan.shiguangschedulemiuix.data.repository.CourseTableRepository
import com.xingheyuzhuan.shiguangschedulemiuix.data.repository.StyleSettingsRepository
import com.xingheyuzhuan.shiguangschedulemiuix.data.repository.TimeSlotRepository
import com.xingheyuzhuan.shiguangschedulemiuix.navigation.AddEditCourseChannel
import com.xingheyuzhuan.shiguangschedulemiuix.navigation.PresetCourseData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class CourseScheme(
    val id: String = UUID.randomUUID().toString(),
    val dbId: String? = null,
    val teacher: String = "",
    val position: String = "",
    val day: Int = 1,
    val startSection: Int = 1,
    val endSection: Int = 1,
    val isCustomTime: Boolean = false,
    val customStartTime: String = "08:00",
    val customEndTime: String = "09:35",
    val weeks: Set<Int> = emptySet(),
    val colorIndex: Int = 0
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AddEditCourseViewModel @Inject constructor(
    private val courseTableRepository: CourseTableRepository,
    private val timeSlotRepository: TimeSlotRepository,
    private val appSettingsRepository: AppSettingsRepository,
    private val styleSettingsRepository: StyleSettingsRepository
    // 🚨 1. 删掉这里的 savedStateHandle
) : ViewModel() {

    // 🚨 2. 删掉 rawCourseId 和 courseId 这两行定义

    private val _uiState = MutableStateFlow(AddEditCourseUiState())
    val uiState: StateFlow<AddEditCourseUiState> = _uiState.asStateFlow()

    private val _uiEvent = Channel<UiEvent>(Channel.BUFFERED)
    val uiEvent = _uiEvent.receiveAsFlow()

    private var originalDbIds = setOf<String>()
    private var initialName: String = ""
    private var initialSchemes: List<CourseScheme> = emptyList()

    private var isInitialized: String? = null
    private var loadJob: kotlinx.coroutines.Job? = null

    // 🌟 4. 把原来 init {} 里的代码提取到这里
    fun loadCourse(paramId: String?) {
        // 如果 ID 没变，说明是同一次编辑（比如屏幕旋转），直接返回
        if (isInitialized == paramId) return
        isInitialized = paramId

        // 1. 取消之前的加载任务，防止数据错乱
        loadJob?.cancel()

        // 2. 彻底重置所有内部状态
        originalDbIds = emptySet()
        initialName = ""
        initialSchemes = emptyList()
        _uiState.value = AddEditCourseUiState()

        val targetId = if (paramId == "new_course") null else paramId

        loadJob = viewModelScope.launch {
            val initialPresetData: PresetCourseData? = if (targetId == null) {
                try {
                    AddEditCourseChannel.presetDataFlow.first()
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }

            val appSettingsFlow = appSettingsRepository.getAppSettings()
            val styleFlow = styleSettingsRepository.styleFlow

            val timeSlotsFlow = appSettingsFlow.flatMapLatest { settings ->
                val tid = settings.currentCourseTableId
                timeSlotRepository.getTimeSlotsByCourseTableId(tid)
            }

            val courseConfigFlow = appSettingsFlow.flatMapLatest { settings ->
                val tid = settings.currentCourseTableId
                appSettingsRepository.getCourseTableConfigFlow(tid)
            }

            combine(
                timeSlotsFlow,
                appSettingsFlow,
                courseConfigFlow,
                styleFlow,
                if (targetId != null) {
                    appSettingsFlow.flatMapLatest { settings ->
                        courseTableRepository.getCoursesWithWeeksByTableId(settings.currentCourseTableId)
                            .map { all ->
                                val current = all.find { it.course.id == targetId }
                                if (current != null) {
                                    all.filter { it.course.name == current.course.name }
                                } else {
                                    emptyList()
                                }
                            }
                    }
                } else {
                    flowOf(emptyList())
                }
            ) { timeSlots, appSettings, courseConfig, currentStyle, relatedCourseWithWeeks ->
                _uiState.update { currentState ->
                    val totalWeeks = courseConfig?.semesterTotalWeeks ?: 20
                    val currentColorMaps = currentStyle.courseColorMaps

                    if (currentState.schemes.isEmpty() && !currentState.isDataLoaded) {
                        val schemes = if (targetId == null) { // 👈 这里换成 targetId
                            val newColor = currentStyle.generateRandomColorIndex()
                            listOf(
                                CourseScheme(
                                    teacher = initialPresetData?.teacher.orEmpty(),
                                    position = initialPresetData?.position.orEmpty(),
                                    day = initialPresetData?.day ?: 1,
                                    startSection = initialPresetData?.startSection ?: 1,
                                    endSection = initialPresetData?.endSection ?: 1,
                                    weeks = (1..totalWeeks).toSet(),
                                    colorIndex = newColor
                                )
                            )
                        } else {
                            originalDbIds = relatedCourseWithWeeks.map { it.course.id }.toSet()
                            relatedCourseWithWeeks.map { cw ->
                                CourseScheme(
                                    id = cw.course.id,
                                    dbId = cw.course.id,
                                    teacher = cw.course.teacher,
                                    position = cw.course.position,
                                    day = cw.course.day,
                                    startSection = cw.course.startSection ?: 1,
                                    endSection = cw.course.endSection ?: 1,
                                    isCustomTime = cw.course.isCustomTime,
                                    customStartTime = cw.course.customStartTime.orEmpty(),
                                    customEndTime = cw.course.customEndTime.orEmpty(),
                                    weeks = cw.weeks.map { it.weekNumber }.toSet(),
                                    colorIndex = cw.course.colorInt.coerceIn(
                                        0,
                                        currentColorMaps.size - 1
                                    )
                                )
                            }.sortedWith(schemeComparator())
                        }

                        initialName = if (targetId == null) {
                            initialPresetData?.name.orEmpty()
                        } else {
                            relatedCourseWithWeeks.firstOrNull()?.course?.name.orEmpty()
                        }
                        initialSchemes = schemes

                        currentState.copy(
                            isEditing = targetId != null,
                            isDataLoaded = true,
                            name = initialName,
                            schemes = schemes,
                            timeSlots = timeSlots,
                            currentCourseTableId = appSettings.currentCourseTableId,
                            semesterTotalWeeks = totalWeeks,
                            courseColorMaps = currentColorMaps,
                            colorSchemeMode = currentStyle.colorSchemeMode
                        )
                    } else {
                        currentState.copy(
                            isEditing = targetId != null, // 保持编辑状态
                            timeSlots = timeSlots,
                            semesterTotalWeeks = totalWeeks,
                            courseColorMaps = currentColorMaps,
                            colorSchemeMode = currentStyle.colorSchemeMode
                        )
                    }
                }
            }.collect()
        }
    }

    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    /**
     * 判断是否有未保存的内容变更
     */
    fun hasUnsavedChanges(): Boolean {
        val state = uiState.value
        // 如果数据还没加载好，认为没有变更
        if (!state.isDataLoaded) return false

        // 比较名称或方案列表是否发生变化（CourseScheme 是 data class，支持内容比较）
        return state.name != initialName || state.schemes != initialSchemes
    }

    /**
     * 直接追加到末尾，不触发自动重排，方便用户立即编辑
     */
    fun addScheme() {
        _uiState.update { state ->
            val lastScheme = state.schemes.lastOrNull()
            val newScheme = CourseScheme(
                teacher = lastScheme?.teacher.orEmpty(),
                position = lastScheme?.position.orEmpty(),
                colorIndex = lastScheme?.colorIndex ?: 0,
                weeks = (1..state.semesterTotalWeeks).toSet()
            )
            state.copy(schemes = state.schemes + newScheme)
        }
    }

    fun removeScheme(schemeId: String) {
        _uiState.update { state ->
            if (state.schemes.size <= 1) return@update state
            state.copy(schemes = state.schemes.filter { it.id != schemeId })
        }
    }

    fun updateScheme(schemeId: String, transform: (CourseScheme) -> CourseScheme) {
        _uiState.update { state ->
            state.copy(schemes = state.schemes.map {
                if (it.id == schemeId) transform(it) else it
            })
        }
    }

    /**
     * 切换自定义时间
     */
    fun toggleCustomTime(schemeId: String, isCustom: Boolean) {
        updateScheme(schemeId) { scheme ->
            scheme.copy(
                isCustomTime = isCustom,
                customStartTime = if (isCustom && scheme.customStartTime.isBlank()) "08:00" else scheme.customStartTime,
                customEndTime = if (isCustom && scheme.customEndTime.isBlank()) "09:35" else scheme.customEndTime
            )
        }
    }

    /**
     * 主动排序
     */
    fun requestSort() {
        _uiState.update { state ->
            state.copy(schemes = state.schemes.sortedWith(schemeComparator()))
        }
    }

    fun onSave() {
        viewModelScope.launch {
            val state = uiState.value
            if (state.name.isBlank()) return@launch

            val tableId = state.currentCourseTableId.orEmpty()
            val currentSchemeDbIds = state.schemes.mapNotNull { it.dbId }.toSet()

            (originalDbIds - currentSchemeDbIds).forEach { idToRemove ->
                courseTableRepository.deleteCourse(createEmptyCourseForDelete(idToRemove, tableId))
            }

            state.schemes.forEach { scheme ->
                val course = Course(
                    id = scheme.dbId ?: UUID.randomUUID().toString(),
                    courseTableId = tableId,
                    name = state.name,
                    teacher = scheme.teacher,
                    position = scheme.position,
                    day = scheme.day,
                    startSection = if (scheme.isCustomTime) null else scheme.startSection,
                    endSection = if (scheme.isCustomTime) null else scheme.endSection,
                    isCustomTime = scheme.isCustomTime,
                    customStartTime = if (scheme.isCustomTime) scheme.customStartTime else null,
                    customEndTime = if (scheme.isCustomTime) scheme.customEndTime else null,
                    colorInt = scheme.colorIndex
                )
                courseTableRepository.upsertCourse(course, scheme.weeks.toList())
            }
            _uiEvent.send(UiEvent.SaveSuccess)
        }
    }

    fun onDelete() {
        viewModelScope.launch {
            val tableId = uiState.value.currentCourseTableId.orEmpty()
            originalDbIds.forEach { id ->
                courseTableRepository.deleteCourse(createEmptyCourseForDelete(id, tableId))
            }
            _uiEvent.send(UiEvent.DeleteSuccess)
        }
    }

    fun onCancel() {
        viewModelScope.launch { _uiEvent.send(UiEvent.Cancel) }
    }

    private fun createEmptyCourseForDelete(id: String, tableId: String) = Course(
        id = id,
        courseTableId = tableId,
        name = "",
        teacher = "",
        position = "",
        day = 1,
        startSection = null,
        endSection = null,
        isCustomTime = false,
        customStartTime = null,
        customEndTime = null,
        colorInt = 0
    )

    private fun schemeComparator() = compareBy<CourseScheme>(
        { it.day },
        { if (it.isCustomTime) it.customStartTime else it.startSection.toString().padStart(2, '0') }
    )
}

sealed interface UiEvent {
    data object SaveSuccess : UiEvent
    data object DeleteSuccess : UiEvent
    data object Cancel : UiEvent
}

data class AddEditCourseUiState(
    val isEditing: Boolean = false,
    val isDataLoaded: Boolean = false,
    val name: String = "",
    val schemes: List<CourseScheme> = emptyList(),
    val timeSlots: List<TimeSlot> = emptyList(),
    val currentCourseTableId: String? = null,
    val semesterTotalWeeks: Int = 20,
    val courseColorMaps: List<DualColor> = emptyList(),
    val colorSchemeMode: top.yukonga.miuix.kmp.theme.ColorSchemeMode = top.yukonga.miuix.kmp.theme.ColorSchemeMode.System
)
package com.xingheyuzhuan.shiguangschedulemiuix.ui.settings.course

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.xingheyuzhuan.shiguangschedulemiuix.data.db.main.Course
import com.xingheyuzhuan.shiguangschedulemiuix.data.db.main.TimeSlot
import com.xingheyuzhuan.shiguangschedulemiuix.data.model.DualColor
import com.xingheyuzhuan.shiguangschedulemiuix.data.repository.AppSettingsRepository
import com.xingheyuzhuan.shiguangschedulemiuix.data.repository.CourseTableRepository
import com.xingheyuzhuan.shiguangschedulemiuix.data.repository.StyleSettingsRepository
import com.xingheyuzhuan.shiguangschedulemiuix.data.repository.TimeSlotRepository
import com.xingheyuzhuan.shiguangschedulemiuix.navigation.AddEditCourseChannel
import com.xingheyuzhuan.shiguangschedulemiuix.navigation.PresetCourseData
import com.xingheyuzhuan.shiguangschedulemiuix.service.CourseNotificationWorker
import com.xingheyuzhuan.shiguangschedulemiuix.service.DndSchedulerWorker
import com.xingheyuzhuan.shiguangschedulemiuix.widget.FullDataSyncWorker
import com.xingheyuzhuan.shiguangschedulemiuix.widget.WidgetUiUpdateWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
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
import kotlinx.coroutines.withTimeoutOrNull
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
    private val styleSettingsRepository: StyleSettingsRepository,
    @ApplicationContext private val context: Context // 🌟 修复 1：注入全局上下文用于启动后台任务
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditCourseUiState())
    val uiState: StateFlow<AddEditCourseUiState> = _uiState.asStateFlow()

    private val _uiEvent = Channel<UiEvent>(Channel.BUFFERED)
    val uiEvent = _uiEvent.receiveAsFlow()

    private var originalDbIds = setOf<String>()
    private var initialName: String = ""
    private var initialSchemes: List<CourseScheme> = emptyList()

    private var isLoaded = false
    private var loadedCourseId: String? = null
    private var loadJob: kotlinx.coroutines.Job? = null

    private var cleanupJob: Job? = null

    fun scheduleCleanUp() {
        cleanupJob?.cancel()
        cleanupJob = viewModelScope.launch {
            // 延迟 500ms，避开 Compose 页面退场的动画时间窗口
            delay(500)
            cleanUp()
        }
    }

    // 🌟 修复 2：封装一条完美的自动化流水线
    private fun triggerDataSyncAndRefresh() {
        val syncWork = OneTimeWorkRequestBuilder<FullDataSyncWorker>().build()
        val uiWork = OneTimeWorkRequestBuilder<WidgetUiUpdateWorker>().build()
        val notifWork = OneTimeWorkRequestBuilder<CourseNotificationWorker>().build()
        val dndWork = OneTimeWorkRequestBuilder<DndSchedulerWorker>().build()

        // 链式调用：必须等数据完全同步完(syncWork)，再同时更新UI、闹钟和勿扰模式
        WorkManager.getInstance(context)
            .beginUniqueWork("manual_sync_after_edit", ExistingWorkPolicy.REPLACE, syncWork)
            .then(listOf(uiWork, notifWork, dndWork))
            .enqueue()
    }

    fun cleanUp() {
        isLoaded = false
        loadedCourseId = null
        loadJob?.cancel()
        _uiState.value = AddEditCourseUiState()
        initialName = ""
        initialSchemes = emptyList()
        originalDbIds = emptySet()
    }

    fun loadCourse(paramId: String?) {
        if (cleanupJob?.isActive == true) {
            cleanupJob?.cancel()
            cleanUp()
        }
        if (isLoaded && loadedCourseId == paramId) return
        isLoaded = true
        loadedCourseId = paramId

        loadJob?.cancel()

        val targetId = if (paramId == "new_course") null else paramId

        loadJob = viewModelScope.launch {
            val initialPresetData: PresetCourseData? = if (targetId == null) {
                try {
                    withTimeoutOrNull(200L) {
                        AddEditCourseChannel.presetDataFlow.first()
                    }
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
                        val schemes = if (targetId == null) {
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
                            isEditing = targetId != null,
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

    fun hasUnsavedChanges(): Boolean {
        val state = uiState.value
        if (!state.isDataLoaded) return false
        return state.name != initialName || state.schemes != initialSchemes
    }

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

    fun toggleCustomTime(schemeId: String, isCustom: Boolean) {
        updateScheme(schemeId) { scheme ->
            scheme.copy(
                isCustomTime = isCustom,
                customStartTime = if (isCustom && scheme.customStartTime.isBlank()) "08:00" else scheme.customStartTime,
                customEndTime = if (isCustom && scheme.customEndTime.isBlank()) "09:35" else scheme.customEndTime
            )
        }
    }

    fun requestSort() {
        _uiState.update { state ->
            state.copy(schemes = state.schemes.sortedWith(schemeComparator()))
        }
    }

    fun onSave() {
        viewModelScope.launch {
            val state = uiState.value
            if (!state.isDataLoaded) return@launch
            if (state.name.isBlank()) return@launch

            val tableId = state.currentCourseTableId.orEmpty()
            if (tableId.isBlank()) return@launch

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

            // 🌟 修复 3：保存成功后，立刻启动小部件与通知的关联同步链
            triggerDataSyncAndRefresh()

            _uiEvent.send(UiEvent.SaveSuccess)
        }
    }

    fun onDelete() {
        viewModelScope.launch {
            val state = uiState.value
            if (!state.isDataLoaded) return@launch

            val tableId = state.currentCourseTableId.orEmpty()
            if (tableId.isBlank()) return@launch

            originalDbIds.forEach { id ->
                courseTableRepository.deleteCourse(createEmptyCourseForDelete(id, tableId))
            }

            // 🌟 修复 4：删除成功后，同样要让小部件取消相关的闹钟并重绘
            triggerDataSyncAndRefresh()

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
package com.xingheyuzhuan.shiguangschedule.ui.settings.course

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.xingheyuzhuan.shiguangschedule.data.db.main.Course
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlot
import com.xingheyuzhuan.shiguangschedule.data.repository.AppSettingsRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseTableRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.TimeSlotRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseImportExport
import com.xingheyuzhuan.shiguangschedule.data.repository.DualColor
import com.xingheyuzhuan.shiguangschedule.MyApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import com.xingheyuzhuan.shiguangschedule.navigation.AddEditCourseChannel
import com.xingheyuzhuan.shiguangschedule.navigation.PresetCourseData

class AddEditCourseViewModel(
    private val courseTableRepository: CourseTableRepository,
    private val timeSlotRepository: TimeSlotRepository,
    private val appSettingsRepository: AppSettingsRepository,
    private val courseId: String?,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditCourseUiState())
    val uiState: StateFlow<AddEditCourseUiState> = _uiState.asStateFlow()

    private val _uiEvent = Channel<UiEvent>(Channel.BUFFERED)
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        viewModelScope.launch {

            val initialPresetData: PresetCourseData? = if (courseId == null) {
                try {
                    AddEditCourseChannel.presetDataFlow.first()
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }

            val appSettingsFlow = appSettingsRepository.getAppSettings()

            @OptIn(ExperimentalCoroutinesApi::class)
            val timeSlotsFlow = appSettingsFlow.flatMapLatest { settings ->
                val courseTableId = settings.currentCourseTableId
                if (courseTableId != null) {
                    timeSlotRepository.getTimeSlotsByCourseTableId(courseTableId)
                } else {
                    flowOf(emptyList())
                }
            }

            @OptIn(ExperimentalCoroutinesApi::class)
            val courseConfigFlow = appSettingsFlow.flatMapLatest { settings ->
                val courseTableId = settings.currentCourseTableId
                if (courseTableId != null) {
                    appSettingsRepository.getCourseTableConfigFlow(courseTableId)
                } else {
                    flowOf(null)
                }
            }

            combine(
                timeSlotsFlow,
                appSettingsFlow,
                courseConfigFlow,
                if (courseId != null) {
                    courseTableRepository.getCoursesWithWeeksByTableId(appSettingsRepository.getAppSettings().first().currentCourseTableId.orEmpty())
                        .map { courses ->
                            courses.find { it.course.id == courseId }
                        }
                } else {
                    MutableStateFlow(null)
                }
            ) { timeSlots, appSettings, courseConfig, courseWithWeeks ->
                _uiState.update { currentState ->

                    val totalWeeks = courseConfig?.semesterTotalWeeks ?: 20
                    val maxColorIndex = CourseImportExport.COURSE_COLOR_MAPS.size - 1

                    val (course: Course?, initialColorIndex: Int) = if (currentState.course != null) {
                        Pair(currentState.course, currentState.colorIndex)
                    } else if (courseId == null) {
                        val newColorIndex = CourseImportExport.getRandomColorIndex()
                        val newCourse = Course(
                            id = UUID.randomUUID().toString(),
                            courseTableId = appSettings.currentCourseTableId.orEmpty(),
                            name = "", teacher = "", position = "",
                            day = 1,
                            startSection = 1,
                            endSection = 1,
                            isCustomTime = false,
                            customStartTime = null,
                            customEndTime = null,
                            colorInt = newColorIndex
                        )
                        Pair(newCourse, newColorIndex)
                    } else {
                        val existingCourse = courseWithWeeks?.course
                        val existingColorIndex = existingCourse?.colorInt

                        val validatedIndex = if (existingColorIndex != null && existingColorIndex >= 0 && existingColorIndex <= maxColorIndex) {
                            existingColorIndex
                        } else {
                            CourseImportExport.getRandomColorIndex()
                        }

                        Pair(existingCourse, validatedIndex)
                    }

                    val weeks = currentState.weeks.takeIf { it.isNotEmpty() } ?: if(courseId == null) {
                        (1..totalWeeks).toSet()
                    } else {
                        courseWithWeeks?.weeks?.map { it.weekNumber }?.toSet() ?: emptySet()
                    }

                    val finalDay = initialPresetData?.day ?: course?.day ?: 1
                    val finalStartSection = initialPresetData?.startSection ?: course?.startSection ?: 1
                    val finalEndSection = initialPresetData?.endSection ?: course?.endSection ?: 1

                    currentState.copy(
                        isEditing = courseId != null,
                        course = course,
                        name = initialPresetData?.name ?: course?.name.orEmpty(),
                        teacher = initialPresetData?.teacher ?: course?.teacher.orEmpty(),
                        position = initialPresetData?.position ?: course?.position.orEmpty(),
                        day = finalDay,
                        startSection = finalStartSection,
                        endSection = finalEndSection,
                        isCustomTime = course?.isCustomTime ?: false,
                        customStartTime = course?.customStartTime.orEmpty(),
                        customEndTime = course?.customEndTime.orEmpty(),
                        colorIndex = initialColorIndex,
                        weeks = weeks,
                        timeSlots = timeSlots, // 将非空的 timeSlots 列表传递给 UI 状态
                        currentCourseTableId = appSettings.currentCourseTableId,
                        semesterTotalWeeks = totalWeeks
                    )
                }
            }.collect()
        }
    }

    fun onNameChange(name: String) { _uiState.update { it.copy(name = name) } }
    fun onTeacherChange(teacher: String) { _uiState.update { it.copy(teacher = teacher) } }
    fun onPositionChange(position: String) { _uiState.update { it.copy(position = position) } }
    fun onDayChange(day: Int) { _uiState.update { it.copy(day = day) } }

    fun onStartSectionChange(startSection: Int) {
        _uiState.update { it.copy(startSection = startSection) }
    }

    fun onEndSectionChange(endSection: Int) { _uiState.update { it.copy(endSection = endSection) } }

    fun onWeeksChange(newWeeks: Set<Int>) {
        _uiState.update { it.copy(weeks = newWeeks) }
    }

    fun onColorChange(colorIndex: Int) { _uiState.update { it.copy(colorIndex = colorIndex) } }

    fun onIsCustomTimeChange(isCustom: Boolean) {
        _uiState.update { it.copy(isCustomTime = isCustom) }
    }

    fun onCustomStartTimeChange(time: String) {
        _uiState.update { it.copy(customStartTime = time) }
    }

    fun onCustomEndTimeChange(time: String) {
        _uiState.update { it.copy(customEndTime = time) }
    }

    // 统一的保存函数
    fun onSave() {
        viewModelScope.launch {
            val state = uiState.value

            val colorIndexToSave = state.colorIndex

            val courseToSave = state.course?.copy(
                name = state.name,
                teacher = state.teacher,
                position = state.position,
                day = state.day,

                startSection = state.startSection.takeUnless { state.isCustomTime },
                endSection = state.endSection.takeUnless { state.isCustomTime },

                isCustomTime = state.isCustomTime,
                customStartTime = state.customStartTime.takeIf { state.isCustomTime && it.isNotEmpty() },
                customEndTime = state.customEndTime.takeIf { state.isCustomTime && it.isNotEmpty() },

                colorInt = colorIndexToSave,
                courseTableId = state.currentCourseTableId.orEmpty()
            )
            if (courseToSave != null) {
                courseTableRepository.upsertCourse(courseToSave, state.weeks.toList())
                _uiEvent.send(UiEvent.SaveSuccess)
            }
        }
    }

    // 统一的删除函数
    fun onDelete() {
        viewModelScope.launch {
            uiState.value.course?.let { course ->
                courseTableRepository.deleteCourse(course)
                _uiEvent.send(UiEvent.DeleteSuccess)
            }
        }
    }

    // 统一的取消函数
    fun onCancel() {
        viewModelScope.launch {
            _uiEvent.send(UiEvent.Cancel)
        }
    }

    companion object {
        fun Factory(courseId: String?): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                    if (modelClass.isAssignableFrom(AddEditCourseViewModel::class.java)) {
                        val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as MyApplication
                        return AddEditCourseViewModel(
                            courseTableRepository = application.courseTableRepository,
                            timeSlotRepository = application.timeSlotRepository,
                            appSettingsRepository = application.appSettingsRepository,
                            courseId = courseId,
                        ) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
    }
}

sealed interface UiEvent {
    object SaveSuccess : UiEvent
    object DeleteSuccess : UiEvent
    object Cancel : UiEvent
}

data class AddEditCourseUiState(
    val isEditing: Boolean = false,
    val course: Course? = null,
    val name: String = "",
    val teacher: String = "",
    val position: String = "",
    val day: Int = 1,
    val startSection: Int = 1,
    val endSection: Int = 2,
    val isCustomTime: Boolean = false,
    val customStartTime: String = "",
    val customEndTime: String = "",


    val colorIndex: Int = 0,
    val weeks: Set<Int> = emptySet(),
    val timeSlots: List<TimeSlot> = emptyList(),
    val currentCourseTableId: String? = null,
    val semesterTotalWeeks: Int = 20,
    val courseColorMaps: List<DualColor> = CourseImportExport.COURSE_COLOR_MAPS
)
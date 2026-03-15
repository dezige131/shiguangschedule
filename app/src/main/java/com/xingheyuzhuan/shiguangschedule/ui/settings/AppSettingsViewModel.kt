package com.xingheyuzhuan.shiguangschedule.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseTableConfig
import com.xingheyuzhuan.shiguangschedule.data.model.AppSettingsModel
import com.xingheyuzhuan.shiguangschedule.data.repository.AppSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * 设置页面的逻辑控制器
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appSettingsRepository: AppSettingsRepository
) : ViewModel() {

    // 观察全局偏好设置 (DataStore)
    val appSettingsState: StateFlow<AppSettingsModel> = appSettingsRepository.getAppSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettingsModel())

    // 根据选中的课表 ID 动态切换物理配置流 (Room)
    @OptIn(ExperimentalCoroutinesApi::class)
    val courseTableConfigState: StateFlow<CourseTableConfig?> = appSettingsState
        .flatMapLatest { appSettings ->
            val id = appSettings.currentCourseTableId
            if (id.isNotEmpty()) appSettingsRepository.getCourseTableConfigFlow(id)
            else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // 响应式衍生状态：配置更新时自动重算当前周数
    val currentWeekState: StateFlow<Int?> = courseTableConfigState
        .map { config ->
            if (config == null) return@map null
            val rawWeek = appSettingsRepository.getWeekIndexAtDate(
                targetDate = java.time.LocalDate.now(),
                startDateStr = config.semesterStartDate,
                firstDayOfWeekInt = config.firstDayOfWeek
            )
            rawWeek?.takeIf { it in 1..config.semesterTotalWeeks }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /**
     * 更新周末显示
     */
    fun onShowWeekendsChanged(show: Boolean) {
        viewModelScope.launch {
            courseTableConfigState.value?.let { currentConfig ->
                val update = if (!show) {
                    currentConfig.copy(
                        showWeekends = false,
                        firstDayOfWeek = java.time.DayOfWeek.MONDAY.value
                    )
                } else {
                    currentConfig.copy(showWeekends = true)
                }
                appSettingsRepository.insertOrUpdateCourseConfig(update)
            }
        }
    }

    /**
     * 更新起始日期
     */
    fun onSemesterStartDateSelected(selectedDateMillis: Long?) {
        viewModelScope.launch {
            val dateMillis = selectedDateMillis ?: return@launch
            courseTableConfigState.value?.let { currentConfig ->
                val selectedDate = Instant.ofEpochMilli(dateMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                val newConfig = currentConfig.copy(
                    semesterStartDate = selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                )
                appSettingsRepository.insertOrUpdateCourseConfig(newConfig)
            }
        }
    }

    /**
     * 更新总周数
     */
    fun onSemesterTotalWeeksSelected(totalWeeks: Int) {
        viewModelScope.launch {
            courseTableConfigState.value?.let {
                appSettingsRepository.insertOrUpdateCourseConfig(it.copy(semesterTotalWeeks = totalWeeks))
            }
        }
    }

    /**
     * 手动对齐周数 (联动：反向推算开学日期)
     */
    fun onCurrentWeekManuallySet(weekNumber: Int?) {
        viewModelScope.launch {
            appSettingsRepository.setSemesterStartDateFromWeek(weekNumber)
        }
    }

    /**
     * 更新每周起始日
     */
    fun onFirstDayOfWeekSelected(dayOfWeekInt: Int) {
        viewModelScope.launch {
            courseTableConfigState.value?.let { currentConfig ->
                appSettingsRepository.insertOrUpdateCourseConfig(
                    currentConfig.copy(firstDayOfWeek = dayOfWeekInt)
                )
            }
        }
    }
}
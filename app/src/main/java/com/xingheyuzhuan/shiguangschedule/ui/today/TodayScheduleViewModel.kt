package com.xingheyuzhuan.shiguangschedule.ui.today

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.data.db.widget.WidgetAppSettings
import com.xingheyuzhuan.shiguangschedule.data.db.widget.WidgetCourse
import com.xingheyuzhuan.shiguangschedule.data.model.ScheduleGridStyle
import com.xingheyuzhuan.shiguangschedule.data.repository.StyleSettingsRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.WidgetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class TodayScheduleViewModel @Inject constructor(
    private val application: Application,
    private val widgetRepository: WidgetRepository,
    private val styleSettingsRepository: StyleSettingsRepository
) : ViewModel() {

    private val widgetSettingsFlow: Flow<WidgetAppSettings?> = widgetRepository.getAppSettingsFlow()

    val gridStyle: StateFlow<ScheduleGridStyle> = styleSettingsRepository.styleFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ScheduleGridStyle.DEFAULT
        )

    private fun getString(resId: Int, vararg formatArgs: Any): String {
        return application.getString(resId, *formatArgs)
    }

    val semesterStatus: StateFlow<String> = widgetSettingsFlow.map { widgetSettings ->
        val semesterStartDateStr = widgetSettings?.semesterStartDate
        val totalWeeks = widgetSettings?.semesterTotalWeeks ?: 20
        val semesterStartDate: LocalDate? = try {
            semesterStartDateStr?.let { LocalDate.parse(it) }
        } catch (e: Exception) { null }
        val today = LocalDate.now()

        when {
            semesterStartDate == null -> getString(R.string.title_semester_not_set)
            today.isBefore(semesterStartDate) -> {
                val daysUntilStart = ChronoUnit.DAYS.between(today, semesterStartDate)
                getString(R.string.title_vacation_until_start, daysUntilStart.toString())
            }
            else -> {
                val currentWeek = ChronoUnit.WEEKS.between(semesterStartDate, today).toInt() + 1
                if (currentWeek in 1..totalWeeks) {
                    getString(R.string.title_current_week, currentWeek.toString())
                } else if (currentWeek > totalWeeks) {
                    val weeksOver = currentWeek - totalWeeks
                    getString(R.string.status_semester_ended, weeksOver.toString())
                } else getString(R.string.status_week_calc_error)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = getString(R.string.title_loading)
    )

    private val _todayCourses = MutableStateFlow<List<WidgetCourse>>(emptyList())
    val todayCourses: StateFlow<List<WidgetCourse>> = _todayCourses.asStateFlow()

    init {
        loadTodayCourses()
        viewModelScope.launch {
            widgetRepository.dataUpdatedFlow.collect {
                loadTodayCourses()
            }
        }
    }

    private fun loadTodayCourses() {
        viewModelScope.launch {
            val today = LocalDate.now()
            val todayString = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
            widgetRepository.getWidgetCoursesByDateRange(todayString, todayString).collect { courses ->
                _todayCourses.value = courses
            }
        }
    }
}
package com.xingheyuzhuan.shiguangschedule.ui.today

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xingheyuzhuan.shiguangschedule.MyApplication
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.data.db.widget.WidgetCourse
import com.xingheyuzhuan.shiguangschedule.data.db.widget.WidgetAppSettings
import com.xingheyuzhuan.shiguangschedule.data.model.ScheduleGridStyle
import com.xingheyuzhuan.shiguangschedule.data.repository.StyleSettingsRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.WidgetRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class TodayScheduleViewModel(
    application: Application,
    private val widgetRepository: WidgetRepository,
    private val styleSettingsRepository: StyleSettingsRepository
) : AndroidViewModel(application) {

    private val widgetSettingsFlow: Flow<WidgetAppSettings?> = widgetRepository.getAppSettingsFlow()

    // 暴露样式配置流给 UI
    val gridStyle: StateFlow<ScheduleGridStyle> = styleSettingsRepository.styleFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ScheduleGridStyle.DEFAULT
        )

    // 辅助函数
    private fun getString(resId: Int, vararg formatArgs: Any): String {
        return getApplication<Application>().getString(resId, *formatArgs)
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

    // 更新 Factory 以支持注入 StyleSettingsRepository
    class TodayScheduleViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TodayScheduleViewModel::class.java)) {
                val myApp = application as MyApplication
                return TodayScheduleViewModel(
                    myApp,
                    myApp.widgetRepository,
                    myApp.styleSettingsRepository
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
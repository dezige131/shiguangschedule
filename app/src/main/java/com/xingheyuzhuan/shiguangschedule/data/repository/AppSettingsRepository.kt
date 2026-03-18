package com.xingheyuzhuan.shiguangschedule.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseTableConfig
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseTableConfigDao
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseTableDao
import com.xingheyuzhuan.shiguangschedule.data.model.AppSettingsModel
import com.xingheyuzhuan.shiguangschedule.data.model.AutoControlMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 应用配置领域仓库
 *
 * 核心职责：
 * 1. 采用 SSOT 原则，将底层存储从 Room 迁移至 DataStore，同时对外部调用方保持接口兼容。
 * 2. 协调全局偏好设置 (DataStore) 与课表物理配置 (Room) 之间的数据流。
 * 3. 提供时间维度计算算法（周次偏移、日期回溯）。
 */
@Singleton
class AppSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val courseTableDao: CourseTableDao,
    private val courseTableConfigDao: CourseTableConfigDao
) {
    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    /**
     * 课表配置模板
     * 当 DataStore 选中的课表在数据库中尚未初始化配置时，以此模板为基础进行创建。
     */
    private val COURSE_CONFIG_TEMPLATE = CourseTableConfig(
        courseTableId = "",
        showWeekends = false,
        semesterStartDate = null,
        semesterTotalWeeks = 20,
        defaultClassDuration = 45,
        defaultBreakDuration = 10,
        firstDayOfWeek = DayOfWeek.MONDAY.value
    )

    // 应用全局设置 (原实现：AppSettingsDao -> 现实现：DataStore)

    /**
     * 获取应用设置数据流。
     * 内部实现了 DataStore 到业务模型的映射，并包含“保底课表 ID”获取逻辑。
     */
    fun getAppSettings(): Flow<AppSettingsModel> = dataStore.data.map { prefs ->
        val tableId = prefs[AppSettingsModel.KEY_CURRENT_COURSE_TABLE_ID]
            ?: courseTableDao.getFirstTableOnce()?.id
            ?: ""

        AppSettingsModel(
            currentCourseTableId = tableId,
            reminderEnabled = prefs[AppSettingsModel.KEY_REMINDER_ENABLED] ?: false,
            remindBeforeMinutes = prefs[AppSettingsModel.KEY_REMIND_BEFORE_MINUTES] ?: 15,
            skippedDates = prefs[AppSettingsModel.KEY_SKIPPED_DATES] ?: emptySet(),
            autoModeEnabled = prefs[AppSettingsModel.KEY_AUTO_MODE_ENABLED] ?: false,
            autoControlMode = AutoControlMode.fromString(
                prefs[AppSettingsModel.KEY_AUTO_CONTROL_MODE]
            ),
            compatWearableSync = prefs[AppSettingsModel.KEY_COMPAT_WEARABLE_SYNC] ?: false
        )
    }

    /**
     * 获取一次性的应用设置快照。
     */
    suspend fun getAppSettingsOnce(): AppSettingsModel? {
        return getAppSettings().first()
    }

    /**
     * 更新应用设置。
     * 将对象解构并原子化地写入 DataStore。
     */
    suspend fun insertOrUpdateAppSettings(newSettings: AppSettingsModel) {
        dataStore.edit { prefs ->
            prefs[AppSettingsModel.KEY_CURRENT_COURSE_TABLE_ID] = newSettings.currentCourseTableId
            prefs[AppSettingsModel.KEY_REMINDER_ENABLED] = newSettings.reminderEnabled
            prefs[AppSettingsModel.KEY_REMIND_BEFORE_MINUTES] = newSettings.remindBeforeMinutes
            prefs[AppSettingsModel.KEY_SKIPPED_DATES] = newSettings.skippedDates
            prefs[AppSettingsModel.KEY_AUTO_MODE_ENABLED] = newSettings.autoModeEnabled
            prefs[AppSettingsModel.KEY_AUTO_CONTROL_MODE] = newSettings.autoControlMode.value
            prefs[AppSettingsModel.KEY_COMPAT_WEARABLE_SYNC] = newSettings.compatWearableSync
        }
    }

    // 课表具体物理配置 (由 Room 驱动)

    /**
     * 根据课表ID获取一次性配置快照。
     */
    suspend fun getCourseConfigOnce(tableId: String): CourseTableConfig? {
        return courseTableConfigDao.getConfigOnce(tableId)
    }

    /**
     * 根据课表ID实时获取配置数据流。
     */
    fun getCourseTableConfigFlow(courseTableId: String): Flow<CourseTableConfig?> {
        return courseTableConfigDao.getConfigById(courseTableId)
    }

    /**
     * 更新或插入特定课表的物理配置。
     */
    suspend fun insertOrUpdateCourseConfig(newConfig: CourseTableConfig) {
        val constrainedConfig = when {
            newConfig.firstDayOfWeek == DayOfWeek.SUNDAY.value -> {
                newConfig.copy(showWeekends = true)
            }
            !newConfig.showWeekends -> {
                newConfig.copy(firstDayOfWeek = DayOfWeek.MONDAY.value)
            }
            else -> newConfig
        }
        courseTableConfigDao.insertOrUpdate(constrainedConfig)
    }

    // 业务算法 (时间、周次计算)

    /**
     * 核心周次偏移算法。
     */
    fun getWeekIndexAtDate(
        targetDate: LocalDate,
        startDateStr: String?,
        firstDayOfWeekInt: Int
    ): Int? {
        if (startDateStr.isNullOrEmpty()) return null
        return try {
            val firstDayOfWeek = DayOfWeek.of(firstDayOfWeekInt)
            val alignedStartDate = LocalDate.parse(startDateStr, DATE_FORMATTER)
                .with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
            val alignedTargetDate = targetDate.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
            val diffWeeks = ChronoUnit.WEEKS.between(alignedStartDate, alignedTargetDate).toInt()
            diffWeeks + 1
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 基于当前数据库/DataStore状态计算当前自然周次。
     */
    suspend fun calculateCurrentWeekFromDb(): Int? {
        val appSettings = getAppSettingsOnce() ?: return null
        val currentCourseId = appSettings.currentCourseTableId.ifEmpty { return null }
        val config = courseTableConfigDao.getConfigOnce(currentCourseId) ?: return null

        val rawWeek = getWeekIndexAtDate(
            targetDate = LocalDate.now(),
            startDateStr = config.semesterStartDate,
            firstDayOfWeekInt = config.firstDayOfWeek
        ) ?: return null

        return if (rawWeek in 1..config.semesterTotalWeeks) rawWeek else null
    }

    /**
     * 根据目标周数反推开学日期。
     */
    suspend fun setSemesterStartDateFromWeek(week: Int?) {
        val appSettings = getAppSettingsOnce() ?: return
        val currentCourseId = appSettings.currentCourseTableId.ifEmpty { return }

        val currentConfig = courseTableConfigDao.getConfigOnce(currentCourseId)
            ?: COURSE_CONFIG_TEMPLATE.copy(courseTableId = currentCourseId)

        val newStartDate = if (week != null) {
            calculateSemesterStartDate(week, currentConfig.firstDayOfWeek)
        } else {
            null
        }

        val updatedConfig = currentConfig.copy(semesterStartDate = newStartDate)
        courseTableConfigDao.insertOrUpdate(updatedConfig)
    }

    /**
     * 辅助函数：根据目标周数反推开学日期。
     */
    private fun calculateSemesterStartDate(week: Int, firstDayOfWeekInt: Int): String {
        val today = LocalDate.now()
        val firstDayOfWeek = DayOfWeek.of(firstDayOfWeekInt)
        val startOfThisWeek = today.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
        val weeksToSubtract = (week - 1).toLong()
        val semesterStartDate = startOfThisWeek.minusWeeks(weeksToSubtract)
        return semesterStartDate.format(DATE_FORMATTER)
    }
}
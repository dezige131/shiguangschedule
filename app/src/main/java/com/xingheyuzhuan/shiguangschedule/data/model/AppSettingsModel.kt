package com.xingheyuzhuan.shiguangschedule.data.model

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

/**
 * 上课时的自动化控制模式枚举
 */
enum class AutoControlMode(val value: String) {
    /** 请勿打扰模式 */
    DND("DND"),

    /** 静音模式 */
    SILENT("SILENT");

    companion object {
        /**
         * 根据字符串获取对应的枚举值，如果匹配失败则返回默认的 DND 模式
         */
        fun fromString(value: String?): AutoControlMode {
            return entries.find { it.value == value } ?: DND
        }
    }
}

/**
 * 应用全局设置业务模型（DataStore 专用）
 * 集中管理业务字段、存储键 (Keys) 以及默认值。
 */
data class AppSettingsModel(
    /** 当前正在使用的课表 ID */
    val currentCourseTableId: String = "",

    /** 是否开启上课前提醒 */
    val reminderEnabled: Boolean = false,

    /** 提前提醒的时间（分钟） */
    val remindBeforeMinutes: Int = 15,

    /** 需要跳过的日期集合 (例如: "2024-03-15") */
    val skippedDates: Set<String> = emptySet(),

    /** 自动化模式的总开关 */
    val autoModeEnabled: Boolean = false,

    /** 自动化控制的具体模式，限定为 [AutoControlMode] */
    val autoControlMode: AutoControlMode = AutoControlMode.DND,

    /** * 兼容穿戴设备同步通知的开关
     * true: 开启兼容模式（关闭 Ongoing，方便手环抓取）
     * false: 关闭兼容模式（默认，使用 Android 16 实时更新特性）
     */
    val compatWearableSync: Boolean = false // 默认为关闭
) {
    /**
     * 将 DataStore 的 Key 定义在伴生对象中。
     * 这样在 Repository 中可以直接通过 AppSettingsModel.KEY_xxx 访问，
     * 避免了修改一处逻辑需要动多个文件的问题。
     */
    companion object {
        val KEY_CURRENT_COURSE_TABLE_ID = stringPreferencesKey("current_course_table_id")
        val KEY_REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val KEY_REMIND_BEFORE_MINUTES = intPreferencesKey("remind_before_minutes")
        val KEY_SKIPPED_DATES = stringSetPreferencesKey("skipped_dates")
        val KEY_AUTO_MODE_ENABLED = booleanPreferencesKey("auto_mode_enabled")
        val KEY_AUTO_CONTROL_MODE = stringPreferencesKey("auto_control_mode")
        val KEY_COMPAT_WEARABLE_SYNC = booleanPreferencesKey("compat_wearable_sync")
    }
}
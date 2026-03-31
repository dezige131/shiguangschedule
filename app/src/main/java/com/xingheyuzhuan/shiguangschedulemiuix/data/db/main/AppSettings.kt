package com.xingheyuzhuan.shiguangschedulemiuix.data.db.main

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * [已废弃] Room 实体类，代表“应用设置”数据表。
 * * 迁移说明：
 * 该表中的所有配置项已迁移至 Preferences DataStore。
 * 业务逻辑请使用 [com.xingheyuzhuan.shiguangschedulemiuix.data.repository.AppSettingsRepository]。
 * 本类仅保留用于数据库迁移逻辑，计划在版本 4 (或更高) 物理移除。
 */
@Deprecated(
    message = "应用设置已迁移至 Preferences DataStore。请使用新的数据模型，不要再将其作为业务实体使用。",
    level = DeprecationLevel.WARNING
)
@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey
    val id: Int = 1, // 固定主键，确保数据库中只有一条设置记录
    val currentCourseTableId: String? = null, // 当前正在使用的课表的 ID
    val reminderEnabled: Boolean = false, // 是否开启上课前提醒
    val remindBeforeMinutes: Int = 15, // 上课前提前多少分钟提醒，单位：分钟
    val skippedDates: Set<String>? = null, //跳过的日期
    val autoModeEnabled: Boolean = false, // 上课时行为模式的总开关状态
    val autoControlMode: String = "DND", // 控制上课时采取的具体模式 ("DND" 或 "SILENT")
)
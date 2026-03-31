package com.xingheyuzhuan.shiguangschedulemiuix.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.xingheyuzhuan.shiguangschedulemiuix.data.db.main.MainAppDatabase
import com.xingheyuzhuan.shiguangschedulemiuix.data.model.AppSettingsModel
import com.xingheyuzhuan.shiguangschedulemiuix.data.model.schedule_style.ScheduleGridStyleProto
import com.xingheyuzhuan.shiguangschedulemiuix.data.repository.scheduleGridStyleDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Named
import javax.inject.Singleton

// 定义 AppSettings Preferences DataStore 委托
private val Context.appSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")
// 定义 SchoolHistory Preferences DataStore 委托
private val Context.schoolHistoryDataStore: DataStore<Preferences> by preferencesDataStore(name = "school_history")

@Module
@InstallIn(SingletonComponent::class)
@Suppress("unused")
object DataStoreModule {

    /**
     * 提供课表网格样式 DataStore (Proto 模式)
     */
    @Provides
    @Singleton
    fun provideScheduleStyleDataStore(@ApplicationContext context: Context): DataStore<ScheduleGridStyleProto> {
        return context.scheduleGridStyleDataStore
    }

    /**
     * 提供学校选择历史 DataStore
     */
    @Provides
    @Singleton
    @Named("SchoolHistory")
    fun provideSchoolHistoryDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.schoolHistoryDataStore
    }

    /**
     * 提供全局设置 DataStore，并集成从 Room 到 DataStore 的单次自动迁移逻辑。
     * * [重要提醒 / WARNING]:
     * 当未来决定从项目中物理删除 'AppSettings.kt' 或 'AppSettingsDao.kt' 时，
     * 必须同步清理本方法下方的【异步迁移逻辑块】，否则将导致编译失败。
     */
    @Provides
    @Singleton
    @Named("AppSettings")
    fun provideAppSettingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        val dataStore = context.appSettingsDataStore

        // --- 异步迁移逻辑块开始 (待旧版用户迁移完成后可删除) ---
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                val prefs = dataStore.data.first()

                // 若 DataStore 未写入过核心字段，则视为首次从旧版 Room 迁移
                if (prefs[AppSettingsModel.KEY_CURRENT_COURSE_TABLE_ID] == null) {
                    val db = MainAppDatabase.getDatabase(context)
                    val oldSettings = db.appSettingsDao().getAppSettings().first()

                    oldSettings?.let { old ->
                        dataStore.edit { p ->
                            p[AppSettingsModel.KEY_CURRENT_COURSE_TABLE_ID] = old.currentCourseTableId ?: ""
                            p[AppSettingsModel.KEY_REMINDER_ENABLED] = old.reminderEnabled
                            p[AppSettingsModel.KEY_REMIND_BEFORE_MINUTES] = old.remindBeforeMinutes
                            p[AppSettingsModel.KEY_SKIPPED_DATES] = old.skippedDates ?: emptySet()
                            p[AppSettingsModel.KEY_AUTO_MODE_ENABLED] = old.autoModeEnabled
                            p[AppSettingsModel.KEY_AUTO_CONTROL_MODE] = old.autoControlMode
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace() // 仅记录迁移异常，不阻塞应用启动
            }
        }
        // --- 异步迁移逻辑块结束 ---

        return dataStore
    }
}
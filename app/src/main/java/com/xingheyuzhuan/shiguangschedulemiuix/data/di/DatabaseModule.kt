package com.xingheyuzhuan.shiguangschedulemiuix.data.di

import android.content.Context
import com.xingheyuzhuan.shiguangschedulemiuix.data.db.main.MainAppDatabase
import com.xingheyuzhuan.shiguangschedulemiuix.data.db.widget.WidgetDatabase
import com.xingheyuzhuan.shiguangschedulemiuix.data.db.main.AppSettingsDao
import com.xingheyuzhuan.shiguangschedulemiuix.data.db.main.CourseTableConfigDao
import com.xingheyuzhuan.shiguangschedulemiuix.data.db.main.TimeSlotDao
import com.xingheyuzhuan.shiguangschedulemiuix.data.db.main.CourseDao
import com.xingheyuzhuan.shiguangschedulemiuix.data.db.main.CourseTableDao
import com.xingheyuzhuan.shiguangschedulemiuix.data.db.main.CourseWeekDao
import com.xingheyuzhuan.shiguangschedulemiuix.data.db.widget.WidgetCourseDao
import com.xingheyuzhuan.shiguangschedulemiuix.data.db.widget.WidgetAppSettingsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
@Suppress("unused")
object DatabaseModule {

    // --- 1. 提供数据库实例 ---

    @Provides
    @Singleton
    fun provideMainDatabase(@ApplicationContext context: Context): MainAppDatabase {
        return MainAppDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideWidgetDatabase(@ApplicationContext context: Context): WidgetDatabase {
        return WidgetDatabase.getDatabase(context)
    }

    // --- 2. 提供主数据库 (MainAppDatabase) 的 DAO ---

    @Provides
    fun provideAppSettingsDao(db: MainAppDatabase): AppSettingsDao = db.appSettingsDao()

    @Provides
    fun provideCourseTableConfigDao(db: MainAppDatabase): CourseTableConfigDao = db.courseTableConfigDao()

    @Provides
    fun provideTimeSlotDao(db: MainAppDatabase): TimeSlotDao = db.timeSlotDao()

    @Provides
    fun provideCourseDao(db: MainAppDatabase): CourseDao = db.courseDao()

    @Provides
    fun provideCourseTableDao(db: MainAppDatabase): CourseTableDao = db.courseTableDao()

    @Provides
    fun provideCourseWeekDao(db: MainAppDatabase): CourseWeekDao = db.courseWeekDao()

    // --- 3. 提供小组件数据库 (WidgetDatabase) 的 DAO ---

    @Provides
    fun provideWidgetCourseDao(db: WidgetDatabase): WidgetCourseDao = db.widgetCourseDao()

    @Provides
    fun provideWidgetAppSettingsDao(db: WidgetDatabase): WidgetAppSettingsDao = db.widgetAppSettingsDao()
}
package com.xingheyuzhuan.shiguangschedulemiuix

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.xingheyuzhuan.shiguangschedulemiuix.ui.schoolselection.list.AdapterSelectionScreen
import com.xingheyuzhuan.shiguangschedulemiuix.ui.schoolselection.list.SchoolSelectionListScreen
import com.xingheyuzhuan.shiguangschedulemiuix.ui.schoolselection.web.WebViewScreen
import com.xingheyuzhuan.shiguangschedulemiuix.ui.settings.additional.MoreOptionsScreen
import com.xingheyuzhuan.shiguangschedulemiuix.ui.settings.additional.OpenSourceLicensesScreen
import com.xingheyuzhuan.shiguangschedulemiuix.ui.settings.contribution.ContributionScreen
import com.xingheyuzhuan.shiguangschedulemiuix.ui.settings.conversion.CourseTableConversionScreen
import com.xingheyuzhuan.shiguangschedulemiuix.ui.settings.course.AddEditCourseScreen
import com.xingheyuzhuan.shiguangschedulemiuix.ui.settings.coursemanagement.CourseInstanceListScreen
import com.xingheyuzhuan.shiguangschedulemiuix.ui.settings.coursemanagement.CourseNameListScreen
import com.xingheyuzhuan.shiguangschedulemiuix.ui.settings.coursetables.ManageCourseTablesScreen
import com.xingheyuzhuan.shiguangschedulemiuix.ui.settings.notification.NotificationSettingsScreen
import com.xingheyuzhuan.shiguangschedulemiuix.ui.settings.quickactions.QuickActionsScreen
import com.xingheyuzhuan.shiguangschedulemiuix.ui.settings.quickactions.delete.QuickDeleteScreen
import com.xingheyuzhuan.shiguangschedulemiuix.ui.settings.quickactions.tweaks.TweakScheduleScreen
import com.xingheyuzhuan.shiguangschedulemiuix.ui.settings.style.StyleSettingsScreen
import com.xingheyuzhuan.shiguangschedulemiuix.ui.settings.time.TimeSlotManagementScreen
import com.xingheyuzhuan.shiguangschedulemiuix.ui.settings.update.UpdateRepoScreen

// 🌟 1. 提供全局 Navigator，彻底摆脱 NavHostController 的参数传递噩梦
val LocalAppNavigator = staticCompositionLocalOf<MutableList<NavKey>> { error("No navigator provided") }

// 🌟 2. 现代化的路由定义：类型安全，彻底取代容易拼错的 String 路由
sealed interface AppRoute : NavKey {
    data object Main : AppRoute
    data object TimeSlotSettings : AppRoute
    data object ManageCourseTables : AppRoute
    data object SchoolSelectionList : AppRoute
    data class AdapterSelection(val schoolId: String, val schoolName: String, val categoryNumber: Int, val resourceFolder: String) : AppRoute
    data class WebView(val initialUrl: String?, val assetJsPath: String?) : AppRoute
    data object NotificationSettings : AppRoute
    data class AddEditCourse(val courseId: String?) : AppRoute
    data object CourseTableConversion : AppRoute
    data object MoreOptions : AppRoute
    data object OpenSourceLicenses : AppRoute
    data object UpdateRepo : AppRoute
    data object QuickActions : AppRoute
    data object QuickDelete : AppRoute
    data object TweakSchedule : AppRoute
    data object ContributionList : AppRoute
    data object CourseManagementList : AppRoute
    data class CourseManagementDetail(val courseName: String) : AppRoute
    data object StyleSettings : AppRoute
}

@Composable
fun AppNavigation() {
    val backStack = remember { mutableStateListOf<NavKey>(AppRoute.Main) }

    BackHandler(enabled = backStack.size > 1) {
        backStack.removeAt(backStack.size - 1)
    }

    CompositionLocalProvider(LocalAppNavigator provides backStack) {
        val entryProvider = remember(backStack) {
            entryProvider<NavKey> {
                entry<AppRoute.Main> { MainScreen() }
                entry<AppRoute.TimeSlotSettings> { TimeSlotManagementScreen(onBackClick = { backStack.removeAt(backStack.size - 1) }) }
                entry<AppRoute.ManageCourseTables> { ManageCourseTablesScreen() }
                entry<AppRoute.SchoolSelectionList> { SchoolSelectionListScreen() }
                entry<AppRoute.AdapterSelection> { screen -> AdapterSelectionScreen(schoolId = screen.schoolId, schoolName = screen.schoolName, categoryNumber = screen.categoryNumber, resourceFolder = screen.resourceFolder) }
                entry<AppRoute.WebView> { screen -> WebViewScreen(initialUrl = screen.initialUrl, assetJsPath = screen.assetJsPath) }

                // 🛠️ 修复 1：按需补充对应的回调参数
                entry<AppRoute.NotificationSettings> { NotificationSettingsScreen(onNavigateBack = { backStack.removeAt(backStack.size - 1) }) }
                entry<AppRoute.AddEditCourse> { screen -> AddEditCourseScreen(courseId = screen.courseId, onNavigateBack = { backStack.removeAt(backStack.size - 1) }) } // 不再传废弃的 courseId

                entry<AppRoute.CourseTableConversion> { CourseTableConversionScreen() }
                entry<AppRoute.MoreOptions> { MoreOptionsScreen() }
                entry<AppRoute.OpenSourceLicenses> { OpenSourceLicensesScreen() }
                entry<AppRoute.UpdateRepo> { UpdateRepoScreen() }

                // 🛠️ 修复 2：这三个界面内部已经改为使用 navigator.removeLast()，去掉多余的 onNavigateBack 传参
                entry<AppRoute.QuickActions> { QuickActionsScreen() }
                entry<AppRoute.QuickDelete> { QuickDeleteScreen() }
                entry<AppRoute.TweakSchedule> { TweakScheduleScreen() }

                entry<AppRoute.ContributionList> { ContributionScreen() }
                entry<AppRoute.CourseManagementList> { CourseNameListScreen() }
                entry<AppRoute.CourseManagementDetail> { screen -> CourseInstanceListScreen(courseName = Uri.decode(screen.courseName)) }
                entry<AppRoute.StyleSettings> { StyleSettingsScreen() }
            }
        }

        val entries = rememberDecoratedNavEntries(
            backStack = backStack,
            entryProvider = entryProvider,
            entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator())
        )

        Box(Modifier.fillMaxSize()) {
            NavDisplay(
                entries = entries,
                onBack = { if (backStack.size > 1) backStack.removeAt(backStack.size - 1) }
            )
        }
    }
}
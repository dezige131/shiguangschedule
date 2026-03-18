package com.xingheyuzhuan.shiguangschedule

import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.xingheyuzhuan.shiguangschedule.ui.schedule.WeeklyScheduleScreen
import com.xingheyuzhuan.shiguangschedule.ui.schoolselection.list.AdapterSelectionScreen
import com.xingheyuzhuan.shiguangschedule.ui.schoolselection.list.SchoolSelectionListScreen
import com.xingheyuzhuan.shiguangschedule.ui.schoolselection.web.WebViewScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.SettingsScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.additional.MoreOptionsScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.additional.OpenSourceLicensesScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.contribution.ContributionScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.conversion.CourseTableConversionScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.course.AddEditCourseScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.coursemanagement.COURSE_NAME_ARG
import com.xingheyuzhuan.shiguangschedule.ui.settings.coursemanagement.CourseInstanceListScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.coursemanagement.CourseNameListScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.coursetables.ManageCourseTablesScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.notification.NotificationSettingsScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.quickactions.QuickActionsScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.quickactions.delete.QuickDeleteScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.quickactions.tweaks.TweakScheduleScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.style.StyleSettingsScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.time.TimeSlotManagementScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.update.UpdateRepoScreen
import com.xingheyuzhuan.shiguangschedule.ui.theme.ShiguangScheduleTheme
import com.xingheyuzhuan.shiguangschedule.ui.today.TodayScheduleScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            ShiguangScheduleTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    val emphasizedEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val animSpec = tween<IntOffset>(durationMillis = 500, easing = emphasizedEasing)
    val fadeSpec = tween<Float>(durationMillis = 500, easing = emphasizedEasing)

    // 进入新页面：从左侧滑入 + 放大（从 0.96f 到 1.0f）
    val slideInLeft = { scope: AnimatedContentTransitionScope<*> ->
        scope.slideIntoContainer(towards = AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = animSpec) +
                fadeIn(animationSpec = fadeSpec) +
                androidx.compose.animation.scaleIn(initialScale = 0.96f, animationSpec = fadeSpec)
    }

    // 退出当前页面：向左侧滑出 + 缩小（从 1.0f 到 0.96f）
    val slideOutLeft = { scope: AnimatedContentTransitionScope<*> ->
        scope.slideOutOfContainer(towards = AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = animSpec) +
                fadeOut(animationSpec = fadeSpec) +
                androidx.compose.animation.scaleOut(targetScale = 0.96f, animationSpec = fadeSpec)
    }

    // 返回旧页面：从右侧滑入 + 放大
    val slideInRight = { scope: AnimatedContentTransitionScope<*> ->
        scope.slideIntoContainer(towards = AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = animSpec) +
                fadeIn(animationSpec = fadeSpec) +
                androidx.compose.animation.scaleIn(initialScale = 0.96f, animationSpec = fadeSpec)
    }

    // 弹回上一级：向右侧滑出 + 缩小
    val slideOutRight = { scope: AnimatedContentTransitionScope<*> ->
        scope.slideOutOfContainer(towards = AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = animSpec) +
                fadeOut(animationSpec = fadeSpec) +
                androidx.compose.animation.scaleOut(targetScale = 0.96f, animationSpec = fadeSpec)
    }

    val mainScreens = listOf(
        Screen.CourseSchedule.route,
        Screen.Settings.route,
        Screen.TodaySchedule.route
    )

    /**
     * 封装通用的导航逻辑
     * 自动处理 mainScreens 之间的无动画逻辑，以及子页面的滑动逻辑
     */
    fun NavGraphBuilder.standardComposable(
        route: String,
        arguments: List<NamedNavArgument> = emptyList(),
        content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit
    ) {
        composable(
            route = route,
            arguments = arguments,
            enterTransition = {
                if (initialState.destination.route in mainScreens && targetState.destination.route in mainScreens)
                    EnterTransition.None else slideInLeft(this)
            },
            exitTransition = {
                if (initialState.destination.route in mainScreens && targetState.destination.route in mainScreens)
                    ExitTransition.None else slideOutLeft(this)
            },
            popEnterTransition = {
                if (initialState.destination.route in mainScreens && targetState.destination.route in mainScreens)
                    EnterTransition.None else slideInRight(this)
            },
            popExitTransition = {
                if (initialState.destination.route in mainScreens && targetState.destination.route in mainScreens)
                    ExitTransition.None else slideOutRight(this)
            },
            content = content
        )
    }

    NavHost(
        navController = navController,
        startDestination = Screen.CourseSchedule.route,
        modifier = Modifier.fillMaxSize()
    ) {
        // --- 主界面 ---
        standardComposable(Screen.CourseSchedule.route) { WeeklyScheduleScreen(navController = navController) }
        standardComposable(Screen.Settings.route) { SettingsScreen(navController = navController) }
        standardComposable(Screen.TodaySchedule.route) { TodayScheduleScreen(navController = navController) }

        // --- 子页面 ---
        standardComposable(Screen.TimeSlotSettings.route) {
            TimeSlotManagementScreen(onBackClick = { navController.popBackStack() })
        }

        standardComposable(Screen.ManageCourseTables.route) { ManageCourseTablesScreen(navController = navController) }
        standardComposable(Screen.SchoolSelectionListScreen.route) { SchoolSelectionListScreen(navController = navController) }

        standardComposable(
            route = "adapterSelection/{schoolId}/{schoolName}/{categoryNumber}/{resourceFolder}",
            arguments = listOf(
                navArgument("schoolId") { type = NavType.StringType },
                navArgument("schoolName") { type = NavType.StringType },
                navArgument("categoryNumber") { type = NavType.IntType },
                navArgument("resourceFolder") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            AdapterSelectionScreen(
                navController = navController,
                schoolId = backStackEntry.arguments?.getString("schoolId") ?: "",
                schoolName = backStackEntry.arguments?.getString("schoolName") ?: "未知学校",
                categoryNumber = backStackEntry.arguments?.getInt("categoryNumber") ?: 0,
                resourceFolder = backStackEntry.arguments?.getString("resourceFolder") ?: ""
            )
        }

        standardComposable(
            route = Screen.WebView.route,
            arguments = listOf(
                navArgument("initialUrl") { type = NavType.StringType },
                navArgument("assetJsPath") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            WebViewScreen(
                navController = navController,
                initialUrl = backStackEntry.arguments?.getString("initialUrl"),
                assetJsPath = backStackEntry.arguments?.getString("assetJsPath"),
                courseScheduleRoute = Screen.CourseSchedule.route,
            )
        }

        standardComposable(Screen.NotificationSettings.route) {
            NotificationSettingsScreen(onNavigateBack = { navController.popBackStack() })
        }

        standardComposable(
            route = Screen.AddEditCourse.route,
            arguments = listOf(navArgument("courseId") { type = NavType.StringType; nullable = true })
        ) {
            AddEditCourseScreen(onNavigateBack = { navController.popBackStack() })
        }

        standardComposable(Screen.CourseTableConversion.route) { CourseTableConversionScreen(navController = navController) }
        standardComposable(Screen.MoreOptions.route) { MoreOptionsScreen(navController = navController) }
        standardComposable(Screen.OpenSourceLicenses.route) { OpenSourceLicensesScreen(navController = navController) }
        standardComposable(Screen.UpdateRepo.route) { UpdateRepoScreen(navController = navController) }
        standardComposable(Screen.QuickActions.route) { QuickActionsScreen(navController = navController) }
        standardComposable(Screen.TweakSchedule.route) { TweakScheduleScreen(navController = navController) }
        standardComposable(Screen.ContributionList.route) { ContributionScreen(navController = navController) }
        standardComposable(Screen.CourseManagementList.route) { CourseNameListScreen(navController = navController) }

        standardComposable(
            route = Screen.CourseManagementDetail.route,
            arguments = listOf(navArgument(COURSE_NAME_ARG) { type = NavType.StringType })
        ) { backStackEntry ->
            val courseName = Uri.decode(backStackEntry.arguments?.getString(COURSE_NAME_ARG) ?: "")
            CourseInstanceListScreen(
                courseName = courseName,
                onNavigateBack = { navController.popBackStack() },
                navController = navController
            )
        }

        standardComposable(Screen.StyleSettings.route) { StyleSettingsScreen(navController = navController) }
        standardComposable(Screen.QuickDelete.route) { QuickDeleteScreen(navController = navController) }
    }
}
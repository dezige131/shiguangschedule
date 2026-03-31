package com.xingheyuzhuan.shiguangschedulemiuix

import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.xingheyuzhuan.shiguangschedulemiuix.ui.components.FloatingBottomBar
import com.xingheyuzhuan.shiguangschedulemiuix.ui.components.FloatingBottomBarItem
import com.xingheyuzhuan.shiguangschedulemiuix.ui.schedule.WeeklyScheduleScreen
import com.xingheyuzhuan.shiguangschedulemiuix.ui.schoolselection.list.AdapterSelectionScreen
import com.xingheyuzhuan.shiguangschedulemiuix.ui.schoolselection.list.SchoolSelectionListScreen
import com.xingheyuzhuan.shiguangschedulemiuix.ui.schoolselection.web.WebViewScreen
import com.xingheyuzhuan.shiguangschedulemiuix.ui.settings.SettingsScreen
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
import com.xingheyuzhuan.shiguangschedulemiuix.ui.settings.style.StyleSettingsViewModel
import com.xingheyuzhuan.shiguangschedulemiuix.ui.settings.time.TimeSlotManagementScreen
import com.xingheyuzhuan.shiguangschedulemiuix.ui.settings.update.UpdateRepoScreen
import com.xingheyuzhuan.shiguangschedulemiuix.ui.theme.ShiguangScheduleTheme
import com.xingheyuzhuan.shiguangschedulemiuix.ui.today.TodayScheduleScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme

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

// 🌟 5. 新的 MainScreen，内部自闭环包含三个子 Tab 和底栏
@Composable
fun MainScreen(
    styleViewModel: StyleSettingsViewModel = hiltViewModel()
) {
    val navigator = LocalAppNavigator.current
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()

    // 🛠️ 核心修复：
    // 只有当导航栈里只有 MainScreen 自己（size == 1）时，才允许拦截返回键来滚动 Pager。
    // 如果 size > 1，说明正在进入或已经在二级页面，此时 BackHandler 应该处于 disabled 状态，
    // 🛠️ 优化：增加 targetPage 判断，确保在 Pager 切换动画过程中（包括手动滑动中）
    // 也能拦截返回键并“打断”动画回到课表页。
    val isNotAtHome = pagerState.currentPage != 1 || pagerState.targetPage != 1
    
    BackHandler(enabled = isNotAtHome && navigator.size == 1) {
        coroutineScope.launch {
            // 立即发起回到课表页（索引 1）的动画
            // 这会打断当前正在进行的向 0 或 2 的动画
            pagerState.animateScrollToPage(
                page = 1,
                animationSpec = androidx.compose.animation.core.tween(
                    durationMillis = 350,
                    easing = androidx.compose.animation.core.FastOutSlowInEasing
                )
            )
        }
    }

    val styleState by styleViewModel.styleState.collectAsStateWithLifecycle()
    val enableFloatingBottomBar = (styleState?.enableFloatingBottomBar ?: false) && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
    val enableBottomBarBlur = (styleState?.enableBottomBarBlur ?: false) && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
    val hasBackgroundImage = !styleState?.backgroundImagePath.isNullOrEmpty()

    val systemIsDark = isSystemInDarkTheme()
    val isDark = remember(styleState?.colorSchemeMode, systemIsDark) {
        when (styleState?.colorSchemeMode) {
            ColorSchemeMode.Dark, ColorSchemeMode.MonetDark -> true
            ColorSchemeMode.Light, ColorSchemeMode.MonetLight -> false
            else -> systemIsDark
        }
    }

    // 🛠️ 修复 3：提前在 Composable 环境下获取颜色。
    val layerBackgroundColor = MiuixTheme.colorScheme.background

    val backdrop = rememberLayerBackdrop {
        drawRect(layerBackgroundColor) // 使用提取好的颜色，完美解决 @Composable invocations 报错
        drawContent()
    }

    val onTabClick: (Int) -> Unit = { index ->
        // 避免重复点击当前已经选中的，或者正在前往的页面
        if (pagerState.targetPage != index) {
            coroutineScope.launch {
                pagerState.animateScrollToPage(
                    page = index,
                    // 强制使用 350 毫秒的线性补间动画，摒弃不稳定的弹簧动画
                    // 这能确保它“动力十足”地跨越中间页面，百分百到达目的地
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = 350,
                        easing = androidx.compose.animation.core.FastOutSlowInEasing
                    )
                )
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .then(if (enableFloatingBottomBar && enableBottomBarBlur) Modifier.layerBackdrop(backdrop) else Modifier),
            beyondViewportPageCount = 2,
            userScrollEnabled = false
        ) { page ->
            when (page) {
                0 -> TodayScheduleScreen()
                1 -> WeeklyScheduleScreen(
                    onNavigateToSettings = { onTabClick(2) }
                )
                2 -> SettingsScreen()
            }
        }

        val selectedIndex = pagerState.targetPage
        val navBarBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        Box(
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            if (!enableFloatingBottomBar) {
                val isCourseSchedule = selectedIndex == 1
                val shouldBeTransparent = isCourseSchedule && hasBackgroundImage
                NavigationBar(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (shouldBeTransparent) Color.Transparent else MiuixTheme.colorScheme.surface,
                    showDivider = !shouldBeTransparent
                ) {
                    // 🛠️ 修复 4：Miuix 的 NavigationBarItem 直接传值，不能包大括号！
                    NavigationBarItem(selected = selectedIndex == 0, onClick = { onTabClick(0) }, icon = Icons.Default.Today, label = stringResource(R.string.nav_today_schedule))
                    NavigationBarItem(selected = selectedIndex == 1, onClick = { onTabClick(1) }, icon = Icons.Default.DateRange, label = stringResource(R.string.nav_course_schedule))
                    NavigationBarItem(selected = selectedIndex == 2, onClick = { onTabClick(2) }, icon = Icons.Default.Settings, label = stringResource(R.string.nav_settings_main))
                }
            } else {
                FloatingBottomBar(
                    modifier = Modifier.padding(bottom = 12.dp + navBarBottomPadding),
                    selectedIndex = { selectedIndex },
                    onSelected = onTabClick,
                    backdrop = backdrop,
                    tabsCount = 3,
                    isBlurEnabled = enableBottomBarBlur,
                    isDark = isDark
                ) {
                    // 🛠️ 修复 5：FloatingBottomBarItem 是支持闭包的，老老实实还原成闭包布局即可
                    FloatingBottomBarItem(modifier = Modifier.width(76.dp), onClick = { onTabClick(0) }) {
                        Icon(imageVector = Icons.Default.Today, contentDescription = stringResource(R.string.nav_today_schedule), modifier = Modifier.size(24.dp), tint = MiuixTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = stringResource(R.string.nav_today_schedule), fontSize = 12.sp, fontWeight = if (selectedIndex == 0) FontWeight.Bold else FontWeight.Normal, color = MiuixTheme.colorScheme.onSurface)
                    }
                    FloatingBottomBarItem(modifier = Modifier.width(76.dp), onClick = { onTabClick(1) }) {
                        Icon(imageVector = Icons.Default.DateRange, contentDescription = stringResource(R.string.nav_course_schedule), modifier = Modifier.size(24.dp), tint = MiuixTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = stringResource(R.string.nav_course_schedule), fontSize = 12.sp, fontWeight = if (selectedIndex == 1) FontWeight.Bold else FontWeight.Normal, color = MiuixTheme.colorScheme.onSurface)
                    }
                    FloatingBottomBarItem(modifier = Modifier.width(76.dp), onClick = { onTabClick(2) }) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = stringResource(R.string.nav_settings_main), modifier = Modifier.size(24.dp), tint = MiuixTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = stringResource(R.string.nav_settings_main), fontSize = 12.sp, fontWeight = if (selectedIndex == 2) FontWeight.Bold else FontWeight.Normal, color = MiuixTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}
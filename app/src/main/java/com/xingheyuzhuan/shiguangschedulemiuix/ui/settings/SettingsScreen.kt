package com.xingheyuzhuan.shiguangschedulemiuix.ui.settings

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.xingheyuzhuan.shiguangschedulemiuix.AppRoute
import com.xingheyuzhuan.shiguangschedulemiuix.LocalAppNavigator
import com.xingheyuzhuan.shiguangschedulemiuix.R
import com.xingheyuzhuan.shiguangschedulemiuix.ui.components.DatePickerModal
import com.xingheyuzhuan.shiguangschedulemiuix.ui.settings.style.StyleSettingsViewModel
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.NumberPicker
import top.yukonga.miuix.kmp.basic.NumberPickerDefaults
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.preference.WindowDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private val SETTING_PADDING = 16.dp
private val SECTION_SPACING = 24.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    styleViewModel: StyleSettingsViewModel = hiltViewModel()
) {
    val navigator = LocalAppNavigator.current
    val uiState by viewModel.uiState.collectAsState()

    // 现在这里就不会报 "Unresolved reference" 了
    val styleState by styleViewModel.styleState.collectAsStateWithLifecycle()
    val isFloating = styleState?.enableFloatingBottomBar == true

    val scrollBehavior = MiuixScrollBehavior()

    Scaffold(
        containerColor = MiuixTheme.colorScheme.surface,
        topBar = {
            // 使用 Miuix 的 TopAppBar 替换原有的 CenterAlignedTopAppBar
            TopAppBar(
                title = stringResource(R.string.title_schedule_settings),
                largeTitle = stringResource(R.string.title_schedule_settings),
                scrollBehavior = scrollBehavior
                // 不传入 navigationIcon 和 actions 即可隐藏返回按钮和右侧菜单
            )
        }
    ) { innerPadding ->
        if (!uiState.isReady) {
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding))
        } else {
            val appSettings = uiState.appSettings
            val courseTableConfig = uiState.courseConfig
            val displayCurrentWeek = uiState.currentWeek

            val showWeekends = courseTableConfig?.showWeekends ?: false
            val semesterStartDateString = courseTableConfig?.semesterStartDate
            val semesterTotalWeeks = courseTableConfig?.semesterTotalWeeks ?: 20
            val firstDayOfWeekInt = courseTableConfig?.firstDayOfWeek ?: DayOfWeek.MONDAY.value

            val semesterStartDate: LocalDate? = remember(semesterStartDateString) {
                semesterStartDateString?.let {
                    try {
                        LocalDate.parse(it, DateTimeFormatter.ISO_LOCAL_DATE)
                    } catch (e: DateTimeParseException) {
                        Log.e("SettingsScreen", "Failed to parse date string: $it", e)
                        null
                    }
                }
            }

            var showTotalWeeksDialog by remember { mutableStateOf(false) }
            var showManualWeekDialog by remember { mutableStateOf(false) }
            var showDatePickerModal by remember { mutableStateOf(false) }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding = innerPadding,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(SECTION_SPACING)
            ) {
                item {
                    // 通用设置区块
                    GeneralSettingsSection(
                        showNonCurrentWeek = appSettings.showNonCurrentWeekCourses,
                        onShowNonCurrentWeekChanged = { viewModel.onShowNonCurrentWeekChanged(it) },
                        showWeekends = showWeekends,
                        onShowWeekendsChanged = { viewModel.onShowWeekendsChanged(it) },
                        semesterStartDate = semesterStartDate,
                        semesterTotalWeeks = semesterTotalWeeks,
                        firstDayOfWeekInt = firstDayOfWeekInt,
                        displayCurrentWeek = displayCurrentWeek,
                        onSemesterStartDateClick = { showDatePickerModal = true },
                        onSemesterTotalWeeksClick = { showTotalWeeksDialog = true },
                        onManualWeekClick = { showManualWeekDialog = true },
                        onFirstDayOfWeekSelected = { viewModel.onFirstDayOfWeekSelected(it) },
                        onQuickActionsClick = { navigator.add(AppRoute.QuickActions) },
                        defaultHomePage = appSettings.defaultHomePage,
                        onDefaultHomePageChanged = { viewModel.onDefaultHomePageChanged(it) }
                    )
                }

                item {
                    // 高级功能区块
                    AdvancedSettingsSection(navigator)
                }

                item {
                    val baseSpacerHeight = if (isFloating) 65.dp else 50.dp
                    Spacer(modifier = Modifier.height(baseSpacerHeight))
                }
            }

            DatePickerModal(
                show = showDatePickerModal, // 传入状态
                onDateSelected = { selectedDateMillis ->
                    viewModel.onSemesterStartDateSelected(selectedDateMillis)
                    showDatePickerModal = false // 记得关闭
                },
                onDismiss = { showDatePickerModal = false }
            )

            NumberPickerDialog(
                show = showTotalWeeksDialog, // 传入状态
                title = stringResource(R.string.dialog_title_select_total_weeks),
                range = 1..30,
                initialValue = semesterTotalWeeks,
                onDismiss = { showTotalWeeksDialog = false },
                onConfirm = { selectedWeeks ->
                    viewModel.onSemesterTotalWeeksSelected(selectedWeeks)
                    showTotalWeeksDialog = false // 记得关闭
                }
            )

            ManualWeekPickerDialog(
                show = showManualWeekDialog, // 传入状态
                totalWeeks = semesterTotalWeeks,
                currentWeek = displayCurrentWeek,
                onDismiss = { showManualWeekDialog = false },
                onConfirm = { weekNumber ->
                    viewModel.onCurrentWeekManuallySet(weekNumber)
                    showManualWeekDialog = false // 记得关闭
                }
            )
        }
    }
}

/**
 * 通用设置卡片
 */
@Composable
private fun GeneralSettingsSection(
    showNonCurrentWeek: Boolean,
    onShowNonCurrentWeekChanged: (Boolean) -> Unit,
    showWeekends: Boolean,
    onShowWeekendsChanged: (Boolean) -> Unit,
    semesterStartDate: LocalDate?,
    semesterTotalWeeks: Int,
    firstDayOfWeekInt: Int,
    displayCurrentWeek: Int?,
    onSemesterStartDateClick: () -> Unit,
    onSemesterTotalWeeksClick: () -> Unit,
    onManualWeekClick: () -> Unit,
    onFirstDayOfWeekSelected: (Int) -> Unit,
    onQuickActionsClick: () -> Unit,

    defaultHomePage: Int,

    onDefaultHomePageChanged: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.section_title_general_settings),
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            fontSize = 13.sp,
            modifier = Modifier.padding(start = SETTING_PADDING * 2, bottom = 8.dp, top = 8.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SETTING_PADDING)
                .clip(RoundedCornerShape(16.dp))
                .background(MiuixTheme.colorScheme.surfaceContainer)
        ) {
            // 是否显示非本周课程开关
            SwitchPreference(
                title = stringResource(R.string.item_show_non_current_week),
                summary = stringResource(R.string.desc_show_non_current_week),
                checked = showNonCurrentWeek,
                onCheckedChange = onShowNonCurrentWeekChanged
            )

            // 显示周末设置项
            SwitchPreference(
                title = stringResource(R.string.item_show_weekends),
                summary = stringResource(R.string.desc_show_weekends),
                checked = showWeekends,
                onCheckedChange = onShowWeekendsChanged
            )

            // 开始上课时间设置项
            ArrowPreference(
                title = stringResource(R.string.item_set_start_date),
                summary = semesterStartDate?.format(DateTimeFormatter.ofPattern(stringResource(R.string.date_format_year_month_day)))
                    ?: stringResource(R.string.status_not_set),
                onClick = onSemesterStartDateClick
            )

            // 本学期总周数设置项
            ArrowPreference(
                title = stringResource(R.string.item_total_weeks),
                summary = stringResource(R.string.status_total_weeks_format, semesterTotalWeeks),
                onClick = onSemesterTotalWeeksClick
            )

            // 当前周数设置项
            val weekStatusText = when {
                semesterStartDate == null -> stringResource(R.string.status_set_start_date_first)
                displayCurrentWeek == null -> stringResource(R.string.status_on_vacation)
                else -> stringResource(R.string.status_current_week_format, displayCurrentWeek)
            }
            ArrowPreference(
                title = stringResource(R.string.item_current_week),
                summary = weekStatusText,
                onClick = onManualWeekClick
            )

            // 每周起始日 (使用 WindowDropdownPreference 替换原有的 Dialog)
            val firstDayOptions = listOf(
                stringResource(R.string.day_of_week_monday),
                stringResource(R.string.day_of_week_sunday)
            )
            val selectedDayIndex = if (firstDayOfWeekInt == DayOfWeek.SUNDAY.value) 1 else 0

            WindowDropdownPreference(
                title = stringResource(R.string.item_first_day_of_week),
                summary = stringResource(R.string.desc_first_day_of_week),
                items = firstDayOptions,
                selectedIndex = selectedDayIndex,
                onSelectedIndexChange = { index ->
                    val selectedDayInt =
                        if (index == 1) DayOfWeek.SUNDAY.value else DayOfWeek.MONDAY.value
                    onFirstDayOfWeekSelected(selectedDayInt)
                }
            )

            // 默认主界面设置
            val homePageOptions = listOf(
                stringResource(R.string.nav_today_schedule), // 今日课表
                stringResource(R.string.nav_course_schedule) // 课表页
            )
            WindowDropdownPreference(
                title = stringResource(R.string.item_default_home_page),
                summary = stringResource(R.string.desc_default_home_page),
                items = homePageOptions,
                selectedIndex = defaultHomePage,
                onSelectedIndexChange = { index ->
                    onDefaultHomePageChanged(index)
                }
            )

            // 快捷操作页面
            ArrowPreference(
                title = stringResource(R.string.item_quick_actions),
                summary = stringResource(R.string.desc_quick_actions),
                onClick = onQuickActionsClick
            )
        }
    }
}

/**
 * 高级功能卡片
 */
@Composable
private fun AdvancedSettingsSection(navigator: MutableList<NavKey>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.section_title_advanced_features),
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            fontSize = 13.sp,
            modifier = Modifier.padding(start = SETTING_PADDING * 2, bottom = 8.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SETTING_PADDING)
                .clip(RoundedCornerShape(16.dp))
                .background(MiuixTheme.colorScheme.surfaceContainer)
        ) {
            // 课表导入/导出
            ArrowPreference(
                title = stringResource(R.string.item_course_conversion),
                summary = stringResource(R.string.desc_course_conversion),
                onClick = { navigator.add(AppRoute.CourseTableConversion) }
            )

            // 课程提醒
            ArrowPreference(
                title = stringResource(R.string.title_course_notification_settings),
                summary = stringResource(R.string.desc_notification_settings),
                onClick = { navigator.add(AppRoute.NotificationSettings) }
            )

            // 管理课表
            ArrowPreference(
                title = stringResource(R.string.title_manage_course_tables),
                summary = stringResource(R.string.desc_manage_course_tables),
                onClick = { navigator.add(AppRoute.ManageCourseTables) }
            )

            // 课程管理
            ArrowPreference(
                title = stringResource(R.string.item_course_management),
                summary = stringResource(R.string.desc_course_management),
                onClick = { navigator.add(AppRoute.CourseManagementList) }
            )

            // 自定义时间段
            ArrowPreference(
                title = stringResource(R.string.item_time_slot_customization),
                summary = stringResource(R.string.desc_time_slot_customization),
                onClick = { navigator.add(AppRoute.TimeSlotSettings) }
            )

            // 个性化配置
            ArrowPreference(
                title = stringResource(R.string.item_personalization),
                summary = stringResource(R.string.desc_personalization),
                onClick = { navigator.add(AppRoute.StyleSettings) }
            )

            // 更多选项
            ArrowPreference(
                title = stringResource(R.string.item_more_options),
                summary = stringResource(R.string.desc_more_options),
                onClick = { navigator.add(AppRoute.MoreOptions) }
            )
        }
    }
}

// ==========================================
// 剩余的 Dialog 组件保持不变
// （已删除废弃的 DayOfWeekPickerDialog 和 SettingItem）
// ==========================================
/**
 * 手动周数选择器对话框
 */
@Composable
fun ManualWeekPickerDialog(
    show: Boolean, // 新增 show 参数
    totalWeeks: Int,
    currentWeek: Int?,
    onDismiss: () -> Unit,
    onConfirm: (Int?) -> Unit
) {
    val optionOnVacationText = stringResource(R.string.dialog_option_on_vacation)
    val context = LocalContext.current

    val initialValue = currentWeek ?: 0
    var dialogSelectedValue by remember { mutableIntStateOf(initialValue) }

    val pickerColors = NumberPickerDefaults.colors(
        selectedTextColor = MiuixTheme.colorScheme.primary,
        unselectedTextColor = MiuixTheme.colorScheme.onSurface
    )

    WindowDialog(
        show = show,
        title = stringResource(R.string.dialog_title_manual_set_week),
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                NumberPicker(
                    value = dialogSelectedValue,
                    onValueChange = { dialogSelectedValue = it },
                    range = 0..totalWeeks,
                    label = {
                        if (it == 0) optionOnVacationText
                        else context.getString(R.string.status_current_week_format, it)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors()
                ) {
                    Text(
                        stringResource(R.string.action_cancel),
                        color = MiuixTheme.colorScheme.onSurface
                    )
                }

                Button(
                    onClick = {
                        val weekNumber = if (dialogSelectedValue == 0) null else dialogSelectedValue
                        onConfirm(weekNumber)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColorsPrimary()
                ) {
                    Text(
                        stringResource(R.string.action_confirm),
                        color = MiuixTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

/**
 * 数字选择器对话框 (用于选择总周数)
 */
@Composable
fun NumberPickerDialog(
    show: Boolean, // 新增 show 参数
    title: String,
    range: IntRange,
    initialValue: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var dialogSelectedValue by remember { mutableIntStateOf(initialValue.coerceIn(range)) }

    val pickerColors = NumberPickerDefaults.colors(
        selectedTextColor = MiuixTheme.colorScheme.primary,
        unselectedTextColor = MiuixTheme.colorScheme.onSurface
    )

    WindowDialog(
        show = show,
        title = title,
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                NumberPicker(
                    value = dialogSelectedValue,
                    onValueChange = { dialogSelectedValue = it },
                    range = range,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors()
                ) {
                    Text(
                        stringResource(R.string.action_cancel),
                        color = MiuixTheme.colorScheme.onSurface
                    )
                }

                Button(
                    onClick = { onConfirm(dialogSelectedValue) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColorsPrimary()
                ) {
                    Text(
                        stringResource(R.string.action_confirm),
                        color = MiuixTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}
package com.xingheyuzhuan.shiguangschedulemiuix.ui.schedule

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.xingheyuzhuan.shiguangschedulemiuix.AppRoute
import com.xingheyuzhuan.shiguangschedulemiuix.LocalAppNavigator
import com.xingheyuzhuan.shiguangschedulemiuix.R
import com.xingheyuzhuan.shiguangschedulemiuix.data.db.main.CourseWithWeeks
import com.xingheyuzhuan.shiguangschedulemiuix.navigation.AddEditCourseChannel
import com.xingheyuzhuan.shiguangschedulemiuix.navigation.PresetCourseData
import com.xingheyuzhuan.shiguangschedulemiuix.ui.schedule.components.OverlapCourseBottomSheet
import com.xingheyuzhuan.shiguangschedulemiuix.ui.schedule.components.ScheduleGrid
import com.xingheyuzhuan.shiguangschedulemiuix.ui.schedule.components.ScheduleGridStyleComposed
import com.xingheyuzhuan.shiguangschedulemiuix.ui.schedule.components.WeekSelectorBottomSheet
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold

private const val INFINITE_PAGER_CENTER = Int.MAX_VALUE / 2

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WeeklyScheduleScreen(
    viewModel: WeeklyScheduleViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit = {}
) {
    val navigator = LocalAppNavigator.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val today = LocalDate.now()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val promptMsg = stringResource(id = R.string.snackbar_add_course_after_start)
    val appContext = remember { context.applicationContext }

    LaunchedEffect(Unit) {
        viewModel.setStringProvider { id, args ->
            appContext.resources.getString(id, *args)
        }
    }

    val pagerState = rememberPagerState(
        initialPage = INFINITE_PAGER_CENTER,
        pageCount = { Int.MAX_VALUE }
    )

    LaunchedEffect(pagerState.currentPage, uiState.firstDayOfWeek) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { pageIndex ->
                val offsetWeeks = (pageIndex - INFINITE_PAGER_CENTER).toLong()
                val firstDay = DayOfWeek.of(uiState.firstDayOfWeek)
                val thisMonday = today.with(TemporalAdjusters.previousOrSame(firstDay))
                val targetMonday = thisMonday.plusWeeks(offsetWeeks)
                viewModel.updatePagerDate(targetMonday)
            }
    }

    var showWeekSelector by remember { mutableStateOf(false) }
    var showOverlapBottomSheet by remember { mutableStateOf(false) }
    var overlapCoursesToShow by remember { mutableStateOf(emptyList<CourseWithWeeks>()) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val composedStyle by remember(uiState.style) {
        derivedStateOf { with(ScheduleGridStyleComposed) { uiState.style.toComposedStyle() } }
    }

    // 统一定义 Miuix 的颜色
    val defaultBackgroundColor = MiuixTheme.colorScheme.surfaceContainer
    val onSurfaceColor = MiuixTheme.colorScheme.onSurface

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (composedStyle.backgroundImagePath.isNotEmpty()) Color.Transparent
                else defaultBackgroundColor
            )
    ) {
        // 壁纸层
        if (composedStyle.backgroundImagePath.isNotEmpty()) {
            AsyncImage(
                model = composedStyle.backgroundImagePath,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        MiuixScaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = Color.Transparent, // 背景交给外层的 Box 控制
            topBar = {
                val isTransparent = composedStyle.backgroundImagePath.isNotEmpty()
                CenterAlignedTopAppBar(
                    title = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable {
                                    if (!uiState.isSemesterSet || uiState.semesterStartDate == null) {
                                        // 🌟 修改点：这里增加了 Toast 提示
                                        Toast.makeText(context, promptMsg, Toast.LENGTH_SHORT)
                                            .show()
                                        onNavigateToSettings()
                                    } else {
                                        showWeekSelector = true
                                    }
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = uiState.weekTitle,
                                style = MaterialTheme.typography.titleLarge,
                                color = onSurfaceColor
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(20.dp)
                                    .offset(y = (-4).dp),
                                tint = onSurfaceColor.copy(alpha = 0.7f)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = if (isTransparent) Color.Transparent else defaultBackgroundColor,
                        scrolledContainerColor = if (isTransparent) Color.Transparent else defaultBackgroundColor,
                        titleContentColor = if (isTransparent) Color.Unspecified else onSurfaceColor,
                        navigationIconContentColor = if (isTransparent) Color.Unspecified else onSurfaceColor,
                        actionIconContentColor = if (isTransparent) Color.Unspecified else onSurfaceColor
                    ),
                    scrollBehavior = scrollBehavior
                )
            }
        ) { innerPadding ->

            // 🌟 计算你需要垫高的完美高度
            val baseSpacerHeight = 115.dp

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    // 🌟 核心修改：只取顶部的 padding 避让状态栏，底部完全放开不限制，实现沉浸式
                    .padding(top = innerPadding.calculateTopPadding())
                    .fillMaxSize(),
                beyondViewportPageCount = 1
            ) { pageIndex ->

                val pageMondayDate = remember(pageIndex, uiState.firstDayOfWeek) {
                    val offsetWeeks = (pageIndex - INFINITE_PAGER_CENTER).toLong()
                    val firstDay = DayOfWeek.of(uiState.firstDayOfWeek)
                    today.with(TemporalAdjusters.previousOrSame(firstDay)).plusWeeks(offsetWeeks)
                }

                val pageYearString = remember(pageMondayDate) {
                    pageMondayDate.year.toString()
                }

                val pageDateStrings = remember(pageMondayDate) {
                    val formatter = DateTimeFormatter.ofPattern("MM-dd")
                    (0..6).map { pageMondayDate.plusDays(it.toLong()).format(formatter) }
                }

                val pageTodayIndex = remember(pageMondayDate) {
                    val weekDates = (0..6).map { pageMondayDate.plusDays(it.toLong()) }
                    weekDates.indexOf(today)
                }

                val pageCourses = uiState.courseCache[pageMondayDate.toString()] ?: emptyList()

                ScheduleGrid(
                    style = composedStyle,
                    dates = pageDateStrings,
                    currentYear = pageYearString,
                    timeSlots = uiState.timeSlots,
                    mergedCourses = pageCourses,
                    showWeekends = uiState.showWeekends,
                    todayIndex = pageTodayIndex,
                    firstDayOfWeek = uiState.firstDayOfWeek,
                    currentSectionIndex = if (pageTodayIndex >= 0) uiState.currentSectionIndex else -1,
                    onCourseBlockClicked = { mergedBlock ->
                        if (mergedBlock.courses.size > 1) {
                            overlapCoursesToShow = mergedBlock.courses
                            showOverlapBottomSheet = true
                        } else {
                            mergedBlock.courses.firstOrNull()?.course?.id?.let {
                                navigator.add(AppRoute.AddEditCourse(it))
                            }
                        }
                    },
                    onGridCellClicked = { day, section ->
                        if (uiState.semesterStartDate != null && !today.isBefore(uiState.semesterStartDate)) {
                            coroutineScope.launch {
                                AddEditCourseChannel.sendEvent(
                                    PresetCourseData(
                                        day,
                                        section,
                                        section
                                    )
                                )
                                navigator.add(AppRoute.AddEditCourse(null))
                            }
                        } else {
                            // 🌟 修改点：这里改成了 Toast
                            Toast.makeText(context, promptMsg, Toast.LENGTH_SHORT).show()
                        }
                    },
                    onTimeSlotClicked = {
                        navigator.add(AppRoute.TimeSlotSettings)
                    },
                    // 🌟 核心修改：将算好的高度传递给网格内部
                    bottomSpacerHeight = baseSpacerHeight
                )
            }

            // 【关键所在】：将所有抽屉弹窗放到 MiuixScaffold 内部，才能正常挂载
            WeekSelectorBottomSheet(
                show = showWeekSelector,
                totalWeeks = uiState.totalWeeks,
                currentWeek = uiState.currentWeekNumber ?: 1,
                selectedWeek = uiState.weekIndexInPager ?: (uiState.currentWeekNumber ?: 1),
                onWeekSelected = { week ->
                    val currentWeekAtPage = uiState.weekIndexInPager ?: 1
                    val offset = week - currentWeekAtPage
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + offset)
                    }
                    showWeekSelector = false
                },
                onDismissRequest = { showWeekSelector = false }
            )

            OverlapCourseBottomSheet(
                show = showOverlapBottomSheet,
                style = composedStyle,
                courses = overlapCoursesToShow,
                timeSlots = uiState.timeSlots,
                currentWeek = uiState.weekIndexInPager,
                onCourseClicked = { course ->
                    showOverlapBottomSheet = false
                    navigator.add(AppRoute.AddEditCourse(course.course.id))
                },
                onDismissRequest = { showOverlapBottomSheet = false }
            )
        }
    }
}
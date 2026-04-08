package com.xingheyuzhuan.shiguangschedulemiuix.ui.settings.coursemanagement

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.xingheyuzhuan.shiguangschedulemiuix.AppRoute
import com.xingheyuzhuan.shiguangschedulemiuix.LocalAppNavigator
import com.xingheyuzhuan.shiguangschedulemiuix.R
import com.xingheyuzhuan.shiguangschedulemiuix.data.db.main.CourseWithWeeks
import com.xingheyuzhuan.shiguangschedulemiuix.navigation.AddEditCourseChannel
import com.xingheyuzhuan.shiguangschedulemiuix.navigation.PresetCourseData
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CourseInstanceListScreen(
    courseName: String, // 👈 MainActivity 传进来的名字
    viewModel: CourseInstanceListViewModel = hiltViewModel()
) {
    // 🌟 新增这一段：页面初始化时，把名字传给 ViewModel 触发数据加载
    LaunchedEffect(courseName) {
        viewModel.loadCourseName(courseName)
    }

    val navigator = LocalAppNavigator.current
    val uiState by viewModel.uiState.collectAsState()
    val courseInstances by viewModel.courseInstances.collectAsState()
    val scrollBehavior = MiuixScrollBehavior()
    val coroutineScope = rememberCoroutineScope()
    val weekDaysFullNames = stringArrayResource(R.array.week_days_full_names)

    val isSelectionMode = uiState.isSelectionMode
    val selectedCourseIds = uiState.selectedCourseIds

    // 修复直接退出问题：拦截系统返回键
    BackHandler(enabled = isSelectionMode) {
        viewModel.clearSelection()
    }

    // 修复多选状态泄露的兜底：页面被销毁时清理状态
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearSelection()
        }
    }

    Scaffold(
        containerColor = MiuixTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = if (isSelectionMode) stringResource(
                    R.string.title_selected_items_count,
                    selectedCourseIds.size
                ) else courseName,
                largeTitle = if (isSelectionMode) stringResource(
                    R.string.title_selected_items_count,
                    selectedCourseIds.size
                ) else courseName,
                navigationIcon = {
                    if (isSelectionMode) {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.action_cancel),
                                tint = MiuixTheme.colorScheme.onBackground
                            )
                        }
                    } else {
                        IconButton(onClick = {
                            if (navigator.isNotEmpty()) {
                                navigator.removeAt(navigator.size - 1)
                            }
                        }) {
                            Icon(
                                MiuixIcons.Back,
                                contentDescription = stringResource(R.string.a11y_back),
                                tint = MiuixTheme.colorScheme.onBackground
                            )
                        }
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        val isAllSelected =
                            selectedCourseIds.size == courseInstances.size && courseInstances.isNotEmpty()
                        IconButton(onClick = { viewModel.toggleSelectAll() }) {
                            Icon(
                                imageVector = if (isAllSelected) Icons.Default.Close else Icons.Default.Check,
                                contentDescription = if (isAllSelected) stringResource(R.string.action_deselect_all) else stringResource(
                                    R.string.action_select_all
                                ),
                                tint = MiuixTheme.colorScheme.onBackground
                            )
                        }
                        IconButton(onClick = { viewModel.deleteSelectedCourses() }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.a11y_delete),
                                tint = MiuixTheme.colorScheme.error
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            AddEditCourseChannel.sendEvent(PresetCourseData(name = courseName))
                            navigator.add(AppRoute.AddEditCourse(null)) // <- 修改为新建
                        }
                    },
                    containerColor = MiuixTheme.colorScheme.primary
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.action_add),
                        tint = MiuixTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(innerPadding)
        ) {
            if (courseInstances.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.text_no_instances_hint),
                        fontSize = 16.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = 8.dp,
                        bottom = 100.dp,
                        start = 12.dp,
                        end = 12.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(courseInstances, key = { it.course.id }) { item ->
                        val isSelected = selectedCourseIds.contains(item.course.id)

                        CourseInstanceCard(
                            courseWithWeeks = item,
                            weekDaysFullNames = weekDaysFullNames,
                            isSelected = isSelected,
                            modifier = Modifier.combinedClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = LocalIndication.current,
                                onClick = {
                                    if (isSelectionMode) {
                                        viewModel.toggleCourseSelection(item.course.id)
                                    } else {
                                        navigator.add(AppRoute.AddEditCourse(item.course.id)) // <- 修改为编辑
                                    }
                                },
                                onLongClick = {
                                    viewModel.toggleCourseSelection(item.course.id)
                                }
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CourseInstanceCard(
    courseWithWeeks: CourseWithWeeks,
    weekDaysFullNames: Array<String>,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val course = courseWithWeeks.course
    val dayName =
        if (course.day in 1..7) weekDaysFullNames[course.day - 1] else stringResource(R.string.label_unknown)

    // 选中时使用原生蓝色背景，文字使用白色
    val backgroundColor =
        if (isSelected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.surfaceContainer
    val textColor =
        if (isSelected) MiuixTheme.colorScheme.onPrimary else MiuixTheme.colorScheme.onSurface
    val secondaryTextColor =
        if (isSelected) MiuixTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MiuixTheme.colorScheme.onSurfaceVariantSummary

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(color = backgroundColor),
        cornerRadius = 16.dp,
        insideMargin = PaddingValues(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = course.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = course.teacher.ifBlank { stringResource(R.string.label_teacher_unknown) },
                fontSize = 12.sp,
                color = secondaryTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = course.position.ifBlank { stringResource(R.string.label_position_unknown) },
                fontSize = 12.sp,
                color = secondaryTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            val timeText = if (course.isCustomTime) {
                stringResource(
                    R.string.course_time_day_time_details_tweak,
                    dayName,
                    course.customStartTime ?: "?",
                    course.customEndTime ?: "?"
                )
            } else {
                stringResource(
                    R.string.course_time_day_section_details_tweak,
                    dayName,
                    course.startSection ?: "?",
                    course.endSection ?: "?"
                )
            }

            Text(
                text = timeText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = textColor
            )

            Text(
                text = stringResource(
                    R.string.label_weeks_format,
                    courseWithWeeks.weeks.map { it.weekNumber }.joinToString(", ")
                ),
                fontSize = 11.sp,
                color = if (isSelected) textColor else MiuixTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
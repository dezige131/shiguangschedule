package com.xingheyuzhuan.shiguangschedulemiuix.ui.settings.quickactions.delete

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.xingheyuzhuan.shiguangschedulemiuix.LocalAppNavigator
import com.xingheyuzhuan.shiguangschedulemiuix.R
import com.xingheyuzhuan.shiguangschedulemiuix.data.db.main.CourseWithWeeks
import com.xingheyuzhuan.shiguangschedulemiuix.ui.components.DatePickerModal
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.WindowDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import top.yukonga.miuix.kmp.window.WindowDialog
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun QuickDeleteScreen(
    viewModel: QuickDeleteViewModel = hiltViewModel()
) {
    val navigator = LocalAppNavigator.current
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val weekDays = stringArrayResource(R.array.week_days_full_names)

    // 0: 按周次与星期, 1: 按日期范围
    var selectedMode by remember { mutableIntStateOf(0) }

    var showFilterSheet by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    val scrollBehavior = MiuixScrollBehavior()
    val titleText = stringResource(R.string.item_quick_delete)

    LaunchedEffect(uiState.successMessage, uiState.errorMessage) {
        uiState.successMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.resetMessages()
        }
        uiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.resetMessages()
        }
    }

    Scaffold(
        containerColor = MiuixTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = titleText,
                largeTitle = titleText,
                navigationIcon = {
                    IconButton(onClick = {
                        if (navigator.isNotEmpty()) {
                            navigator.removeAt(navigator.size - 1)
                        }
                    }) {
                        Icon(
                            MiuixIcons.Back,
                            stringResource(R.string.a11y_back),
                            tint = MiuixTheme.colorScheme.onBackground
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            if (uiState.affectedCourses.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MiuixTheme.colorScheme.surface)
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = { showConfirmDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        colors = ButtonDefaults.buttonColors(color = MiuixTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.DeleteForever, null, tint = Color.White)
                        Spacer(Modifier.size(8.dp))
                        Text(
                            stringResource(R.string.confirm_delete),
                            color = MiuixTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 100.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(R.string.label_filter_conditions),
                            color = MiuixTheme.colorScheme.onBackgroundVariant,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(start = 32.dp, bottom = 8.dp, top = 16.dp)
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MiuixTheme.colorScheme.surfaceContainer)
                        ) {
                            // 模式选择器
                            WindowDropdownPreference(
                                title = stringResource(R.string.label_filter_mode),
                                items = listOf(
                                    stringResource(R.string.filter_mode_weeks_days),
                                    stringResource(R.string.filter_mode_date_range)
                                ),
                                selectedIndex = selectedMode,
                                onSelectedIndexChange = { index ->
                                    selectedMode = index
                                    // 切换模式时自动清空另一模式的数据，防止影响筛选结果
                                    if (index == 0) viewModel.clearDateRange() else viewModel.clearWeeksAndDays()
                                }
                            )

                            // 根据模式动态显示对应的设置项
                            if (selectedMode == 0) {
                                // 加回数据处理逻辑：未选择时为 null（不显示小字），已选择时拼接具体数据
                                val weekDaySummary =
                                    if (uiState.selectedWeeks.isEmpty() && uiState.selectedDays.isEmpty()) {
                                        null
                                    } else {
                                        val w =
                                            if (uiState.selectedWeeks.isNotEmpty()) uiState.selectedWeeks.sorted()
                                                .joinToString(", ") + "周 " else ""
                                        val d =
                                            if (uiState.selectedDays.isNotEmpty()) uiState.selectedDays.sorted()
                                                .joinToString("、") { weekDays[it - 1] } else ""
                                        "$w $d".trim()
                                    }

                                ArrowPreference(
                                    title = stringResource(R.string.quick_delete_filter_weeks_days_hint),
                                    summary = weekDaySummary, // <--- 这里把 summary 加回来
                                    onClick = { showFilterSheet = true },
                                    endActions = {
                                        if (uiState.selectedWeeks.isNotEmpty() || uiState.selectedDays.isNotEmpty()) {
                                            IconButton(onClick = { viewModel.clearWeeksAndDays() }) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    null,
                                                    modifier = Modifier.size(20.dp),
                                                    tint = MiuixTheme.colorScheme.onSurfaceVariantActions
                                                )
                                            }
                                        }
                                    }
                                )
                            } else {
                                ArrowPreference(
                                    title = stringResource(R.string.label_start_date),
                                    summary = uiState.startDate?.toString()
                                        ?: stringResource(R.string.status_not_set),
                                    onClick = { showStartDatePicker = true },
                                    endActions = {
                                        if (uiState.startDate != null) {
                                            IconButton(onClick = { viewModel.clearDateRange() }) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    null,
                                                    modifier = Modifier.size(20.dp),
                                                    tint = MiuixTheme.colorScheme.onSurfaceVariantActions
                                                )
                                            }
                                        }
                                    }
                                )

                                ArrowPreference(
                                    title = stringResource(R.string.label_end_date),
                                    summary = uiState.endDate?.toString()
                                        ?: stringResource(R.string.status_not_set),
                                    onClick = { showEndDatePicker = true },
                                    endActions = {
                                        if (uiState.endDate != null) {
                                            IconButton(onClick = { viewModel.clearDateRange() }) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    null,
                                                    modifier = Modifier.size(20.dp),
                                                    tint = MiuixTheme.colorScheme.onSurfaceVariantActions
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    val count = uiState.affectedCourses.size
                    Text(
                        text = if (count > 0) stringResource(
                            R.string.hint_affected_count,
                            count
                        ) else stringResource(R.string.hint_no_selection),
                        color = if (count > 0) MiuixTheme.colorScheme.error else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
                    )
                }

                items(uiState.affectedCourses) { previewItem ->
                    DeletePreviewCard(previewItem.courseWithWeeks, previewItem.targetWeek)
                }
            }

            FilterBottomSheet(
                show = showFilterSheet,
                uiState = uiState,
                viewModel = viewModel,
                onDismiss = { showFilterSheet = false }
            )

            DatePickerModal(
                show = showStartDatePicker,
                title = stringResource(R.string.title_select_start_date),
                initialDate = uiState.startDate,
                onDateSelected = { millis ->
                    val newStart = millis.toLocalDate()
                    viewModel.setDateRange(newStart, uiState.endDate ?: newStart)
                    showStartDatePicker = false
                },
                onDismiss = { showStartDatePicker = false }
            )

            DatePickerModal(
                show = showEndDatePicker,
                title = stringResource(R.string.title_select_end_date),
                initialDate = uiState.endDate,
                onDateSelected = { millis ->
                    val newEnd = millis.toLocalDate()
                    viewModel.setDateRange(uiState.startDate ?: newEnd, newEnd)
                    showEndDatePicker = false
                },
                onDismiss = { showEndDatePicker = false }
            )

            WindowDialog(
                show = showConfirmDialog,
                title = stringResource(R.string.confirm_delete),
                summary = stringResource(R.string.dialog_delete_confirm_msg),
                onDismissRequest = { showConfirmDialog = false }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = { showConfirmDialog = false },
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
                                showConfirmDialog = false
                                viewModel.executeDelete()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(color = MiuixTheme.colorScheme.error)
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
    }
}

@Composable
fun FilterBottomSheet(
    show: Boolean,
    uiState: QuickDeleteUiState,
    viewModel: QuickDeleteViewModel,
    onDismiss: () -> Unit
) {
    val weekDays = stringArrayResource(R.array.week_days_full_names)

    WindowBottomSheet(
        show = show,
        title = stringResource(R.string.title_filter_precise),
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                stringResource(R.string.title_select_weeks),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MiuixTheme.colorScheme.onSurface
            )

            // 重构为自适应 5xN 布局网格
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                (1..20).chunked(5).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { week ->
                            val isSelected = uiState.selectedWeeks.contains(week)
                            Button(
                                onClick = { viewModel.toggleWeek(week) },
                                modifier = Modifier
                                    .weight(1f)
                                    .then(
                                        if (!isSelected) Modifier.border(
                                            1.dp,
                                            MiuixTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                            RoundedCornerShape(16.dp)
                                        )
                                        else Modifier
                                    ),
                                colors = if (isSelected) {
                                    ButtonDefaults.buttonColors(color = MiuixTheme.colorScheme.primary)
                                } else {
                                    ButtonDefaults.buttonColors(color = Color.Transparent)
                                },
                                insideMargin = PaddingValues(vertical = 8.dp)
                            ) {
                                Text(
                                    text = week.toString(),
                                    textAlign = TextAlign.Center,
                                    color = if (isSelected) MiuixTheme.colorScheme.onSurface else MiuixTheme.colorScheme.onSurface
                                )
                            }
                        }
                        // 补齐末尾空白占位，保证宽度统一
                        repeat(5 - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.label_day_of_week),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MiuixTheme.colorScheme.onSurface
                )

                TextButton(
                    text = if (uiState.selectedDays.size == 7) stringResource(R.string.action_deselect_all) else stringResource(
                        R.string.action_select_all
                    ),
                    onClick = { if (uiState.selectedDays.size == 7) viewModel.clearAllDays() else viewModel.selectAllDays() }
                )
            }

            // 星期选择器：重构为 4xN 布局
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                (1..7).chunked(4).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { day ->
                            val isSelected = uiState.selectedDays.contains(day)
                            Button(
                                onClick = { viewModel.toggleDay(day) },
                                modifier = Modifier
                                    .weight(1f)
                                    .then(
                                        if (!isSelected) Modifier.border(
                                            1.dp,
                                            MiuixTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                            RoundedCornerShape(16.dp)
                                        )
                                        else Modifier
                                    ),
                                colors = if (isSelected) {
                                    ButtonDefaults.buttonColors(color = MiuixTheme.colorScheme.primary)
                                } else {
                                    ButtonDefaults.buttonColors(color = Color.Transparent)
                                },
                                insideMargin = PaddingValues(vertical = 8.dp)
                            ) {
                                Text(
                                    text = weekDays[day - 1],
                                    textAlign = TextAlign.Center,
                                    color = if (isSelected) MiuixTheme.colorScheme.onSurface else MiuixTheme.colorScheme.onSurface
                                )
                            }
                        }
                        repeat(4 - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                colors = ButtonDefaults.buttonColors(color = MiuixTheme.colorScheme.primary)
            ) {
                Text(
                    stringResource(R.string.action_confirm),
                    color = MiuixTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
fun DeletePreviewCard(
    courseWithWeeks: CourseWithWeeks,
    targetWeek: Int
) {
    val weekDays = stringArrayResource(R.array.week_days_full_names)
    val course = courseWithWeeks.course

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MiuixTheme.colorScheme.error.copy(alpha = 0.1f))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = course.name,
                    fontSize = 16.sp,
                    color = MiuixTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )

                val weekText = stringResource(R.string.title_current_week, targetWeek.toString())
                val dayString = weekDays[course.day - 1]
                val detailsText = if (course.isCustomTime) {
                    stringResource(
                        R.string.course_time_day_time_details_tweak,
                        dayString,
                        course.customStartTime ?: stringResource(R.string.label_none),
                        course.customEndTime ?: stringResource(R.string.label_none)
                    )
                } else {
                    stringResource(
                        R.string.course_time_day_section_details_tweak,
                        dayString,
                        (course.startSection ?: 0).toString(),
                        (course.endSection ?: 0).toString()
                    )
                }

                Text(
                    text = "$weekText · $detailsText",
                    fontSize = 13.sp,
                    color = MiuixTheme.colorScheme.error.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Icon(
                Icons.Default.DeleteForever,
                null,
                tint = MiuixTheme.colorScheme.error.copy(alpha = 0.5f)
            )
        }
    }
}

private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
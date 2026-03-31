// TweakScheduleScreen.kt
package com.xingheyuzhuan.shiguangschedulemiuix.ui.settings.quickactions.tweaks

import android.content.res.Configuration
import android.text.format.DateFormat
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoubleArrow
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.xingheyuzhuan.shiguangschedulemiuix.LocalAppNavigator
import com.xingheyuzhuan.shiguangschedulemiuix.R
import com.xingheyuzhuan.shiguangschedulemiuix.data.db.main.CourseWithWeeks
import com.xingheyuzhuan.shiguangschedulemiuix.data.repository.CourseTableRepository.TweakMode
import com.xingheyuzhuan.shiguangschedulemiuix.ui.components.CourseTablePickerDialog
import com.xingheyuzhuan.shiguangschedulemiuix.ui.components.DatePickerModal
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.WindowDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun TweakScheduleScreen(
    viewModel: TweakScheduleViewModel = hiltViewModel()
) {
    val navigator = LocalAppNavigator.current
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showCourseTablePicker by remember { mutableStateOf(false) }
    var showFromDatePicker by remember { mutableStateOf(false) }
    var showToDatePicker by remember { mutableStateOf(false) }

    val titleTweakSchedule = stringResource(R.string.title_tweak_schedule)
    val a11yBack = stringResource(R.string.a11y_back)
    val a11ySaveTweak = stringResource(R.string.a11y_save_tweak)
    val actionSelectTable = stringResource(R.string.action_select_table)
    val dialogTitleSelectExportTable = stringResource(R.string.dialog_title_select_export_table)
    val a11yArrow = stringResource(R.string.a11y_arrow)

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.resetMessages()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.resetMessages()
        }
    }

    val scrollBehavior = MiuixScrollBehavior()

    Scaffold(
        containerColor = MiuixTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = titleTweakSchedule,
                largeTitle = titleTweakSchedule,
                navigationIcon = {
                    IconButton(onClick = {
                        if (navigator.isNotEmpty()) {
                            navigator.removeAt(navigator.size - 1)
                        }
                    }) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = a11yBack,
                            tint = MiuixTheme.colorScheme.onBackground
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MiuixTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                Button(
                    onClick = { viewModel.moveCourses() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    colors = ButtonDefaults.buttonColors(color = MiuixTheme.colorScheme.primary)
                ) {
                    Icon(
                        Icons.Default.Done,
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.size(8.dp))
                    // 直接将原先用来辅助发音/无障碍的确认文本用作按钮文字
                    Text(a11ySaveTweak, color = MiuixTheme.colorScheme.onPrimary)
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(paddingValues),
            // 给底部留出空间，防止内容被底部的保存按钮挡住
            contentPadding = PaddingValues(bottom = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                val currentLocale = LocalConfiguration.current.locales.get(0)
                val dateFormatter = remember(currentLocale) {
                    val bestPattern = DateFormat.getBestDateTimePattern(currentLocale, "Md")
                    DateTimeFormatter.ofPattern(bestPattern, currentLocale)
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.section_title_config),
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(start = 32.dp, bottom = 8.dp, top = 8.dp)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MiuixTheme.colorScheme.surfaceContainer)
                    ) {
                        ArrowPreference(
                            title = stringResource(R.string.label_select_tweak_table),
                            summary = uiState.selectedCourseTable?.name ?: actionSelectTable,
                            onClick = { showCourseTablePicker = true }
                        )

                        ArrowPreference(
                            title = stringResource(R.string.label_tweak_from_date),
                            summary = uiState.fromDate.format(dateFormatter),
                            onClick = { showFromDatePicker = true }
                        )

                        ArrowPreference(
                            title = stringResource(R.string.label_tweak_to_date),
                            summary = uiState.toDate.format(dateFormatter),
                            onClick = { showToDatePicker = true }
                        )

                        val modeOptions = listOf(
                            stringResource(R.string.tweak_mode_merge),
                            stringResource(R.string.tweak_mode_overwrite),
                            stringResource(R.string.tweak_mode_exchange)
                        )

                        val selectedModeIndex = when (uiState.tweakMode) {
                            TweakMode.MERGE -> 0
                            TweakMode.OVERWRITE -> 1
                            TweakMode.EXCHANGE -> 2
                            else -> 0
                        }

                        WindowDropdownPreference(
                            title = stringResource(R.string.label_operation_mode),
                            items = modeOptions,
                            selectedIndex = selectedModeIndex,
                            onSelectedIndexChange = { index ->
                                val mode = when (index) {
                                    0 -> TweakMode.MERGE
                                    1 -> TweakMode.OVERWRITE
                                    2 -> TweakMode.EXCHANGE
                                    else -> TweakMode.EXCHANGE
                                }
                                viewModel.onTweakModeChanged(mode)
                            }
                        )
                    }
                }
            }

            item {
                val isLandscape =
                    LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
                val (modeIcon, _) = getTweakModeDisplayInfo(uiState.tweakMode)

                if (isLandscape) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CourseDisplayCard(
                            modifier = Modifier.weight(1f),
                            title = stringResource(R.string.title_tweak_from_course),
                            courses = uiState.fromCourses
                        )

                        Icon(
                            imageVector = modeIcon,
                            contentDescription = a11yArrow,
                            modifier = Modifier.size(32.dp),
                            tint = MiuixTheme.colorScheme.primary
                        )

                        CourseDisplayCard(
                            modifier = Modifier.weight(1f),
                            title = stringResource(R.string.title_tweak_to_course),
                            courses = uiState.toCourses
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CourseDisplayCard(
                            title = stringResource(R.string.title_tweak_from_course),
                            courses = uiState.fromCourses
                        )

                        val verticalIcon = when (uiState.tweakMode) {
                            TweakMode.EXCHANGE -> Icons.Default.SyncAlt
                            TweakMode.OVERWRITE -> Icons.Default.DoubleArrow
                            TweakMode.MERGE -> Icons.Default.ArrowDownward
                            else -> Icons.Default.ArrowDownward
                        }
                        val rotationAngle = if (uiState.tweakMode != TweakMode.MERGE) 90f else 0f

                        Icon(
                            imageVector = verticalIcon,
                            contentDescription = a11yArrow,
                            modifier = Modifier
                                .size(32.dp)
                                .rotate(rotationAngle),
                            tint = MiuixTheme.colorScheme.primary
                        )

                        CourseDisplayCard(
                            title = stringResource(R.string.title_tweak_to_course),
                            courses = uiState.toCourses
                        )
                    }
                }
            }
        }
    }

    if (showCourseTablePicker) {
        CourseTablePickerDialog(
            show = showCourseTablePicker,
            title = dialogTitleSelectExportTable,
            onDismissRequest = { showCourseTablePicker = false },
            onTableSelected = { viewModel.onCourseTableSelected(it); showCourseTablePicker = false }
        )
    }

    DatePickerModal(
        show = showFromDatePicker,
        title = stringResource(R.string.title_select_from_date),
        initialDate = uiState.fromDate,
        onDateSelected = {
            viewModel.onFromDateSelected(it.toLocalDate())
            showFromDatePicker = false
        },
        onDismiss = { showFromDatePicker = false }
    )

    DatePickerModal(
        show = showToDatePicker,
        title = stringResource(R.string.title_select_to_date),
        initialDate = uiState.toDate,
        onDateSelected = {
            viewModel.onToDateSelected(it.toLocalDate())
            showToDatePicker = false
        },
        onDismiss = { showToDatePicker = false }
    )
}

@Composable
private fun getTweakModeDisplayInfo(mode: TweakMode): Pair<ImageVector, String> {
    return when (mode) {
        TweakMode.MERGE -> Icons.AutoMirrored.Filled.ArrowForward to stringResource(R.string.tweak_mode_merge)
        TweakMode.OVERWRITE -> Icons.Default.DoubleArrow to stringResource(R.string.tweak_mode_overwrite)
        TweakMode.EXCHANGE -> Icons.Default.SyncAlt to stringResource(R.string.tweak_mode_exchange)
        else -> Icons.AutoMirrored.Filled.ArrowForward to stringResource(R.string.tweak_mode_merge)
    }
}

@Composable
fun CourseDisplayCard(
    title: String,
    courses: List<CourseWithWeeks>,
    modifier: Modifier = Modifier
) {
    val textNoCourse = stringResource(R.string.text_no_course)
    val sectionFormatRes = R.string.course_time_day_section_details_tweak
    val customTimeFormatRes = R.string.course_time_day_time_details_tweak

    Column(modifier = modifier.fillMaxWidth()) {
        // 小标题提取到容器外部
        Text(
            text = title,
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            fontSize = 13.sp,
            modifier = Modifier.padding(start = 32.dp, bottom = 8.dp)
        )

        // 数据显示容器
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MiuixTheme.colorScheme.surfaceContainer)
                .padding(16.dp)
        ) {
            LazyColumn(modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 250.dp)) {
                if (courses.isEmpty()) {
                    item {
                        Text(
                            text = textNoCourse,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                } else {
                    items(courses) { courseWithWeeks ->
                        val course = courseWithWeeks.course
                        val dayString = getLocalizedDayString(course.day)
                        val detailsText = if (course.isCustomTime) {
                            stringResource(
                                id = customTimeFormatRes,
                                dayString,
                                course.customStartTime ?: "??:??",
                                course.customEndTime ?: "??:??"
                            )
                        } else {
                            stringResource(
                                id = sectionFormatRes,
                                dayString,
                                course.startSection.toString(),
                                course.endSection.toString()
                            )
                        }
                        Column(modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)) {
                            Text(
                                text = course.name,
                                fontSize = 16.sp,
                                color = MiuixTheme.colorScheme.onSurface
                            )
                            Text(
                                text = detailsText,
                                fontSize = 13.sp,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

@Composable
private fun getLocalizedDayString(day: Int): String {
    val weekDays = stringArrayResource(R.array.week_days_full_names)
    return if (day in 1..7) weekDays[day - 1] else stringResource(R.string.text_error)
}
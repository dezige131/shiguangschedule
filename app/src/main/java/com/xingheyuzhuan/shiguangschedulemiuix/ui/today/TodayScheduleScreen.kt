package com.xingheyuzhuan.shiguangschedulemiuix.ui.today

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xingheyuzhuan.shiguangschedulemiuix.R
import com.xingheyuzhuan.shiguangschedulemiuix.data.model.ScheduleGridStyle
import com.xingheyuzhuan.shiguangschedulemiuix.ui.settings.style.StyleSettingsViewModel
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun TodayScheduleScreen(
    viewModel: TodayScheduleViewModel = hiltViewModel(),
    styleViewModel: StyleSettingsViewModel = hiltViewModel()
) {
    val semesterStatus by viewModel.semesterStatus.collectAsState()
    val todayCourses by viewModel.todayCourses.collectAsState()
    val gridStyle by viewModel.gridStyle.collectAsState()

    val styleState by styleViewModel.styleState.collectAsStateWithLifecycle()
    val isFloating = styleState?.enableFloatingBottomBar == true

    // 监测深色模式状态（适配手动切换）
    val systemIsDark = isSystemInDarkTheme()
    val isDark = remember(gridStyle.colorSchemeMode, systemIsDark) {
        when (gridStyle.colorSchemeMode) {
            ColorSchemeMode.Dark, ColorSchemeMode.MonetDark -> true
            ColorSchemeMode.Light, ColorSchemeMode.MonetLight -> false
            else -> systemIsDark
        }
    }

    val scrollBehavior = MiuixScrollBehavior()
    val titleText = stringResource(R.string.title_today_schedule)

    Scaffold(
        containerColor = MiuixTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = titleText,
                largeTitle = titleText,
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            val today = LocalDate.now()
            val todayDateString = remember(today) {
                today.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
            }

            val todayDayOfWeekString = remember(today) {
                today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
            }

            // 头部日期信息
            Text(
                text = "$todayDateString $todayDayOfWeekString",
                fontSize = 22.sp,
                color = MiuixTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = semesterStatus,
                fontSize = 14.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (todayCourses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.text_no_courses_today),
                        fontSize = 16.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                }
            } else {
                val currentTime = LocalTime.now()

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    todayCourses.forEach { course ->
                        val isCourseFinished = remember(currentTime, course) {
                            try {
                                val courseEndTime = LocalTime.parse(course.endTime)
                                currentTime.isAfter(courseEndTime)
                            } catch (e: Exception) {
                                false
                            }
                        }

                        // 获取对应的配色对
                        val colorPair = gridStyle.courseColorMaps.getOrElse(course.colorInt) {
                            ScheduleGridStyle.DEFAULT_COLOR_MAPS[0]
                        }

                        // 根据当前深色/浅色模式选择背景颜色
                        val baseColor = if (isDark) colorPair.dark else colorPair.light

                        // 计算卡片容器颜色（如果已结束，降低透明度）
                        val cardColor = if (isCourseFinished) {
                            baseColor.copy(alpha = 0.3f)
                        } else {
                            baseColor
                        }

                        // 计算文字颜色：深色模式用白色，浅色用黑色。已结束的课程追加透明度处理
                        val contentColor = if (isCourseFinished) {
                            if (isDark) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.4f)
                        } else {
                            if (isDark) Color.White else Color.Black
                        }

                        // ==== 修改这里 ====
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.defaultColors(color = cardColor),
                            cornerRadius = 16.dp,
                            insideMargin = PaddingValues(16.dp)
                        ) {
                            Column {
                                Text(
                                    text = course.name,
                                    fontSize = 18.sp,
                                    textDecoration = if (isCourseFinished) TextDecoration.LineThrough else TextDecoration.None,
                                    fontWeight = FontWeight.Bold,
                                    color = contentColor
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    Text(
                                        text = "${course.startTime} - ${course.endTime}",
                                        fontSize = 13.sp,
                                        color = contentColor.copy(alpha = if (isCourseFinished) 0.5f else 0.8f)
                                    )

                                    course.position.takeIf { it.isNotBlank() }?.let { position ->
                                        Text(
                                            " | ",
                                            fontSize = 13.sp,
                                            color = contentColor.copy(alpha = if (isCourseFinished) 0.5f else 0.8f)
                                        )
                                        Text(
                                            text = position,
                                            fontSize = 13.sp,
                                            color = contentColor.copy(alpha = if (isCourseFinished) 0.5f else 0.8f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    course.teacher.takeIf { it.isNotBlank() }?.let { teacher ->
                                        Text(
                                            " | ",
                                            fontSize = 13.sp,
                                            color = contentColor.copy(alpha = if (isCourseFinished) 0.5f else 0.8f)
                                        )
                                        Text(
                                            text = teacher,
                                            fontSize = 13.sp,
                                            color = contentColor.copy(alpha = if (isCourseFinished) 0.5f else 0.8f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    val baseSpacerHeight = if (isFloating) 65.dp else 50.dp
                    Spacer(modifier = Modifier.height(baseSpacerHeight))
                }
            }
        }
    }
}
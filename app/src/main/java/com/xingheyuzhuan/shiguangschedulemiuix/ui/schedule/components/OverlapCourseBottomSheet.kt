package com.xingheyuzhuan.shiguangschedulemiuix.ui.schedule.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedulemiuix.R
import com.xingheyuzhuan.shiguangschedulemiuix.data.db.main.CourseWithWeeks
import com.xingheyuzhuan.shiguangschedulemiuix.data.db.main.TimeSlot
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet

/**
 * 重叠课程列表底部动作条。
 * 展示同一位置下的所有课程（包含本周与非本周）。
 */
@Composable
fun OverlapCourseBottomSheet(
    show: Boolean, // [新增] WindowBottomSheet 需要这个状态来执行退出动画
    courses: List<CourseWithWeeks>,
    timeSlots: List<TimeSlot>,
    style: ScheduleGridStyleComposed,
    currentWeek: Int?,
    onCourseClicked: (CourseWithWeeks) -> Unit,
    onDismissRequest: () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDarkTheme = when (style.colorSchemeMode) {
        top.yukonga.miuix.kmp.theme.ColorSchemeMode.Dark,
        top.yukonga.miuix.kmp.theme.ColorSchemeMode.MonetDark -> true

        top.yukonga.miuix.kmp.theme.ColorSchemeMode.Light,
        top.yukonga.miuix.kmp.theme.ColorSchemeMode.MonetLight -> false

        else -> systemDark
    }

    val fallbackColorAdapted = if (isDarkTheme) {
        style.courseColorMaps.first().dark
    } else {
        style.courseColorMaps.first().light
    }

    WindowBottomSheet(
        show = show,
        title = stringResource(R.string.title_course_overlap),
        onDismissRequest = onDismissRequest
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            // 将底部 padding 移至 contentPadding，使得滚动体验更平滑
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(courses) { courseWithWeeks ->
                val course = courseWithWeeks.course

                val isCurrentWeek = currentWeek?.let { cw ->
                    courseWithWeeks.weeks.any { it.weekNumber == cw }
                } ?: true

                val isCustomTimeCourse =
                    course.customStartTime != null && course.customEndTime != null
                val startSlot =
                    timeSlots.find { it.number == course.startSection }?.startTime ?: "N/A"
                val endSlot = timeSlots.find { it.number == course.endSection }?.endTime ?: "N/A"

                val colorIndex = course.colorInt.takeIf { it in style.courseColorMaps.indices }
                val cardBaseColor = colorIndex?.let { index ->
                    val dualColor = style.courseColorMaps[index]
                    if (isDarkTheme) dualColor.dark else dualColor.light
                } ?: fallbackColorAdapted

                val cardColor = cardBaseColor.copy(alpha = style.courseBlockAlpha)
                // 改用 Miuix 颜色进行深浅适配
                val textColor = MiuixTheme.colorScheme.onSurface

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCourseClicked(courseWithWeeks) },
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = course.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = textColor
                            )
                            Spacer(Modifier.height(8.dp))

                            if (isCustomTimeCourse) {
                                Text(
                                    text = "${course.customStartTime} - ${course.customEndTime}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = textColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                            } else {
                                Text(
                                    text = stringResource(
                                        R.string.course_time_description,
                                        course.startSection ?: 0,
                                        course.endSection ?: 0,
                                        startSlot,
                                        endSlot
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = textColor
                                )
                            }

                            Text(
                                text = stringResource(
                                    R.string.course_position_prefix,
                                    course.position
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = textColor
                            )

                            Text(
                                text = stringResource(
                                    R.string.course_teacher_prefix,
                                    course.teacher
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = textColor
                            )
                        }

                        if (!isCurrentWeek) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(
                                        color = (if (isDarkTheme) Color.Black else Color.White)
                                            .copy(alpha = 0.618f)
                                    )
                                    .drawBehind {
                                        val stripeWidth = 5.dp.toPx()
                                        val stripeColor =
                                            (if (isDarkTheme) Color.White else Color.Black)
                                                .copy(alpha = 0.06f)
                                        val brush =
                                            androidx.compose.ui.graphics.Brush.linearGradient(
                                                0.0f to stripeColor,
                                                0.45f to stripeColor,
                                                0.55f to Color.Transparent,
                                                1.0f to Color.Transparent,
                                                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                                end = androidx.compose.ui.geometry.Offset(
                                                    stripeWidth,
                                                    stripeWidth
                                                ),
                                                tileMode = androidx.compose.ui.graphics.TileMode.Repeated
                                            )
                                        drawRect(brush = brush)
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}
package com.xingheyuzhuan.shiguangschedulemiuix.ui.schedule.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import com.xingheyuzhuan.shiguangschedulemiuix.R
import com.xingheyuzhuan.shiguangschedulemiuix.data.db.main.TimeSlot
import com.xingheyuzhuan.shiguangschedulemiuix.ui.schedule.MergedCourseBlock
import top.yukonga.miuix.kmp.theme.MiuixTheme

interface ISchedulable {
    val columnIndex: Int
    val startSection: Float
    val endSection: Float
    val rawData: MergedCourseBlock
}

@Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
@Composable
fun ScheduleGrid(
    style: ScheduleGridStyleComposed,
    dates: List<String>,
    currentYear: String,
    timeSlots: List<TimeSlot>,
    mergedCourses: List<MergedCourseBlock>,
    showWeekends: Boolean,
    todayIndex: Int,
    firstDayOfWeek: Int,
    currentSectionIndex: Int = -1,
    onCourseBlockClicked: (MergedCourseBlock) -> Unit,
    onGridCellClicked: (Int, Int) -> Unit,
    onTimeSlotClicked: () -> Unit,
    // 新增防遮挡底部高度参数
    bottomSpacerHeight: Dp = 0.dp
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val screenWidth = maxWidth

        val weekDays = stringArrayResource(R.array.week_days_short_names).toList()
        val reorderedWeekDays = rearrangeDays(weekDays, firstDayOfWeek)
        val displayDays = if (showWeekends) reorderedWeekDays else reorderedWeekDays.take(5)

        val cellWidth = (screenWidth - style.timeColumnWidth) / displayDays.size
        val totalGridHeight = style.sectionHeight * timeSlots.size

        // 使用 Miuix 的颜色
        val gridLineColor = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.1f)

        val schedulables = mergedCourses.mapNotNull { block ->
            val displayIdx = mapDayToDisplayIndex(block.day, firstDayOfWeek, showWeekends)
            if (displayIdx == -1) return@mapNotNull null
            object : ISchedulable {
                override val columnIndex = displayIdx
                override val startSection = block.startSection
                override val endSection = block.endSection
                override val rawData = block
            }
        }

        Column(Modifier.fillMaxSize()) {
            DayHeader(style, displayDays, dates, currentYear, todayIndex, gridLineColor)

            // 🌟 核心修改：新建一个带滚动的 Column，包裹住网格和底部的 Spacer
            Column(Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())) {

                // 去掉这里原来的 verticalScroll，改为 fillMaxWidth
                Row(Modifier.fillMaxWidth()) {
                    TimeColumn(
                        style,
                        timeSlots,
                        onTimeSlotClicked,
                        Modifier.height(totalGridHeight),
                        gridLineColor,
                        currentSectionIndex
                    )

                    Box(Modifier
                        .height(totalGridHeight)
                        .weight(1f)) {
                        ClickableGrid(
                            dayCount = displayDays.size,
                            slotCount = timeSlots.size,
                            sectionHeight = style.sectionHeight,
                            lineColor = if (style.hideGridLines) Color.Transparent else gridLineColor
                        ) { dayIdx, sec ->
                            onGridCellClicked(mapDisplayIndexToDay(dayIdx, firstDayOfWeek), sec)
                        }

                        schedulables.forEach { item ->
                            val topOffset = item.startSection * style.sectionHeight
                            val blockHeight =
                                (item.endSection - item.startSection) * style.sectionHeight

                            Box(
                                modifier = Modifier
                                    .offset(x = item.columnIndex * cellWidth, y = topOffset)
                                    .size(width = cellWidth, height = blockHeight)
                                    .padding(style.courseBlockOuterPadding)
                                    .clickable { onCourseBlockClicked(item.rawData) }
                            ) {
                                CourseBlock(
                                    mergedBlock = item.rawData,
                                    style = style,
                                    startTime = item.rawData.courses.firstOrNull()?.course?.let {
                                        if (it.isCustomTime) it.customStartTime
                                        else timeSlots.find { ts -> ts.number == it.startSection }?.startTime
                                    }
                                )
                            }
                        }
                    }
                } // Row 结束

                // 🌟 新增：在滑动的最底端垫入防遮挡高度
                Spacer(modifier = Modifier.height(bottomSpacerHeight))

            } // 滚动 Column 结束
        }
    }
}

// 子组件
@Composable
private fun DayHeader(
    style: ScheduleGridStyleComposed,
    displayDays: List<String>,
    dates: List<String>,
    currentYear: String,
    todayIndex: Int,
    lineColor: Color
) {
    Row(Modifier
        .fillMaxWidth()
        .height(style.dayHeaderHeight)) {
        Box(
            Modifier
                .width(style.timeColumnWidth)
                .fillMaxHeight()
                .drawBehind {
                    if (!style.hideGridLines) {
                        drawLine(
                            lineColor,
                            Offset(size.width, 0f),
                            Offset(size.width, size.height),
                            1f
                        )
                        drawLine(
                            lineColor,
                            Offset(0f, size.height),
                            Offset(size.width, size.height),
                            1f
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = currentYear,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
        }
        displayDays.forEachIndexed { index, day ->
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (index == todayIndex) MiuixTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                    .drawBehind {
                        if (!style.hideGridLines) {
                            drawLine(
                                lineColor,
                                Offset(size.width, 0f),
                                Offset(size.width, size.height),
                                1f
                            )
                            drawLine(
                                lineColor,
                                Offset(0f, size.height),
                                Offset(size.width, size.height),
                                1f
                            )
                        }
                    }, contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = day,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MiuixTheme.colorScheme.onSurface
                    )
                    if (!style.hideDateUnderDay && dates.size > index) {
                        Text(
                            text = dates[index],
                            fontSize = 10.sp,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeColumn(
    style: ScheduleGridStyleComposed,
    timeSlots: List<TimeSlot>,
    onTimeSlotClicked: () -> Unit,
    modifier: Modifier,
    lineColor: Color,
    currentSectionIndex: Int = -1
) {
    Column(modifier.width(style.timeColumnWidth)) {
        timeSlots.forEachIndexed { index, slot ->
            val isCurrentSection = index + 1 == currentSectionIndex

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(style.sectionHeight)
                    .clickable { onTimeSlotClicked() }
                    .background(if (isCurrentSection) MiuixTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                    .drawBehind {
                        if (!style.hideGridLines) {
                            drawLine(
                                lineColor,
                                Offset(size.width, 0f),
                                Offset(size.width, size.height),
                                1f
                            )
                            drawLine(
                                lineColor,
                                Offset(0f, size.height),
                                Offset(size.width, size.height),
                                1f
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                val h = maxHeight

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = slot.number.toString(),
                        fontSize = if (h < 32.dp) 12.sp else 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MiuixTheme.colorScheme.onSurface
                    )
                    if (!style.hideSectionTime) {
                        when {
                            h >= 52.dp -> {
                                Spacer(Modifier.height(2.dp))
                                TimeText(slot.startTime)
                                TimeText(slot.endTime)
                            }

                            h >= 38.dp -> {
                                Text(
                                    text = "${slot.startTime}-${slot.endTime}",
                                    fontSize = 8.sp,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeText(text: String) {
    Text(
        text = text,
        fontSize = 10.sp,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        style = TextStyle(lineHeight = 1.em)
    )
}

@Composable
private fun ClickableGrid(
    dayCount: Int,
    slotCount: Int,
    sectionHeight: Dp,
    lineColor: Color,
    onClick: (Int, Int) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        for (sec in 1..slotCount) {
            Row(Modifier
                .fillMaxWidth()
                .height(sectionHeight)) {
                repeat(dayCount) { idx ->
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .drawBehind {
                                drawLine(
                                    lineColor,
                                    Offset(size.width, 0f),
                                    Offset(size.width, size.height),
                                    1f
                                )
                                drawLine(
                                    lineColor,
                                    Offset(0f, size.height),
                                    Offset(size.width, size.height),
                                    1f
                                )
                            }
                            .clickable { onClick(idx, sec) }
                    )
                }
            }
        }
    }
}

// 辅助逻辑
private fun rearrangeDays(originalDays: List<String>, firstDayOfWeek: Int): List<String> {
    val startIndex = (firstDayOfWeek - 1).coerceIn(0, 6)
    return originalDays.subList(startIndex, originalDays.size) + originalDays.subList(0, startIndex)
}

private fun mapDayToDisplayIndex(courseDay: Int, firstDayOfWeek: Int, showWeekends: Boolean): Int {
    val idx = (courseDay - firstDayOfWeek + 7) % 7
    return if (idx >= if (showWeekends) 7 else 5) -1 else idx
}

private fun mapDisplayIndexToDay(idx: Int, firstDayOfWeek: Int): Int =
    (firstDayOfWeek - 1 + idx) % 7 + 1
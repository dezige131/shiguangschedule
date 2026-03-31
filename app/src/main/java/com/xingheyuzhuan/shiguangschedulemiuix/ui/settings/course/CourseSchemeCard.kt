package com.xingheyuzhuan.shiguangschedulemiuix.ui.settings.course

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xingheyuzhuan.shiguangschedulemiuix.R
import com.xingheyuzhuan.shiguangschedulemiuix.data.model.DualColor
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 课程方案卡片
 * 负责展示单个课程的时间、地点、老师等具体安排
 */
@Composable
fun CourseSchemeCard(
    scheme: CourseScheme,
    courseColorMaps: List<DualColor>,
    colorSchemeMode: top.yukonga.miuix.kmp.theme.ColorSchemeMode,
    onTeacherChange: (String) -> Unit,
    onPositionChange: (String) -> Unit,
    onColorClick: () -> Unit,
    onTimeClick: () -> Unit,
    onWeeksClick: () -> Unit,
    onDayClick: () -> Unit,
    onRemoveClick: () -> Unit,
    onToggleCustomTime: (Boolean) -> Unit,
    showRemoveButton: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        cornerRadius = 16.dp,
        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer),
        insideMargin = PaddingValues(0.dp) // 去除默认边距以支持左侧色条
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {

            // 左侧颜色指示器块
            ColorIndicatorSection(
                colorIndex = scheme.colorIndex,
                colorMaps = courseColorMaps,
                colorSchemeMode = colorSchemeMode,
                onClick = onColorClick
            )

            // 右侧内容区
            Column(modifier = Modifier
                .padding(16.dp)
                .weight(1f)) {

                // 1. 教师输入行 (包含图标、输入框、自定义时间开关、删除按钮)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = stringResource(R.string.label_teacher),
                        tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                        modifier = Modifier
                            .padding(start = 4.dp, end = 16.dp)
                            .size(24.dp) // 统一 24.dp 大小
                    )

                    TextField(
                        value = scheme.teacher,
                        onValueChange = onTeacherChange,
                        label = stringResource(R.string.label_teacher),
                        modifier = Modifier.weight(1f),
                        backgroundColor = Color.Transparent,
                        singleLine = true,
                        textStyle = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MiuixTheme.colorScheme.onSurface
                        )
                    )

                    Text(
                        stringResource(R.string.label_custom_time),
                        fontSize = 12.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                    Switch(
                        checked = scheme.isCustomTime,
                        onCheckedChange = onToggleCustomTime,
                        modifier = Modifier
                            .scale(0.7f)
                            .padding(horizontal = 4.dp)
                    )

                    if (showRemoveButton) {
                        IconButton(onClick = onRemoveClick) {
                            Icon(Icons.Default.Delete, null, tint = MiuixTheme.colorScheme.error)
                        }
                    }
                }

                // 添加行间距，避免太拥挤
                Spacer(modifier = Modifier.height(16.dp))

                // 2. 地点输入行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = stringResource(R.string.label_position),
                        tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                        modifier = Modifier
                            .padding(start = 4.dp, end = 16.dp)
                            .size(24.dp) // 统一 24.dp 大小
                    )

                    TextField(
                        value = scheme.position,
                        onValueChange = onPositionChange,
                        label = stringResource(R.string.label_position),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                        // 移除了这里的 leadingIcon，改为外置
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 底部区域：时间与周次
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 时间显示逻辑
                    if (scheme.isCustomTime) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val dayNames = stringArrayResource(R.array.week_days_full_names)
                            TimeSection(
                                dayName = stringResource(R.string.label_day_of_week),
                                timeDesc = dayNames.getOrNull(scheme.day - 1) ?: "",
                                onClick = onDayClick,
                                modifier = Modifier.fillMaxWidth()
                            )
                            TimeSection(
                                dayName = stringResource(R.string.label_custom_time),
                                timeDesc = "${scheme.customStartTime.ifBlank { "00:00" }}-${scheme.customEndTime.ifBlank { "00:00" }}",
                                onClick = onTimeClick,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        val dayNames = stringArrayResource(R.array.week_days_full_names)
                        TimeSection(
                            dayName = dayNames.getOrNull(scheme.day - 1) ?: "",
                            timeDesc = "${scheme.startSection}-${scheme.endSection}${
                                stringResource(
                                    R.string.label_section_range_suffix
                                )
                            }",
                            onClick = onTimeClick,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }

                    // 周次展示
                    WeekSection(
                        selectedWeeks = scheme.weeks,
                        onClick = onWeeksClick,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
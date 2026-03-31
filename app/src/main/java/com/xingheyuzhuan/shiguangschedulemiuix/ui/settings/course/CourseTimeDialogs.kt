package com.xingheyuzhuan.shiguangschedulemiuix.ui.settings.course

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xingheyuzhuan.shiguangschedulemiuix.R
import com.xingheyuzhuan.shiguangschedulemiuix.data.db.main.TimeSlot
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.NumberPicker
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet

/**
 * 节次时间选择底部弹窗 (节次模式)
 */
@Composable
fun CourseTimePickerBottomSheet(
    show: Boolean = true,
    selectedDay: Int,
    onDaySelected: (Int) -> Unit,
    startSection: Int,
    onStartSectionChange: (Int) -> Unit,
    endSection: Int,
    onEndSectionChange: (Int) -> Unit,
    timeSlots: List<TimeSlot>,
    onDismissRequest: () -> Unit
) {
    var tempSelectedDay by remember(selectedDay) { mutableIntStateOf(selectedDay) }
    var tempStartSection by remember(startSection) { mutableIntStateOf(startSection) }
    var tempEndSection by remember(endSection) { mutableIntStateOf(endSection) }

    val context = LocalContext.current
    val timeInvalidText = stringResource(R.string.toast_time_invalid)

    WindowBottomSheet(
        show = show,
        title = stringResource(R.string.title_select_time),
        onDismissRequest = onDismissRequest,
        startAction = {
            TextButton(
                text = stringResource(R.string.action_cancel),
                onClick = onDismissRequest
            )
        },
        endAction = {
            TextButton(
                text = stringResource(R.string.action_confirm),
                onClick = {
                    if (tempStartSection > tempEndSection) {
                        Toast.makeText(context, timeInvalidText, Toast.LENGTH_SHORT).show()
                    } else {
                        onDaySelected(tempSelectedDay)
                        onStartSectionChange(tempStartSection)
                        onEndSectionChange(tempEndSection)
                        onDismissRequest()
                    }
                },
                colors = ButtonDefaults.textButtonColorsPrimary()
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 星期
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.label_day_of_week),
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    DayPicker(
                        selectedDay = tempSelectedDay,
                        onDaySelected = { tempSelectedDay = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                // 开始节次
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.label_start_section),
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    SectionPicker(selectedSection = tempStartSection, onSectionSelected = {
                        tempStartSection = it
                        if (it > tempEndSection) tempEndSection = it
                    }, timeSlots = timeSlots, modifier = Modifier.fillMaxWidth())
                }
                // 结束节次
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.label_end_section),
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    SectionPicker(
                        selectedSection = tempEndSection,
                        onSectionSelected = { tempEndSection = it },
                        timeSlots = timeSlots,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * 星期选择对话框
 */
@Composable
fun DayPickerDialog(
    show: Boolean = true,
    selectedDay: Int,
    onDismissRequest: () -> Unit,
    onDaySelected: (Int) -> Unit
) {
    var tempSelectedDay by remember(selectedDay) { mutableIntStateOf(selectedDay) }

    WindowBottomSheet(
        show = show,
        title = stringResource(R.string.label_day_of_week),
        onDismissRequest = onDismissRequest,
        startAction = {
            TextButton(
                text = stringResource(R.string.action_cancel),
                onClick = onDismissRequest
            )
        },
        endAction = {
            TextButton(
                text = stringResource(R.string.action_confirm),
                onClick = {
                    onDaySelected(tempSelectedDay)
                    onDismissRequest()
                },
                colors = ButtonDefaults.textButtonColorsPrimary()
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DayPicker(
                selectedDay = tempSelectedDay,
                onDaySelected = { tempSelectedDay = it },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun DayPicker(selectedDay: Int, onDaySelected: (Int) -> Unit, modifier: Modifier = Modifier) {
    val days = stringArrayResource(R.array.week_days_full_names)

    NumberPicker(
        value = selectedDay,
        onValueChange = onDaySelected,
        range = 1..7,
        label = { days.getOrNull(it - 1) ?: it.toString() },
        modifier = modifier
    )
}

@Composable
fun SectionPicker(
    selectedSection: Int,
    onSectionSelected: (Int) -> Unit,
    timeSlots: List<TimeSlot>,
    modifier: Modifier = Modifier
) {
    val sectionNumbers = timeSlots.map { it.number }.sorted()
    val validSelected =
        if (selectedSection in sectionNumbers) selectedSection else sectionNumbers.firstOrNull()
            ?: 1

    val minSec = sectionNumbers.minOrNull() ?: 1
    val maxSec = sectionNumbers.maxOrNull() ?: 12

    NumberPicker(
        value = validSelected,
        onValueChange = onSectionSelected,
        range = minSec..maxSec,
        modifier = modifier
    )
}
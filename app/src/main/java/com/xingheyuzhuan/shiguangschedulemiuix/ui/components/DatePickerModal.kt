package com.xingheyuzhuan.shiguangschedulemiuix.ui.components

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedulemiuix.R
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.NumberPicker
import top.yukonga.miuix.kmp.basic.NumberPickerDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * 日期选择器对话框 (年月日)
 */
@Composable
fun DatePickerModal(
    show: Boolean, // 新增 show 参数
    title: String = stringResource(R.string.item_set_start_date),
    initialDate: LocalDate? = null,
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val defaultDate = initialDate ?: LocalDate.now()
    var year by remember { mutableIntStateOf(defaultDate.year) }
    var month by remember { mutableIntStateOf(defaultDate.monthValue) }
    var day by remember { mutableIntStateOf(defaultDate.dayOfMonth) }

    val maxDays = remember(year, month) { YearMonth.of(year, month).lengthOfMonth() }
    if (day > maxDays) day = maxDays

    val pickerColors = NumberPickerDefaults.colors(
        selectedTextColor = MiuixTheme.colorScheme.primary,
        unselectedTextColor = MiuixTheme.colorScheme.onSurface
    )

    // 替换为 WindowDialog
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    stringResource(R.string.label_year),
                    modifier = Modifier.weight(1.2f),
                    textAlign = TextAlign.Center,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
                Text(
                    stringResource(R.string.label_month),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
                Text(
                    stringResource(R.string.label_day),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NumberPicker(
                    value = year,
                    onValueChange = { year = it },
                    range = 2000..2100,
                    modifier = Modifier.weight(1.2f)
                )
                NumberPicker(
                    value = month,
                    onValueChange = { month = it },
                    range = 1..12,
                    wrapAround = true,
                    modifier = Modifier.weight(1f)
                )
                NumberPicker(
                    value = day,
                    onValueChange = { day = it },
                    range = 1..maxDays,
                    wrapAround = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 底部双按钮布局
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors() // 次要按钮颜色 (灰色)
                ) {
                    Text(
                        stringResource(R.string.action_cancel),
                        color = MiuixTheme.colorScheme.onSurface
                    )
                }

                Button(
                    onClick = {
                        val selectedDate = LocalDate.of(year, month, day)
                        val millis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
                            .toEpochMilli()
                        onDateSelected(millis)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColorsPrimary() // 主要按钮颜色 (蓝色)
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
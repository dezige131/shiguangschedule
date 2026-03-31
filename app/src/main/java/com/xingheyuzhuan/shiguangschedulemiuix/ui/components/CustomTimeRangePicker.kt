package com.xingheyuzhuan.shiguangschedulemiuix.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xingheyuzhuan.shiguangschedulemiuix.R
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.NumberPicker
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import java.util.Locale

/**
 * 自定义时间模式：4滚轮时间范围选择底部弹窗
 */
@Composable
fun CustomTimeRangePickerBottomSheet(
    show: Boolean = true,
    initialStartTime: String,
    initialEndTime: String,
    onDismissRequest: () -> Unit,
    onTimeRangeSelected: (startTime: String, endTime: String) -> Unit
) {
    val context = LocalContext.current
    val endTimeInvalidText = stringResource(R.string.toast_end_time_must_be_later)

    fun parse(t: String) = t.split(":").let {
        (it.getOrNull(0)?.toIntOrNull() ?: 8) to (it.getOrNull(1)?.toIntOrNull() ?: 0)
    }

    val (startH, startM) = parse(initialStartTime)
    val (endH, endM) = parse(initialEndTime)

    var sH by remember(initialStartTime) { mutableIntStateOf(startH) }
    var sM by remember(initialStartTime) { mutableIntStateOf(startM) }
    var eH by remember(initialEndTime) { mutableIntStateOf(endH) }
    var eM by remember(initialEndTime) { mutableIntStateOf(endM) }

    WindowBottomSheet(
        show = show,
        title = stringResource(R.string.label_custom_time),
        onDismissRequest = onDismissRequest,
        startAction = {
            TextButton(text = stringResource(R.string.action_cancel), onClick = onDismissRequest)
        },
        endAction = {
            TextButton(
                text = stringResource(R.string.action_confirm),
                onClick = {
                    val startTotal = sH * 60 + sM
                    val endTotal = eH * 60 + eM

                    if (startTotal >= endTotal) {
                        Toast.makeText(context, endTimeInvalidText, Toast.LENGTH_SHORT).show()
                    } else {
                        val startStr = String.format(Locale.US, "%02d:%02d", sH, sM)
                        val endStr = String.format(Locale.US, "%02d:%02d", eH, eM)
                        onTimeRangeSelected(startStr, endStr)
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
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Text(
                    text = stringResource(R.string.label_start_time),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.width(32.dp))
                Text(
                    text = stringResource(R.string.label_end_time),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // 开始时间滚轮组
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NumberPicker(
                        value = sH,
                        onValueChange = { sH = it },
                        range = 0..23,
                        label = { String.format(Locale.US, "%02d", it) },
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        ":",
                        fontSize = 18.sp,
                        color = MiuixTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    NumberPicker(
                        value = sM,
                        onValueChange = { sM = it },
                        range = 0..59,
                        label = { String.format(Locale.US, "%02d", it) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    "-",
                    modifier = Modifier.padding(horizontal = 12.dp),
                    fontSize = 18.sp,
                    color = MiuixTheme.colorScheme.onSurface
                )

                // 结束时间滚轮组
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NumberPicker(
                        value = eH,
                        onValueChange = { eH = it },
                        range = 0..23,
                        label = { String.format(Locale.US, "%02d", it) },
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        ":",
                        fontSize = 18.sp,
                        color = MiuixTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    NumberPicker(
                        value = eM,
                        onValueChange = { eM = it },
                        range = 0..59,
                        label = { String.format(Locale.US, "%02d", it) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
// src/main/java/com/xingheyuzhuan/shiguangschedulemiuix/ui/schedule/components/WeekSelectorBottomSheet.kt

package com.xingheyuzhuan.shiguangschedulemiuix.ui.schedule.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedulemiuix.R
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet

/**
 * 周选择器底部动作条 (5xN Miuix Button 网格样式)
 */
@Composable
fun WeekSelectorBottomSheet(
    show: Boolean,
    totalWeeks: Int,
    currentWeek: Int?,
    selectedWeek: Int?,
    onWeekSelected: (Int) -> Unit,
    onDismissRequest: () -> Unit
) {
    WindowBottomSheet(
        show = show,
        title = stringResource(R.string.title_select_week),
        onDismissRequest = onDismissRequest
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                (1..totalWeeks).chunked(5).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { weekNumber ->
                            val isCurrentWeek = weekNumber == currentWeek
                            val isSelectedWeek = weekNumber == selectedWeek

                            Button(
                                onClick = { onWeekSelected(weekNumber) },
                                modifier = Modifier
                                    .weight(1f)
                                    .then(
                                        if (!isSelectedWeek) Modifier.border(
                                            1.dp,
                                            MiuixTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                            RoundedCornerShape(16.dp)
                                        )
                                        else Modifier
                                    ),
                                colors = if (isSelectedWeek) {
                                    ButtonDefaults.buttonColorsPrimary()
                                } else {
                                    ButtonDefaults.buttonColors(color = Color.Transparent)
                                },
                                insideMargin = PaddingValues(vertical = 8.dp)
                            ) {
                                val textColor = if (isSelectedWeek) MiuixTheme.colorScheme.onPrimary
                                else if (isCurrentWeek) MiuixTheme.colorScheme.primary
                                else MiuixTheme.colorScheme.onSurface

                                Text(
                                    text = weekNumber.toString(),
                                    textAlign = TextAlign.Center,
                                    color = textColor
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
            Spacer(Modifier.height(16.dp)) // 留出呼吸空间
        }
    }
}
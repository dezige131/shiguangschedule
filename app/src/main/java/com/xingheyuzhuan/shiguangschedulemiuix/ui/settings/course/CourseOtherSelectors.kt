package com.xingheyuzhuan.shiguangschedulemiuix.ui.settings.course

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xingheyuzhuan.shiguangschedulemiuix.R
import com.xingheyuzhuan.shiguangschedulemiuix.data.model.DualColor
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet

@Composable
fun WeekSection(
    selectedWeeks: Set<Int>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val label = stringResource(R.string.label_course_weeks)
    val noneSelected = stringResource(R.string.label_none)

    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.primary.copy(alpha = 0.1f)),
        cornerRadius = 12.dp,
        insideMargin = PaddingValues(12.dp)
    ) {
        Column {
            Text(text = label, fontSize = 12.sp, color = MiuixTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
            if (selectedWeeks.isEmpty()) {
                Text(
                    text = noneSelected,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MiuixTheme.colorScheme.onSurface
                )
            } else {
                Text(
                    text = stringResource(
                        R.string.text_weeks_selected,
                        selectedWeeks.sorted().joinToString(", ")
                    ),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MiuixTheme.colorScheme.onSurface,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun TimeSection(
    dayName: String,
    timeDesc: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        // 使用 surfaceContainerHighest 保证在浅色模式下也能清晰地看到灰色的背景框
        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainerHighest),
        cornerRadius = 12.dp,
        insideMargin = PaddingValues(12.dp)
    ) {
        Column {
            Text(
                text = dayName,
                fontSize = 12.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = timeDesc,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MiuixTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ColorIndicatorSection(
    colorIndex: Int,
    colorMaps: List<DualColor>,
    colorSchemeMode: top.yukonga.miuix.kmp.theme.ColorSchemeMode,
    onClick: () -> Unit
) {
    val isDark = when (colorSchemeMode) {
        top.yukonga.miuix.kmp.theme.ColorSchemeMode.Light, top.yukonga.miuix.kmp.theme.ColorSchemeMode.MonetLight -> false
        top.yukonga.miuix.kmp.theme.ColorSchemeMode.Dark, top.yukonga.miuix.kmp.theme.ColorSchemeMode.MonetDark -> true
        else -> isSystemInDarkTheme()
    }
    val displayColor = colorMaps.getOrNull(colorIndex)?.let {
        if (isDark) it.dark else it.light
    } ?: MiuixTheme.colorScheme.outline

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(16.dp)
            .background(displayColor)
            .clickable(onClick = onClick)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WeekSelectorBottomSheet(
    show: Boolean = true,
    totalWeeks: Int,
    selectedWeeks: Set<Int>,
    onDismissRequest: () -> Unit,
    onConfirm: (Set<Int>) -> Unit
) {
    var tempSelectedWeeks by remember(selectedWeeks, show) { mutableStateOf(selectedWeeks) }

    val actionSelectAll = stringResource(R.string.action_select_all)
    val actionSingleWeek = stringResource(R.string.action_single_week)
    val actionDoubleWeek = stringResource(R.string.action_double_week)

    WindowBottomSheet(
        show = show,
        title = stringResource(R.string.title_select_weeks),
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
                onClick = { onConfirm(tempSelectedWeeks); onDismissRequest() },
                colors = ButtonDefaults.textButtonColorsPrimary()
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 核心重构：5xN 按钮网格布局
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 350.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                (1..totalWeeks).chunked(5).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { weekNumber ->
                            val isSelected = tempSelectedWeeks.contains(weekNumber)
                            Button(
                                onClick = {
                                    tempSelectedWeeks =
                                        if (isSelected) tempSelectedWeeks - weekNumber else tempSelectedWeeks + weekNumber
                                },
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
                                    text = weekNumber.toString(),
                                    textAlign = TextAlign.Center,
                                    color = if (isSelected) MiuixTheme.colorScheme.onPrimary else MiuixTheme.colorScheme.onSurface
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

            // 快捷操作栏
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 全选
                val allSelected = tempSelectedWeeks.size == totalWeeks && totalWeeks > 0
                Button(
                    onClick = {
                        tempSelectedWeeks = if (allSelected) emptySet() else (1..totalWeeks).toSet()
                    },
                    modifier = Modifier.then(
                        if (!allSelected) Modifier.border(
                            1.dp,
                            MiuixTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                            RoundedCornerShape(16.dp)
                        )
                        else Modifier
                    ),
                    colors = if (allSelected) ButtonDefaults.buttonColors(color = MiuixTheme.colorScheme.primary)
                    else ButtonDefaults.buttonColors(color = Color.Transparent),
                    insideMargin = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        actionSelectAll,
                        fontSize = 14.sp,
                        color = if (allSelected) MiuixTheme.colorScheme.onPrimary else MiuixTheme.colorScheme.onSurface
                    )
                }

                // 单周
                val singleSelected =
                    tempSelectedWeeks.isNotEmpty() && tempSelectedWeeks.all { it % 2 != 0 }
                Button(
                    onClick = {
                        tempSelectedWeeks = (1..totalWeeks).filter { it % 2 != 0 }.toSet()
                    },
                    modifier = Modifier.then(
                        if (!singleSelected) Modifier.border(
                            1.dp,
                            MiuixTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                            RoundedCornerShape(16.dp)
                        )
                        else Modifier
                    ),
                    colors = if (singleSelected) ButtonDefaults.buttonColors(color = MiuixTheme.colorScheme.primary)
                    else ButtonDefaults.buttonColors(color = Color.Transparent),
                    insideMargin = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        actionSingleWeek,
                        fontSize = 14.sp,
                        color = if (singleSelected) MiuixTheme.colorScheme.onPrimary else MiuixTheme.colorScheme.onSurface
                    )
                }

                // 双周
                val doubleSelected =
                    tempSelectedWeeks.isNotEmpty() && tempSelectedWeeks.all { it % 2 == 0 }
                Button(
                    onClick = {
                        tempSelectedWeeks = (1..totalWeeks).filter { it % 2 == 0 }.toSet()
                    },
                    modifier = Modifier.then(
                        if (!doubleSelected) Modifier.border(
                            1.dp,
                            MiuixTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                            RoundedCornerShape(16.dp)
                        )
                        else Modifier
                    ),
                    colors = if (doubleSelected) ButtonDefaults.buttonColors(color = MiuixTheme.colorScheme.primary)
                    else ButtonDefaults.buttonColors(color = Color.Transparent),
                    insideMargin = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        actionDoubleWeek,
                        fontSize = 14.sp,
                        color = if (doubleSelected) MiuixTheme.colorScheme.onPrimary else MiuixTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun ColorPickerBottomSheet(
    show: Boolean = true,
    colorMaps: List<DualColor>,
    colorSchemeMode: top.yukonga.miuix.kmp.theme.ColorSchemeMode,
    selectedIndex: Int,
    onDismissRequest: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var tempSelectedIndex by remember(selectedIndex, show) { mutableIntStateOf(selectedIndex) }
    val isDark = when (colorSchemeMode) {
        top.yukonga.miuix.kmp.theme.ColorSchemeMode.Light, top.yukonga.miuix.kmp.theme.ColorSchemeMode.MonetLight -> false
        top.yukonga.miuix.kmp.theme.ColorSchemeMode.Dark, top.yukonga.miuix.kmp.theme.ColorSchemeMode.MonetDark -> true
        else -> isSystemInDarkTheme()
    }

    WindowBottomSheet(
        show = show,
        title = stringResource(R.string.title_select_color),
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
                onClick = { onConfirm(tempSelectedIndex); onDismissRequest() },
                colors = ButtonDefaults.textButtonColorsPrimary()
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(colorMaps) { index, dualColor ->
                    val color = if (isDark) dualColor.dark else dualColor.light
                    val isSelected = tempSelectedIndex == index

                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .clickable { tempSelectedIndex = index }
                            .then(
                                if (isSelected) Modifier.border(
                                    3.dp,
                                    MiuixTheme.colorScheme.primary,
                                    CircleShape
                                )
                                else Modifier
                            )
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(color),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Done,
                                contentDescription = null,
                                tint = if (color.luminance() > 0.5f) Color.Black else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
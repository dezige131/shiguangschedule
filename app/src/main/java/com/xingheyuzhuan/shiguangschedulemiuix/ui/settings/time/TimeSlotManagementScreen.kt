package com.xingheyuzhuan.shiguangschedulemiuix.ui.settings.time

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.xingheyuzhuan.shiguangschedulemiuix.R
import com.xingheyuzhuan.shiguangschedulemiuix.data.db.main.TimeSlot
import com.xingheyuzhuan.shiguangschedulemiuix.ui.components.CustomTimeRangePickerBottomSheet
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@Composable
fun TimeSlotManagementScreen(
    onBackClick: () -> Unit,
    timeSlotViewModel: TimeSlotViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val uiState by timeSlotViewModel.timeSlotsUiState.collectAsState()

    val localTimeSlots = remember {
        mutableStateListOf<TimeSlot>().apply { addAll(uiState.timeSlots.sortedBy { it.number }) }
    }
    var localDefaultClassDuration by remember { mutableStateOf(uiState.defaultClassDuration) }
    var localDefaultBreakDuration by remember { mutableStateOf(uiState.defaultBreakDuration) }

    var showExitConfirmDialog by remember { mutableStateOf(false) }

    val titleTimeSlotManagement = stringResource(R.string.title_time_slot_management)
    val a11yBack = stringResource(R.string.a11y_back)
    val a11yAddTimeSlot = stringResource(R.string.a11y_add_time_slot)
    val a11ySaveAllSettings = stringResource(R.string.a11y_save_all_settings)
    val toastSettingsSaved = stringResource(R.string.toast_settings_saved)
    val toastSlotRemovedUnsaved = stringResource(R.string.toast_slot_removed_unsaved)
    val textNoTimeSlotsHint = stringResource(R.string.text_no_time_slots_hint)
    val toastSlotModifiedUnsaved = stringResource(R.string.toast_slot_modified_unsaved)
    val toastSlotAddedUnsaved = stringResource(R.string.toast_slot_added_unsaved)

    // 数据加载同步逻辑
    LaunchedEffect(uiState) {
        if (uiState.isDataLoaded) {
            localTimeSlots.clear()
            localTimeSlots.addAll(uiState.timeSlots.sortedBy { it.number })
            localDefaultClassDuration = uiState.defaultClassDuration
            localDefaultBreakDuration = uiState.defaultBreakDuration
        }
    }

    /**
     * 核心拦截逻辑：判断是否有变更，决定直接返回还是弹窗
     */
    val handleBackPress = {
        val hasChanged = timeSlotViewModel.hasUnsavedChanges(
            currentTimeSlots = localTimeSlots.toList(),
            currentClassDuration = localDefaultClassDuration,
            currentBreakDuration = localDefaultBreakDuration
        )
        if (hasChanged) {
            showExitConfirmDialog = true
        } else {
            onBackClick()
        }
    }

    BackHandler {
        handleBackPress()
    }

    var showTimePicker by remember { mutableStateOf(false) }
    var editingTimeSlot by remember { mutableStateOf<TimeSlot?>(null) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    val scrollBehavior = MiuixScrollBehavior()
    Scaffold(
        containerColor = MiuixTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = titleTimeSlotManagement,
                largeTitle = titleTimeSlotManagement,
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = handleBackPress) {
                        Icon(
                            MiuixIcons.Back,
                            contentDescription = a11yBack,
                            tint = MiuixTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        editingTimeSlot = null
                        editingIndex = null
                        showTimePicker = true
                    }) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = a11yAddTimeSlot,
                            tint = MiuixTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(onClick = {
                        coroutineScope.launch {
                            val sortedAndNumberedSlots = localTimeSlots
                                .sortedBy {
                                    it.startTime.let { timeStr ->
                                        try {
                                            LocalTime.parse(timeStr)
                                        } catch (e: DateTimeParseException) {
                                            LocalTime.MAX
                                        }
                                    }
                                }
                                .mapIndexed { index, slot -> slot.copy(number = index + 1) }

                            timeSlotViewModel.onSaveAllSettings(
                                timeSlots = sortedAndNumberedSlots,
                                classDuration = localDefaultClassDuration,
                                breakDuration = localDefaultBreakDuration,
                                onSuccess = {
                                    Toast.makeText(context, toastSettingsSaved, Toast.LENGTH_SHORT)
                                        .show()
                                }
                            )
                        }
                    }) {
                        Icon(
                            Icons.Filled.Save,
                            contentDescription = a11ySaveAllSettings,
                            tint = MiuixTheme.colorScheme.onBackground
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                DefaultDurationSettings(
                    defaultClassDuration = localDefaultClassDuration,
                    onClassDurationChange = { newValue -> localDefaultClassDuration = newValue },
                    defaultBreakDuration = localDefaultBreakDuration,
                    onBreakDurationChange = { newValue -> localDefaultBreakDuration = newValue }
                )
            }

            item {
                Text(
                    text = stringResource(R.string.title_time_slot_list),
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 32.dp, top = 24.dp, bottom = 8.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer),
                    cornerRadius = 16.dp,
                    insideMargin = PaddingValues(0.dp)
                ) {
                    if (localTimeSlots.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                textNoTimeSlotsHint,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        Column {
                            localTimeSlots.forEachIndexed { index, timeSlot ->
                                TimeSlotItem(
                                    timeSlot = timeSlot,
                                    onEditClick = {
                                        editingTimeSlot = timeSlot
                                        editingIndex = index
                                        showTimePicker = true
                                    },
                                    onDeleteClick = {
                                        localTimeSlots.removeAt(index)
                                        val renumberedList = localTimeSlots
                                            .sortedBy {
                                                it.startTime.let { timeStr ->
                                                    try {
                                                        LocalTime.parse(timeStr)
                                                    } catch (e: DateTimeParseException) {
                                                        LocalTime.MAX
                                                    }
                                                }
                                            }
                                            .mapIndexed { i, slot -> slot.copy(number = i + 1) }
                                        localTimeSlots.clear()
                                        localTimeSlots.addAll(renumberedList)
                                        Toast.makeText(
                                            context,
                                            toastSlotRemovedUnsaved,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                )
                                if (index < localTimeSlots.lastIndex) {
                                    CustomDivider()
                                }
                            }
                        }
                    }
                }
            }
        }

        // ================= 使用导入的 CustomTimeRangePickerBottomSheet =================
        if (showTimePicker) {
            val isEditing = editingTimeSlot != null
            val (initialStart, initialEnd) = calculateInitialTimes(
                isEditing,
                editingTimeSlot,
                localTimeSlots,
                localDefaultBreakDuration,
                localDefaultClassDuration
            )

            CustomTimeRangePickerBottomSheet(
                show = showTimePicker,
                initialStartTime = initialStart,
                initialEndTime = initialEnd,
                onDismissRequest = { showTimePicker = false },
                onTimeRangeSelected = { startTime, endTime ->
                    val number =
                        editingTimeSlot?.number ?: (localTimeSlots.maxOfOrNull { it.number }
                            ?.plus(1) ?: 1)
                    val newOrUpdatedSlot = TimeSlot(number, startTime, endTime, courseTableId = "")

                    if (isEditing && editingIndex != null) {
                        localTimeSlots[editingIndex!!] = newOrUpdatedSlot
                        Toast.makeText(context, toastSlotModifiedUnsaved, Toast.LENGTH_SHORT).show()
                    } else {
                        localTimeSlots.add(newOrUpdatedSlot)
                        Toast.makeText(context, toastSlotAddedUnsaved, Toast.LENGTH_SHORT).show()
                    }

                    // 重新排序并分配编号
                    val finalSorted = localTimeSlots.sortedBy {
                        it.startTime.let { timeStr ->
                            try {
                                LocalTime.parse(timeStr)
                            } catch (e: Exception) {
                                LocalTime.MAX
                            }
                        }
                    }.mapIndexed { i, slot -> slot.copy(number = i + 1) }
                    localTimeSlots.clear()
                    localTimeSlots.addAll(finalSorted)
                }
            )
        }

        // ================= 退出未保存警告弹窗 =================
        WindowDialog(
            show = showExitConfirmDialog,
            title = stringResource(R.string.common_dialog_title_abandon_changes),
            summary = stringResource(R.string.common_dialog_msg_unsaved_changes),
            onDismissRequest = { showExitConfirmDialog = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = { showExitConfirmDialog = false },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors()
                    ) {
                        Text(
                            stringResource(R.string.common_action_continue_editing),
                            color = MiuixTheme.colorScheme.onSurface
                        )
                    }
                    Button(
                        onClick = {
                            showExitConfirmDialog = false
                            onBackClick()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(color = MiuixTheme.colorScheme.error)
                    ) {
                        Text(
                            stringResource(R.string.common_action_exit_without_save),
                            color = MiuixTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

/**
 * 提取时间计算逻辑
 */
private fun calculateInitialTimes(
    isEditing: Boolean,
    editingTimeSlot: TimeSlot?,
    localTimeSlots: List<TimeSlot>,
    breakDur: Int,
    classDur: Int
): Pair<String, String> {
    if (isEditing && editingTimeSlot != null) return Pair(
        editingTimeSlot.startTime,
        editingTimeSlot.endTime
    )

    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    return if (localTimeSlots.isNotEmpty()) {
        val lastEndTime = localTimeSlots.maxOf { it.endTime }.let {
            try {
                LocalTime.parse(it, formatter)
            } catch (e: Exception) {
                LocalTime.of(8, 0)
            }
        }
        val start = lastEndTime.plusMinutes(if (breakDur >= 0) breakDur.toLong() else 0L)
        val end = start.plusMinutes(classDur.toLong())
        Pair(start.format(formatter), end.format(formatter))
    } else {
        val start = LocalTime.of(8, 0)
        Pair(start.format(formatter), start.plusMinutes(classDur.toLong()).format(formatter))
    }
}

// ================== 子组件区域 ==================

@Composable
fun DefaultDurationSettings(
    defaultClassDuration: Int,
    onClassDurationChange: (Int) -> Unit,
    defaultBreakDuration: Int,
    onBreakDurationChange: (Int) -> Unit
) {
    val context = LocalContext.current
    val labelClassDuration = stringResource(R.string.label_class_duration_minutes)
    val toastClassDurationPositive = stringResource(R.string.toast_class_duration_positive)
    val labelBreakDuration = stringResource(R.string.label_break_duration_minutes)
    val toastBreakDurationNonNegative = stringResource(R.string.toast_break_duration_non_negative)

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.title_default_duration_settings),
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            fontSize = 13.sp,
            modifier = Modifier.padding(start = 32.dp, bottom = 8.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer),
            cornerRadius = 16.dp,
            insideMargin = PaddingValues(0.dp)
        ) {
            Column {
                // 上课时长
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        labelClassDuration,
                        fontSize = 16.sp,
                        color = MiuixTheme.colorScheme.onSurface
                    )
                    TextField(
                        value = if (defaultClassDuration == 0) "" else defaultClassDuration.toString(),
                        onValueChange = { newValueStr ->
                            val newIntValue = newValueStr.toIntOrNull()
                            if (newValueStr.isEmpty()) {
                                onClassDurationChange(0)
                            } else if (newIntValue != null && newIntValue > 0) {
                                onClassDurationChange(newIntValue)
                            } else if (newIntValue != null) {
                                Toast.makeText(
                                    context,
                                    toastClassDurationPositive,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier.width(80.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }

                CustomDivider()

                // 课间时长
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        labelBreakDuration,
                        fontSize = 16.sp,
                        color = MiuixTheme.colorScheme.onSurface
                    )
                    TextField(
                        value = if (defaultBreakDuration == -1) "" else defaultBreakDuration.toString(),
                        onValueChange = { newValueStr ->
                            val newIntValue = newValueStr.toIntOrNull()
                            if (newValueStr.isEmpty()) {
                                onBreakDurationChange(-1)
                            } else if (newIntValue != null && newIntValue >= 0) {
                                onBreakDurationChange(newIntValue)
                            } else if (newIntValue != null) {
                                Toast.makeText(
                                    context,
                                    toastBreakDurationNonNegative,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier.width(80.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            }
        }
    }
}

@Composable
fun TimeSlotItem(
    timeSlot: TimeSlot,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val timeSlotSectionNumber = stringResource(R.string.time_slot_section_number)
    val a11yDeleteTimeSlot = stringResource(R.string.a11y_delete_time_slot)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEditClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .background(
                        MiuixTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = top.yukonga.miuix.kmp.theme.miuixCapsuleShape()
                    )
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = timeSlotSectionNumber.format(timeSlot.number),
                    fontSize = 14.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = MiuixTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "${timeSlot.startTime} - ${timeSlot.endTime}",
                fontSize = 16.sp,
                color = MiuixTheme.colorScheme.onSurface
            )
        }
        top.yukonga.miuix.kmp.basic.IconButton(onClick = onDeleteClick) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = a11yDeleteTimeSlot,
                tint = MiuixTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun CustomDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(horizontal = 16.dp)
            .background(MiuixTheme.colorScheme.surfaceVariant)
    )
}
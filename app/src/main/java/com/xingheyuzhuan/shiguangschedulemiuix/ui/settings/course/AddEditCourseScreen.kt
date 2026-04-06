package com.xingheyuzhuan.shiguangschedulemiuix.ui.settings.course

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xingheyuzhuan.shiguangschedulemiuix.R
import com.xingheyuzhuan.shiguangschedulemiuix.ui.components.CustomTimeRangePickerBottomSheet
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

@Composable
fun AddEditCourseScreen(
    courseId: String?,
    onNavigateBack: () -> Unit,
    viewModel: AddEditCourseViewModel = hiltViewModel()
) {
    LaunchedEffect(courseId) {
        viewModel.loadCourse(courseId)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 🌟 修复点 1：带入 rememberTopAppBarState 避免滚动状态重置
    val scrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())

    // 状态追踪：记录当前正在操作哪一个方案
    var activeSchemeId by remember { mutableStateOf<String?>(null) }

    // 弹窗控制状态
    var showWeekSelectorDialog by remember { mutableStateOf(false) }
    var showColorSelectorDialog by remember { mutableStateOf(false) }
    var showTimePickerSelector by remember { mutableStateOf(false) }
    var showDayPickerDialog by remember { mutableStateOf(false) }

    // 拦截退出弹窗状态
    var showExitConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // 提示文本资源
    val saveSuccessText = stringResource(R.string.toast_save_success)
    val deleteSuccessText = stringResource(R.string.toast_delete_success)
    val nameEmptyText = stringResource(R.string.toast_name_empty)
    val toastTimeInvalid = stringResource(R.string.toast_time_invalid)

    // 🌟 修复点 2：建立统一的安全退出函数，在执行 Navigation pop 前彻底洗净 ViewModel 的脏数据
    val exitAndCleanup = {
        viewModel.cleanUp()
        onNavigateBack()
    }

    // 统一的退出拦截逻辑
    val handleBackPress = {
        if (viewModel.hasUnsavedChanges()) {
            showExitConfirmDialog = true
        } else {
            exitAndCleanup() // 👈 使用安全退出
        }
    }

    // 拦截物理返回键
    BackHandler {
        handleBackPress()
    }

    // 处理 ViewModel 事件
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                UiEvent.SaveSuccess -> {
                    Toast.makeText(context, saveSuccessText, Toast.LENGTH_SHORT).show()
                    exitAndCleanup() // 👈 使用安全退出
                }

                UiEvent.DeleteSuccess -> {
                    Toast.makeText(context, deleteSuccessText, Toast.LENGTH_SHORT).show()
                    exitAndCleanup() // 👈 使用安全退出
                }

                UiEvent.Cancel -> exitAndCleanup() // 👈 使用安全退出
            }
        }
    }

    val titleText =
        if (uiState.isEditing) stringResource(R.string.title_edit_course) else stringResource(R.string.title_add_course)

    Scaffold(
        containerColor = MiuixTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = titleText,
                largeTitle = titleText,
                navigationIcon = {
                    IconButton(onClick = handleBackPress) {
                        Icon(
                            MiuixIcons.Back,
                            contentDescription = stringResource(R.string.a11y_back),
                            tint = MiuixTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    if (uiState.isEditing) {
                        IconButton(onClick = { showDeleteConfirmDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.a11y_delete),
                                tint = MiuixTheme.colorScheme.error
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            if (uiState.name.isBlank()) {
                                Toast.makeText(context, nameEmptyText, Toast.LENGTH_SHORT).show()
                            } else {
                                val allValid = uiState.schemes.all { s ->
                                    if (s.isCustomTime) {
                                        s.customStartTime.isNotBlank() && s.customEndTime.isNotBlank() && s.customStartTime < s.customEndTime
                                    } else {
                                        s.startSection <= s.endSection
                                    }
                                }
                                if (allValid) viewModel.onSave()
                                else Toast.makeText(context, toastTimeInvalid, Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.Done,
                            contentDescription = stringResource(R.string.a11y_save),
                            tint = MiuixTheme.colorScheme.primary
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            // 🌟 修复点 3：不强制切断边界，而是交给 ContentPadding 处理边缘边距，确保 Miuix 的大标题自然滚出
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() + 24.dp
            )
        ) {
            // 课程名称输入
            item {
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = uiState.name,
                    onValueChange = viewModel::onNameChange,
                    label = stringResource(R.string.label_course_name),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MiuixTheme.colorScheme.surfaceVariant)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 方案卡片列表
            items(uiState.schemes, key = { it.id }) { scheme ->
                CourseSchemeCard(
                    scheme = scheme,
                    courseColorMaps = uiState.courseColorMaps,
                    colorSchemeMode = uiState.colorSchemeMode,
                    onTeacherChange = { newTeacher ->
                        viewModel.updateScheme(scheme.id) { it.copy(teacher = newTeacher) }
                    },
                    onPositionChange = { newPos ->
                        viewModel.updateScheme(scheme.id) { it.copy(position = newPos) }
                    },
                    onColorClick = {
                        activeSchemeId = scheme.id
                        showColorSelectorDialog = true
                    },
                    onTimeClick = {
                        activeSchemeId = scheme.id
                        showTimePickerSelector = true
                    },
                    onWeeksClick = {
                        activeSchemeId = scheme.id
                        showWeekSelectorDialog = true
                    },
                    onDayClick = {
                        activeSchemeId = scheme.id
                        showDayPickerDialog = true
                    },
                    onRemoveClick = { viewModel.removeScheme(scheme.id) },
                    onToggleCustomTime = { isCustom ->
                        viewModel.toggleCustomTime(scheme.id, isCustom)
                    },
                    showRemoveButton = uiState.schemes.size > 1
                )
            }

            // 添加方案按钮
            item {
                Button(
                    onClick = viewModel::addScheme,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    // 🌟 修复点 4：使用 Miuix 规范的 ButtonDefaults
                    colors = ButtonDefaults.buttonColorsPrimary()
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.action_add),
                        fontSize = 16.sp,
                        color = MiuixTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }

    val activeScheme =
        uiState.schemes.find { it.id == activeSchemeId } ?: uiState.schemes.firstOrNull()

    if (activeScheme != null) {
        WeekSelectorBottomSheet(
            show = showWeekSelectorDialog,
            totalWeeks = uiState.semesterTotalWeeks,
            selectedWeeks = activeScheme.weeks,
            onDismissRequest = { showWeekSelectorDialog = false },
            onConfirm = { weeks ->
                viewModel.updateScheme(activeScheme.id) { it.copy(weeks = weeks) }
                showWeekSelectorDialog = false
            }
        )

        ColorPickerBottomSheet(
            show = showColorSelectorDialog,
            colorMaps = uiState.courseColorMaps,
            colorSchemeMode = uiState.colorSchemeMode,
            selectedIndex = activeScheme.colorIndex,
            onDismissRequest = { showColorSelectorDialog = false },
            onConfirm = { index ->
                viewModel.updateScheme(activeScheme.id) { it.copy(colorIndex = index) }
                showColorSelectorDialog = false
            }
        )

        if (activeScheme.isCustomTime) {
            CustomTimeRangePickerBottomSheet(
                show = showTimePickerSelector,
                initialStartTime = activeScheme.customStartTime.ifBlank { "08:00" },
                initialEndTime = activeScheme.customEndTime.ifBlank { "09:45" },
                onDismissRequest = { showTimePickerSelector = false },
                onTimeRangeSelected = { start, end ->
                    viewModel.updateScheme(activeScheme.id) {
                        it.copy(
                            customStartTime = start,
                            customEndTime = end
                        )
                    }
                    showTimePickerSelector = false
                }
            )
        } else {
            CourseTimePickerBottomSheet(
                show = showTimePickerSelector,
                selectedDay = activeScheme.day,
                onDaySelected = { d -> viewModel.updateScheme(activeScheme.id) { it.copy(day = d) } },
                startSection = activeScheme.startSection,
                onStartSectionChange = { s ->
                    viewModel.updateScheme(activeScheme.id) {
                        it.copy(
                            startSection = s
                        )
                    }
                },
                endSection = activeScheme.endSection,
                onEndSectionChange = { e ->
                    viewModel.updateScheme(activeScheme.id) {
                        it.copy(
                            endSection = e
                        )
                    }
                },
                timeSlots = uiState.timeSlots,
                onDismissRequest = { showTimePickerSelector = false }
            )
        }

        DayPickerDialog(
            show = showDayPickerDialog,
            selectedDay = activeScheme.day,
            onDismissRequest = { showDayPickerDialog = false },
            onDaySelected = { newDay ->
                viewModel.updateScheme(activeScheme.id) { it.copy(day = newDay) }
                showDayPickerDialog = false
            }
        )
    }

    // 退出确认弹窗
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
                    // 🌟 修复点 5：即使是不保存直接退出，也要执行干净的清除！
                    onClick = { showExitConfirmDialog = false; exitAndCleanup() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(color = MiuixTheme.colorScheme.error)
                ) {
                    Text(
                        stringResource(R.string.common_action_exit_without_save),
                        color = Color.White
                    )
                }
            }
        }
    }

    // 删除确认弹窗
    WindowDialog(
        show = showDeleteConfirmDialog,
        title = stringResource(R.string.dialog_title_delete_course),
        summary = stringResource(R.string.dialog_msg_delete_course_confirm),
        onDismissRequest = { showDeleteConfirmDialog = false }
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
                    onClick = { showDeleteConfirmDialog = false },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors()
                ) {
                    Text(
                        stringResource(R.string.action_cancel),
                        color = MiuixTheme.colorScheme.onSurface
                    )
                }
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.onDelete()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(color = MiuixTheme.colorScheme.error)
                ) {
                    Text(
                        stringResource(R.string.action_confirm),
                        color = Color.White
                    )
                }
            }
        }
    }
}
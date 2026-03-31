package com.xingheyuzhuan.shiguangschedulemiuix.ui.settings.notification

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.xingheyuzhuan.shiguangschedulemiuix.R
import com.xingheyuzhuan.shiguangschedulemiuix.data.model.AutoControlMode
import com.xingheyuzhuan.shiguangschedulemiuix.service.CourseNotificationWorker
import com.xingheyuzhuan.shiguangschedulemiuix.service.DndSchedulerWorker
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.NumberPicker
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.preference.WindowDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

@Composable
fun NotificationSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: NotificationSettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val notificationLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) Toast.makeText(
                context,
                context.getString(R.string.toast_notification_permission_denied),
                Toast.LENGTH_LONG
            ).show()
        }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission(
                context
            )
        ) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.updateExactAlarmStatus(hasExactAlarmPermission(context))
                viewModel.updateDndPermissionStatus(hasDndPermission(context))
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val scrollBehavior = MiuixScrollBehavior()
    val titleText = stringResource(R.string.title_course_notification_settings)

    Scaffold(
        containerColor = MiuixTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = titleText,
                largeTitle = titleText,
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(MiuixIcons.Back, null, tint = MiuixTheme.colorScheme.onBackground)
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
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                GeneralSettingsSection(
                    uiState = uiState,
                    onReminderToggle = {
                        viewModel.onReminderEnabledChange(
                            it,
                            { triggerNotificationWorker(context) },
                            context
                        )
                    },
                    onCompatWearableToggle = {
                        viewModel.onCompatWearableSyncChange(
                            it,
                            { triggerNotificationWorker(context) },
                            context
                        )
                    },
                    onAutoModeChange = { isEnabled, mode ->
                        // 将状态变更直接抛给 ViewModel 处理
                        viewModel.onAutoModeStateChange(
                            isEnabled,
                            mode,
                            { triggerDndSchedulerWorker(context) },
                            context
                        )
                    },
                    onRemindTimeClick = { viewModel.showDialog(NotificationDialogType.EditRemindMinutes) },
                    onExactAlarmClick = { openExactAlarmSettings(context) },
                    onDndPermissionClick = { openDndSettings(context) },
                    onAppSettingsClick = { openAppSettings(context) },
                    onBatteryOptimizationClick = { openIgnoreBatteryOptimizationSettings(context) }
                )
            }
            item {
                AdvancedSettingsSection(
                    uiState = uiState,
                    onUpdateHolidays = {
                        viewModel.onUpdateHolidays(
                            onSuccess = {
                                Toast.makeText(
                                    context,
                                    R.string.toast_holidays_update_success,
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            onFailure = { _, msg ->
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.toast_update_failed, msg),
                                    Toast.LENGTH_LONG
                                ).show()
                            },
                            context = context
                        )
                    },
                    onClearSkippedDates = { viewModel.showDialog(NotificationDialogType.ClearConfirmation) },
                    onViewSkippedDates = { viewModel.showDialog(NotificationDialogType.ViewSkippedDates) }
                )
            }
        }
    }

    // ================== 所有弹窗调度 ==================

    val activeDialog = uiState.activeDialog

    EditRemindMinutesDialog(
        show = activeDialog is NotificationDialogType.EditRemindMinutes,
        currentMinutes = uiState.remindBeforeMinutes,
        onConfirm = { mins ->
            viewModel.onSaveRemindBeforeMinutes(
                mins,
                { triggerNotificationWorker(context) },
                context
            )
            // 注意：这里 ViewModel 会自行 dismissDialog，不需要再手动关一次
        },
        onDismiss = { viewModel.dismissDialog() }
    )

    WindowDialog(
        show = activeDialog is NotificationDialogType.ClearConfirmation,
        title = stringResource(R.string.dialog_title_clear_confirmation),
        summary = stringResource(R.string.dialog_text_clear_confirmation),
        onDismissRequest = { viewModel.dismissDialog() }
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
                    onClick = { viewModel.dismissDialog() },
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
                        viewModel.onClearSkippedDates(
                            onSuccess = {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.toast_clear_success),
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            onFailure = { _, msg ->
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.toast_clear_failed, msg),
                                    Toast.LENGTH_LONG
                                ).show()
                            },
                            context = context
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(color = MiuixTheme.colorScheme.error)
                ) {
                    Text(
                        stringResource(R.string.action_confirm),
                        color = MiuixTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }

    ViewSkippedDatesDialog(
        show = activeDialog is NotificationDialogType.ViewSkippedDates,
        dates = uiState.skippedDates,
        onDismiss = { viewModel.dismissDialog() }
    )

    PermissionGuideDialog(
        show = activeDialog is NotificationDialogType.ExactAlarmPermission,
        title = stringResource(R.string.dialog_title_exact_alarm_permission),
        text = stringResource(R.string.dialog_text_exact_alarm_permission),
        onConfirm = { openExactAlarmSettings(context) },
        onDismiss = { viewModel.dismissDialog() }
    )

    PermissionGuideDialog(
        show = activeDialog is NotificationDialogType.DndPermission,
        title = stringResource(R.string.dialog_title_dnd_permission),
        text = stringResource(R.string.dialog_text_dnd_permission),
        onConfirm = { openDndSettings(context) },
        onDismiss = { viewModel.dismissDialog() }
    )
}

// ================== 设置卡片 UI 部分 ==================

@Composable
private fun GeneralSettingsSection(
    uiState: NotificationSettingsUiState,
    onReminderToggle: (Boolean) -> Unit,
    onCompatWearableToggle: (Boolean) -> Unit,
    onAutoModeChange: (Boolean, AutoControlMode) -> Unit,
    onRemindTimeClick: () -> Unit,
    onExactAlarmClick: () -> Unit,
    onDndPermissionClick: () -> Unit,
    onAppSettingsClick: () -> Unit,
    onBatteryOptimizationClick: () -> Unit
) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.section_title_general),
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
                Text(
                    text = stringResource(R.string.text_permission_importance_title),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MiuixTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)
                )
                Text(
                    text = stringResource(R.string.text_permission_importance_detail),
                    fontSize = 13.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 4.dp,
                        bottom = 12.dp
                    )
                )

                // 1. 上课提醒主开关
                SwitchPreference(
                    title = stringResource(R.string.item_course_reminder),
                    checked = uiState.reminderEnabled,
                    onCheckedChange = onReminderToggle
                )

                // 2. 兼容穿戴设备同步通知开关
                SwitchPreference(
                    title = stringResource(R.string.item_compat_wearable_sync),
                    summary = stringResource(R.string.desc_compat_wearable_sync),
                    checked = uiState.compatWearableSync,
                    onCheckedChange = onCompatWearableToggle
                )

                // 3. 上课自动模式 (重构为 Miuix WindowDropdownPreference)
                val modeOptions = listOf(
                    stringResource(R.string.auto_mode_off),
                    stringResource(R.string.auto_mode_dnd),
                    stringResource(R.string.auto_mode_silent)
                )
                val selectedModeIndex = if (!uiState.autoModeEnabled) 0 else {
                    when (uiState.autoControlMode) {
                        AutoControlMode.DND -> 1
                        AutoControlMode.SILENT -> 2
                    }
                }

                WindowDropdownPreference(
                    title = stringResource(R.string.item_auto_mode),
                    items = modeOptions,
                    selectedIndex = selectedModeIndex,
                    onSelectedIndexChange = { index ->
                        if (!uiState.reminderEnabled) {
                            Toast.makeText(
                                context,
                                R.string.toast_enable_reminder_first,
                                Toast.LENGTH_SHORT
                            ).show()
                            return@WindowDropdownPreference
                        }
                        if (index == 0) {
                            onAutoModeChange(false, uiState.autoControlMode)
                        } else {
                            val newMode =
                                if (index == 1) AutoControlMode.DND else AutoControlMode.SILENT
                            onAutoModeChange(true, newMode)
                        }
                    }
                )
                if (!uiState.reminderEnabled) {
                    Text(
                        text = stringResource(R.string.text_auto_mode_dependency),
                        fontSize = 12.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                    )
                }

                // 4. 提前提醒时间
                ArrowPreference(
                    title = stringResource(R.string.item_remind_time_before),
                    summary = stringResource(
                        R.string.remind_time_minutes_format,
                        uiState.remindBeforeMinutes
                    ),
                    onClick = onRemindTimeClick
                )

                // 5. 精确闹钟权限 (Android 12+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val statusText =
                        if (uiState.exactAlarmStatus) stringResource(R.string.status_enabled) else stringResource(
                            R.string.status_disabled
                        )
                    ArrowPreference(
                        title = stringResource(R.string.item_exact_alarm_permission),
                        summary = statusText,
                        onClick = onExactAlarmClick
                    )
                }

                // 6. 勿扰模式权限
                val dndStatusText =
                    if (uiState.dndPermissionStatus) stringResource(R.string.status_authorized) else stringResource(
                        R.string.status_unauthorized
                    )
                ArrowPreference(
                    title = stringResource(R.string.item_dnd_permission),
                    summary = dndStatusText,
                    onClick = onDndPermissionClick
                )

                // 7. 后台与自启动
                ArrowPreference(
                    title = stringResource(R.string.item_background_and_autostart),
                    onClick = onAppSettingsClick
                )

                // 8. 忽略电池优化
                ArrowPreference(
                    title = stringResource(R.string.item_ignore_battery_optimization),
                    onClick = onBatteryOptimizationClick
                )
            }
        }
    }
}

@Composable
private fun AdvancedSettingsSection(
    uiState: NotificationSettingsUiState,
    onUpdateHolidays: () -> Unit,
    onClearSkippedDates: () -> Unit,
    onViewSkippedDates: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.section_title_advanced),
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
                Text(
                    text = stringResource(R.string.section_title_skip_dates),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MiuixTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)
                )
                Text(
                    text = stringResource(R.string.text_skip_dates_experimental),
                    fontSize = 13.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 4.dp,
                        bottom = 12.dp
                    )
                )

                ArrowPreference(
                    title = stringResource(R.string.item_update_holiday_info),
                    onClick = onUpdateHolidays,
                    endActions = {
                        if (uiState.isLoading) {
                            InfiniteProgressIndicator(
                                color = MiuixTheme.colorScheme.onSurface,
                                size = 20.dp,
                                strokeWidth = 2.dp
                            )
                        }
                    }
                )
                Text(
                    text = stringResource(R.string.update_holiday_info_hint),
                    fontSize = 12.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )

                ArrowPreference(
                    title = stringResource(R.string.item_clear_skipped_dates),
                    onClick = onClearSkippedDates
                )

                ArrowPreference(
                    title = stringResource(R.string.item_view_skipped_dates),
                    summary = if (uiState.skippedDates.isNotEmpty()) stringResource(
                        R.string.skipped_dates_count_format,
                        uiState.skippedDates.size
                    ) else stringResource(R.string.skipped_dates_none),
                    onClick = onViewSkippedDates
                )
            }
        }
    }
}

// ================== Miuix 组件封装 ==================

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

// ================== 所有的弹窗定义 ==================

@Composable
fun EditRemindMinutesDialog(
    show: Boolean,
    currentMinutes: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedValue by remember(currentMinutes, show) { mutableIntStateOf(currentMinutes) }

    WindowDialog(
        show = show,
        title = stringResource(R.string.dialog_title_set_remind_time),
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                NumberPicker(
                    value = selectedValue,
                    onValueChange = { selectedValue = it },
                    range = 0..120,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors()
                ) {
                    Text(
                        stringResource(R.string.action_cancel),
                        color = MiuixTheme.colorScheme.onSurface
                    )
                }
                Button(
                    onClick = { onConfirm(selectedValue) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColorsPrimary()
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

@Composable
fun ViewSkippedDatesDialog(show: Boolean, dates: Set<String>, onDismiss: () -> Unit) {
    WindowDialog(
        show = show,
        title = stringResource(R.string.dialog_title_view_skipped_dates),
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(top = 16.dp)
        ) {
            if (dates.isEmpty()) {
                Text(
                    stringResource(R.string.skipped_dates_none),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    fontSize = 14.sp
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(100.dp),
                    modifier = Modifier
                        .heightIn(max = 300.dp)
                        .fillMaxWidth()
                ) {
                    items(dates.toList().sorted()) { date ->
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MiuixTheme.colorScheme.surfaceVariant)
                                .border(
                                    1.dp,
                                    MiuixTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                    RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                date,
                                modifier = Modifier.padding(8.dp),
                                fontSize = 13.sp,
                                color = MiuixTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColorsPrimary()
            ) {
                Text(
                    stringResource(R.string.action_close),
                    color = MiuixTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
fun PermissionGuideDialog(
    show: Boolean,
    title: String,
    text: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    WindowDialog(
        show = show,
        title = title,
        summary = text,
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
                    .padding(top = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors()
                ) {
                    Text(
                        stringResource(R.string.action_cancel),
                        color = MiuixTheme.colorScheme.onSurface
                    )
                }
                Button(
                    onClick = { onConfirm(); onDismiss() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColorsPrimary()
                ) {
                    Text(
                        stringResource(R.string.action_go_to_settings),
                        color = MiuixTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

// ================== 工作及工具函数 ==================

private fun triggerNotificationWorker(context: Context) {
    val req = OneTimeWorkRequestBuilder<CourseNotificationWorker>().build()
    WorkManager.getInstance(context).enqueueUniqueWork(
        "CourseNotificationWorker_Settings_Update",
        ExistingWorkPolicy.REPLACE,
        req
    )
}

private fun triggerDndSchedulerWorker(context: Context) {
    DndSchedulerWorker.enqueueWork(context)
}

private fun openExactAlarmSettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
        context.startActivity(intent)
    } else {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            "package:${context.packageName}".toUri()
        )
        context.startActivity(intent)
    }
}

private fun hasExactAlarmPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val alarmManager = context.getSystemService<AlarmManager>()
        alarmManager?.canScheduleExactAlarms() ?: false
    } else true
}

private fun hasNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else true
}

private fun hasDndPermission(context: Context): Boolean {
    val notificationManager = context.getSystemService<NotificationManager>()
    return notificationManager?.isNotificationPolicyAccessGranted ?: false
}

private fun openDndSettings(context: Context) {
    val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
    context.startActivity(intent)
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = "package:${context.packageName}".toUri()
    }
    context.startActivity(intent)
}

private fun openIgnoreBatteryOptimizationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = "package:${context.packageName}".toUri()
    }
    context.startActivity(intent)
}
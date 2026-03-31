package com.xingheyuzhuan.shiguangschedulemiuix.ui.settings.conversion

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.xingheyuzhuan.shiguangschedulemiuix.AppRoute
import com.xingheyuzhuan.shiguangschedulemiuix.LocalAppNavigator
import com.xingheyuzhuan.shiguangschedulemiuix.R
import com.xingheyuzhuan.shiguangschedulemiuix.tool.shareFile
import com.xingheyuzhuan.shiguangschedulemiuix.ui.components.CourseTablePickerDialog
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.NumberPicker
import top.yukonga.miuix.kmp.basic.NumberPickerDefaults
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// 自定义文件选择器 Contract，用于导入，只允许选择 JSON 文件
class OpenJsonDocumentContract : ActivityResultContract<Unit, Uri?>() {
    override fun createIntent(context: Context, input: Unit): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return if (resultCode == Activity.RESULT_OK) intent?.data else null
    }
}

// 自定义文件创建器 Contract，用于导出，接受文件名作为输入
class CreateJsonDocumentContract : ActivityResultContract<String, Uri?>() {
    override fun createIntent(context: Context, input: String): Intent {
        return Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, input)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return if (resultCode == Activity.RESULT_OK) intent?.data else null
    }
}

// 自定义 ICS 文件创建器 Contract
class CreateIcsDocumentContract : ActivityResultContract<String, Uri?>() {
    override fun createIntent(context: Context, input: String): Intent {
        return Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/calendar" // ICS 文件的 MIME 类型
            putExtra(Intent.EXTRA_TITLE, input)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return if (resultCode == Activity.RESULT_OK) intent?.data else null
    }
}

// 采用 Miuix NumberPicker 重构的提醒时间选择器
@Composable
fun AlarmMinutesPicker(
    modifier: Modifier = Modifier,
    initialValue: Int? = 15,
    onValueSelected: (Int?) -> Unit,
    itemHeight: Dp = 45.dp
) {
    val noneStr = stringResource(R.string.alarm_option_none)
    val onTimeStr = stringResource(R.string.alarm_option_on_time)

    // 将特殊值映射为连续的 Int： 0 -> None, 1 -> 0(准时), 2~61 -> 1~60分钟
    var pickerValue by remember(initialValue) {
        mutableIntStateOf(if (initialValue == null) 0 else initialValue + 1)
    }

    val pickerColors = NumberPickerDefaults.colors(
        selectedTextColor = MiuixTheme.colorScheme.primary,
        unselectedTextColor = MiuixTheme.colorScheme.onSurface
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        NumberPicker(
            value = pickerValue,
            onValueChange = {
                pickerValue = it
                // 转换回去
                val actualValue = if (it == 0) null else it - 1
                onValueSelected(actualValue)
            },
            range = 0..61,
            label = { index ->
                when (index) {
                    0 -> noneStr
                    1 -> onTimeStr
                    else -> (index - 1).toString()
                }
            },
            itemHeight = itemHeight,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// 采用 Miuix WindowDialog 重构的 ICS 导出对话框
@Composable
fun IcsExportDialog(
    show: Boolean, // <--- 关键参数，控制动画和显示
    onDismissRequest: () -> Unit,
    onConfirm: (String, Int?) -> Unit
) {
    var alarmMinutes by remember { mutableStateOf<Int?>(15) }
    var showTablePicker by remember { mutableStateOf(false) }

    // 当整个弹窗流程重新触发时，重置子步骤状态
    LaunchedEffect(show) {
        if (show) {
            showTablePicker = false
        }
    }

    // 第一步：提醒时间选择 (采用 WindowDialog + 纯正的 Miuix 滚轮样式)
    WindowDialog(
        show = show && !showTablePicker,
        title = stringResource(R.string.dialog_title_ics_export_settings),
        onDismissRequest = onDismissRequest
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.label_select_alarm_time),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
            Spacer(modifier = Modifier.height(16.dp))
            AlarmMinutesPicker(
                modifier = Modifier.fillMaxWidth(), // 占满全宽恢复全屏滑动
                initialValue = alarmMinutes,
                onValueSelected = { minutes -> alarmMinutes = minutes }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = onDismissRequest,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors()
                ) {
                    Text(
                        stringResource(R.string.action_cancel),
                        color = MiuixTheme.colorScheme.onSurface
                    )
                }

                Button(
                    onClick = { showTablePicker = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColorsPrimary()
                ) {
                    Text(
                        stringResource(R.string.action_next_step),
                        color = MiuixTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }

    // 第二步：课表选择 (之前已重构的 WindowBottomSheet)
    CourseTablePickerDialog(
        show = show && showTablePicker,
        title = stringResource(R.string.dialog_title_select_export_table),
        onDismissRequest = onDismissRequest,
        onTableSelected = { selectedTable ->
            onConfirm(selectedTable.id, alarmMinutes)
        }
    )
}

@Composable
fun CourseTableConversionScreen(
    viewModel: CourseTableConversionViewModel = hiltViewModel()
) {
    val navigator = LocalAppNavigator.current
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val msgCannotOpenFile = stringResource(R.string.snackbar_cannot_open_file)
    val msgFileSelectionCanceled = stringResource(R.string.snackbar_file_selection_canceled)
    val msgCannotSaveFile = stringResource(R.string.snackbar_cannot_save_file)
    val msgFileSaveCanceled = stringResource(R.string.snackbar_file_save_canceled)
    val msgFileCopyFailedForShare = stringResource(R.string.snackbar_file_copy_failed_for_share)

    val dialogTitleFileSaved = stringResource(R.string.dialog_title_file_saved)
    val dialogTextFileSavedSharePrompt =
        stringResource(R.string.dialog_text_file_saved_share_prompt)
    val actionShare = stringResource(R.string.action_share)
    val actionCancel = stringResource(R.string.action_cancel)

    var pendingImportTableId by remember { mutableStateOf<String?>(null) }
    var pendingExportJsonContent by remember { mutableStateOf<String?>(null) }
    var pendingExportIcsTableId by remember { mutableStateOf<String?>(null) }
    var pendingAlarmMinutes by remember { mutableStateOf<Int?>(null) }

    var showShareDialog by remember { mutableStateOf<Triple<Uri, String, String>?>(null) }

    val importLauncher =
        rememberLauncherForActivityResult(OpenJsonDocumentContract()) { uri: Uri? ->
            val tableId = pendingImportTableId
            if (uri != null && tableId != null) {
                val inputStream: InputStream? = try {
                    context.contentResolver.openInputStream(uri)
                } catch (e: Exception) {
                    null
                }
                if (inputStream != null) {
                    viewModel.handleFileImport(tableId, inputStream)
                } else {
                    Toast.makeText(context, msgCannotOpenFile, Toast.LENGTH_SHORT).show()
                }
            } else if (uri == null) {
                Toast.makeText(context, msgFileSelectionCanceled, Toast.LENGTH_SHORT).show()
            }
            pendingImportTableId = null
        }

    val exportLauncher =
        rememberLauncherForActivityResult(CreateJsonDocumentContract()) { uri: Uri? ->
            val jsonContent = pendingExportJsonContent
            val filename = "shiguangschedule_${
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            }.json"
            if (uri != null && jsonContent != null) {
                val outputStream: OutputStream? = try {
                    context.contentResolver.openOutputStream(uri)
                } catch (e: Exception) {
                    null
                }
                if (outputStream != null) {
                    outputStream.bufferedWriter().use { writer ->
                        writer.write(jsonContent)
                    }
                    showShareDialog = Triple(uri, "application/json", filename)
                } else {
                    Toast.makeText(context, msgCannotSaveFile, Toast.LENGTH_SHORT).show()
                }
            } else if (uri == null) {
                Toast.makeText(context, msgFileSaveCanceled, Toast.LENGTH_SHORT).show()
            }
            pendingExportJsonContent = null
        }

    val icsExportLauncher =
        rememberLauncherForActivityResult(CreateIcsDocumentContract()) { uri: Uri? ->
            val tableId = pendingExportIcsTableId
            val alarmMinutes = pendingAlarmMinutes
            val filename = "shiguangschedule_${
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            }.ics"
            if (uri != null && tableId != null) {
                val outputStream: OutputStream? = try {
                    context.contentResolver.openOutputStream(uri)
                } catch (e: Exception) {
                    null
                }
                if (outputStream != null) {
                    viewModel.handleIcsExport(tableId, outputStream, alarmMinutes)
                    showShareDialog = Triple(uri, "text/calendar", filename)
                } else {
                    Toast.makeText(context, msgCannotSaveFile, Toast.LENGTH_SHORT).show()
                }
            } else if (uri == null) {
                Toast.makeText(context, msgFileSaveCanceled, Toast.LENGTH_SHORT).show()
            }
            pendingExportIcsTableId = null
            pendingAlarmMinutes = null
        }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ConversionEvent.LaunchImportFilePicker -> {
                    pendingImportTableId = event.tableId
                    importLauncher.launch(Unit)
                }

                is ConversionEvent.LaunchExportFileCreator -> {
                    pendingExportJsonContent = event.jsonContent
                    val now = LocalDateTime.now()
                    val formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                    val timestamp = now.format(formatter)
                    val filename = "shiguangschedule_$timestamp.json"
                    exportLauncher.launch(filename)
                }

                is ConversionEvent.LaunchExportIcsFileCreator -> {
                    pendingExportIcsTableId = event.tableId
                    pendingAlarmMinutes = event.alarmMinutes
                    val now = LocalDateTime.now()
                    val formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                    val timestamp = now.format(formatter)
                    val filename = "shiguangschedule_$timestamp.ics"
                    icsExportLauncher.launch(filename)
                }

                is ConversionEvent.ShowMessage -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val scrollBehavior = MiuixScrollBehavior()
    val titleText = stringResource(R.string.title_conversion)

    Scaffold(
        containerColor = MiuixTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = titleText,
                largeTitle = titleText,
                navigationIcon = {
                    IconButton(onClick = {
                        if (navigator.isNotEmpty()) {
                            navigator.removeAt(navigator.size - 1)
                        }
                    }) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = stringResource(R.string.a11y_back),
                            tint = MiuixTheme.colorScheme.onBackground
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // == 第一个卡片组 ==
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.section_file_conversion),
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 32.dp, bottom = 8.dp, top = 8.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer),
                    cornerRadius = 16.dp
                ) {
                    Column {
                        // 导入选项
                        ArrowPreference(
                            title = stringResource(R.string.item_import_course_file),
                            summary = stringResource(R.string.desc_import_json),
                            onClick = { viewModel.onImportClick() }
                        )

                        // 导出 JSON 选项
                        ArrowPreference(
                            title = stringResource(R.string.item_export_course_file),
                            summary = stringResource(R.string.desc_export_json_with_config),
                            onClick = { viewModel.onExportClick() }
                        )

                        // 导出 ICS 选项
                        ArrowPreference(
                            title = stringResource(R.string.item_export_ics_file),
                            summary = stringResource(R.string.desc_export_ics_with_alarm),
                            onClick = { viewModel.onExportIcsClick() },
                            endActions = {
                                if (uiState.isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.height(20.dp),
                                        strokeWidth = 2.dp,
                                        color = MiuixTheme.colorScheme.primary
                                    )
                                }
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // == 第二个卡片组 ==
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.section_school_import),
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 32.dp, bottom = 8.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer),
                    cornerRadius = 16.dp
                ) {
                    Column {
                        ArrowPreference(
                            title = stringResource(R.string.item_school_system_import),
                            summary = stringResource(R.string.desc_school_import_quick),
                            onClick = { navigator.add(AppRoute.SchoolSelectionList) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    // ============================================
    // 以下是全局弹窗层
    // ============================================

    // 导入课表选择弹窗
    CourseTablePickerDialog(
        show = uiState.showImportTableDialog,
        title = stringResource(R.string.dialog_title_select_import_table),
        onDismissRequest = { viewModel.dismissDialog() },
        onTableSelected = { selectedTable ->
            viewModel.onImportTableSelected(selectedTable.id)
        }
    )

    // JSON 导出课表选择弹窗
    CourseTablePickerDialog(
        show = uiState.showExportTableDialog && uiState.exportType == ExportType.JSON,
        title = stringResource(R.string.dialog_title_select_export_table),
        onDismissRequest = { viewModel.dismissDialog() },
        onTableSelected = { selectedTable ->
            viewModel.onExportTableSelected(selectedTable.id, null)
        }
    )

    // ICS 导出复杂流程弹窗 (提醒时间 -> 课表选择)
    IcsExportDialog(
        show = uiState.showExportTableDialog && uiState.exportType == ExportType.ICS,
        onDismissRequest = { viewModel.dismissDialog() },
        onConfirm = { tableId, alarmMinutes ->
            viewModel.onExportTableSelected(tableId, alarmMinutes)
        }
    )

    // 分享确认弹窗
    WindowDialog(
        show = showShareDialog != null,
        title = dialogTitleFileSaved,
        summary = dialogTextFileSavedSharePrompt,
        onDismissRequest = { showShareDialog = null }
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
                    onClick = { showShareDialog = null },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors()
                ) {
                    Text(actionCancel, color = MiuixTheme.colorScheme.onSurface)
                }

                Button(
                    onClick = {
                        showShareDialog?.let { (publicUri, mimeType, defaultFilename) ->
                            val userDefinedFilename = context.contentResolver.query(
                                publicUri,
                                arrayOf(OpenableColumns.DISPLAY_NAME),
                                null,
                                null,
                                null
                            )?.use { cursor ->
                                if (cursor.moveToFirst()) {
                                    val nameIndex =
                                        cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                                    if (nameIndex >= 0) cursor.getString(nameIndex) else null
                                } else null
                            } ?: defaultFilename

                            val shareTempDir = File(context.cacheDir, "share_temp")
                            if (!shareTempDir.exists()) shareTempDir.mkdirs()

                            val tempFile = File(shareTempDir, userDefinedFilename)

                            try {
                                context.contentResolver.openInputStream(publicUri)?.use { input ->
                                    FileOutputStream(tempFile).use { output ->
                                        input.copyTo(output)
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(
                                    context,
                                    msgFileCopyFailedForShare,
                                    Toast.LENGTH_SHORT
                                ).show()
                                showShareDialog = null
                                return@Button
                            }

                            val shareUri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                tempFile
                            )

                            shareFile(context, shareUri, mimeType)
                            showShareDialog = null
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColorsPrimary()
                ) {
                    Text(actionShare, color = MiuixTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}
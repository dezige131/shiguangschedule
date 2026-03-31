package com.xingheyuzhuan.shiguangschedulemiuix.ui.settings.coursetables

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.xingheyuzhuan.shiguangschedulemiuix.LocalAppNavigator
import com.xingheyuzhuan.shiguangschedulemiuix.R
import com.xingheyuzhuan.shiguangschedulemiuix.data.db.main.CourseTable
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import top.yukonga.miuix.kmp.window.WindowDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ManageCourseTablesScreen(
    viewModel: ManageCourseTablesViewModel = hiltViewModel()
) {
    val navigator = LocalAppNavigator.current
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var showAddTableDialog by remember { mutableStateOf(false) }
    var newTableName by remember { mutableStateOf("") }

    var showEditTableDialog by remember { mutableStateOf(false) }
    var editingTableInfo by remember { mutableStateOf<CourseTable?>(null) }
    var editedTableName by remember { mutableStateOf("") }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var tableToDelete by remember { mutableStateOf<CourseTable?>(null) }

    val titleManageTables = stringResource(R.string.title_manage_course_tables)
    val a11yBack = stringResource(R.string.a11y_back)
    val a11yAddNewTable = stringResource(R.string.a11y_add_new_table)
    val textNoTablesHint = stringResource(R.string.text_no_tables_hint)
    val dialogTitleAddTable = stringResource(R.string.dialog_title_add_table)
    val labelTableName = stringResource(R.string.label_table_name)
    val actionAdd = stringResource(R.string.action_add)
    val actionCancel = stringResource(R.string.action_cancel)
    val toastNameEmpty = stringResource(R.string.toast_name_empty)
    val toastSwitchSuccess = stringResource(R.string.toast_switch_table_success)
    val toastAddSuccess = stringResource(R.string.toast_add_table_success)
    val dialogTitleEditTable = stringResource(R.string.dialog_title_edit_table)
    val a11ySave = stringResource(R.string.a11y_save)
    val toastEditSuccess = stringResource(R.string.toast_edit_table_success)
    val dialogTitleConfirmDelete = stringResource(R.string.confirm_delete)
    val dialogTextConfirmDelete = stringResource(R.string.dialog_text_confirm_delete)
    val actionDelete = stringResource(R.string.a11y_delete)
    val toastDeleteSuccess = stringResource(R.string.toast_delete_table_success)
    val toastDeleteLastFailed = stringResource(R.string.toast_delete_last_table_failed)

    val scrollBehavior = MiuixScrollBehavior()

    // FocusRequesters & Keyboard Controller
    val addFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    Scaffold(
        containerColor = MiuixTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = titleManageTables,
                largeTitle = titleManageTables,
                navigationIcon = {
                    IconButton(onClick = {
                        if (navigator.isNotEmpty()) {
                            navigator.removeAt(navigator.size - 1)
                        }
                    }) {
                        Icon(
                            MiuixIcons.Back,
                            contentDescription = a11yBack,
                            tint = MiuixTheme.colorScheme.onBackground
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddTableDialog = true },
                containerColor = MiuixTheme.colorScheme.primary
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = a11yAddNewTable,
                    tint = MiuixTheme.colorScheme.onPrimary
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (uiState.courseTables.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = textNoTablesHint,
                        fontSize = 16.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    // 底部留出充分的边距，防止悬浮按钮遮挡最后一张卡片
                    contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp),
                ) {
                    items(uiState.courseTables) { tableInfo ->
                        val isSelected = tableInfo.id == uiState.currentActiveTableId
                        CourseTableCard(
                            tableInfo = tableInfo,
                            isSelected = isSelected,
                            onDeleteClick = {
                                tableToDelete = it
                                showDeleteConfirmDialog = true
                            },
                            onEditClick = {
                                editingTableInfo = it
                                editedTableName = it.name
                                showEditTableDialog = true
                            },
                            onCardClick = {
                                viewModel.switchCourseTable(it.id)
                                Toast.makeText(
                                    context,
                                    toastSwitchSuccess.format(it.name),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                }
            }
        }

        // ======================= 全局弹窗层 ======================= //

        // 1. 添加课表弹窗
        WindowDialog(
            show = showAddTableDialog,
            title = dialogTitleAddTable,
            onDismissRequest = { showAddTableDialog = false; newTableName = "" }
        ) {
            // 当弹窗打开时，触发获取焦点及打开键盘操作
            LaunchedEffect(showAddTableDialog) {
                if (showAddTableDialog) {
                    delay(200) // 轻微延迟，等待弹窗展开完成
                    try {
                        addFocusRequester.requestFocus()
                        keyboardController?.show()
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(top = 16.dp)
            ) {
                TextField(
                    value = newTableName,
                    onValueChange = { newTableName = it },
                    label = labelTableName,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(addFocusRequester) // 绑定焦点请求器
                )
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = { showAddTableDialog = false; newTableName = "" },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors()
                    ) {
                        Text(actionCancel, color = MiuixTheme.colorScheme.onSurface)
                    }
                    Button(
                        onClick = {
                            if (newTableName.isNotBlank()) {
                                viewModel.createNewCourseTable(newTableName)
                                Toast.makeText(
                                    context,
                                    toastAddSuccess.format(newTableName),
                                    Toast.LENGTH_SHORT
                                ).show()
                                showAddTableDialog = false
                                newTableName = ""
                            } else {
                                Toast.makeText(context, toastNameEmpty, Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColorsPrimary()
                    ) {
                        Text(actionAdd, color = MiuixTheme.colorScheme.onPrimary)
                    }
                }
            }
        }

        // 2. 编辑课表弹窗
        WindowDialog(
            show = showEditTableDialog && editingTableInfo != null,
            title = dialogTitleEditTable,
            onDismissRequest = {
                showEditTableDialog = false; editingTableInfo = null; editedTableName = ""
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(top = 16.dp)
            ) {
                TextField(
                    value = editedTableName,
                    onValueChange = { editedTableName = it },
                    label = labelTableName,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = {
                            showEditTableDialog = false; editingTableInfo = null; editedTableName =
                            ""
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors()
                    ) {
                        Text(actionCancel, color = MiuixTheme.colorScheme.onSurface)
                    }
                    Button(
                        onClick = {
                            if (editedTableName.isNotBlank()) {
                                editingTableInfo?.let { tableToEdit ->
                                    val updatedTable = tableToEdit.copy(name = editedTableName)
                                    viewModel.updateCourseTable(updatedTable)
                                    Toast.makeText(context, toastEditSuccess, Toast.LENGTH_SHORT)
                                        .show()
                                    showEditTableDialog = false
                                    editingTableInfo = null
                                    editedTableName = ""
                                }
                            } else {
                                Toast.makeText(context, toastNameEmpty, Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColorsPrimary()
                    ) {
                        Text(a11ySave, color = MiuixTheme.colorScheme.onPrimary)
                    }
                }
            }
        }

        // 3. 删除确认弹窗
        WindowDialog(
            show = showDeleteConfirmDialog && tableToDelete != null,
            title = dialogTitleConfirmDelete,
            summary = dialogTextConfirmDelete.format(tableToDelete?.name ?: ""),
            onDismissRequest = { showDeleteConfirmDialog = false; tableToDelete = null }
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
                        onClick = { showDeleteConfirmDialog = false; tableToDelete = null },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors()
                    ) {
                        Text(actionCancel, color = MiuixTheme.colorScheme.onSurface)
                    }
                    Button(
                        onClick = {
                            if (uiState.courseTables.size > 1) {
                                tableToDelete?.let {
                                    viewModel.deleteCourseTable(it)
                                    Toast.makeText(
                                        context,
                                        toastDeleteSuccess.format(it.name),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                showDeleteConfirmDialog = false
                                tableToDelete = null
                            } else {
                                Toast.makeText(context, toastDeleteLastFailed, Toast.LENGTH_SHORT)
                                    .show()
                                showDeleteConfirmDialog = false
                                tableToDelete = null
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(color = MiuixTheme.colorScheme.error)
                    ) {
                        Text(actionDelete, color = MiuixTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    }
}

@Composable
fun CourseTableCard(
    tableInfo: CourseTable,
    isSelected: Boolean,
    onDeleteClick: (CourseTable) -> Unit,
    onEditClick: (CourseTable) -> Unit,
    onCardClick: (CourseTable) -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    val a11yCurrentTable = stringResource(R.string.a11y_current_table)
    val a11yEdit = stringResource(R.string.a11y_edit)
    val a11yDelete = stringResource(R.string.a11y_delete)
    val idPrefix = stringResource(R.string.course_table_id_prefix)
    val createdAtPrefix = stringResource(R.string.course_table_created_at_prefix)

    // 动态适配颜色策略
    val backgroundColor =
        if (isSelected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.surfaceContainer
    val textColor =
        if (isSelected) MiuixTheme.colorScheme.onPrimary else MiuixTheme.colorScheme.onSurface
    val secondaryTextColor =
        if (isSelected) MiuixTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MiuixTheme.colorScheme.onSurfaceVariantSummary
    val actionIconColor =
        if (isSelected) MiuixTheme.colorScheme.onPrimary else MiuixTheme.colorScheme.onSurfaceVariantActions

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.defaultColors(color = backgroundColor),
        cornerRadius = 16.dp,
        onClick = { onCardClick(tableInfo) },
        pressFeedbackType = PressFeedbackType.Sink, // 开启卡片下沉交互
        showIndication = true
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tableInfo.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = idPrefix.format(tableInfo.id.take(8) + "..."),
                    fontSize = 12.sp,
                    color = secondaryTextColor
                )
                Text(
                    text = createdAtPrefix.format(dateFormatter.format(Date(tableInfo.createdAt))),
                    fontSize = 12.sp,
                    color = secondaryTextColor
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = a11yCurrentTable,
                        tint = textColor,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
                IconButton(onClick = { onEditClick(tableInfo) }) {
                    Icon(Icons.Default.Edit, contentDescription = a11yEdit, tint = actionIconColor)
                }
                IconButton(onClick = { onDeleteClick(tableInfo) }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = a11yDelete,
                        tint = actionIconColor
                    )
                }
            }
        }
    }
}

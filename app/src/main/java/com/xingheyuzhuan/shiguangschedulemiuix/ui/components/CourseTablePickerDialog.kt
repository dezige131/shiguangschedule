package com.xingheyuzhuan.shiguangschedulemiuix.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.xingheyuzhuan.shiguangschedulemiuix.R
import com.xingheyuzhuan.shiguangschedulemiuix.data.db.main.CourseTable
import com.xingheyuzhuan.shiguangschedulemiuix.data.repository.AppSettingsRepository
import com.xingheyuzhuan.shiguangschedulemiuix.data.repository.CourseTableRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * 专门为弹窗提供 Hilt 注入的仓库
 */
@HiltViewModel
class CourseTablePickerDeps @Inject constructor(
    val courseTableRepository: CourseTableRepository,
    val appSettingsRepository: AppSettingsRepository
) : ViewModel()

@Composable
fun CourseTablePickerDialog(
    show: Boolean,
    title: String,
    onDismissRequest: () -> Unit,
    onTableSelected: (CourseTable) -> Unit,
    deps: CourseTablePickerDeps = hiltViewModel()
) {
    val courseTables by deps.courseTableRepository.getAllCourseTables()
        .collectAsState(initial = emptyList())
    val appSettings by deps.appSettingsRepository.getAppSettings().collectAsState(initial = null)

    var selectedTable by remember { mutableStateOf<CourseTable?>(null) }

    LaunchedEffect(courseTables, appSettings, show) {
        // 每次打开弹窗时，如果还没选过，默认选中当前激活的课表
        if (show && selectedTable == null && appSettings?.currentCourseTableId != null) {
            selectedTable = courseTables.find { it.id == appSettings?.currentCourseTableId }
        }
    }

    WindowBottomSheet(
        show = show,
        title = title,
        onDismissRequest = onDismissRequest,
        // 左侧取消按钮
        startAction = {
            TextButton(
                text = stringResource(R.string.action_cancel),
                onClick = onDismissRequest
            )
        },
        // 右侧确定按钮
        endAction = {
            TextButton(
                text = stringResource(R.string.action_confirm),
                onClick = {
                    selectedTable?.let { onTableSelected(it) }
                    onDismissRequest()
                },
                enabled = selectedTable != null,
                colors = ButtonDefaults.textButtonColorsPrimary()
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            if (courseTables.isEmpty()) {
                Text(
                    text = stringResource(R.string.text_no_course_tables),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(courseTables) { courseTable ->
                        val isCurrentActive = courseTable.id == appSettings?.currentCourseTableId
                        val isSelectedForDialog = courseTable.id == selectedTable?.id

                        CourseTablePickerCard(
                            courseTable = courseTable,
                            isSelected = isSelectedForDialog,
                            isCurrentActive = isCurrentActive,
                            onCardClick = {
                                selectedTable = it
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CourseTablePickerCard(
    courseTable: CourseTable,
    isSelected: Boolean,
    isCurrentActive: Boolean,
    onCardClick: (CourseTable) -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    // 背景色：选中时纯蓝，未选中时 surface 颜色
    val backgroundColor = if (isSelected) {
        MiuixTheme.colorScheme.primary
    } else {
        MiuixTheme.colorScheme.secondaryContainer
    }

    // 主文字色：选中时为白色 (onPrimary)，未选中时为普通黑色/白色 (onSurface)
    val textColor = if (isSelected) {
        MiuixTheme.colorScheme.onPrimary
    } else {
        MiuixTheme.colorScheme.onSurface
    }

    // 次要文字色：选中时为半透明白，未选中时为灰色
    val secondaryTextColor = if (isSelected) {
        MiuixTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
    } else {
        MiuixTheme.colorScheme.onSurfaceVariantSummary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.defaultColors(color = backgroundColor),
        cornerRadius = 16.dp,
        onClick = { onCardClick(courseTable) },
        pressFeedbackType = PressFeedbackType.Sink, // 开启 Miuix 下沉交互动效
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
                    text = courseTable.name,
                    fontSize = 18.sp,
                    color = textColor,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(
                        R.string.course_table_id_prefix,
                        courseTable.id.take(8) + "..."
                    ),
                    fontSize = 12.sp,
                    color = secondaryTextColor
                )
                Text(
                    text = stringResource(
                        R.string.course_table_created_at_prefix,
                        dateFormatter.format(Date(courseTable.createdAt))
                    ),
                    fontSize = 12.sp,
                    color = secondaryTextColor
                )
            }
            if (isCurrentActive) {
                Text(
                    stringResource(R.string.label_current),
                    fontSize = 14.sp,
                    // 如果被选中了，"当前" 这两个字也要变成白色以保证能看清
                    color = if (isSelected) MiuixTheme.colorScheme.onPrimary else MiuixTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
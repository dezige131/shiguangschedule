// com/xingheyuzhuan/shiguangschedulemiuix.ui.schoolselection.web/WebDialogHost.kt
package com.xingheyuzhuan.shiguangschedulemiuix.ui.schoolselection.web

import android.webkit.WebView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xingheyuzhuan.shiguangschedulemiuix.R
import kotlinx.coroutines.flow.Flow
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.RadioButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

/**
 * 宿主：监听 AndroidBridge 事件，负责显示 JS 触发的 Compose 弹窗。
 */
@Composable
fun WebDialogHost(
    webView: WebView, // 仍需传入 WebView，但对话框逻辑已解耦。
    uiEvents: Flow<WebUiEvent> // 从 AndroidBridge 接收的 UI 事件流
) {
    var currentEvent by remember { mutableStateOf<WebUiEvent?>(null) }

    // 监听事件流
    LaunchedEffect(uiEvents) {
        uiEvents.collect { event ->
            currentEvent = event
        }
    }

    // 根据当前事件类型显示弹窗
    when (val event = currentEvent) {
        is WebUiEvent.ShowAlert -> {
            AlertHost(event.data, onConfirm = {
                event.callback(true)
                currentEvent = null
            }, onDismiss = {
                event.callback(false)
                currentEvent = null
            })
        }

        is WebUiEvent.ShowPrompt -> {
            PromptHost(
                event.data,
                onRequestValidation = { input ->
                    event.onRequestValidation(input)
                },
                errorFlow = event.errorFeedbackFlow, // 错误反馈流
                onCancel = {
                    event.onCancel()
                    currentEvent = null
                }
            )
        }

        is WebUiEvent.ShowSingleSelection -> {
            SingleSelectionHost(event.data, onResult = { index ->
                event.callback(index)
                currentEvent = null
            })
        }

        null -> Unit
    }
}

/** 显示 Alert/Confirm 弹窗。 */
@Composable
private fun AlertHost(data: AlertDialogData, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    WindowDialog(
        show = true,
        title = data.title,
        summary = data.content,
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
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColorsPrimary()
                ) {
                    Text(data.confirmText, color = MiuixTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

/**
 * 显示 Prompt 弹窗，通过 Flow 接收 JS 验证的错误反馈。
 */
@Composable
private fun PromptHost(
    data: PromptDialogData,
    onRequestValidation: (String) -> Unit,
    errorFlow: Flow<String?>,
    onCancel: () -> Unit
) {
    var inputText by rememberSaveable { mutableStateOf(data.defaultText) }
    var errorText by rememberSaveable { mutableStateOf<String?>(null) } // JS 验证返回的错误信息

    // 监听错误反馈流
    LaunchedEffect(errorFlow) {
        errorFlow.collect { message ->
            // 收到错误时更新 UI 状态
            errorText = message
        }
    }

    WindowDialog(
        show = true,
        title = data.title,
        onDismissRequest = onCancel
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(top = 16.dp)
        ) {
            TextField(
                value = inputText,
                onValueChange = {
                    inputText = it
                    errorText = null // 用户修改输入时清空错误
                },
                label = data.tip,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MiuixTheme.colorScheme.surfaceContainerHigh
            )
            errorText?.let {
                Text(
                    text = it,
                    color = MiuixTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors()
                ) {
                    Text(
                        stringResource(R.string.action_cancel),
                        color = MiuixTheme.colorScheme.onSurface
                    )
                }

                Button(
                    onClick = { onRequestValidation(inputText) },
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

/** 显示单选列表弹窗。 */
@Composable
private fun SingleSelectionHost(data: SingleSelectionDialogData, onResult: (Int?) -> Unit) {
    var selectedIndex by rememberSaveable { mutableStateOf(data.defaultSelectedIndex) }

    WindowDialog(
        show = true,
        title = data.title,
        onDismissRequest = { onResult(null) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(top = 16.dp)
        ) {

            // 使用 Card 作为选项列表的底层容器，契合 Miuix UI 规范
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp), // 限制最大高度，防止列表过长撑出屏幕
                colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainerHigh),
                cornerRadius = 16.dp,
                insideMargin = PaddingValues(0.dp) // 移除默认边距，让列表项直接贴边
            ) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    data.items.forEachIndexed { index, item ->
                        // 手动使用 Row 和 RadioButton 封装单选列表项
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedIndex = index }
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = item,
                                fontSize = 16.sp,
                                color = MiuixTheme.colorScheme.onSurface
                            )
                            RadioButton(
                                selected = index == selectedIndex,
                                onClick = null // 点击事件交给外层的 Row 处理，体验更好
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { onResult(null) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors()
                ) {
                    Text(
                        stringResource(R.string.action_cancel),
                        color = MiuixTheme.colorScheme.onSurface
                    )
                }

                val confirmEnabled = selectedIndex != -1
                Button(
                    onClick = { onResult(selectedIndex) },
                    enabled = confirmEnabled,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColorsPrimary()
                ) {
                    Text(
                        text = stringResource(R.string.action_confirm),
                        color = if (confirmEnabled) MiuixTheme.colorScheme.onPrimary else MiuixTheme.colorScheme.disabledOnPrimaryButton
                    )
                }
            }
        }
    }
}
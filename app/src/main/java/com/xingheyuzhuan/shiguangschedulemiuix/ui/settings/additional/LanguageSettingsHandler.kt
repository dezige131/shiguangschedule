package com.xingheyuzhuan.shiguangschedulemiuix.ui.settings.additional

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.core.os.LocaleListCompat
import com.xingheyuzhuan.shiguangschedulemiuix.R
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.RadioButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

/**
 * 外部调用的处理函数
 */
fun handleLanguageSettingClick(
    context: Context,
    onShowDialog: () -> Unit
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        // Android 13+: 直接跳转系统应用语言设置
        try {
            val intent = Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply {
                data = "package:${context.packageName}".toUri()
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // 预防万一跳转失败（某些精简版 ROM），回退到应用内对话框
            onShowDialog()
        }
    } else {
        // Android 13 以下：显示应用内对话框
        onShowDialog()
    }
}

/**
 * 语言选择对话框组件
 */
@Composable
fun LanguageSelectionDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit
) {
    if (!showDialog) return

    val currentLanguageTag = remember(showDialog) {
        AppCompatDelegate.getApplicationLocales().toLanguageTags()
    }
    val context = LocalContext.current

    val supportedLocales = listOf(
        Pair(stringResource(R.string.language_follow_system), ""),
        Pair("简体中文", "zh-CN"),
        Pair("繁體中文", "zh-TW"),
        Pair("English", "en")
    )

    WindowDialog(
        show = showDialog,
        title = stringResource(R.string.item_language_settings),
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(top = 16.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.defaultColors(
                    color = MiuixTheme.colorScheme.surfaceVariant.copy(
                        alpha = 0.5f
                    )
                ),
                cornerRadius = 12.dp,
                insideMargin = PaddingValues(0.dp)
            ) {
                Column {
                    supportedLocales.forEach { (name, tag) ->
                        val isSelected = if (tag.isEmpty()) {
                            currentLanguageTag.isEmpty()
                        } else {
                            currentLanguageTag.startsWith(tag)
                        }

                        val onClickAction = {
                            val locales = if (tag.isEmpty()) {
                                LocaleListCompat.getEmptyLocaleList()
                            } else {
                                LocaleListCompat.forLanguageTags(tag)
                            }

                            AppCompatDelegate.setApplicationLocales(locales)

                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                                (context as? android.app.Activity)?.recreate()
                            }

                            onDismiss()
                        }

                        // 使用基础组件拼装，确保与新版源码 100% 兼容，并还原 MIUI 经典样式
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onClickAction)
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = name,
                                fontSize = 16.sp,
                                color = MiuixTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            RadioButton(
                                selected = isSelected,
                                onClick = null // 设为 null，由外层 Row 统一接管点击事件
                            )
                        }
                    }
                }
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, start = 16.dp, end = 16.dp),
                colors = ButtonDefaults.buttonColors()
            ) {
                Text(
                    stringResource(R.string.action_cancel),
                    color = MiuixTheme.colorScheme.onSurface
                )
            }
        }
    }
}
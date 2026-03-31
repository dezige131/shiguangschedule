package com.xingheyuzhuan.shiguangschedulemiuix.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xingheyuzhuan.shiguangschedulemiuix.ui.settings.style.StyleSettingsViewModel
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemeController

@Composable
fun ShiguangScheduleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val viewModel: StyleSettingsViewModel = hiltViewModel()
    val styleState by viewModel.styleState.collectAsStateWithLifecycle()

    // 初始化 Miuix 主题控制器
    val controller = remember(styleState?.colorSchemeMode, styleState?.monetSeedColor) {
        ThemeController(
            colorSchemeMode = styleState?.colorSchemeMode ?: ColorSchemeMode.System,
            keyColor = styleState?.monetSeedColor,
            colorSpec = ThemeColorSpec.Spec2021
        )
    }

    MiuixTheme(
        controller = controller,
        content = {
            // 处理沉浸式状态栏与导航栏 (参考 KernelSU 的实现)
            val isDark = controller.isDark ?: darkTheme
            LaunchedEffect(isDark) {
                val window = (context as? Activity)?.window ?: return@LaunchedEffect
                WindowInsetsControllerCompat(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !isDark
                    isAppearanceLightNavigationBars = !isDark
                }
            }

            content()
        }
    )
}
package com.xingheyuzhuan.shiguangschedule.ui.settings.conversion

import androidx.compose.runtime.Composable

@Composable
actual fun rememberLanSharePermissionAction(onGranted: () -> Unit): () -> Unit = onGranted
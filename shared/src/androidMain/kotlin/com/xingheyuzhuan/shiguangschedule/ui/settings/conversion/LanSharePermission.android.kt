package com.xingheyuzhuan.shiguangschedule.ui.settings.conversion

import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext

private const val ACCESS_LOCAL_NETWORK = "android.permission.ACCESS_LOCAL_NETWORK"

@Composable
actual fun rememberLanSharePermissionAction(onGranted: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) onGranted()
    }
    return remember(context, launcher, onGranted) {
        {
            if (Build.VERSION.SDK_INT < 37 ||
                ContextCompat.checkSelfPermission(context, ACCESS_LOCAL_NETWORK) == PackageManager.PERMISSION_GRANTED
            ) {
                onGranted()
            } else {
                launcher.launch(ACCESS_LOCAL_NETWORK)
            }
        }
    }
}
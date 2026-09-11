package com.xingheyuzhuan.shiguangschedule.tool

import android.os.Build
import java.net.NetworkInterface

actual fun platformDeviceName(): String {
    val manufacturer = Build.MANUFACTURER.trim()
    val model = Build.MODEL.trim()
    return if (model.startsWith(manufacturer, ignoreCase = true)) {
        model
    } else {
        "$manufacturer $model".trim()
    }
}

actual fun platformBroadcastAddresses(): List<String> = runCatching {
    NetworkInterface.getNetworkInterfaces().toList()
        .filter { it.isUp && !it.isLoopback }
        .flatMap { network -> network.interfaceAddresses.mapNotNull { it.broadcast?.hostAddress } }
        .distinct()
        .ifEmpty { listOf("255.255.255.255") }
}.getOrDefault(listOf("255.255.255.255"))
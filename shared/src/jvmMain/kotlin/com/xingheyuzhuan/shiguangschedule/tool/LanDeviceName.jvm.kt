package com.xingheyuzhuan.shiguangschedule.tool

import java.net.InetAddress
import java.net.NetworkInterface

actual fun platformDeviceName(): String = runCatching {
    InetAddress.getLocalHost().hostName
}.getOrDefault("")

actual fun platformBroadcastAddresses(): List<String> = runCatching {
    NetworkInterface.getNetworkInterfaces().toList()
        .filter { it.isUp && !it.isLoopback }
        .flatMap { network -> network.interfaceAddresses.mapNotNull { it.broadcast?.hostAddress } }
        .distinct()
        .ifEmpty { listOf("255.255.255.255") }
}.getOrDefault(listOf("255.255.255.255"))
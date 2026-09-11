package com.xingheyuzhuan.shiguangschedule.tool

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import platform.posix.uname
import platform.posix.utsname

@OptIn(ExperimentalForeignApi::class)
actual fun platformDeviceName(): String = memScoped {
    val systemInfo = alloc<utsname>()
    if (uname(systemInfo.ptr) == 0) systemInfo.machine.toKString() else ""
}

actual fun platformBroadcastAddresses(): List<String> = listOf("255.255.255.255")
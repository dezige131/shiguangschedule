// com/xingheyuzhuan/shiguangschedulemiuix/navigation/AppChannels.kt

package com.xingheyuzhuan.shiguangschedulemiuix.navigation

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

// --- 课程预设数据通道 ---

/**
 * 传递课程编辑/添加页面的预设数据。
 * 所有参数均为可空类型，支持选择性预填充。
 */
data class PresetCourseData(
    // 周几
    val day: Int? = null,
    // 节次范围 (单独传递，避免推算逻辑)
    val startSection: Int? = null,
    val endSection: Int? = null,
    // 课程基础信息
    val name: String? = null,
    val teacher: String? = null,
    val position: String? = null
)

/**
 * 安全传递一次性数据的通道。
 */
object AddEditCourseChannel {
    private val channel = Channel<PresetCourseData>(Channel.CONFLATED)
    fun sendEvent(data: PresetCourseData) {
        channel.trySend(data)
    }
    val presetDataFlow = channel.receiveAsFlow()
}
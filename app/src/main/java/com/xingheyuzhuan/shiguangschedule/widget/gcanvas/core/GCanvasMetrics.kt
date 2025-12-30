package com.xingheyuzhuan.shiguangschedule.widget.gcanvas.core

/**
 * GCanvas 绘图指标
 * 记录了虚拟设计稿与物理屏幕之间的所有映射关系
 */
data class GCanvasMetrics(
    val designWidth: Float,     // 设计稿原始宽度 (虚拟单位)
    val designHeight: Float,    // 设计稿原始高度 (虚拟单位)
    val finalScale: Float,      // 缩放倍率 (物理像素 / 虚拟像素)
    val safePadding: Float,     // 内边距 (虚拟单位)
    val contentOffsetX: Float,  // 内容区在物理容器中的水平起始偏移 (物理像素)
    val contentOffsetY: Float,  // 内容区在物理容器中的垂直起始偏移 (物理像素)
    val physicalWidth: Float,   // 缩放后内容占用的实际物理宽度
    val physicalHeight: Float   // 缩放后内容占用的实际物理高度
)
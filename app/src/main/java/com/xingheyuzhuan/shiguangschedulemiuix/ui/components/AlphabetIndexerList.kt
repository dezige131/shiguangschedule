package com.xingheyuzhuan.shiguangschedulemiuix.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

// 绘制气泡水滴形状（左侧圆形，右侧带有平滑尖角）
private val IndicatorShape = GenericShape { size, _ ->
    val width = size.width
    val height = size.height
    val radius = height / 2f

    // 左侧半圆
    addArc(Rect(0f, 0f, radius * 2, radius * 2), 90f, 270f)
    // 连接到右侧尖角
    moveTo(radius, 0f)
    cubicTo(
        width * 0.7f, 0f,
        width * 0.9f, height * 0.35f,
        width, height / 2f
    )
    cubicTo(
        width * 0.9f, height * 0.65f,
        width * 0.7f, height,
        radius, height
    )
    close()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> AlphabetIndexerList(
    data: List<T>,
    getInitial: (T) -> String,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
    headerContent: (@Composable LazyItemScope.() -> Unit)? = null,
    itemContent: @Composable (T) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current

    // 1. 提取去重后的首字母
    val initials = remember(data) {
        data.map { getInitial(it).uppercase() }.distinct().sorted()
    }

    // 2. 位置映射表：计算每个字母 Header 对应的 LazyColumn 索引
    val indexMap = remember(data, initials, headerContent) {
        val map = mutableMapOf<String, Int>()
        var currentIndex = 0
        if (headerContent != null) currentIndex++
        var lastInitial = ""
        data.forEach { item ->
            val initial = getInitial(item).uppercase()
            if (initial != lastInitial) {
                map[initial] = currentIndex
                lastInitial = initial
                currentIndex++
            }
            currentIndex++
        }
        map
    }

    Row(modifier = modifier.fillMaxSize()) {
        // --- 左侧：主列表 ---
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            headerContent?.let { item { it() } }
            var currentInitial = ""
            data.forEach { item ->
                val initial = getInitial(item).uppercase()
                if (initial != currentInitial) {
                    stickyHeader { DefaultAlphabetHeader(initial) }
                    currentInitial = initial
                }
                item { itemContent(item) }
            }
        }

        // --- 右侧：优化后的通讯录索引条 ---
        if (initials.isNotEmpty()) {
            var barHeight by remember { mutableFloatStateOf(0f) }
            var selectedIndex by remember { mutableStateOf<Int?>(null) }
            var isDragging by remember { mutableStateOf(false) }

            // 处理跳转逻辑
            val handleSelection: (Float) -> Unit = { yPosition ->
                if (barHeight > 0) {
                    val index = (yPosition / barHeight * initials.size)
                        .toInt()
                        .coerceIn(0, initials.lastIndex)

                    // 当选中的字母发生变化时才执行
                    if (selectedIndex != index) {
                        selectedIndex = index

                        // 触发轻微的震动反馈 (TextHandleMove 这种类型非常适合连续的“滴答”手感)
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)

                        val target = initials[index]
                        coroutineScope.launch {
                            indexMap[target]?.let { actualIndex ->
                                lazyListState.scrollToItem(actualIndex)
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(48.dp) // 放宽触控区域防止误触
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                // 气泡指示器
                if (isDragging && selectedIndex != null && barHeight > 0) {
                    val itemHeight = barHeight / initials.size
                    // 计算 Y 轴绝对偏移量（相对于胶囊体中心点）
                    val relativeY =
                        (selectedIndex!! * itemHeight) + (itemHeight / 2) - (barHeight / 2)

                    Box(
                        modifier = Modifier
                            .offset(
                                x = (-40).dp, // 向左偏移气泡
                                y = with(LocalDensity.current) { relativeY.toDp() }
                            )
                            .size(width = 52.dp, height = 44.dp)
                            .shadow(elevation = 6.dp, shape = IndicatorShape)
                            .background(MiuixTheme.colorScheme.surface, IndicatorShape)
                            .padding(end = 8.dp), // 将文字重心往左调，对齐气泡圆心
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials[selectedIndex!!],
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MiuixTheme.colorScheme.primary
                        )
                    }
                }

                // 胶囊体容器
                Box(
                    modifier = Modifier
                        .background(
                            color = MiuixTheme.colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(percent = 50)
                        )
                        .padding(vertical = 8.dp, horizontal = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .wrapContentHeight()
                            .onGloballyPositioned { coordinates ->
                                barHeight = coordinates.size.height.toFloat()
                            }
                            // 使用 awaitEachGesture 实现平滑的按压+滑动逻辑
                            .pointerInput(initials) {
                                awaitEachGesture {
                                    val down = awaitFirstDown()
                                    isDragging = true
                                    handleSelection(down.position.y)

                                    do {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull()
                                        if (change != null) {
                                            if (change.pressed) {
                                                handleSelection(change.position.y)
                                                change.consume()
                                            }
                                        }
                                    } while (event.changes.any { it.pressed })

                                    isDragging = false
                                    selectedIndex = null
                                }
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        initials.forEachIndexed { index, initial ->
                            val isSelected = isDragging && selectedIndex == index
                            Text(
                                text = initial,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                // 根据选中状态切换颜色：Miuix Theme 主色 vs 未选中时的次级文本色
                                color = if (isSelected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceContainerHigh,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DefaultAlphabetHeader(initial: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MiuixTheme.colorScheme.surface)
            .padding(horizontal = 24.dp, vertical = 6.dp)
    ) {
        Text(
            text = initial,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MiuixTheme.colorScheme.onSurface // 修复：使用更合理的onSurface作为表层文本
        )
    }
}
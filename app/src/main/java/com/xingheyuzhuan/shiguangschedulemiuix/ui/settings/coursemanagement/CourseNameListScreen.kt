package com.xingheyuzhuan.shiguangschedulemiuix.ui.settings.coursemanagement

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.xingheyuzhuan.shiguangschedulemiuix.AppRoute
import com.xingheyuzhuan.shiguangschedulemiuix.LocalAppNavigator
import com.xingheyuzhuan.shiguangschedulemiuix.R
import com.xingheyuzhuan.shiguangschedulemiuix.navigation.AddEditCourseChannel
import com.xingheyuzhuan.shiguangschedulemiuix.navigation.PresetCourseData
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CourseNameListScreen(
    viewModel: CourseNameListViewModel = hiltViewModel()
) {
    val navigator = LocalAppNavigator.current
    val courseNames by viewModel.uniqueCourseNames.collectAsState()
    val scrollBehavior = MiuixScrollBehavior()
    val coroutineScope = rememberCoroutineScope()

    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedCourseNames by remember { mutableStateOf(setOf<String>()) }

    BackHandler(enabled = isSelectionMode) {
        isSelectionMode = false
        selectedCourseNames = emptySet()
    }

    Scaffold(
        containerColor = MiuixTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = if (isSelectionMode) stringResource(
                    R.string.title_selected_items_count,
                    selectedCourseNames.size
                ) else stringResource(R.string.item_course_management),
                largeTitle = if (isSelectionMode) stringResource(
                    R.string.title_selected_items_count,
                    selectedCourseNames.size
                ) else stringResource(R.string.item_course_management),
                navigationIcon = {
                    if (isSelectionMode) {
                        IconButton(onClick = {
                            isSelectionMode = false
                            selectedCourseNames = emptySet()
                        }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.action_cancel),
                                tint = MiuixTheme.colorScheme.onBackground
                            )
                        }
                    } else {
                        IconButton(onClick = {
                            if (navigator.isNotEmpty()) {
                                navigator.removeAt(navigator.size - 1)
                            }
                        }) {
                            Icon(
                                MiuixIcons.Back,
                                contentDescription = stringResource(R.string.a11y_back),
                                tint = MiuixTheme.colorScheme.onBackground
                            )
                        }
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        val isAllSelected =
                            selectedCourseNames.size == courseNames.size && courseNames.isNotEmpty()
                        IconButton(onClick = {
                            if (isAllSelected) {
                                selectedCourseNames = emptySet()
                            } else {
                                selectedCourseNames = courseNames.map { it.name }.toSet()
                            }
                        }) {
                            Icon(
                                imageVector = if (isAllSelected) Icons.Default.Close else Icons.Default.Check,
                                contentDescription = if (isAllSelected) stringResource(R.string.action_deselect_all) else stringResource(
                                    R.string.action_select_all
                                ),
                                tint = MiuixTheme.colorScheme.onBackground
                            )
                        }
                        IconButton(onClick = {
                            if (selectedCourseNames.isNotEmpty()) {
                                coroutineScope.launch {
                                    viewModel.deleteSelectedCourses(selectedCourseNames.toList())
                                    isSelectionMode = false
                                    selectedCourseNames = emptySet()
                                }
                            }
                        }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.a11y_delete),
                                tint = MiuixTheme.colorScheme.error
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            AddEditCourseChannel.sendEvent(PresetCourseData())
                            navigator.add(AppRoute.AddEditCourse(null))
                        }
                    },
                    containerColor = MiuixTheme.colorScheme.primary
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.action_add),
                        tint = MiuixTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(innerPadding)
        ) {
            if (courseNames.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.text_no_unique_courses_hint),
                        fontSize = 16.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = 8.dp,
                        bottom = 100.dp,
                        start = 12.dp,
                        end = 12.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(courseNames, key = { it.name }) { item ->
                        val isSelected = selectedCourseNames.contains(item.name)

                        CourseNameCard(
                            name = item.name,
                            instanceCount = item.count,
                            isSelected = isSelected,
                            modifier = Modifier.combinedClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = LocalIndication.current,
                                onClick = {
                                    if (isSelectionMode) {
                                        selectedCourseNames =
                                            if (isSelected) selectedCourseNames - item.name else selectedCourseNames + item.name
                                        if (selectedCourseNames.isEmpty()) isSelectionMode = false
                                    } else {
                                        // 核心修复：直接使用安全的类传参，无需 URL Encode 和 replace！
                                        navigator.add(AppRoute.CourseManagementDetail(item.name)) // <- 替换进入详情页
                                    }
                                },
                                onLongClick = {
                                    if (!isSelectionMode) {
                                        isSelectionMode = true
                                        selectedCourseNames = setOf(item.name)
                                    } else {
                                        selectedCourseNames =
                                            if (isSelected) selectedCourseNames - item.name else selectedCourseNames + item.name
                                        if (selectedCourseNames.isEmpty()) isSelectionMode = false
                                    }
                                }
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CourseNameCard(
    name: String,
    instanceCount: Int,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    // 选中时使用原生蓝色背景，文字使用白色
    val backgroundColor =
        if (isSelected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.surfaceContainer
    val textColor =
        if (isSelected) MiuixTheme.colorScheme.onPrimary else MiuixTheme.colorScheme.onSurface
    val secondaryTextColor =
        if (isSelected) MiuixTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MiuixTheme.colorScheme.onSurfaceVariantSummary

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp),
        colors = CardDefaults.defaultColors(color = backgroundColor),
        cornerRadius = 16.dp,
        insideMargin = PaddingValues(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = stringResource(R.string.label_instance_count, instanceCount),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = secondaryTextColor
            )
        }
    }
}
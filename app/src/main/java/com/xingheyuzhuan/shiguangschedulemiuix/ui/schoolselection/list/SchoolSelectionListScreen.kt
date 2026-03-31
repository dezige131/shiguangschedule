package com.xingheyuzhuan.shiguangschedulemiuix.ui.schoolselection.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.NavKey
import com.xingheyuzhuan.shiguangschedulemiuix.AppRoute
import com.xingheyuzhuan.shiguangschedulemiuix.LocalAppNavigator
import com.xingheyuzhuan.shiguangschedulemiuix.R
import com.xingheyuzhuan.shiguangschedulemiuix.data.model.SchoolHistoryModel
import com.xingheyuzhuan.shiguangschedulemiuix.ui.components.AlphabetIndexerList
import kotlinx.coroutines.launch
import school_index.AdapterCategory
import school_index.School
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SearchBar
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SchoolSelectionListScreen(
    viewModel: SchoolSelectionViewModel = hiltViewModel()
) {
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.refreshSchools()
    }
    val navigator = LocalAppNavigator.current
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val filteredSchools by viewModel.filteredSchools.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val schoolHistory by viewModel.schoolHistory.collectAsState()

    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val scrollBehavior = MiuixScrollBehavior()
    var isSearchActive by remember { mutableStateOf(false) }

    val titleText = stringResource(R.string.title_select_school)
    val placeholderText = stringResource(R.string.search_hint_school)

    Scaffold(
        containerColor = MiuixTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = titleText,
                largeTitle = titleText,
                navigationIcon = {
                    IconButton(onClick = {
                        if (navigator.isNotEmpty()) {
                            navigator.removeAt(navigator.size - 1)
                        }
                    }) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = stringResource(R.string.a11y_back),
                            tint = MiuixTheme.colorScheme.onBackground
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // =======================================
                // 固定区域：分类 Tab 和 搜索栏
                // =======================================

                // 搜索时隐藏分类 Tabs 以提供更沉浸的搜索体验
                if (!isSearchActive) {
                    CategoryTabs(
                        selectedCategory = selectedCategory,
                        onCategorySelected = { category ->
                            viewModel.updateSelectedCategory(category)
                            coroutineScope.launch { lazyListState.scrollToItem(0) }
                        },
                        displayCategories = viewModel.displayCategories
                    )
                }

                // 搜索栏紧跟其后，完全固定
                SchoolSearchBar(
                    query = searchQuery,
                    onQueryChange = viewModel::updateSearchQuery,
                    active = isSearchActive,
                    onActiveChange = { isSearchActive = it },
                    placeholder = placeholderText,
                    filteredSchools = filteredSchools,
                    onSchoolSelected = { school ->
                        navigateToAdapter(navigator, school, selectedCategory, viewModel)
                        isSearchActive = false
                    }
                )

                // =======================================
                // 滚动区域：剩余空间交给列表
                // =======================================
                if (!isSearchActive) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f) // 将权重提升到 Box 上
                    ) {
                        AlphabetIndexerList(
                            modifier = Modifier
                                .fillMaxSize() // 占满 Box 空间
                                .nestedScroll(scrollBehavior.nestedScrollConnection),
                            data = filteredSchools,
                            getInitial = { it.initial },
                            lazyListState = lazyListState,
                            headerContent = {
                                // 最近访问依然作为列表头部，随列表滚动
                                RecentVisitSection(
                                    selectedCategory = selectedCategory,
                                    schoolHistory = schoolHistory,
                                    onClearHistory = { viewModel.clearHistory(it) },
                                    onSchoolSelected = { school, category ->
                                        navigateToAdapter(navigator, school, category, viewModel)
                                    }
                                )
                            }
                        ) { school ->
                            SchoolItem(school = school) {
                                navigateToAdapter(navigator, it, selectedCategory, viewModel)
                            }
                        }

                        // 空列表时的提示文字 (确保非加载状态且列表为空)
                        if (!isLoading && filteredSchools.isEmpty()) {
                            Text(
                                text = "暂无适配学校",
                                modifier = Modifier.align(Alignment.Center),
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                            )
                        }
                    }
                }
            }

            // 加载指示器（居中显示在 Box 中）
            if (isLoading && !isSearchActive) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MiuixTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun SchoolSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    active: Boolean,
    onActiveChange: (Boolean) -> Unit,
    placeholder: String,
    filteredSchools: List<School>,
    onSchoolSelected: (School) -> Unit
) {
    SearchBar(
        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        inputField = {
            InputField(
                query = query,
                onQueryChange = onQueryChange,
                onSearch = { onActiveChange(false) },
                expanded = active,
                onExpandedChange = onActiveChange,
                label = placeholder,
                leadingIcon = {
                    Icon(
                        modifier = Modifier.padding(start = 12.dp, end = 8.dp),
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary
                            )
                        }
                    }
                }
            )
        },
        expanded = active,
        onExpandedChange = onActiveChange,
        outsideEndAction = {
            if (active) {
                Text(
                    modifier = Modifier
                        .padding(start = 12.dp, end = 16.dp)
                        .clickable(interactionSource = null, indication = null) {
                            onActiveChange(false)
                            onQueryChange("")
                        },
                    text = stringResource(R.string.action_cancel),
                    color = MiuixTheme.colorScheme.primary
                )
            }
        }
    ) {
        // 搜索展开后的结果列表
        Column(modifier = Modifier.fillMaxSize()) {

            // 1. 固定在这里的透明间隙，绝对不会跟着上滑
            Spacer(modifier = Modifier.height(12.dp))

            // 2. 下方才是真正滚动的列表
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // 如果输入框为空，直接跳过（默认空结果）
                if (query.isNotBlank()) {
                    // 如果没搜到对应的学校
                    if (filteredSchools.isEmpty()) {
                        item {
                            Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = stringResource(R.string.text_no_school_found),
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                )
                            }
                        }
                    }
                    // 正常显示搜索到的学校列表
                    else {
                        items(filteredSchools.size) { index ->
                            SchoolItem(
                                school = filteredSchools[index],
                                onClick = onSchoolSelected
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentVisitSection(
    selectedCategory: AdapterCategory,
    schoolHistory: SchoolHistoryModel,
    onClearHistory: (AdapterCategory) -> Unit,
    onSchoolSelected: (School, AdapterCategory) -> Unit
) {
    val recentRecord = when (selectedCategory) {
        AdapterCategory.BACHELOR_AND_ASSOCIATE -> schoolHistory.bachelor
        AdapterCategory.POSTGRADUATE -> schoolHistory.postgraduate
        AdapterCategory.GENERAL_TOOL -> schoolHistory.general
        else -> null
    }

    if (recentRecord != null && !recentRecord.isEmpty) {
        Column(modifier = Modifier.padding(bottom = 8.dp)) {
            Text(
                text = stringResource(R.string.label_recent_visit),
                fontSize = 16.sp,
                color = MiuixTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 24.dp, top = 8.dp, bottom = 4.dp)
            )

            // 1. 直接移除外层的 Box，使用 SchoolItem
            SchoolItem(
                school = recentRecord.toSchool(),
                onClick = { onSchoolSelected(it, selectedCategory) },
                // 2. 将关闭按钮通过 trailingContent 传入
                trailingContent = {
                    // 替换原本的 IconButton
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.action_cancel), // 建议加上无障碍描述
                        modifier = Modifier
                            .size(24.dp) // 保持和普通列表的 Icon 默认大小一致
                            .clickable(
                                onClick = { onClearHistory(selectedCategory) },
                                // 可选：为了让点击区域稍微大点且好看，可以加个无界面的点击波纹
                                // interactionSource = remember { MutableInteractionSource() },
                                // indication = ripple(bounded = false, radius = 24.dp)
                            )
                            .padding(4.dp), // 内部 padding 缩小一点图标主体，让视觉更协调
                        tint = MiuixTheme.colorScheme.outline
                    )
                }
            )

            Box(
                modifier = Modifier
                    .padding(top = 8.dp, start = 24.dp, end = 24.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MiuixTheme.colorScheme.dividerLine)
            )
        }
    }
}

@Composable
fun CategoryTabs(
    selectedCategory: AdapterCategory,
    onCategorySelected: (AdapterCategory) -> Unit,
    displayCategories: List<AdapterCategory>
) {
    val tabs = displayCategories.map { category ->
        when (category) {
            AdapterCategory.BACHELOR_AND_ASSOCIATE -> stringResource(R.string.category_bachelor_associate)
            AdapterCategory.POSTGRADUATE -> stringResource(R.string.category_postgraduate)
            AdapterCategory.GENERAL_TOOL -> stringResource(R.string.category_general_tool)
            else -> stringResource(R.string.category_other)
        }
    }
    val selectedIndex = displayCategories.indexOf(selectedCategory).coerceAtLeast(0)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        TabRowWithContour(
            tabs = tabs,
            selectedTabIndex = selectedIndex,
            onTabSelected = { index -> onCategorySelected(displayCategories[index]) }
        )
    }
}

@Composable
fun SchoolItem(
    school: School,
    trailingContent: @Composable () -> Unit = {
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MiuixTheme.colorScheme.onSurfaceVariantActions
        )
    },
    onClick: (School) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer),
        cornerRadius = 16.dp
    ) {
        Row(
            modifier = Modifier
                .clickable { onClick(school) }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.School,
                contentDescription = null,
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(24.dp),
                tint = MiuixTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = school.name,
                    fontSize = 16.sp,
                    color = MiuixTheme.colorScheme.onSurface
                )
            }

            trailingContent()
        }
    }
}

private fun navigateToAdapter(
    navigator: MutableList<NavKey>, // <- 替换 NavController
    school: School,
    category: AdapterCategory,
    viewModel: SchoolSelectionViewModel
) {
    viewModel.saveLastSchool(school)
    // 替换原有的 navController.navigateSafe
    navigator.add(
        AppRoute.AdapterSelection(
            schoolId = school.id,
            schoolName = school.name,
            categoryNumber = category.number,
            resourceFolder = school.resourceFolder
        )
    )
}
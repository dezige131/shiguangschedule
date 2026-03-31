package com.xingheyuzhuan.shiguangschedulemiuix.ui.schoolselection.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.xingheyuzhuan.shiguangschedulemiuix.AppRoute
import com.xingheyuzhuan.shiguangschedulemiuix.LocalAppNavigator
import com.xingheyuzhuan.shiguangschedulemiuix.R
import school_index.Adapter
import school_index.AdapterCategory
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AdapterSelectionScreen(
    schoolId: String,
    schoolName: String,
    categoryNumber: Int,
    resourceFolder: String,
    viewModel: SchoolSelectionViewModel = hiltViewModel()
) {
    val navigator = LocalAppNavigator.current
    var adapters by remember { mutableStateOf<List<Adapter>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scrollBehavior = MiuixScrollBehavior()

    val currentCategory =
        AdapterCategory.forNumber(categoryNumber) ?: AdapterCategory.BACHELOR_AND_ASSOCIATE

    @Composable
    fun getCategoryDisplayName(): String {
        return when (currentCategory) {
            AdapterCategory.BACHELOR_AND_ASSOCIATE -> stringResource(R.string.category_bachelor_associate)
            AdapterCategory.POSTGRADUATE -> stringResource(R.string.category_postgraduate)
            AdapterCategory.GENERAL_TOOL -> stringResource(R.string.category_general_tool)
            else -> stringResource(R.string.category_other)
        }
    }

    LaunchedEffect(schoolId, currentCategory) {
        isLoading = true
        try {
            viewModel.updateSelectedCategory(currentCategory)
            val loadedAdapters = viewModel.getAdaptersForSchoolAndCategory(schoolId)
            adapters = loadedAdapters
        } catch (e: Exception) {
            adapters = emptyList()
        } finally {
            isLoading = false
        }
    }

    val categoryDisplayName = getCategoryDisplayName()
    val titleText = "$schoolName - $categoryDisplayName"

    Scaffold(
        containerColor = MiuixTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = titleText,
                largeTitle = titleText,
                navigationIconPadding = 12.dp,
                navigationIcon = {
                    IconButton(onClick = {
                        if (navigator.isNotEmpty()) {
                            navigator.removeAt(navigator.size - 1)
                        }
                    }) {
                        Icon(
                            MiuixIcons.Back,
                            contentDescription = stringResource(R.string.a11y_back_to_school_list),
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
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MiuixTheme.colorScheme.primary
                    )
                }

                adapters.isEmpty() -> {
                    Text(
                        text = stringResource(
                            R.string.text_no_adapter_for_category_school,
                            categoryDisplayName
                        ),
                        modifier = Modifier.align(Alignment.Center),
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                        contentPadding = PaddingValues(bottom = 32.dp),
                    ) {
                        items(adapters, key = { it.adapterId }) { adapter ->
                            AdapterCard(
                                adapter = adapter,
                                onClick = { selectedAdapter ->
                                    val initialUrl =
                                        selectedAdapter.importUrl.ifBlank { "about:blank" }
                                    val jsFileName = selectedAdapter.assetJsPath
                                    val assetJsPath = if (jsFileName.isNotBlank()) {
                                        "$resourceFolder/$jsFileName"
                                    } else {
                                        "$resourceFolder/${selectedAdapter.adapterId}.js"
                                    }
                                    navigator.add(
                                        AppRoute.WebView(
                                            initialUrl = initialUrl,
                                            assetJsPath = assetJsPath
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdapterCard(
    adapter: Adapter,
    onClick: (Adapter) -> Unit
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
                .clickable { onClick(adapter) }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = adapter.adapterName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MiuixTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = adapter.description.ifBlank { stringResource(R.string.text_no_detailed_description) },
                    fontSize = 13.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )

                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.outline,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(
                            R.string.label_contributor_format,
                            adapter.maintainer.ifBlank { stringResource(R.string.label_contributor_unknown) }
                        ),
                        fontSize = 12.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
        }
    }
}
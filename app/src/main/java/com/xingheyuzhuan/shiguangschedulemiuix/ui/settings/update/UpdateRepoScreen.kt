package com.xingheyuzhuan.shiguangschedulemiuix.ui.settings.update

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xingheyuzhuan.shiguangschedulemiuix.BuildConfig
import com.xingheyuzhuan.shiguangschedulemiuix.LocalAppNavigator
import com.xingheyuzhuan.shiguangschedulemiuix.R
import com.xingheyuzhuan.shiguangschedulemiuix.data.model.RepoType
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.WindowDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun UpdateRepoScreen(
    viewModel: UpdateRepoViewModel = viewModel(factory = UpdateRepoViewModelFactory)
) {
    val navigator = LocalAppNavigator.current
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = MiuixScrollBehavior()
    val titleText = stringResource(R.string.title_update_repo_screen)

    // 背景色严格对齐参考界面：surface
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ==========================================
            // 第一个卡片组：仓库设置
            // ==========================================
            Column(modifier = Modifier.fillMaxWidth()) {
                // 完美对齐风格的小标题
                Text(
                    text = stringResource(R.string.label_select_repo),
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 32.dp, bottom = 8.dp, top = 8.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer),
                    cornerRadius = 16.dp
                ) {
                    Column {
                        // 1. 下拉菜单项
                        val displayRepos = remember(uiState.repoList) {
                            uiState.repoList.filter { repo ->
                                if (BuildConfig.HIDE_CUSTOM_REPOS) {
                                    repo.repoType != RepoType.CUSTOM && repo.repoType != RepoType.PRIVATE_REPO
                                } else true
                            }
                        }

                        val options = displayRepos.map { it.name }
                        val selectedIndex =
                            displayRepos.indexOf(uiState.selectedRepo).coerceAtLeast(0)

                        if (options.isNotEmpty()) {
                            WindowDropdownPreference(
                                title = stringResource(R.string.label_select_repo),
                                items = options,
                                selectedIndex = selectedIndex,
                                onSelectedIndexChange = { index ->
                                    viewModel.selectRepository(displayRepos[index])
                                }
                            )
                        }

                        // 2. 动态展开的编辑表单
                        val currentRepo = uiState.selectedRepo // 将其捕获为局部变量以支持 Smart Cast
                        if (currentRepo?.editable == true) {
                            MiuixDivider()

                            // 输入框背景设为透明以完美融入 Card
                            TextField(
                                value = uiState.currentEditableUrl,
                                onValueChange = { viewModel.updateCurrentUrl(it) },
                                label = stringResource(R.string.label_repo_url),
                                modifier = Modifier.fillMaxWidth(),
                                backgroundColor = Color.Transparent
                            )

                            MiuixDivider()

                            TextField(
                                value = uiState.currentEditableBranch,
                                onValueChange = { viewModel.updateCurrentBranch(it) },
                                label = stringResource(R.string.label_repo_branch),
                                modifier = Modifier.fillMaxWidth(),
                                backgroundColor = Color.Transparent
                            )

                            if (currentRepo.repoType == RepoType.PRIVATE_REPO) {
                                MiuixDivider()

                                TextField(
                                    value = uiState.currentEditableUsername,
                                    onValueChange = { viewModel.updateCurrentUsername(it) },
                                    label = stringResource(R.string.label_username_or_token_key),
                                    modifier = Modifier.fillMaxWidth(),
                                    backgroundColor = Color.Transparent
                                )

                                MiuixDivider()

                                TextField(
                                    value = uiState.currentEditablePassword,
                                    onValueChange = { viewModel.updateCurrentPassword(it) },
                                    label = stringResource(R.string.label_password_or_token_value),
                                    modifier = Modifier.fillMaxWidth(),
                                    backgroundColor = Color.Transparent
                                )
                            }
                        }

                        MiuixDivider()

                        // 3. 操作按钮与不确定进度条
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Button(
                                onClick = { viewModel.startUpdate() },
                                enabled = !uiState.isUpdating,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColorsPrimary()
                            ) {
                                Text(
                                    text = if (uiState.isUpdating) {
                                        stringResource(R.string.action_updating)
                                    } else {
                                        stringResource(R.string.action_update)
                                    },
                                    color = if (uiState.isUpdating) MiuixTheme.colorScheme.disabledOnPrimaryButton else MiuixTheme.colorScheme.onPrimary
                                )
                            }

                            AnimatedVisibility(visible = uiState.isUpdating) {
                                LinearProgressIndicator(
                                    progress = null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ==========================================
            // 第二个卡片组：更新日志
            // ==========================================
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.title_update_log),
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 32.dp, bottom = 8.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer),
                    cornerRadius = 16.dp
                ) {
                    val scrollState = rememberScrollState()

                    LaunchedEffect(uiState.logs) {
                        if (uiState.logs.isNotEmpty()) {
                            scrollState.animateScrollTo(scrollState.maxValue)
                        }
                    }

                    SelectionContainer {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 150.dp, max = 350.dp)
                        ) {
                            Text(
                                text = uiState.logs,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(scrollState)
                                    .padding(16.dp),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = MiuixTheme.colorScheme.onSurfaceVariantActions
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// 抽取对齐风格的 1dp 细分割线
@Composable
private fun MiuixDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(horizontal = 16.dp)
            .background(MiuixTheme.colorScheme.surfaceVariant)
    )
}
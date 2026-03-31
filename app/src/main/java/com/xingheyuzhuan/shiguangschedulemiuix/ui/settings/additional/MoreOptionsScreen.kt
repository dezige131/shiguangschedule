package com.xingheyuzhuan.shiguangschedulemiuix.ui.settings.additional

import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PeopleAlt
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import com.xingheyuzhuan.shiguangschedulemiuix.AppRoute
import com.xingheyuzhuan.shiguangschedulemiuix.LocalAppNavigator
import com.xingheyuzhuan.shiguangschedulemiuix.R
import com.xingheyuzhuan.shiguangschedulemiuix.tool.UpdateChecker
import com.xingheyuzhuan.shiguangschedulemiuix.tool.UpdateChecker.Companion.UPDATE_CHANNELS
import com.xingheyuzhuan.shiguangschedulemiuix.tool.UpdateStatus
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

private const val GITHUB_REPO_URL = "https://github.com/XingHeYuZhuan/shiguangschedule"

@Composable
private fun AcknowledgmentContent(modifier: Modifier = Modifier) {
    val a11yAcknowledgment = stringResource(R.string.a11y_acknowledgment)
    val labelSpecialThanks = stringResource(R.string.label_special_thanks)
    val textAcknowledgmentBody = stringResource(R.string.text_acknowledgment_body)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = a11yAcknowledgment,
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = labelSpecialThanks,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
        }

        Text(
            text = textAcknowledgmentBody,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

/**
 * 更新渠道选择弹窗 (Miuix 规范重构)
 */
@Composable
private fun ChannelSelectionDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    currentSelectedUrl: String,
    onChannelSelected: (String) -> Unit
) {
    WindowDialog(
        show = showDialog,
        title = stringResource(R.string.dialog_select_update_channel),
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(top = 16.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.defaultColors(
                    color = MiuixTheme.colorScheme.surfaceVariant.copy(
                        alpha = 0.5f
                    )
                ),
                cornerRadius = 12.dp,
                insideMargin = PaddingValues(0.dp)
            ) {
                Column {
                    UPDATE_CHANNELS.forEachIndexed { index, channel ->
                        val isSelected = channel.url == currentSelectedUrl
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onChannelSelected(channel.url) }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(channel.titleResId),
                                fontSize = 16.sp,
                                color = if (isSelected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = MiuixTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        if (index < UPDATE_CHANNELS.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .padding(horizontal = 16.dp)
                                    .background(MiuixTheme.colorScheme.surfaceVariant)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors()
                ) {
                    Text(
                        stringResource(R.string.action_cancel),
                        color = MiuixTheme.colorScheme.onSurface
                    )
                }
                Button(
                    onClick = {
                        onConfirm(currentSelectedUrl)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColorsPrimary()
                ) {
                    Text(
                        stringResource(R.string.action_confirm),
                        color = MiuixTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

/**
 * 更新检查结果弹窗 (Miuix 规范重构)
 */
@Composable
private fun UpdateResultDialog(
    showDialog: Boolean,
    updateStatus: UpdateStatus,
    onDismiss: () -> Unit,
    onDownloadClick: (String) -> Unit
) {
    val context = LocalContext.current

    if (!showDialog || updateStatus is UpdateStatus.Idle) return

    val isChecking = updateStatus is UpdateStatus.Checking
    val title = when (updateStatus) {
        is UpdateStatus.Checking -> stringResource(R.string.dialog_checking_update)
        is UpdateStatus.Found -> stringResource(
            R.string.dialog_new_version_found,
            updateStatus.flavorInfo.latestVersionName
        )

        is UpdateStatus.Latest -> stringResource(R.string.dialog_current_version_latest)
        is UpdateStatus.Error -> stringResource(R.string.dialog_update_check_failed)
        else -> ""
    }

    val text = when (updateStatus) {
        is UpdateStatus.Checking -> stringResource(R.string.tip_please_wait)
        is UpdateStatus.Found -> updateStatus.flavorInfo.changelog
        is UpdateStatus.Latest -> context.getString(
            R.string.label_version_prefix,
            updateStatus.versionName
        )

        is UpdateStatus.Error -> stringResource(R.string.label_error_message, updateStatus.message)
        else -> ""
    }

    WindowDialog(
        show = showDialog,
        title = title,
        onDismissRequest = if (isChecking) {
            {}
        } else onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {

            Box(
                modifier = Modifier
                    .heightIn(min = 40.dp, max = 300.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = text,
                    fontSize = 14.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    lineHeight = 20.sp
                )
            }

            if (!isChecking) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (updateStatus is UpdateStatus.Found) {
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors()
                        ) {
                            Text(
                                stringResource(R.string.action_cancel),
                                color = MiuixTheme.colorScheme.onSurface
                            )
                        }
                        Button(
                            onClick = { onDownloadClick(updateStatus.downloadUrl) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColorsPrimary()
                        ) {
                            Text(
                                stringResource(R.string.btn_download_update),
                                color = MiuixTheme.colorScheme.onPrimary
                            )
                        }
                    } else {
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColorsPrimary()
                        ) {
                            Text(
                                stringResource(R.string.action_confirm),
                                color = MiuixTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun MoreOptionsScreen() {
    val navigator = LocalAppNavigator.current

    val context = LocalContext.current
    val coroutineScope = LocalLifecycleOwner.current.lifecycleScope
    val scrollState = rememberScrollState()

    // 状态管理
    val checker = remember { UpdateChecker(context) }
    var updateStatus by remember { mutableStateOf<UpdateStatus>(UpdateStatus.Idle) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showChannelSelectionDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    var selectedChannelUrl by remember {
        mutableStateOf(UpdateChecker.DEFAULT_PLATFORM_URL)
    }

    // 应用信息获取
    val defaultAppName = stringResource(R.string.default_app_name)
    val a11yAppIcon = stringResource(R.string.a11y_app_icon)

    val (appName, appVersion, appIconId) = remember(context) {
        val info = try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }

        val name =
            info?.applicationInfo?.loadLabel(context.packageManager)?.toString() ?: defaultAppName
        val version = info?.versionName ?: "N/A"
        val iconId = info?.applicationInfo?.icon ?: 0

        Triple(name, version, iconId)
    }

    // 开始更新检查的逻辑
    val startUpdateCheck: (String) -> Unit = { platformUrl ->
        updateStatus = UpdateStatus.Checking
        showUpdateDialog = true
        coroutineScope.launch {
            val result = checker.checkUpdate(platformUrl)
            updateStatus = result
        }
    }

    // 更新检查触发逻辑 (供列表项点击调用)
    val onCheckClick: () -> Unit = fun() {
        if (updateStatus is UpdateStatus.Checking) return
        selectedChannelUrl = UpdateChecker.DEFAULT_PLATFORM_URL
        showChannelSelectionDialog = true
    }

    // 处理下载点击
    val onDownloadClick: (String) -> Unit = { downloadUrl ->
        checker.launchExternalDownload(downloadUrl)
        showUpdateDialog = false
        updateStatus = UpdateStatus.Idle
    }

    // 处理弹窗关闭
    val onDismissDialog: () -> Unit = {
        showUpdateDialog = false
        if (updateStatus !is UpdateStatus.Found) {
            updateStatus = UpdateStatus.Idle
        }
    }

    Scaffold(
        containerColor = MiuixTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = stringResource(R.string.title_more_options),
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
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 应用信息区域
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (appIconId != 0) {
                    AsyncImage(
                        model = appIconId,
                        contentDescription = a11yAppIcon,
                        modifier = Modifier
                            .size(96.dp)
                            .clip(top.yukonga.miuix.kmp.theme.miuixShape(24.dp)),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = a11yAppIcon,
                        modifier = Modifier.size(96.dp),
                        tint = MiuixTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = appName,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MiuixTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = stringResource(R.string.label_version_prefix, appVersion),
                    fontSize = 14.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 设置项列表区域
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer),
                cornerRadius = 16.dp,
                insideMargin = PaddingValues(0.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // 检查更新
                    ArrowPreference(
                        title = stringResource(R.string.item_check_software_update),
                        startAction = {
                            Icon(
                                imageVector = Icons.Default.Update,
                                contentDescription = null,
                                tint = MiuixTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        onClick = onCheckClick
                    )

                    // 语言切换
                    ArrowPreference(
                        title = stringResource(R.string.item_language_settings),
                        startAction = {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = MiuixTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        onClick = {
                            handleLanguageSettingClick(context) { showLanguageDialog = true }
                        }
                    )

                    // GitHub 仓库
                    ArrowPreference(
                        title = stringResource(R.string.item_github_repo),
                        startAction = {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = null,
                                tint = MiuixTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, GITHUB_REPO_URL.toUri())
                            context.startActivity(intent)
                        }
                    )

                    // 查看开源许可证
                    ArrowPreference(
                        title = stringResource(R.string.item_open_source_licenses),
                        startAction = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ListAlt,
                                contentDescription = null,
                                tint = MiuixTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        onClick = { navigator.add(AppRoute.OpenSourceLicenses) }
                    )

                    // 更新教务适配仓库
                    ArrowPreference(
                        title = stringResource(R.string.item_update_repo),
                        startAction = {
                            Icon(
                                imageVector = Icons.Default.Update,
                                contentDescription = null,
                                tint = MiuixTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        onClick = { navigator.add(AppRoute.UpdateRepo) }
                    )

                    // 贡献者列表
                    ArrowPreference(
                        title = stringResource(R.string.item_contributors),
                        startAction = {
                            Icon(
                                imageVector = Icons.Default.PeopleAlt,
                                contentDescription = null,
                                tint = MiuixTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        onClick = { navigator.add(AppRoute.ContributionList) }
                    )

                    // 鸣谢内容
                    AcknowledgmentContent()
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // 各种弹窗
    UpdateResultDialog(
        showDialog = showUpdateDialog,
        updateStatus = updateStatus,
        onDismiss = onDismissDialog,
        onDownloadClick = onDownloadClick
    )

    ChannelSelectionDialog(
        showDialog = showChannelSelectionDialog,
        onDismiss = { showChannelSelectionDialog = false },
        onConfirm = startUpdateCheck,
        currentSelectedUrl = selectedChannelUrl,
        onChannelSelected = { url -> selectedChannelUrl = url }
    )

    // 原生调用外部组件
    LanguageSelectionDialog(
        showDialog = showLanguageDialog,
        onDismiss = { showLanguageDialog = false }
    )
}
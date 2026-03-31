package com.xingheyuzhuan.shiguangschedulemiuix.ui.schoolselection.web

import android.annotation.SuppressLint
import android.net.http.SslError
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.xingheyuzhuan.shiguangschedulemiuix.BuildConfig
import com.xingheyuzhuan.shiguangschedulemiuix.LocalAppNavigator
import com.xingheyuzhuan.shiguangschedulemiuix.R
import com.xingheyuzhuan.shiguangschedulemiuix.ui.components.CourseTablePickerDialog
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog
import top.yukonga.miuix.kmp.window.WindowListPopup
import java.io.File

@Composable
fun WebViewScreen(
    initialUrl: String?,
    assetJsPath: String?,
    viewModel: WebViewModel = hiltViewModel()
) {
    val navigator = LocalAppNavigator.current
    val courseConversionRepository = viewModel.courseConversionRepository
    val timeSlotRepository = viewModel.timeSlotRepository

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    val startedEmpty: Boolean =
        remember { initialUrl.isNullOrBlank() || initialUrl == "about:blank" }

    // --- 预取字符串资源 ---
    val titleEnterUrl = stringResource(R.string.title_enter_url)
    val titleLoading = stringResource(R.string.title_loading)
    val toastImportFinished = stringResource(R.string.toast_import_script_finished)
    val toastSwitchedToDesktop = stringResource(R.string.toast_switched_to_desktop)
    val toastSwitchedToPhone = stringResource(R.string.toast_switched_to_phone)
    val toastUrlEmpty = stringResource(R.string.toast_url_empty_enter_first)
    val toastDevToolsEnabledFmt = stringResource(R.string.toast_devtools_enabled_format)
    val statusEnabled = stringResource(R.string.status_enabled)
    val statusDisabled = stringResource(R.string.status_disabled)
    val toastNoManualImport = stringResource(R.string.toast_no_script_manual_import)
    val toastExecutingImport = stringResource(R.string.toast_executing_import_script)
    val toastNoImportScript = stringResource(R.string.toast_no_import_script)
    val toastImportNotFoundFmt = stringResource(R.string.toast_import_script_not_found)
    val toastLoadImportFailedFmt = stringResource(R.string.toast_load_import_script_failed)

    var currentUrl by remember { mutableStateOf(initialUrl ?: "about:blank") }
    var inputUrl by remember { mutableStateOf(if (startedEmpty) "" else (initialUrl ?: "")) }
    var loadingProgress by remember { mutableFloatStateOf(0f) }
    var pageTitle by remember {
        mutableStateOf(
            if (startedEmpty) titleEnterUrl else titleLoading
        )
    }
    var expanded by remember { mutableStateOf(false) }
    var isDesktopMode by remember { mutableStateOf(false) }

    var isEditingUrl by remember {
        mutableStateOf(startedEmpty)
    }

    var isDevToolsEnabled by remember { mutableStateOf(false) }
    var showCourseTablePicker by remember { mutableStateOf(false) }

    var sslErrorHandleState by remember {
        mutableStateOf<Pair<SslErrorHandler, SslError>?>(null)
    }

    // --- DevTools 逻辑 ---
    val enableDevToolsOptionInUi = BuildConfig.ENABLE_DEV_TOOLS_OPTION_IN_UI
    val enableAddressBarToggleButton = BuildConfig.ENABLE_ADDRESS_BAR_TOGGLE_BUTTON

    // --- 浏览器配置和 Agent ---
    val defaultUserAgent = remember { WebSettings.getDefaultUserAgent(context) }

    // --- Channel 和 Bridge 实例化 ---
    val uiEventChannel = remember { Channel<WebUiEvent>(Channel.BUFFERED) }
    var androidBridge: AndroidBridge? by remember { mutableStateOf(null) }

    val webView = remember {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            // 实例化 AndroidBridge
            androidBridge = AndroidBridge(
                context = context,
                coroutineScope = coroutineScope,
                webView = this,
                uiEventChannel = uiEventChannel,
                courseConversionRepository = courseConversionRepository,
                timeSlotRepository = timeSlotRepository,
                onTaskCompleted = {
                    Toast.makeText(context, toastImportFinished, Toast.LENGTH_LONG).show()
                    while (navigator.size > 1) {
                        navigator.removeAt(navigator.size - 1)
                    }
                }
            )
            @SuppressLint("JavascriptInterface")
            addJavascriptInterface(androidBridge!!, "AndroidBridge")
        }
    }

    LaunchedEffect(isDesktopMode) {
        val compatDelegate = WebCompatDelegate(webView)
        compatDelegate.enhanceSettings(isDesktopMode)

        webView.settings.userAgentString =
            if (isDesktopMode) DESKTOP_USER_AGENT else defaultUserAgent

        val businessClient = object : WebViewClient() {
            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean = false

            @SuppressLint("WebViewClientOnReceivedSslError")
            override fun onReceivedSslError(
                view: WebView,
                handler: SslErrorHandler,
                error: SslError
            ) {
                sslErrorHandleState = Pair(handler, error)
            }

            override fun onReceivedError(
                view: WebView,
                request: android.webkit.WebResourceRequest,
                error: android.webkit.WebResourceError
            ) {
                if (request.isForMainFrame) {
                    val description = error.description.toString()
                    view.post {
                        Toast.makeText(
                            view.context,
                            view.context.getString(
                                R.string.toast_web_load_error_format,
                                description
                            ),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        val businessChrome = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                loadingProgress = newProgress / 100f
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                if (title != null) pageTitle = title
            }
        }
        webView.webViewClient = compatDelegate.wrapWebViewClient(businessClient, isDesktopMode)
        webView.webChromeClient =
            compatDelegate.wrapWebChromeClient(businessChrome) { /* 进度已在 businessChrome 处理 */ }
    }

    // 状态改变时加载 URL
    LaunchedEffect(currentUrl) {
        if (currentUrl.isNotBlank() && currentUrl != "about:blank") {
            val urlToLoad =
                if (currentUrl.startsWith("http://") || currentUrl.startsWith("https://")) {
                    currentUrl
                } else {
                    "https://$currentUrl"
                }
            webView.loadUrl(urlToLoad)
        } else if (currentUrl == "about:blank") {
            webView.loadUrl("about:blank")
        }
    }

    // 资源清理
    DisposableEffect(webView) {
        onDispose {
            webView.stopLoading()
            webView.clearCache(true)
            webView.clearFormData()
            webView.clearHistory()
            val cookieManager = CookieManager.getInstance()
            cookieManager.removeAllCookies(null)
            cookieManager.flush()
            WebStorage.getInstance().deleteAllData()
            webView.removeAllViews()
            webView.destroy()
        }
    }

    val onSearch: (String) -> Unit = { query ->
        keyboardController?.hide()
        currentUrl = query
        isEditingUrl = false
        pageTitle = titleLoading
    }

    Scaffold(
        containerColor = MiuixTheme.colorScheme.surface,
        topBar = {
            if (isEditingUrl) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MiuixTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .height(56.dp)
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            isEditingUrl = false
                            val rawUrl = webView.url
                            inputUrl =
                                if (rawUrl.isNullOrBlank() || rawUrl == "about:blank") "" else rawUrl
                            keyboardController?.hide()
                        }) {
                            Icon(
                                imageVector = MiuixIcons.Back,
                                contentDescription = stringResource(R.string.a11y_cancel_editing),
                                tint = MiuixTheme.colorScheme.onBackground
                            )
                        }

                        TextField(
                            value = inputUrl,
                            onValueChange = { newQuery: String -> inputUrl = newQuery },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp),
                            backgroundColor = MiuixTheme.colorScheme.surfaceContainerHigh,
                            label = stringResource(R.string.placeholder_enter_url_full),
                            useLabelAsPlaceholder = true,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(
                                onGo = {
                                    onSearch(inputUrl)
                                    isEditingUrl = false
                                }
                            )
                        )

                        IconButton(
                            onClick = {
                                onSearch(inputUrl)
                                isEditingUrl = false
                            },
                            enabled = inputUrl.isNotBlank() && inputUrl != "https://"
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = stringResource(R.string.a11y_load),
                                tint = if (inputUrl.isNotBlank() && inputUrl != "https://") MiuixTheme.colorScheme.onBackground else MiuixTheme.colorScheme.onSurfaceVariantSummary
                            )
                        }
                    }
                }
            } else {
                TopAppBar(
                    title = pageTitle,
                    navigationIcon = {
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
                    },
                    actions = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (enableAddressBarToggleButton || startedEmpty) {
                                IconButton(onClick = {
                                    isEditingUrl = true
                                    val rawUrl = webView.url
                                    inputUrl =
                                        if (rawUrl.isNullOrBlank() || rawUrl == "about:blank") "" else rawUrl
                                    keyboardController?.show()
                                }) {
                                    Icon(
                                        Icons.Default.Link,
                                        contentDescription = stringResource(R.string.a11y_enter_url),
                                        tint = MiuixTheme.colorScheme.onBackground
                                    )
                                }
                            }

                            // 这里利用了 IconButton 的 holdDownState 特性，菜单展开时图标会有细腻的按下状态反馈
                            Box {
                                IconButton(
                                    onClick = { expanded = true },
                                    holdDownState = expanded
                                ) {
                                    Icon(
                                        Icons.Filled.MoreVert,
                                        contentDescription = stringResource(R.string.a11y_more_options),
                                        tint = MiuixTheme.colorScheme.onBackground
                                    )
                                }

                                WindowListPopup(
                                    show = expanded,
                                    onDismissRequest = { expanded = false },
                                    alignment = PopupPositionProvider.Align.BottomEnd
                                ) {
                                    ListPopupColumn {
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(56.dp),
                                            color = Color.Transparent,
                                            onClick = {
                                                webView.reload()
                                                expanded = false
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(horizontal = 20.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Filled.Refresh,
                                                    contentDescription = stringResource(R.string.a11y_refresh),
                                                    tint = MiuixTheme.colorScheme.onSurface
                                                )
                                                Spacer(Modifier.width(12.dp))
                                                Text(
                                                    stringResource(R.string.action_refresh),
                                                    color = MiuixTheme.colorScheme.onSurface
                                                )
                                            }
                                        }

                                        val switchTextId =
                                            if (isDesktopMode) R.string.action_switch_to_phone_mode else R.string.action_switch_to_desktop_mode
                                        val switchIcon =
                                            if (isDesktopMode) Icons.Filled.PhoneAndroid else Icons.Filled.DesktopWindows

                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(56.dp),
                                            color = Color.Transparent,
                                            onClick = {
                                                isDesktopMode = !isDesktopMode

                                                val tText =
                                                    if (isDesktopMode) toastSwitchedToDesktop else toastSwitchedToPhone
                                                Toast.makeText(context, tText, Toast.LENGTH_SHORT)
                                                    .show()

                                                val urlToReload = webView.url
                                                if (!urlToReload.isNullOrBlank() && urlToReload != "about:blank") {
                                                    webView.loadUrl(urlToReload)
                                                } else if (inputUrl.isNotBlank()) {
                                                    currentUrl = inputUrl
                                                } else {
                                                    Toast.makeText(
                                                        context,
                                                        toastUrlEmpty,
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }

                                                expanded = false
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(horizontal = 20.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    switchIcon,
                                                    contentDescription = stringResource(switchTextId),
                                                    tint = MiuixTheme.colorScheme.onSurface
                                                )
                                                Spacer(Modifier.width(12.dp))
                                                Text(
                                                    stringResource(switchTextId),
                                                    color = MiuixTheme.colorScheme.onSurface
                                                )
                                            }
                                        }

                                        if (enableDevToolsOptionInUi) {
                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(56.dp),
                                                color = Color.Transparent,
                                                onClick = {
                                                    isDevToolsEnabled = !isDevToolsEnabled
                                                    WebView.setWebContentsDebuggingEnabled(
                                                        isDevToolsEnabled
                                                    )

                                                    val statusText =
                                                        if (isDevToolsEnabled) statusEnabled else statusDisabled
                                                    Toast.makeText(
                                                        context,
                                                        toastDevToolsEnabledFmt.format(statusText),
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .padding(horizontal = 20.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        Icons.Filled.Build,
                                                        contentDescription = stringResource(R.string.a11y_devtools),
                                                        tint = MiuixTheme.colorScheme.onSurface
                                                    )
                                                    Spacer(Modifier.width(12.dp))
                                                    Text(
                                                        stringResource(R.string.item_devtools_debug),
                                                        modifier = Modifier.weight(1f),
                                                        color = MiuixTheme.colorScheme.onSurface
                                                    )
                                                    Switch(
                                                        checked = isDevToolsEnabled,
                                                        onCheckedChange = {
                                                            isDevToolsEnabled = it
                                                            WebView.setWebContentsDebuggingEnabled(
                                                                isDevToolsEnabled
                                                            )

                                                            val statusText =
                                                                if (isDevToolsEnabled) statusEnabled else statusDisabled
                                                            Toast.makeText(
                                                                context,
                                                                toastDevToolsEnabledFmt.format(
                                                                    statusText
                                                                ),
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                color = MiuixTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.text_import_guide),
                        modifier = Modifier.weight(1f),
                        fontSize = 14.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )

                    Spacer(Modifier.width(12.dp))

                    Button(
                        onClick = {
                            assetJsPath?.let {
                                showCourseTablePicker = true
                            } ?: run {
                                Toast.makeText(context, toastNoManualImport, Toast.LENGTH_LONG)
                                    .show()
                            }
                        },
                        enabled = assetJsPath != null,
                        colors = ButtonDefaults.buttonColorsPrimary()
                    ) {
                        Text(
                            stringResource(R.string.action_execute_import),
                            color = MiuixTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { webView },
                update = {}
            )

            if (loadingProgress < 1.0f) {
                LinearProgressIndicator(
                    progress = loadingProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    colors = ProgressIndicatorDefaults.progressIndicatorColors(
                        foregroundColor = MiuixTheme.colorScheme.primary,
                        backgroundColor = Color.Transparent
                    )
                )
            }

            WebDialogHost(
                webView = webView,
                uiEvents = uiEventChannel.receiveAsFlow()
            )

            if (showCourseTablePicker) {
                CourseTablePickerDialog(
                    show = showCourseTablePicker,
                    title = stringResource(R.string.dialog_title_select_table_for_import),
                    onDismissRequest = { showCourseTablePicker = false },
                    onTableSelected = { selectedTable ->
                        showCourseTablePicker = false
                        assetJsPath?.let { assetPath ->
                            try {
                                androidBridge?.setImportTableId(selectedTable.id)
                                val jsFile =
                                    File(context.filesDir, "repo/schools/resources/$assetPath")
                                if (jsFile.exists()) {
                                    val jsCode = jsFile.readText()
                                    webView.evaluateJavascript(jsCode, null)
                                    Toast.makeText(
                                        context,
                                        toastExecutingImport,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    val msg = toastImportNotFoundFmt.format(jsFile.path)
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                val errMsg =
                                    toastLoadImportFailedFmt.format(e.localizedMessage ?: "")
                                Toast.makeText(context, errMsg, Toast.LENGTH_LONG).show()
                            }
                        } ?: run {
                            Toast.makeText(context, toastNoImportScript, Toast.LENGTH_LONG).show()
                        }
                    }
                )
            }

            sslErrorHandleState?.let { (handler, _) ->
                WindowDialog(
                    show = sslErrorHandleState != null,
                    title = stringResource(R.string.dialog_ssl_error_title),
                    summary = stringResource(R.string.dialog_ssl_error_message),
                    onDismissRequest = {
                        handler.cancel()
                        sslErrorHandleState = null
                    }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Button(
                                onClick = {
                                    handler.cancel()
                                    sslErrorHandleState = null
                                },
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
                                    handler.proceed()
                                    sslErrorHandleState = null
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColorsPrimary()
                              ) {
                                Text(
                                    stringResource(R.string.action_continue_browsing),
                                    color = MiuixTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

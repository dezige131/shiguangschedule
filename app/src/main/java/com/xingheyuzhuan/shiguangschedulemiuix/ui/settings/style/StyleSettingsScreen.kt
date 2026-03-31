package com.xingheyuzhuan.shiguangschedulemiuix.ui.settings.style

import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.xingheyuzhuan.shiguangschedulemiuix.LocalAppNavigator
import com.xingheyuzhuan.shiguangschedulemiuix.R
import com.xingheyuzhuan.shiguangschedulemiuix.data.model.schedule_style.BorderTypeProto
import com.xingheyuzhuan.shiguangschedulemiuix.ui.components.ImageCropper
import com.xingheyuzhuan.shiguangschedulemiuix.ui.schedule.WeeklyScheduleUiState
import com.xingheyuzhuan.shiguangschedulemiuix.ui.schedule.components.ScheduleGrid
import com.xingheyuzhuan.shiguangschedulemiuix.ui.schedule.components.ScheduleGridStyleComposed
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SpinnerEntry
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.preference.WindowDropdownPreference
import top.yukonga.miuix.kmp.preference.WindowSpinnerPreference
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import top.yukonga.miuix.kmp.window.WindowDialog
import kotlin.math.roundToInt

@Composable
fun StyleSettingsScreen(
    viewModel: StyleSettingsViewModel = hiltViewModel()
) {
    val navigator = LocalAppNavigator.current
    val styleState by viewModel.styleState.collectAsStateWithLifecycle()
    val demoUiState by viewModel.demoUiState.collectAsStateWithLifecycle()

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var showColorPicker by remember { mutableStateOf(false) }
    var pickingCategory by remember { mutableIntStateOf(0) }
    var isDarkTarget by remember { mutableStateOf(false) }
    var selectedColorIndex by remember { mutableIntStateOf(0) }
    var currentColorInPicker by remember { mutableStateOf(Color.White) }

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var showCropper by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let {
                selectedUri = it
                showCropper = true
            }
        }

    if (showCropper && selectedUri != null) {
        val screenAspectRatio =
            configuration.screenWidthDp.toFloat() / configuration.screenHeightDp.toFloat()
        ImageCropper(
            uri = selectedUri!!,
            aspectRatio = screenAspectRatio,
            onCropConfirmed = { bitmap ->
                viewModel.saveCroppedWallpaper(context, bitmap)
                showCropper = false
                selectedUri = null
            },
            onDismiss = {
                showCropper = false
                selectedUri = null
            }
        )
    }

    Scaffold(
        containerColor = MiuixTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = stringResource(R.string.item_personalization),
                navigationIcon = {
                    IconButton(onClick = { navigator.removeAt(navigator.size - 1) }) {
                        Icon(
                            MiuixIcons.Back,
                            contentDescription = stringResource(R.string.a11y_back),
                            tint = MiuixTheme.colorScheme.onBackground
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        styleState?.let { currentStyle ->
            val contentModifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()

            val previewContent = @Composable { modifier: Modifier ->
                val density = LocalDensity.current
                val containerSize = LocalWindowInfo.current.containerSize
                val windowWidthDp = with(density) { containerSize.width.toDp() }
                Box(
                    modifier = modifier
                        .background(MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .horizontalScroll(rememberScrollState())
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    awaitPointerEvent()
                                }
                            }
                        }
                ) {
                    Box(modifier = Modifier.requiredWidth(windowWidthDp)) {
                        ScheduleGridContent(currentStyle, demoUiState)
                    }
                }
            }

            if (isLandscape) {
                Row(modifier = contentModifier) {
                    previewContent(Modifier
                        .fillMaxHeight()
                        .weight(0.4f))
                    Spacer(modifier = Modifier.width(8.dp))
                    SettingsListContent(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(0.6f),
                        currentStyle = currentStyle,
                        viewModel = viewModel,
                        onWallpaperClick = {
                            launcher.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                        onPick = { cat, isDark, idx ->
                            pickingCategory = cat; isDarkTarget = isDark; selectedColorIndex = idx
                            currentColorInPicker = if (cat == 1) {
                                if (isDarkTarget) currentStyle.overlapCourseColorDark else currentStyle.overlapCourseColor
                            } else {
                                val pair = currentStyle.courseColorMaps.getOrNull(idx)
                                if (isDarkTarget) pair?.dark ?: Color.Gray else pair?.light
                                    ?: Color.Gray
                            }
                            showColorPicker = true
                        }
                    )
                }
            } else {
                Column(modifier = contentModifier) {
                    previewContent(Modifier
                        .fillMaxWidth()
                        .weight(0.4f))
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsListContent(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.6f),
                        currentStyle = currentStyle,
                        viewModel = viewModel,
                        onWallpaperClick = {
                            launcher.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                        onPick = { cat, isDark, idx ->
                            pickingCategory = cat; isDarkTarget = isDark; selectedColorIndex = idx
                            currentColorInPicker = if (cat == 1) {
                                if (isDarkTarget) currentStyle.overlapCourseColorDark else currentStyle.overlapCourseColor
                            } else {
                                val pair = currentStyle.courseColorMaps.getOrNull(idx)
                                if (isDarkTarget) pair?.dark ?: Color.Gray else pair?.light
                                    ?: Color.Gray
                            }
                            showColorPicker = true
                        }
                    )
                }
            }

            WindowBottomSheet(
                show = showColorPicker,
                title = stringResource(R.string.label_color_picker_title),
                onDismissRequest = { showColorPicker = false },
                startAction = {
                    TextButton(
                        text = stringResource(R.string.action_cancel),
                        onClick = { showColorPicker = false })
                },
                endAction = {
                    TextButton(
                        text = stringResource(R.string.action_confirm),
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                        onClick = {
                            if (pickingCategory == 1) {
                                viewModel.updateOverlapColor(currentColorInPicker, isDarkTarget)
                            } else {
                                viewModel.updatePrimaryColor(
                                    selectedColorIndex,
                                    currentColorInPicker,
                                    isDarkTarget
                                )
                            }
                            showColorPicker = false
                        }
                    )
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    CustomColorPickerPanel(
                        color = currentColorInPicker,
                        onColorChanged = { currentColorInPicker = it },
                        previewContent = { ColorPreviewBox(currentColorInPicker, !isDarkTarget) }
                    )
                }
            }
        } ?: Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) { CircularProgressIndicator() }
    }
}

@Composable
private fun SettingsListContent(
    modifier: Modifier = Modifier,
    currentStyle: ScheduleGridStyleComposed,
    viewModel: StyleSettingsViewModel,
    onWallpaperClick: () -> Unit,
    onPick: (category: Int, isDark: Boolean, index: Int) -> Unit
) {
    var showResetDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    WindowDialog(
        show = showResetDialog,
        title = stringResource(R.string.dialog_reset_title),
        summary = stringResource(R.string.dialog_reset_message),
        onDismissRequest = { showResetDialog = false }
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
                    onClick = { showResetDialog = false },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors()
                ) {
                    Text(
                        stringResource(R.string.action_cancel),
                        color = MiuixTheme.colorScheme.onSurface
                    )
                }
                Button(
                    onClick = { viewModel.resetStyleSettings(); showResetDialog = false },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(color = MiuixTheme.colorScheme.error)
                ) {
                    Text(
                        stringResource(R.string.action_confirm),
                        color = MiuixTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer),
            cornerRadius = 16.dp,
            insideMargin = PaddingValues(0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showResetDialog = true }
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    stringResource(R.string.action_reset_style),
                    fontSize = 16.sp,
                    color = MiuixTheme.colorScheme.error
                )
            }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.style_category_theme),
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 32.dp, bottom = 8.dp)
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer),
                cornerRadius = 16.dp,
                insideMargin = PaddingValues(0.dp)
            ) {
                Column {
                    ThemeModeSelector(currentStyle.colorSchemeMode) {
                        viewModel.updateColorSchemeMode(
                            it
                        )
                    }
                    if (currentStyle.colorSchemeMode == ColorSchemeMode.MonetSystem || currentStyle.colorSchemeMode == ColorSchemeMode.MonetLight || currentStyle.colorSchemeMode == ColorSchemeMode.MonetDark) {
                        CustomDivider()
                        MonetSeedColorSelector(currentStyle.monetSeedColor) {
                            viewModel.updateMonetSeedColor(
                                it
                            )
                        }
                    }
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.label_bottom_bar_style),
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 32.dp, bottom = 8.dp)
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer),
                    cornerRadius = 16.dp,
                    insideMargin = PaddingValues(0.dp)
                ) {
                    Column {
                        // 悬浮底栏开关
                        SwitchPreference(
                            title = stringResource(R.string.label_floating_bottom_bar),
                            checked = currentStyle.enableFloatingBottomBar,
                            onCheckedChange = { viewModel.updateEnableFloatingBottomBar(it) }
                        )

                        // 只有开启了悬浮底栏，才显示/允许开启玻璃效果
                        if (currentStyle.enableFloatingBottomBar) {
                            SwitchPreference(
                                title = stringResource(R.string.label_blur_bottom_bar),
                                checked = currentStyle.enableBottomBarBlur,
                                onCheckedChange = { viewModel.updateEnableBottomBarBlur(it) }
                            )
                        }
                    }
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.style_category_interface),
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 32.dp, bottom = 8.dp)
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer),
                cornerRadius = 16.dp,
                insideMargin = PaddingValues(0.dp)
            ) {
                Column {
                    WallpaperItem(
                        path = currentStyle.backgroundImagePath,
                        onClick = onWallpaperClick,
                        onLongClick = { viewModel.removeWallpaper(context) })
                    SwitchPreference(
                        title = stringResource(R.string.label_hide_section_time),
                        checked = currentStyle.hideSectionTime,
                        onCheckedChange = { viewModel.updateHideSectionTime(it) }
                    )
                    SwitchPreference(
                        title = stringResource(R.string.label_hide_date_under_day),
                        checked = currentStyle.hideDateUnderDay,
                        onCheckedChange = { viewModel.updateHideDateUnderDay(it) }
                    )
                    SwitchPreference(
                        title = stringResource(R.string.label_hide_grid_lines),
                        checked = currentStyle.hideGridLines,
                        onCheckedChange = { viewModel.updateHideGridLines(it) }
                    )
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.style_category_grid_size),
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 32.dp, bottom = 8.dp)
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer),
                cornerRadius = 16.dp,
                insideMargin = PaddingValues(0.dp)
            ) {
                Column {
                    StyleSliderItem(
                        stringResource(R.string.label_section_height),
                        currentStyle.sectionHeight.value,
                        40f..120f
                    ) { viewModel.updateSectionHeight(it) }
                    CustomDivider()
                    StyleSliderItem(
                        stringResource(R.string.label_time_column_width),
                        currentStyle.timeColumnWidth.value,
                        20f..80f
                    ) { viewModel.updateTimeColumnWidth(it) }
                    CustomDivider()
                    StyleSliderItem(
                        stringResource(R.string.label_day_header_height),
                        currentStyle.dayHeaderHeight.value,
                        30f..80f
                    ) { viewModel.updateDayHeaderHeight(it) }
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.style_category_course_block),
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 32.dp, bottom = 8.dp)
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer),
                cornerRadius = 16.dp,
                insideMargin = PaddingValues(0.dp)
            ) {
                Column {
                    SwitchPreference(
                        title = stringResource(R.string.label_show_start_time),
                        checked = currentStyle.showStartTime,
                        onCheckedChange = { viewModel.updateShowStartTime(it) }
                    )
                    SwitchPreference(
                        title = stringResource(R.string.label_hide_location),
                        checked = currentStyle.hideLocation,
                        onCheckedChange = { viewModel.updateHideLocation(it) }
                    )
                    SwitchPreference(
                        title = stringResource(R.string.label_hide_teacher),
                        checked = currentStyle.hideTeacher,
                        onCheckedChange = { viewModel.updateHideTeacher(it) }
                    )
                    SwitchPreference(
                        title = stringResource(R.string.label_remove_location_at),
                        checked = currentStyle.removeLocationAt,
                        onCheckedChange = { viewModel.updateRemoveLocationAt(it) }
                    )
                    SwitchPreference(
                        title = stringResource(R.string.label_text_align_center_h),
                        checked = currentStyle.textAlignCenterHorizontal,
                        onCheckedChange = { viewModel.updateTextAlignCenterHorizontal(it) }
                    )
                    SwitchPreference(
                        title = stringResource(R.string.label_text_align_center_v),
                        checked = currentStyle.textAlignCenterVertical,
                        onCheckedChange = { viewModel.updateTextAlignCenterVertical(it) }
                    )
                    BorderTypeSelector(currentStyle.borderType) { viewModel.updateBorderType(it) }
                    CustomDivider()
                    StyleSliderItem(
                        stringResource(R.string.label_font_scale),
                        currentStyle.fontScale,
                        0.5f..2.0f,
                        0.1f
                    ) { viewModel.updateCourseBlockFontScale(it) }
                    CustomDivider()
                    StyleSliderItem(
                        stringResource(R.string.label_corner_radius),
                        currentStyle.courseBlockCornerRadius.value,
                        0f..24f,
                        1f
                    ) { viewModel.updateCornerRadius(it) }
                    CustomDivider()
                    StyleSliderItem(
                        stringResource(R.string.label_inner_padding),
                        currentStyle.courseBlockInnerPadding.value,
                        0f..12f,
                        1f
                    ) { viewModel.updateInnerPadding(it) }
                    CustomDivider()
                    StyleSliderItem(
                        stringResource(R.string.label_outer_padding),
                        currentStyle.courseBlockOuterPadding.value,
                        0f..8f,
                        1f
                    ) { viewModel.updateOuterPadding(it) }
                    CustomDivider()
                    StyleSliderItem(
                        stringResource(R.string.label_opacity),
                        currentStyle.courseBlockAlpha,
                        0.1f..1f,
                        0.05f
                    ) { viewModel.updateAlpha(it) }
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.style_category_color_scheme),
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 32.dp, bottom = 8.dp)
            )
            ColorSchemeSection(
                stringResource(R.string.title_light_color_pool),
                false,
                currentStyle.courseColorMaps.map { it.light },
                currentStyle.overlapCourseColor,
                { onPick(0, false, it) },
                { onPick(1, false, 0) })
            Spacer(Modifier.height(16.dp))
            ColorSchemeSection(
                stringResource(R.string.title_dark_color_pool),
                true,
                currentStyle.courseColorMaps.map { it.dark },
                currentStyle.overlapCourseColorDark,
                { onPick(0, true, it) },
                { onPick(1, true, 0) })
        }
    }
}

@Composable
private fun ThemeModeSelector(
    currentMode: ColorSchemeMode,
    onModeChange: (ColorSchemeMode) -> Unit
) {
    val modes = listOf(
        ColorSchemeMode.System to stringResource(R.string.theme_mode_system),
        ColorSchemeMode.Light to stringResource(R.string.theme_mode_light),
        ColorSchemeMode.Dark to stringResource(R.string.theme_mode_dark),
        ColorSchemeMode.MonetSystem to stringResource(R.string.theme_mode_monet_system),
        ColorSchemeMode.MonetLight to stringResource(R.string.theme_mode_monet_light),
        ColorSchemeMode.MonetDark to stringResource(R.string.theme_mode_monet_dark)
    )
    val selectedIndex = modes.indexOfFirst { it.first == currentMode }.coerceAtLeast(0)

    WindowSpinnerPreference(
        title = stringResource(R.string.label_theme_color_scheme),
        items = modes.map { SpinnerEntry(title = it.second) },
        selectedIndex = selectedIndex,
        onSelectedIndexChange = { index -> onModeChange(modes[index].first) }
    )
}

@Composable
private fun MonetSeedColorSelector(currentColor: Color?, onColorChange: (Color?) -> Unit) {
    var showColorPicker by remember { mutableStateOf(false) }
    var tempColor by remember { mutableStateOf(currentColor ?: Color.Blue) }

    WindowBottomSheet(
        show = showColorPicker,
        title = stringResource(R.string.label_monet_seed_color_picker),
        onDismissRequest = { showColorPicker = false },
        startAction = {
            TextButton(
                text = stringResource(R.string.action_cancel),
                onClick = { showColorPicker = false })
        },
        endAction = {
            TextButton(
                text = stringResource(R.string.action_confirm),
                colors = ButtonDefaults.textButtonColorsPrimary(),
                onClick = {
                    onColorChange(tempColor)
                    showColorPicker = false
                }
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            CustomColorPickerPanel(
                color = tempColor,
                onColorChanged = { tempColor = it },
                previewContent = { ColorPreviewBox(tempColor, true) }
            )
        }
    }

    val seedColors = listOf(
        null to stringResource(R.string.monet_seed_wallpaper),
        Color.Transparent to stringResource(R.string.monet_seed_custom) // Placeholder for custom
    )

    // If currentColor is not null, it's "Custom"
    val selectedIndex = if (currentColor == null) 0 else 1

    WindowSpinnerPreference(
        title = stringResource(R.string.label_monet_seed_color),
        items = seedColors.map { SpinnerEntry(title = it.second) },
        selectedIndex = selectedIndex,
        onSelectedIndexChange = { index ->
            if (index == 0) {
                onColorChange(null)
            } else {
                showColorPicker = true
            }
        }
    )
}

@Composable
private fun CustomDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(horizontal = 16.dp)
            .background(MiuixTheme.colorScheme.surfaceVariant)
    )
}

@Composable
private fun BorderTypeSelector(
    currentType: BorderTypeProto,
    onTypeChange: (BorderTypeProto) -> Unit
) {
    val types = listOf(
        BorderTypeProto.BORDER_TYPE_NONE to stringResource(R.string.label_none),
        BorderTypeProto.BORDER_TYPE_SOLID to stringResource(R.string.border_type_solid),
        BorderTypeProto.BORDER_TYPE_DASHED to stringResource(R.string.border_type_dashed)
    )
    val selectedIndex = types.indexOfFirst { it.first == currentType }.coerceAtLeast(0)

    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 16.dp)) {
        Text(
            stringResource(R.string.label_border_type),
            fontSize = 16.sp,
            color = MiuixTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        TabRowWithContour(
            tabs = types.map { it.second },
            selectedTabIndex = selectedIndex,
            onTabSelected = { index -> onTypeChange(types[index].first) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// 统一的通用 Slider 也用 Miuix 的 TextField 替换了老的 Material 组件
@Composable
private fun StyleSliderItem(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    stepValue: Float = 1f,
    onValueChange: (Float) -> Unit
) {
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    var showDialog by remember { mutableStateOf(false) }
    val isIntegerStep = stepValue >= 1f
    fun formatValue(v: Float): String = if (isIntegerStep) "${v.toInt()}" else "%.1f".format(v)
    val steps = remember(
        range,
        stepValue
    ) { if (stepValue > 0f) ((range.endInclusive - range.start) / stepValue).toInt() - 1 else 0 }

    var textFieldValue by remember(showDialog) { mutableStateOf(formatValue(value)) }

    WindowDialog(show = showDialog, title = label, onDismissRequest = { showDialog = false }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "${stringResource(R.string.label_range)}: ${formatValue(range.start)} - ${
                    formatValue(
                        range.endInclusive
                    )
                }",
                fontSize = 13.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // 使用 Miuix TextField
            TextField(
                value = textFieldValue,
                onValueChange = { input ->
                    if (isIntegerStep) {
                        if (input.isEmpty() || input.all { it.isDigit() }) textFieldValue = input
                    } else {
                        if (input.isEmpty() || input.all { it.isDigit() || it == '.' }) textFieldValue =
                            input
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = if (isIntegerStep) KeyboardType.Number else KeyboardType.Decimal)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { showDialog = false },
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
                        val newValue = textFieldValue.toFloatOrNull()
                        if (newValue != null) {
                            val clampedValue = newValue.coerceIn(range.start, range.endInclusive)
                            val steppedValue = if (stepValue > 0f) {
                                val count = ((clampedValue - range.start) / stepValue).roundToInt()
                                range.start + count * stepValue
                            } else clampedValue
                            currentOnValueChange(steppedValue)
                            showDialog = false
                        }
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

    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontSize = 16.sp, color = MiuixTheme.colorScheme.onSurface)
            Box(modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable { showDialog = true }
                .padding(horizontal = 4.dp, vertical = 2.dp)) {
                Text(
                    text = formatValue(value),
                    color = MiuixTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
        Slider(
            value = value,
            onValueChange = currentOnValueChange,
            valueRange = range,
            steps = if (steps > 0) steps else 0,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun WallpaperItem(path: String, onClick: () -> Unit, onLongClick: () -> Unit) {
    val hasWallpaper = path.isNotEmpty()
    ArrowPreference(
        title = stringResource(R.string.label_wallpaper),
        summary = if (hasWallpaper) stringResource(R.string.desc_wallpaper_set) else stringResource(
            R.string.desc_wallpaper_unset
        ),
        startAction = {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = null,
                tint = if (hasWallpaper) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantActions,
                modifier = Modifier.size(24.dp)
            )
        },
        modifier = Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        )
    )
}

@Composable
private fun ColorSchemeSection(
    title: String,
    isDarkSection: Boolean,
    colors: List<Color>,
    conflictColor: Color,
    onEditColor: (Int) -> Unit,
    onEditConflict: () -> Unit
) {
    val contentColor = if (isDarkSection) Color.White else Color.Black
    val bgColor = if (isDarkSection) Color(0xFF242424) else Color(0xFFF7F7F7)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.defaultColors(color = bgColor),
        cornerRadius = 16.dp,
        insideMargin = PaddingValues(0.dp)
    ) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)) {
            Text(title, fontSize = 16.sp, color = contentColor, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(conflictColor)
                            .border(2.dp, contentColor.copy(0.3f), CircleShape)
                            .clickable { onEditConflict() })
                    Text(
                        stringResource(R.string.label_color_overlap),
                        fontSize = 12.sp,
                        color = contentColor.copy(0.6f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                VerticalDivider(
                    modifier = Modifier
                        .height(40.dp)
                        .padding(horizontal = 12.dp),
                    color = contentColor.copy(0.1f)
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    colors.forEachIndexed { index, color ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { onEditColor(index) }); Text(
                            "${index + 1}",
                            fontSize = 12.sp,
                            color = contentColor.copy(0.6f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------
// 彻底重构防 Bug 互串的自定义颜色拾取面板
// ------------------------------------------------------------------------------------------------
@Composable
private fun CustomColorPickerPanel(
    color: Color,
    onColorChanged: (Color) -> Unit,
    previewContent: @Composable () -> Unit
) {
    val currentOnColorChanged by rememberUpdatedState(onColorChanged)
    var colorMode by remember { mutableIntStateOf(0) } // 0: RGB, 1: HSV

    // 分离存储所有状态，防止拖动时相互覆盖和极端值(0)导致的数据坍塌
    var currentR by remember { mutableFloatStateOf(color.red) }
    var currentG by remember { mutableFloatStateOf(color.green) }
    var currentB by remember { mutableFloatStateOf(color.blue) }
    var currentA by remember { mutableFloatStateOf(color.alpha) }

    val initHsv = remember { colorToHsv(color) }
    var currentH by remember { mutableFloatStateOf(initHsv[0]) }
    var currentS by remember { mutableFloatStateOf(initHsv[1]) }
    var currentV by remember { mutableFloatStateOf(initHsv[2]) }

    var lastExternalColor by remember { mutableStateOf(color) }

    val derivedRgb = Color(currentR, currentG, currentB, currentA)
    val derivedHsv = hsvToColor(currentH, currentS, currentV, currentA)
    val derivedColor = if (colorMode == 0) derivedRgb else derivedHsv

    // 仅当颜色变化来自于面板外部（例如切了另外一个块的颜色）时才更新组件的数值，防止自己的循环重置
    if (color != lastExternalColor && color != derivedColor) {
        currentR = color.red
        currentG = color.green
        currentB = color.blue
        currentA = color.alpha

        val hsv = colorToHsv(color)
        currentH = hsv[0]
        currentS = hsv[1]
        currentV = hsv[2]
    }
    lastExternalColor = color

    fun updateColor(newColor: Color) {
        currentOnColorChanged(newColor)
    }

    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)) {
        previewContent()
        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer),
            cornerRadius = 16.dp,
            insideMargin = PaddingValues(0.dp)
        ) {
            WindowDropdownPreference(
                title = stringResource(R.string.label_color_space_mode),
                items = listOf(
                    stringResource(R.string.label_rgb_mode),
                    stringResource(R.string.label_hsv_mode)
                ),
                selectedIndex = colorMode,
                onSelectedIndexChange = { colorMode = it }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (colorMode == 0) { // RGB
            CustomColorSliderItem(
                label = stringResource(R.string.label_red_short),
                value = currentR * 255f,
                range = 0f..255f,
                drawBrushColors = listOf(
                    Color(0f, currentG, currentB, currentA),
                    Color(1f, currentG, currentB, currentA)
                )
            ) {
                currentR = it / 255f
                val c = Color(currentR, currentG, currentB, currentA)
                val hsv = colorToHsv(c); currentH = hsv[0]; currentS = hsv[1]; currentV = hsv[2]
                updateColor(c)
            }
            CustomColorSliderItem(
                label = stringResource(R.string.label_green_short),
                value = currentG * 255f,
                range = 0f..255f,
                drawBrushColors = listOf(
                    Color(currentR, 0f, currentB, currentA),
                    Color(currentR, 1f, currentB, currentA)
                )
            ) {
                currentG = it / 255f
                val c = Color(currentR, currentG, currentB, currentA)
                val hsv = colorToHsv(c); currentH = hsv[0]; currentS = hsv[1]; currentV = hsv[2]
                updateColor(c)
            }
            CustomColorSliderItem(
                label = stringResource(R.string.label_blue_short),
                value = currentB * 255f,
                range = 0f..255f,
                drawBrushColors = listOf(
                    Color(currentR, currentG, 0f, currentA),
                    Color(currentR, currentG, 1f, currentA)
                )
            ) {
                currentB = it / 255f
                val c = Color(currentR, currentG, currentB, currentA)
                val hsv = colorToHsv(c); currentH = hsv[0]; currentS = hsv[1]; currentV = hsv[2]
                updateColor(c)
            }
        } else { // HSV
            val hueColors = listOf(
                Color.Red,
                Color.Yellow,
                Color.Green,
                Color.Cyan,
                Color.Blue,
                Color.Magenta,
                Color.Red
            )
            CustomColorSliderItem(
                label = stringResource(R.string.label_hue_short),
                value = currentH,
                range = 0f..360f,
                drawBrushColors = hueColors
            ) {
                currentH = it
                val c = hsvToColor(currentH, currentS, currentV, currentA)
                currentR = c.red; currentG = c.green; currentB = c.blue
                updateColor(c)
            }

            val satColors =
                listOf(hsvToColor(currentH, 0f, currentV), hsvToColor(currentH, 1f, currentV))
            CustomColorSliderItem(
                label = stringResource(R.string.label_saturation_short),
                value = currentS * 100f,
                range = 0f..100f,
                drawBrushColors = satColors
            ) {
                currentS = it / 100f
                val c = hsvToColor(currentH, currentS, currentV, currentA)
                currentR = c.red; currentG = c.green; currentB = c.blue
                updateColor(c)
            }

            val valColors = listOf(Color.Black, hsvToColor(currentH, currentS, 1f))
            CustomColorSliderItem(
                label = stringResource(R.string.label_value_short),
                value = currentV * 100f,
                range = 0f..100f,
                drawBrushColors = valColors
            ) {
                currentV = it / 100f
                val c = hsvToColor(currentH, currentS, currentV, currentA)
                currentR = c.red; currentG = c.green; currentB = c.blue
                updateColor(c)
            }
        }
    }
}

// 修复了拖动丢失和互相牵连 bug，并且完美运用了 Miuix 的 TextField 样式。
@Composable
private fun CustomColorSliderItem(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    drawBrushColors: List<Color>,
    onValueChange: (Float) -> Unit
) {
    // 核心修复点：动态捕捉函数！避免了由 Compose state 捕获导致的重置回原位的 Bug
    val currentOnValueChange by rememberUpdatedState(onValueChange)

    var showDialog by remember { mutableStateOf(false) }
    var textFieldValue by remember(showDialog) { mutableStateOf(value.toInt().toString()) }

    WindowDialog(
        show = showDialog,
        title = stringResource(R.string.title_modify_item, label),
        onDismissRequest = { showDialog = false }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "${stringResource(R.string.label_range)}: ${range.start.toInt()} - ${range.endInclusive.toInt()}",
                fontSize = 13.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // 使用 Miuix 的原生 TextField 替代了老版的输入框
            TextField(
                value = textFieldValue,
                onValueChange = {
                    if (it.isEmpty() || it.all { char -> char.isDigit() }) textFieldValue = it
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { showDialog = false },
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
                        val v = textFieldValue.toFloatOrNull()
                        if (v != null) {
                            currentOnValueChange(v.coerceIn(range))
                            showDialog = false
                        }
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

    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontSize = 16.sp, color = MiuixTheme.colorScheme.onSurface)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { showDialog = true }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = value.toInt().toString(),
                    color = MiuixTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        val density = LocalDensity.current
        var sliderWidthPx by remember { mutableFloatStateOf(0f) }
        val sliderHeightDp = 26.dp
        val indicatorSizeDp = 20.dp
        var dragOffset by remember { mutableFloatStateOf(0f) }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .height(sliderHeightDp)
                .clip(CircleShape)
                .drawWithCache {
                    val widthPx = size.width
                    val halfHeightPx = sliderHeightDp.toPx() / 2f
                    val brush = Brush.horizontalGradient(
                        colors = drawBrushColors,
                        startX = halfHeightPx,
                        endX = widthPx - halfHeightPx,
                        tileMode = TileMode.Clamp
                    )
                    onDrawBehind {
                        drawRect(brush)
                        drawRect(Color.Gray.copy(0.1f), style = Stroke(0.5.dp.toPx()))
                    }
                }
                .onSizeChanged { sliderWidthPx = it.width.toFloat() }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = { offset ->
                            val sliderHeightPx = sliderHeightDp.toPx()
                            val effectiveWidth = sliderWidthPx - sliderHeightPx
                            val fraction =
                                ((offset.x - sliderHeightPx / 2) / effectiveWidth).coerceIn(0f, 1f)
                            currentOnValueChange(range.start + fraction * (range.endInclusive - range.start))
                            dragOffset = offset.x
                        }
                    )
                }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        dragOffset += delta
                        val sliderHeightPx = with(density) { sliderHeightDp.toPx() }
                        val effectiveWidth = sliderWidthPx - sliderHeightPx
                        val fraction =
                            ((dragOffset - sliderHeightPx / 2) / effectiveWidth).coerceIn(0f, 1f)
                        currentOnValueChange(range.start + fraction * (range.endInclusive - range.start))
                    },
                    onDragStarted = { offset -> dragOffset = offset.x }
                )
        ) {
            val fraction =
                if (range.endInclusive == range.start) 0f else (value - range.start) / (range.endInclusive - range.start)
            val sliderHeightPx = with(density) { sliderHeightDp.toPx() }
            val indicatorOffsetX = with(density) {
                val effectiveWidth = maxWidth.toPx() - sliderHeightPx
                (fraction * effectiveWidth + sliderHeightPx / 2).toDp() - (indicatorSizeDp / 2)
            }

            Box(
                modifier = Modifier
                    .offset(x = indicatorOffsetX, y = (sliderHeightDp - indicatorSizeDp) / 2)
                    .size(indicatorSizeDp)
                    .drawWithCache {
                        val strokeWidth = 6.dp.toPx()
                        val halfStroke = strokeWidth / 2f
                        val glowSpread = 2.dp.toPx()
                        val ringCenter = (size.minDimension / 2f) - halfStroke
                        val gradRadius = ringCenter + halfStroke + glowSpread
                        val glowBrush = Brush.radialGradient(
                            colorStops = arrayOf(
                                ((ringCenter - halfStroke - glowSpread).coerceAtLeast(0f) / gradRadius) to Color.Transparent,
                                ((ringCenter - halfStroke) / gradRadius) to Color.Black.copy(0.25f),
                                ((ringCenter + halfStroke) / gradRadius) to Color.Black.copy(0.25f),
                                1f to Color.Transparent
                            ),
                            radius = gradRadius
                        )
                        onDrawBehind {
                            drawCircle(glowBrush, gradRadius)
                            drawCircle(Color.White, ringCenter, style = Stroke(strokeWidth))
                        }
                    }
            )
        }
    }
}

// 独立的 HSV <-> KMP Color 转换器
private fun colorToHsv(color: Color): FloatArray {
    val r = color.red
    val g = color.green
    val b = color.blue
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min
    val h = when {
        delta == 0f -> 0f
        max == r -> 60f * (((g - b) / delta) % 6f)
        max == g -> 60f * (((b - r) / delta) + 2f)
        else -> 60f * (((r - g) / delta) + 4f)
    }
    val s = if (max == 0f) 0f else delta / max
    val v = max
    return floatArrayOf((h + 360f) % 360f, s, v)
}

private fun hsvToColor(h: Float, s: Float, v: Float, alpha: Float = 1f): Color {
    val c = v * s
    val x = c * (1f - kotlin.math.abs((h / 60f) % 2f - 1f))
    val m = v - c
    val (r, g, b) = when ((h % 360f) / 60f) {
        in 0f..1f -> Triple(c, x, 0f)
        in 1f..2f -> Triple(x, c, 0f)
        in 2f..3f -> Triple(0f, c, x)
        in 3f..4f -> Triple(0f, x, c)
        in 4f..5f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(r + m, g + m, b + m, alpha)
}

// ------------------------------------------------------------------------------------------------
// 主页面其他静态设置组件代码
// ------------------------------------------------------------------------------------------------

@Composable
private fun ColorPreviewBox(color: Color, isLightModeUI: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(color), contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isLightModeUI) stringResource(R.string.preview_light_mode) else stringResource(
                R.string.preview_dark_mode
            ),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = if (isLightModeUI) Color.Black else Color.White
        )
    }
}

@Composable
private fun ScheduleGridContent(
    style: ScheduleGridStyleComposed,
    demoUiState: WeeklyScheduleUiState
) {
    val today = remember { java.time.LocalDate.now() }
    val localDates = remember(demoUiState.firstDayOfWeek) {
        val dayOfWeekStart = java.time.DayOfWeek.of(demoUiState.firstDayOfWeek);
        val startOfWeek =
            today.with(java.time.temporal.TemporalAdjusters.previousOrSame(dayOfWeekStart)); (0..6).map {
        startOfWeek.plusDays(
            it.toLong()
        )
    }
    }
    val currentYearString = remember(today) { today.year.toString() }
    val dummyDates = remember(localDates) {
        val formatter = java.time.format.DateTimeFormatter.ofPattern("MM/dd"); localDates.map {
        it.format(
            formatter
        )
    }
    }
    val dynamicTodayIndex = remember(localDates) { localDates.indexOf(today) }
    val defaultBackgroundColor = MiuixTheme.colorScheme.surfaceContainer
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (style.backgroundImagePath.isNotEmpty()) Color.Transparent
                else defaultBackgroundColor
            )
    ) {
        if (style.backgroundImagePath.isNotEmpty()) AsyncImage(
            model = style.backgroundImagePath,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter
        )
        ScheduleGrid(
            style = style,
            dates = dummyDates,
            currentYear = currentYearString,
            timeSlots = demoUiState.timeSlots,
            mergedCourses = demoUiState.currentMergedCourses,
            showWeekends = demoUiState.showWeekends,
            todayIndex = dynamicTodayIndex,
            firstDayOfWeek = demoUiState.firstDayOfWeek,
            onCourseBlockClicked = { },
            onGridCellClicked = { _, _ -> },
            onTimeSlotClicked = { })
    }
}
package com.xingheyuzhuan.shiguangschedule.ui.settings.style

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.ui.components.AdvancedColorPicker
import com.xingheyuzhuan.shiguangschedule.ui.components.ColorPickerConfig
import com.xingheyuzhuan.shiguangschedule.ui.schedule.WeeklyScheduleUiState
import com.xingheyuzhuan.shiguangschedule.ui.schedule.components.ScheduleGrid
import com.xingheyuzhuan.shiguangschedule.ui.schedule.components.ScheduleGridStyleComposed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StyleSettingsScreen(
    navController: NavController,
    viewModel: StyleSettingsViewModel = hiltViewModel()
) {
    val styleState by viewModel.styleState.collectAsStateWithLifecycle()
    val demoUiState by viewModel.demoUiState.collectAsStateWithLifecycle()

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var showColorPicker by remember { mutableStateOf(false) }
    var pickingCategory by remember { mutableIntStateOf(0) }
    var isDarkTarget by remember { mutableStateOf(false) }
    var selectedColorIndex by remember { mutableIntStateOf(0) }

    val sheetState = rememberModalBottomSheetState()

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var showCropper by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let {
            selectedUri = it
            showCropper = true
        }
    }

    if (showCropper && selectedUri != null) {
        WallpaperCropper(
            uri = selectedUri!!,
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
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.item_personalization), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.a11y_back))
                    }
                }
            )
        }
    ) { paddingValues ->
        styleState?.let { currentStyle ->
            val contentModifier = Modifier.padding(paddingValues).fillMaxSize()

            val previewContent = @Composable { modifier: Modifier ->
                val density = LocalDensity.current
                val containerSize = LocalWindowInfo.current.containerSize
                val windowWidthDp = with(density) { containerSize.width.toDp() }
                Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)).horizontalScroll(rememberScrollState()).pointerInput(Unit) { awaitPointerEventScope { while (true) { awaitPointerEvent() } } }) {
                    Box(modifier = Modifier.requiredWidth(windowWidthDp)) {
                        ScheduleGridContent(currentStyle, demoUiState)
                    }
                }
            }

            if (isLandscape) {
                Row(modifier = contentModifier) {
                    previewContent(Modifier.fillMaxHeight().weight(0.4f))
                    Card(modifier = Modifier.fillMaxHeight().weight(0.6f), shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)) {
                        SettingsListContent(
                            currentStyle = currentStyle,
                            viewModel = viewModel,
                            onWallpaperClick = { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) } // 👈 增加这个
                        ) { cat, isDark, idx ->
                            pickingCategory = cat; isDarkTarget = isDark; selectedColorIndex = idx; showColorPicker = true
                        }
                    }
                }
            } else {
                Column(modifier = contentModifier) {
                    previewContent(Modifier.fillMaxWidth().weight(0.45f))
                    Card(modifier = Modifier.fillMaxWidth().weight(0.55f), shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
                        SettingsListContent(
                            currentStyle = currentStyle,
                            viewModel = viewModel,
                            onWallpaperClick = { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) } // 👈 增加这个
                        ) { cat, isDark, idx ->
                            pickingCategory = cat; isDarkTarget = isDark; selectedColorIndex = idx; showColorPicker = true
                        }
                    }
                }
            }

            if (showColorPicker) {
                ModalBottomSheet(onDismissRequest = { showColorPicker = false }, sheetState = sheetState) {
                    val initialColor = if (pickingCategory == 1) {
                        if (isDarkTarget) currentStyle.conflictCourseColorDark else currentStyle.conflictCourseColor
                    } else {
                        val pair = currentStyle.courseColorMaps.getOrNull(selectedColorIndex)
                        if (isDarkTarget) pair?.dark ?: Color.Gray else pair?.light ?: Color.Gray
                    }

                    var currentColorInPicker by remember { mutableStateOf(initialColor) }

                    AdvancedColorPicker(
                        initialColor = initialColor,
                        config = ColorPickerConfig(showAlpha = false),
                        onColorChanged = { newColor ->
                            currentColorInPicker = newColor
                            if (pickingCategory == 1) {
                                viewModel.updateConflictColor(newColor, isDarkTarget)
                            } else {
                                viewModel.updatePrimaryColor(selectedColorIndex, newColor, isDarkTarget)
                            }
                        },
                        previewContent = {
                            ColorPreviewBox(currentColorInPicker, !isDarkTarget)
                        }
                    )
                    Spacer(modifier = Modifier.navigationBarsPadding())
                }
            }
        } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    }
}

@Composable
private fun SettingsListContent(
    currentStyle: ScheduleGridStyleComposed,
    viewModel: StyleSettingsViewModel,
    onWallpaperClick: () -> Unit,
    onPick: (category: Int, isDark: Boolean, index: Int) -> Unit
) {
    var showResetDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.dialog_reset_title)) },
            text = { Text(stringResource(R.string.dialog_reset_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetStyleSettings()
                    showResetDialog = false
                }) { Text(stringResource(R.string.action_confirm), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        OutlinedButton(
            onClick = { showResetDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
        ) {
            Text(stringResource(R.string.action_reset_style))
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        Text(stringResource(R.string.style_category_interface), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        WallpaperItem(
            path = currentStyle.backgroundImagePath,
            onClick = onWallpaperClick,
            onLongClick = { viewModel.removeWallpaper(context) }
        )
        StyleSwitchItem(stringResource(R.string.label_hide_section_time), currentStyle.hideSectionTime) { viewModel.updateHideSectionTime(it) }
        StyleSwitchItem(stringResource(R.string.label_hide_date_under_day), currentStyle.hideDateUnderDay) { viewModel.updateHideDateUnderDay(it) }
        StyleSwitchItem(label = stringResource(R.string.label_hide_grid_lines), checked = currentStyle.hideGridLines) { viewModel.updateHideGridLines(it) }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        Text(stringResource(R.string.style_category_grid_size), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        StyleSliderItem(stringResource(R.string.label_section_height), currentStyle.sectionHeight.value, 40f..120f) { viewModel.updateSectionHeight(it) }
        StyleSliderItem(stringResource(R.string.label_time_column_width), currentStyle.timeColumnWidth.value, 20f..80f) { viewModel.updateTimeColumnWidth(it) }
        StyleSliderItem(stringResource(R.string.label_day_header_height), currentStyle.dayHeaderHeight.value, 30f..80f) { viewModel.updateDayHeaderHeight(it) }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        Text(stringResource(R.string.style_category_course_block), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        StyleSwitchItem(stringResource(R.string.label_show_start_time), currentStyle.showStartTime) { viewModel.updateShowStartTime(it) }
        StyleSwitchItem(stringResource(R.string.label_hide_location), currentStyle.hideLocation) { viewModel.updateHideLocation(it) }
        StyleSwitchItem(stringResource(R.string.label_hide_teacher), currentStyle.hideTeacher) { viewModel.updateHideTeacher(it) }
        StyleSwitchItem(stringResource(R.string.label_remove_location_at), currentStyle.removeLocationAt) { viewModel.updateRemoveLocationAt(it) }
        StyleSliderItem(stringResource(R.string.label_font_scale), currentStyle.fontScale, 0.5f..2.0f) { viewModel.updateCourseBlockFontScale(it) }
        StyleSliderItem(stringResource(R.string.label_corner_radius), currentStyle.courseBlockCornerRadius.value, 0f..24f) { viewModel.updateCornerRadius(it) }
        StyleSliderItem(stringResource(R.string.label_inner_padding), currentStyle.courseBlockInnerPadding.value, 0f..12f) { viewModel.updateInnerPadding(it) }
        StyleSliderItem(stringResource(R.string.label_outer_padding), currentStyle.courseBlockOuterPadding.value, 0f..8f) { viewModel.updateOuterPadding(it) }
        StyleSliderItem(stringResource(R.string.label_opacity), currentStyle.courseBlockAlpha, 0.1f..1f) { viewModel.updateAlpha(it) }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        Text(stringResource(R.string.style_category_color_scheme), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)

        ColorSchemeSection(
            title = stringResource(R.string.title_light_color_pool),
            bgColor = lightColorScheme().surfaceContainerLow,
            isDarkSection = false,
            colors = currentStyle.courseColorMaps.map { it.light },
            conflictColor = currentStyle.conflictCourseColor,
            onEditColor = { onPick(0, false, it) },
            onEditConflict = { onPick(1, false, 0) }
        )

        ColorSchemeSection(
            title = stringResource(R.string.title_dark_color_pool),
            bgColor = darkColorScheme().surfaceContainerLow,
            isDarkSection = true,
            colors = currentStyle.courseColorMaps.map { it.dark },
            conflictColor = currentStyle.conflictCourseColorDark,
            onEditColor = { onPick(0, true, it) },
            onEditConflict = { onPick(1, true, 0) }
        )
    }
}

@Composable
private fun WallpaperCropper(
    uri: Uri,
    onCropConfirmed: (Bitmap) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var containerSize by remember { mutableStateOf(Size.Zero) }

    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val windowSize = windowInfo.containerSize
    val screenAspectRatio = windowSize.width.toFloat() / windowSize.height.toFloat()

    LaunchedEffect(uri) {
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use {
                    bitmap = BitmapFactory.decodeStream(it)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    BackHandler { onDismiss() }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black).zIndex(10f),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            var scale by remember { mutableFloatStateOf(1f) }
            var offset by remember { mutableStateOf(Offset.Zero) }

            BoxWithConstraints(
                modifier = Modifier.fillMaxSize().onGloballyPositioned { containerSize = it.size.toSize() }
            ) {
                val cw = constraints.maxWidth.toFloat()
                val ch = constraints.maxHeight.toFloat()

                // 优化裁剪框大小计算：缩小宽度占比，并居中显示，避开状态栏
                val buttonAreaHeight = with(density) { 120.dp.toPx() }
                val topPadding = with(density) { 60.dp.toPx() } // 增加顶部预留空间，避开状态栏
                val availableHeight = ch - buttonAreaHeight - topPadding

                // 裁剪框宽度设为屏幕宽度的 75%，更精致
                val cropWidthCandidate = cw * 0.75f
                val cropHeightCandidate = cropWidthCandidate / screenAspectRatio

                val (cropWidth, cropHeight) = if (cropHeightCandidate > availableHeight) {
                    val h = availableHeight * 0.85f
                    Pair(h * screenAspectRatio, h)
                } else {
                    Pair(cropWidthCandidate, cropHeightCandidate)
                }

                // 裁剪框居中在 availableHeight 区域内，且整体向下偏移 topPadding
                val cropRect = Rect(
                    left = (cw - cropWidth) / 2,
                    top = topPadding + (availableHeight - cropHeight) / 2,
                    right = (cw + cropWidth) / 2,
                    bottom = topPadding + (availableHeight + cropHeight) / 2
                )

                // 初始缩放确保填满裁剪框
                LaunchedEffect(bitmap, containerSize) {
                    if (bitmap != null && containerSize != Size.Zero) {
                        val imgW = bitmap!!.width.toFloat()
                        val imgH = bitmap!!.height.toFloat()
                        val scaleW = cropWidth / imgW
                        val scaleH = cropHeight / imgH
                        // 使用 max 确保图片至少有一边填满，另一边超出，从而完全覆盖裁剪框
                        scale = max(scaleW, scaleH)

                        // 初始位置使图片在裁剪框内居中
                        val initialImgW = imgW * scale
                        val initialImgH = imgH * scale
                        val centerX = cropRect.left + cropWidth / 2
                        val centerY = cropRect.top + cropHeight / 2

                        offset = Offset(centerX - cw / 2, centerY - ch / 2)
                    }
                }

                fun constrainOffset(newOffset: Offset, newScale: Float): Offset {
                    val imgW = bitmap!!.width * newScale
                    val imgH = bitmap!!.height * newScale

                    // 计算允许的偏移范围。由于浮点数精度问题，使用 min/max 确保 range 不会为空
                    val minX = cropRect.right - (cw + imgW) / 2
                    val maxX = cropRect.left - (cw - imgW) / 2
                    val minY = cropRect.bottom - (ch + imgH) / 2
                    val maxY = cropRect.top - (ch - imgH) / 2

                    return Offset(
                        newOffset.x.coerceIn(min(minX, maxX), max(minX, maxX)),
                        newOffset.y.coerceIn(min(minY, maxY), max(minY, maxY))
                    )
                }

                Box(
                    modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val minScale = max(cropWidth / bitmap!!.width, cropHeight / bitmap!!.height)
                            val newScale = (scale * zoom).coerceAtLeast(minScale)
                            scale = newScale
                            offset = constrainOffset(offset + pan, newScale)
                        }
                    }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val imgW = bitmap!!.width * scale
                        val imgH = bitmap!!.height * scale
                        val startX = (cw - imgW) / 2 + offset.x
                        val startY = (ch - imgH) / 2 + offset.y

                        drawImage(
                            image = bitmap!!.asImageBitmap(),
                            dstOffset = IntOffset(startX.roundToInt(), startY.roundToInt()),
                            dstSize = IntSize(imgW.roundToInt(), imgH.roundToInt())
                        )
                    }

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val path = androidx.compose.ui.graphics.Path().apply { addRect(cropRect) }
                        clipPath(path, clipOp = ClipOp.Difference) {
                            drawRect(Color.Black.copy(alpha = 0.75f))
                        }
                    }

                    Box(
                        modifier = Modifier.offset { IntOffset(cropRect.left.roundToInt(), cropRect.top.roundToInt()) }
                            .size(with(density) { cropWidth.toDp() }, with(density) { cropHeight.toDp() })
                            .border(1.5.dp, Color.White.copy(alpha = 0.8f))
                    )
                }

                Box(
                    modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(bottom = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = {
                            val imgW = bitmap!!.width * scale
                            val imgH = bitmap!!.height * scale
                            val startX = (cw - imgW) / 2 + offset.x
                            val startY = (ch - imgH) / 2 + offset.y

                            val relativeX = (cropRect.left - startX) / scale
                            val relativeY = (cropRect.top - startY) / scale
                            val relativeW = cropWidth / scale
                            val relativeH = cropHeight / scale

                            val cropped = Bitmap.createBitmap(
                                bitmap!!,
                                relativeX.roundToInt().coerceIn(0, bitmap!!.width - 1),
                                relativeY.roundToInt().coerceIn(0, bitmap!!.height - 1),
                                relativeW.roundToInt().coerceAtMost(bitmap!!.width - relativeX.roundToInt()),
                                relativeH.roundToInt().coerceAtMost(bitmap!!.height - relativeY.roundToInt())
                            )
                            onCropConfirmed(cropped)
                        },
                        shape = RoundedCornerShape(24.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                    ) {
                        Text(stringResource(R.string.action_confirm_crop))
                    }
                }
            }
        } else {
            CircularProgressIndicator(color = Color.White)
        }
    }
}

@Composable
private fun ColorSchemeSection(
    title: String,
    bgColor: Color,
    isDarkSection: Boolean,
    colors: List<Color>,
    conflictColor: Color,
    onEditColor: (Int) -> Unit,
    onEditConflict: () -> Unit
) {
    val contentColor = if (isDarkSection) Color.White else Color.Black

    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(bgColor).padding(16.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = contentColor)
        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(conflictColor).border(2.dp, contentColor.copy(0.3f), CircleShape).clickable { onEditConflict() })
                Text(stringResource(R.string.label_color_conflict), style = MaterialTheme.typography.labelSmall, color = contentColor.copy(0.6f), modifier = Modifier.padding(top = 4.dp))
            }

            VerticalDivider(modifier = Modifier.height(40.dp).padding(horizontal = 12.dp), color = contentColor.copy(0.1f))

            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                colors.forEachIndexed { index, color ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(color).clickable { onEditColor(index) })
                        Text("${index + 1}", style = MaterialTheme.typography.labelSmall, color = contentColor.copy(0.6f), modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorPreviewBox(color: Color, isLightModeUI: Boolean) {
    Box(modifier = Modifier.fillMaxWidth().height(100.dp).padding(horizontal = 16.dp).clip(RoundedCornerShape(16.dp)).background(color), contentAlignment = Alignment.Center) {
        Text(
            text = if (isLightModeUI) stringResource(R.string.preview_light_mode) else stringResource(R.string.preview_dark_mode),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = if (isLightModeUI) lightColorScheme().onSurface else Color.White
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
        val dayOfWeekStart = java.time.DayOfWeek.of(demoUiState.firstDayOfWeek)
        val startOfWeek = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(dayOfWeekStart))
        (0..6).map { startOfWeek.plusDays(it.toLong()) }
    }
    val dummyDates = remember(localDates) {
        val formatter = java.time.format.DateTimeFormatter.ofPattern("MM/dd")
        localDates.map { it.format(formatter) }
    }
    val dynamicTodayIndex = remember(localDates) { localDates.indexOf(today) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (style.backgroundImagePath.isNotEmpty()) {
            AsyncImage(
                model = style.backgroundImagePath,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter
            )
        }
        ScheduleGrid(
            style = style,
            dates = dummyDates,
            timeSlots = demoUiState.timeSlots,
            mergedCourses = demoUiState.currentMergedCourses,
            showWeekends = demoUiState.showWeekends,
            todayIndex = dynamicTodayIndex,
            firstDayOfWeek = demoUiState.firstDayOfWeek,
            onCourseBlockClicked = { },
            onGridCellClicked = { _, _ -> },
            onTimeSlotClicked = { }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StyleSliderItem(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = if (value < 100f && value > 0.01f) "%.2f".format(value) else "${value.toInt()}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.height(32.dp),
            thumb = {
                Surface(
                    modifier = Modifier.size(16.dp),
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 1.dp,
                    border = BorderStroke(0.5.dp, Color.LightGray.copy(alpha = 0.5f))
                ) {}
            },
            track = { sliderState ->
                Box(
                    modifier = Modifier.fillMaxWidth().height(22.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.CenterStart
                ) {
                    SliderDefaults.Track(
                        sliderState = sliderState,
                        modifier = Modifier.fillMaxWidth().height(22.dp),
                        colors = SliderDefaults.colors(
                            activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            inactiveTrackColor = Color.Transparent
                        ),
                        thumbTrackGapSize = 0.dp,
                        trackInsideCornerSize = 0.dp
                    )
                }
            }
        )
    }
}

@Composable
private fun StyleSwitchItem(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            thumbContent = if (checked) {
                { Icon(modifier = Modifier.size(SwitchDefaults.IconSize), imageVector = Icons.Filled.Check, contentDescription = null) }
            } else null
        )
    }
}

@Composable
private fun WallpaperItem(
    path: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val hasWallpaper = path.isNotEmpty()

    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.label_wallpaper), style = MaterialTheme.typography.bodyMedium)
            Text(
                text = if (hasWallpaper) stringResource(R.string.desc_wallpaper_set)
                else stringResource(R.string.desc_wallpaper_unset),
                style = MaterialTheme.typography.labelSmall,
                color = if (hasWallpaper) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.Default.Image,
            contentDescription = null,
            tint = if (hasWallpaper) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
    }
}
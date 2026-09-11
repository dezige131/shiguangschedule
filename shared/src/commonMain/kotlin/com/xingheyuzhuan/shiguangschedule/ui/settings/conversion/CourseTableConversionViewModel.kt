package com.xingheyuzhuan.shiguangschedule.ui.settings.conversion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xingheyuzhuan.shiguangschedule.data.model.CourseImportExport
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseConversionRepository
import com.xingheyuzhuan.shiguangschedule.tool.LanScheduleShareService
import com.xingheyuzhuan.shiguangschedule.tool.LanSharePeer
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import okio.BufferedSource
import org.jetbrains.compose.resources.getString
import org.koin.core.annotation.KoinViewModel
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.*

/**
 * 课表导入/导出界面的 ViewModel。
 * 负责处理跨平台课表数据的导入、导出、日历同步等业务逻辑，并通过状态与一次性事件与 UI 交互。
 */
@KoinViewModel
class CourseTableConversionViewModel(
    private val courseConversionRepository: CourseConversionRepository,
    private val lanShareService: LanScheduleShareService
) : ViewModel() {

    // UI 状态流：维护界面加载状态及各类对话框的显隐控制
    private val _uiState = MutableStateFlow(ConversionUiState())
    val uiState = _uiState.asStateFlow()

    // UI 事件通道：用于向前端发送一次性副作用事件（如拉起文件选择器、弹出提示消息等）
    private val _events = Channel<ConversionEvent>()
    val events = _events.receiveAsFlow()
    val lanPeers = lanShareService.peers

    private var pendingLanImport: String? = null
    private var selectedLanPeer: LanSharePeer? = null

    /**
     * 点击导入按钮：显示导入课表选择对话框
     */
    fun onImportClick() {
        pendingLanImport = null
        _uiState.value = _uiState.value.copy(showImportTableDialog = true)
    }

    /**
     * 点击导出 JSON 按钮：显示导出选择对话框并指定类型为 JSON
     */
    fun onExportClick() {
        _uiState.value = _uiState.value.copy(
            showExportTableDialog = true,
            exportType = ExportType.JSON
        )
    }

    /**
     * 点击导出 ICS 按钮：显示导出选择对话框并指定类型为 ICS
     */
    fun onExportIcsClick() {
        _uiState.value = _uiState.value.copy(
            showExportTableDialog = true,
            exportType = ExportType.ICS
        )
    }

    fun onLanShareClick() {
        lanShareService.start { json ->
            viewModelScope.launch {
                pendingLanImport = json
                _uiState.value = _uiState.value.copy(
                    showLanShareDialog = false,
                    showExportTableDialog = false,
                    showImportTableDialog = true
                )
            }
        }
        _uiState.value = _uiState.value.copy(showLanShareDialog = true)
        lanShareService.refresh()
    }

    fun refreshLanPeers() = lanShareService.refresh()

    fun onLanPeerSelected(peer: LanSharePeer) {
        selectedLanPeer = peer
        _uiState.value = _uiState.value.copy(
            showLanShareDialog = false,
            showExportTableDialog = true,
            exportType = ExportType.LAN
        )
    }

    /**
     * 关闭所有弹窗对话框
     */
    fun dismissDialog() {
        pendingLanImport = null
        _uiState.value = _uiState.value.copy(
            showImportTableDialog = false,
            showExportTableDialog = false,
            showLanShareDialog = false
        )
    }

    /**
     * 当用户在弹窗中选择具体某个课表进行导入时触发
     */
    fun onImportTableSelected(tableId: String) {
        viewModelScope.launch {
            val lanJson = pendingLanImport
            if (lanJson == null) {
                _events.send(ConversionEvent.LaunchImportFilePicker(tableId))
            } else {
                _uiState.value = _uiState.value.copy(isLoading = true)
                try {
                    importJson(tableId, lanJson)
                } catch (_: Exception) {
                    _events.send(ConversionEvent.ShowMessage(getString(Res.string.error_import_failed)))
                } finally {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            }
            dismissDialog()
        }
    }

    /**
     * 当用户在弹窗中确认导出课表时触发（根据当前 exportType 区分 JSON 或 ICS）
     */
    fun onExportTableSelected(tableId: String, alarmMinutes: Int?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                if (_uiState.value.exportType == ExportType.JSON || _uiState.value.exportType == ExportType.LAN) {
                    val jsonModel = courseConversionRepository.exportCourseTableToJson(tableId)
                    if (jsonModel != null) {
                        val jsonString = CourseImportExport.json.encodeToString(
                            CourseImportExport.CourseTableExportModel.serializer(),
                            jsonModel
                        )
                        if (_uiState.value.exportType == ExportType.LAN) {
                            val peer = selectedLanPeer ?: error("No LAN peer selected")
                            lanShareService.send(peer, jsonString)
                            _events.send(ConversionEvent.ShowMessage(getString(Res.string.toast_lan_share_success)))
                        } else {
                            _events.send(ConversionEvent.LaunchExportFileCreator(jsonString))
                        }
                    } else {
                        val message = getString(Res.string.error_export_table_not_found)
                        _events.send(ConversionEvent.ShowMessage(message))
                    }
                } else if (_uiState.value.exportType == ExportType.ICS) {
                    val icsContent = courseConversionRepository.exportToIcsString(tableId, alarmMinutes)
                    if (icsContent != null) {
                        _events.send(ConversionEvent.LaunchExportIcsFileCreator(icsContent))
                    } else {
                        val message = getString(Res.string.error_ics_export_data_failed)
                        _events.send(ConversionEvent.ShowMessage(message))
                    }
                }
            } catch (e: Exception) {
                val errorMessage = buildString {
                    if (_uiState.value.exportType == ExportType.LAN) {
                        append(selectedLanPeer?.address.orEmpty())
                        append(": ")
                    }
                    append(e::class.simpleName ?: "Network error")
                    e.message?.takeIf { it.isNotBlank() }?.let {
                        append(": ")
                        append(it)
                    }
                }
                println("LAN/export failed: $errorMessage")
                val message = if (_uiState.value.exportType == ExportType.LAN) {
                    getString(Res.string.error_lan_share_failed, errorMessage)
                } else {
                    getString(Res.string.error_export_failed, errorMessage)
                }
                _events.send(ConversionEvent.ShowMessage(message))
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
                dismissDialog()
            }
        }
    }

    /**
     * 处理文件导入逻辑：通过 Okio 的 BufferedSource 读取文件文本并解析入库
     */
    fun handleFileImport(tableId: String, source: BufferedSource) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                importJson(tableId, source.readUtf8())
            } catch (_: Exception) {
                val message = getString(Res.string.error_import_failed)
                _events.send(ConversionEvent.ShowMessage(message))
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    private suspend fun importJson(
        tableId: String,
        jsonString: String,
        regenerateCourseIds: Boolean = false
    ) {
        var importModel = CourseImportExport.json.decodeFromString<CourseImportExport.CourseTableImportModel>(jsonString)
        if (regenerateCourseIds) {
            importModel = importModel.copy(courses = importModel.courses.map { it.copy(id = null) })
        }
        courseConversionRepository.importCourseTableFromJson(tableId, importModel)
        _events.send(ConversionEvent.ShowMessage(getString(Res.string.toast_import_success)))
    }

    override fun onCleared() {
        lanShareService.stop()
        super.onCleared()
    }

    /**
     * 点击同步到系统日历按钮触发的逻辑
     */
    fun onSyncToCalendarClick() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val success = courseConversionRepository.syncCurrentTableToSystemCalendar()
                val message = if (success) {
                    getString(Res.string.toast_sync_calendar_success)
                } else {
                    getString(Res.string.error_sync_calendar_failed)
                }
                _events.send(ConversionEvent.ShowMessage(message))
            } catch (_: Exception) {
                val message = getString(Res.string.error_sync_calendar_failed)
                _events.send(ConversionEvent.ShowMessage(message))
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
}

/**
 * 课表转换界面的 UI 状态数据类
 */
data class ConversionUiState(
    val isLoading: Boolean = false,
    val showImportTableDialog: Boolean = false,
    val showExportTableDialog: Boolean = false,
    val showLanShareDialog: Boolean = false,
    val exportType: ExportType = ExportType.NONE
)

/**
 * 导出类型枚举
 */
enum class ExportType {
    NONE,
    JSON,
    LAN,
    ICS
}

/**
 * 界面一次性副作用事件密封类
 */
sealed class ConversionEvent {
    data class LaunchImportFilePicker(val tableId: String) : ConversionEvent()
    data class LaunchExportFileCreator(val jsonContent: String) : ConversionEvent()
    data class LaunchExportIcsFileCreator(val icsContent: String) : ConversionEvent()
    data class ShowMessage(val message: String) : ConversionEvent()
}
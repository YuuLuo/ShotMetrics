package com.shotmetrics.app.ui.export

import android.content.Context
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.ViewModel
import com.shotmetrics.app.domain.model.AngleUnit
import com.shotmetrics.app.domain.model.LengthUnit
import com.shotmetrics.app.ui.editor.EditorState
import com.shotmetrics.app.ui.editor.OverlayType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ExportPreviewViewModel : ViewModel() {

    private val _state = MutableStateFlow(ExportPreviewState())
    val state: StateFlow<ExportPreviewState> = _state.asStateFlow()

    private var _editorState: EditorState? = null
    val editorState: EditorState? get() = _editorState

    private var _legendInitialized = false

    fun initializeLegendPosition(imgWidth: Float, imgHeight: Float) {
        if (_legendInitialized) return
        _legendInitialized = true
        _state.update {
            it.copy(legendOffsetPx = Offset(imgWidth * 0.35f, imgHeight * 0.35f))
        }
    }

    fun initialize(editorState: EditorState) {
        if (_editorState != null) return
        _editorState = editorState
        val saved = SavedExportSettings
        _state.update {
            it.copy(
                overlayVisibility = editorState.overlayVisibility,
                labelDataFlags = saved.labelDataFlags,
                labelAngleUnit = saved.labelAngleUnit,
                labelLengthUnit = saved.labelLengthUnit,
                atzDisplayUnit = saved.atzDisplayUnit,
                showLegend = saved.showLegend,
                showLabel = saved.showLabel,
                labelAlpha = saved.labelAlpha
            )
        }
    }

    fun setPhase(phase: ExportPhase) = _state.update { it.copy(phase = phase) }
    fun proceedToCrop() = _state.update { it.copy(phase = ExportPhase.CROP, labelEditZoom = it.imageZoom, imageZoom = 1f, imageOffset = Offset.Zero) }

    fun setCustomTitle(text: String) = _state.update { it.copy(customTitle = text) }
    fun setLocationText(text: String) = _state.update { it.copy(locationText = text) }
    fun setCustomDate(text: String) = _state.update { it.copy(customDate = text) }
    fun setLabelAlpha(alpha: Float) = updateAndSave { it.copy(labelAlpha = alpha) }
    fun setShowLabel(show: Boolean) = updateAndSave { it.copy(showLabel = show) }

    fun setLabelOffsetPx(offset: Offset) = _state.update { it.copy(labelOffsetPx = offset) }
    fun setLabelScale(scale: Float) = _state.update { it.copy(labelScale = scale.coerceIn(0.5f, 3f)) }
    fun setLegendOffsetPx(offset: Offset) = _state.update { it.copy(legendOffsetPx = offset) }

    fun setImageOffset(offset: Offset) = _state.update { it.copy(imageOffset = offset) }
    fun setImageZoom(zoom: Float) = _state.update { it.copy(imageZoom = zoom.coerceIn(0.5f, 5f)) }

    fun toggleLabelData(type: LabelDataType) = updateAndSave { s ->
        val flags = s.labelDataFlags.toMutableSet()
        if (type in flags) flags.remove(type) else flags.add(type)
        s.copy(labelDataFlags = flags)
    }

    fun setLabelAngleUnit(unit: AngleUnit) = updateAndSave { it.copy(labelAngleUnit = unit) }
    fun setLabelLengthUnit(unit: LengthUnit) = updateAndSave { it.copy(labelLengthUnit = unit) }
    fun setAtzDisplayUnit(unit: ATZDisplayUnit) = updateAndSave { it.copy(atzDisplayUnit = unit) }

    fun setCropRect(rect: Rect?) = _state.update { it.copy(cropRect = rect) }
    fun setAspectRatio(ratio: AspectRatioOption) = _state.update { it.copy(aspectRatio = ratio, cropRect = null) }
    fun resetCrop() = _state.update { it.copy(cropRect = null) }

    fun toggleOverlay(type: OverlayType) {
        _state.update { s ->
            val v = s.overlayVisibility
            s.copy(overlayVisibility = when (type) {
                OverlayType.EXTREME_SPREAD -> v.copy(extremeSpread = !v.extremeSpread)
                OverlayType.CEP -> v.copy(cep = !v.cep)
                OverlayType.MEAN_RADIUS -> v.copy(meanRadius = !v.meanRadius)
                OverlayType.MPI -> v.copy(mpi = !v.mpi)
                OverlayType.OFFSET_LINE -> v.copy(offsetLine = !v.offsetLine)
            })
        }
    }

    fun setShowLegend(show: Boolean) = updateAndSave { it.copy(showLegend = show) }
    fun setShowSettingsSheet(show: Boolean) = _state.update { it.copy(showSettingsSheet = show) }

    private fun updateAndSave(transform: (ExportPreviewState) -> ExportPreviewState) {
        _state.update { s ->
            val new = transform(s)
            SavedExportSettings.saveFrom(new)
            new
        }
    }

    fun exportImage(context: Context, onComplete: (Uri?) -> Unit) {
        val es = _editorState ?: run { onComplete(null); return }
        _state.update { it.copy(isExporting = true) }
        try {
            val s = _state.value
            val config = ExportConfig(
                customTitle = s.customTitle,
                locationText = s.locationText,
                customDate = s.customDate,
                labelAlpha = s.labelAlpha,
                labelOffsetPx = s.labelOffsetPx,
                labelScale = s.labelScale,
                labelDataFlags = s.labelDataFlags,
                showLabel = s.showLabel,
                cropRect = s.cropRect,
                overlayVisibility = s.overlayVisibility,
                showLegend = s.showLegend,
                labelAngleUnit = s.labelAngleUnit,
                labelLengthUnit = s.labelLengthUnit,
                atzDisplayUnit = s.atzDisplayUnit,
                legendOffsetPx = s.legendOffsetPx
            )
            val uri = ExportUtils.exportImageWithOptions(context, es, config)
            _state.update { it.copy(isExporting = false) }
            onComplete(uri)
        } catch (e: Exception) {
            _state.update { it.copy(isExporting = false) }
            onComplete(null)
        }
    }
}

data class ExportConfig(
    val customTitle: String,
    val locationText: String,
    val customDate: String,
    val labelAlpha: Float,
    val labelOffsetPx: Offset,
    val labelScale: Float,
    val labelDataFlags: Set<LabelDataType>,
    val showLabel: Boolean,
    val cropRect: Rect?,
    val overlayVisibility: com.shotmetrics.app.ui.editor.OverlayVisibility,
    val showLegend: Boolean,
    val labelAngleUnit: AngleUnit = AngleUnit.MOA,
    val labelLengthUnit: LengthUnit = LengthUnit.INCH,
    val atzDisplayUnit: ATZDisplayUnit = ATZDisplayUnit.INCH,
    val legendOffsetPx: Offset = Offset.Zero
)

object SavedExportSettings {
    var labelDataFlags: Set<LabelDataType> = setOf(
        LabelDataType.GROUP_SIZE, LabelDataType.CEP,
        LabelDataType.MEAN_RADIUS, LabelDataType.ATZ
    )
        private set
    var labelAngleUnit: AngleUnit = AngleUnit.MOA
        private set
    var labelLengthUnit: LengthUnit = LengthUnit.INCH
        private set
    var atzDisplayUnit: ATZDisplayUnit = ATZDisplayUnit.INCH
        private set
    var showLegend: Boolean = true
        private set
    var showLabel: Boolean = true
        private set
    var labelAlpha: Float = 0.75f
        private set

    fun saveFrom(state: ExportPreviewState) {
        labelDataFlags = state.labelDataFlags
        labelAngleUnit = state.labelAngleUnit
        labelLengthUnit = state.labelLengthUnit
        atzDisplayUnit = state.atzDisplayUnit
        showLegend = state.showLegend
        showLabel = state.showLabel
        labelAlpha = state.labelAlpha
    }
}

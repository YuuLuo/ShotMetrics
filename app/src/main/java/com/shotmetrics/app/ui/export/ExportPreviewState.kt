package com.shotmetrics.app.ui.export

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.shotmetrics.app.domain.model.AngleUnit
import com.shotmetrics.app.domain.model.LengthUnit
import com.shotmetrics.app.ui.editor.OverlayVisibility

enum class LabelDataType(val displayName: String) {
    GROUP_SIZE("Group Size"),
    EXTREME_SPREAD("Extreme Spread"),
    MEAN_RADIUS("Mean Radius"),
    CEP("CEP"),
    MPI_OFFSET("MPI Offset"),
    RADIAL_SD("Radial SD"),
    VERTICAL_SD("Vertical SD"),
    HORIZONTAL_SD("Horizontal SD"),
    ATZ("ATZ Clicks")
}

enum class ATZDisplayUnit(val label: String, val abbreviation: String) {
    INCH("Inches", "in"),
    CM("Centimeters", "cm"),
    MOA("MOA", "MOA"),
    MIL("MIL", "MIL"),
    CLICKS("Clicks", "clicks");
}

enum class AspectRatioOption(val label: String, val ratio: Float?) {
    FREE("Free", null),
    SQUARE("1:1", 1f),
    RATIO_4_3("4:3", 4f / 3f),
    RATIO_16_9("16:9", 16f / 9f)
}

enum class ExportPhase {
    LABEL, CROP
}

data class ExportPreviewState(
    val phase: ExportPhase = ExportPhase.LABEL,

    // Label content
    val customTitle: String = "",
    val locationText: String = "",
    val customDate: String = "",
    val labelAlpha: Float = 0.75f,
    val labelDataFlags: Set<LabelDataType> = setOf(
        LabelDataType.GROUP_SIZE, LabelDataType.CEP,
        LabelDataType.MEAN_RADIUS, LabelDataType.ATZ
    ),
    val showLabel: Boolean = true,

    // Label position: offset from image center in image-pixel coordinates
    val labelOffsetPx: Offset = Offset.Zero,
    val labelScale: Float = 1f,

    // Legend position: offset from default bottom-right anchor in image-pixel coordinates
    val legendOffsetPx: Offset = Offset.Zero,

    // Image pan/zoom in preview
    val imageOffset: Offset = Offset.Zero,
    val imageZoom: Float = 1f,
    val labelEditZoom: Float = 1f,

    // Crop (normalized 0..1)
    val cropRect: Rect? = null,
    val aspectRatio: AspectRatioOption = AspectRatioOption.FREE,

    // Overlay
    val overlayVisibility: OverlayVisibility = OverlayVisibility(),
    val showLegend: Boolean = true,

    // Unit overrides for label display
    val labelAngleUnit: AngleUnit = AngleUnit.MOA,
    val labelLengthUnit: LengthUnit = LengthUnit.INCH,
    val atzDisplayUnit: ATZDisplayUnit = ATZDisplayUnit.INCH,

    // Settings sheet
    val showSettingsSheet: Boolean = false,

    val isExporting: Boolean = false
)

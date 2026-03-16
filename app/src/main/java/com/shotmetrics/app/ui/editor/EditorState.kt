package com.shotmetrics.app.ui.editor

import androidx.compose.ui.geometry.Offset
import com.shotmetrics.app.domain.model.ATZResult
import com.shotmetrics.app.domain.model.AngleUnit
import com.shotmetrics.app.domain.model.BallisticsResult
import com.shotmetrics.app.domain.model.DistanceUnit
import com.shotmetrics.app.domain.model.ImpactPoint
import com.shotmetrics.app.domain.model.LengthUnit

enum class EditorMode {
    REFERENCE,
    POA,
    IMPACT;

    val label: String
        get() = when (this) {
            REFERENCE -> "Reference"
            POA -> "Point of Aim"
            IMPACT -> "Impacts"
        }
}

data class OverlayVisibility(
    val extremeSpread: Boolean = true,
    val cep: Boolean = true,
    val meanRadius: Boolean = true,
    val mpi: Boolean = true,
    val offsetLine: Boolean = true
)

data class GuideLine(
    val start: Offset,
    val end: Offset
)

data class EditorState(
    val imageUri: String? = null,
    val sessionId: Long? = null,
    val mode: EditorMode = EditorMode.REFERENCE,

    // Reference calibration
    val referencePoint1: Offset? = null,
    val referencePoint2: Offset? = null,
    val scaleFactor: Double = 0.0,
    val isCalibrated: Boolean = false,

    // Markers
    val pointOfAim: Offset? = null,
    val impacts: List<ImpactPoint> = emptyList(),
    val nextImpactId: Int = 1,

    // Center-based placement: pendingPoint = image coord at screen center
    val pendingPoint: Offset? = null,

    // Guide lines (center-based: first confirm sets start, second confirm draws line)
    val isDrawingGuideLines: Boolean = false,
    val guideLines: List<GuideLine> = emptyList(),
    val guideLineStart: Offset? = null,

    // Session parameters
    val distanceToTarget: Double = 100.0,
    val distanceUnit: DistanceUnit = DistanceUnit.YARDS,
    val caliber: String = "",
    val lengthUnit: LengthUnit = LengthUnit.INCH,
    val angleUnit: AngleUnit = AngleUnit.MOA,
    val referenceSize: Double = 1.0,
    val turretClickValue: Double = 0.25,
    val turretAngleUnit: AngleUnit = AngleUnit.MOA,

    // Results
    val ballisticsResult: BallisticsResult? = null,
    val atzResult: ATZResult? = null,
    val overlayVisibility: OverlayVisibility = OverlayVisibility(),
    val showOverlays: Boolean = false,
    val showResultsPanel: Boolean = false,
    val showATZPanel: Boolean = false,
    val labels: List<TextLabel> = emptyList(),
    val errorMessage: String? = null,
    val isSaving: Boolean = false
) {
    val enabledImpacts: List<ImpactPoint>
        get() = impacts.filter { it.enabled }

    val canCalculate: Boolean
        get() = isCalibrated && enabledImpacts.size >= 2

    val canCalculateATZ: Boolean
        get() = canCalculate && pointOfAim != null

    val needsPlacement: Boolean
        get() = pendingPoint != null

    val confirmButtonText: String
        get() = when {
            isDrawingGuideLines && guideLineStart == null -> "Set Start"
            isDrawingGuideLines && guideLineStart != null -> "Set End"
            else -> "Confirm"
        }

    val placementHint: String
        get() = when {
            isDrawingGuideLines && guideLineStart == null -> "Move image to set guide line start"
            isDrawingGuideLines && guideLineStart != null -> "Move image to set guide line end"
            mode == EditorMode.REFERENCE && referencePoint1 == null -> "Move image to place first reference point"
            mode == EditorMode.REFERENCE && referencePoint2 == null -> "Move image to place second reference point"
            mode == EditorMode.REFERENCE && isCalibrated -> "Calibration complete \u2014 tap Confirm to recalibrate"
            mode == EditorMode.POA -> "Move image to position point of aim"
            mode == EditorMode.IMPACT -> "Move image to place impact #$nextImpactId"
            else -> ""
        }
}

data class TextLabel(
    val id: Int,
    val text: String,
    val position: Offset
)

sealed class EditorAction {
    data class SetMode(val mode: EditorMode) : EditorAction()

    // Center-based placement (used for all: ref, POA, impact, guide lines)
    data class UpdatePendingPoint(val position: Offset) : EditorAction()
    data object ConfirmPlacement : EditorAction()
    data object FinishPlacement : EditorAction()

    data object ToggleGuideLineMode : EditorAction()
    data object ClearGuideLines : EditorAction()

    data class MoveImpact(val impactId: Int, val newPosition: Offset) : EditorAction()
    data class ToggleImpact(val impactId: Int) : EditorAction()
    data class RemoveImpact(val impactId: Int) : EditorAction()
    data class SetDistance(val distance: Double) : EditorAction()
    data class SetCaliber(val caliber: String) : EditorAction()
    data class SetDistanceUnit(val unit: DistanceUnit) : EditorAction()
    data class ToggleOverlay(val overlay: OverlayType) : EditorAction()
    data object ToggleResultsPanel : EditorAction()
    data object ToggleATZPanel : EditorAction()
    data object Undo : EditorAction()
    data object Redo : EditorAction()
    data class AddLabel(val text: String, val position: Offset) : EditorAction()
    data class RemoveLabel(val id: Int) : EditorAction()
    data object Recalculate : EditorAction()
    data object DismissError : EditorAction()
}

enum class OverlayType {
    EXTREME_SPREAD, CEP, MEAN_RADIUS, MPI, OFFSET_LINE
}

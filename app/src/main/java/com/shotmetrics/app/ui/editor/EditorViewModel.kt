package com.shotmetrics.app.ui.editor

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shotmetrics.app.data.repository.SessionRepository
import com.shotmetrics.app.data.repository.SettingsRepository
import com.shotmetrics.app.domain.calculator.ATZCalculator
import com.shotmetrics.app.domain.calculator.BallisticsCalculator
import com.shotmetrics.app.domain.model.AngleUnit
import com.shotmetrics.app.domain.model.DistanceUnit
import com.shotmetrics.app.domain.model.ImpactPoint
import com.shotmetrics.app.domain.model.LengthUnit
import com.shotmetrics.app.domain.model.ShootingSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.sqrt

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val ballisticsCalculator: BallisticsCalculator,
    private val atzCalculator: ATZCalculator,
    private val settingsRepo: SettingsRepository,
    private val sessionRepo: SessionRepository
) : ViewModel() {

    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state.asStateFlow()

    private val undoStack = ArrayDeque<EditorState>()
    private val redoStack = ArrayDeque<EditorState>()
    private val maxUndoSize = 50
    private var initialized = false
    private var autoShowOverlays = false

    fun initialize(imageUri: String?, sessionId: Long?) {
        if (initialized) return
        initialized = true
        viewModelScope.launch {
            val angleUnit = settingsRepo.angleUnit.first()
            val lengthUnit = settingsRepo.lengthUnit.first()
            val distanceUnit = settingsRepo.distanceUnit.first()
            val refSize = settingsRepo.referenceSize.first()
            val turretClick = settingsRepo.turretClickValue.first()
            val turretUnit = settingsRepo.turretAngleUnit.first()
            val defaultDist = settingsRepo.defaultDistance.first()
            val defaultCaliber = settingsRepo.defaultCaliber.first()
            autoShowOverlays = settingsRepo.autoShowOverlays.first()
            if (sessionId != null) {
                loadSession(sessionId, angleUnit, lengthUnit, distanceUnit, refSize, turretClick, turretUnit)
            } else {
                _state.update {
                    it.copy(
                        imageUri = imageUri, angleUnit = angleUnit, lengthUnit = lengthUnit,
                        distanceUnit = distanceUnit, referenceSize = refSize, turretClickValue = turretClick,
                        turretAngleUnit = turretUnit, distanceToTarget = defaultDist, caliber = defaultCaliber
                    )
                }
            }
        }
    }

    private suspend fun loadSession(
        sessionId: Long, angleUnit: AngleUnit, lengthUnit: LengthUnit,
        distanceUnit: DistanceUnit, refSize: Double, turretClick: Double, turretUnit: AngleUnit
    ) {
        val session = sessionRepo.getSession(sessionId) ?: return
        val impacts = sessionRepo.getImpacts(sessionId)
        _state.update {
            it.copy(
                sessionId = sessionId, imageUri = session.imageUri, distanceToTarget = session.distance,
                caliber = session.caliber, scaleFactor = session.scaleFactor, isCalibrated = session.scaleFactor > 0,
                referencePoint1 = if (session.refX1 != null && session.refY1 != null) Offset(session.refX1, session.refY1) else null,
                referencePoint2 = if (session.refX2 != null && session.refY2 != null) Offset(session.refX2, session.refY2) else null,
                pointOfAim = if (session.poaX != null && session.poaY != null) Offset(session.poaX, session.poaY) else null,
                impacts = impacts.mapIndexed { index, imp -> ImpactPoint(id = index + 1, position = Offset(imp.x, imp.y), enabled = imp.enabled) },
                nextImpactId = impacts.size + 1, angleUnit = angleUnit, lengthUnit = lengthUnit,
                distanceUnit = distanceUnit, referenceSize = refSize, turretClickValue = turretClick,
                turretAngleUnit = turretUnit, mode = if (session.scaleFactor > 0) EditorMode.IMPACT else EditorMode.REFERENCE
            )
        }
        recalculate()
    }

    fun onAction(action: EditorAction) {
        when (action) {
            is EditorAction.SetMode -> _state.update {
                it.copy(mode = action.mode, isDrawingGuideLines = false, guideLineStart = null)
            }
            is EditorAction.UpdatePendingPoint -> _state.update { it.copy(pendingPoint = action.position) }
            EditorAction.ConfirmPlacement -> {
                if (_state.value.isDrawingGuideLines) handleGuideLineConfirm()
                else handleConfirmPlacement()
            }
            EditorAction.FinishPlacement -> handleFinishPlacement()

            EditorAction.ToggleGuideLineMode -> _state.update {
                it.copy(isDrawingGuideLines = !it.isDrawingGuideLines, guideLineStart = null)
            }
            EditorAction.ClearGuideLines -> { pushUndo(); _state.update { it.copy(guideLines = emptyList()) } }

            is EditorAction.MoveImpact -> moveImpact(action.impactId, action.newPosition)
            is EditorAction.ToggleImpact -> toggleImpact(action.impactId)
            is EditorAction.RemoveImpact -> removeImpact(action.impactId)
            is EditorAction.SetDistance -> { pushUndo(); _state.update { it.copy(distanceToTarget = action.distance) }; recalculate() }
            is EditorAction.SetCaliber -> _state.update { it.copy(caliber = action.caliber) }
            is EditorAction.SetDistanceUnit -> { _state.update { it.copy(distanceUnit = action.unit) }; recalculate() }
            is EditorAction.ToggleOverlay -> toggleOverlay(action.overlay)
            EditorAction.ToggleResultsPanel -> _state.update { it.copy(showResultsPanel = !it.showResultsPanel) }
            EditorAction.ToggleATZPanel -> _state.update { it.copy(showATZPanel = !it.showATZPanel) }
            EditorAction.Undo -> undo()
            EditorAction.Redo -> redo()
            is EditorAction.AddLabel -> addLabel(action.text, action.position)
            is EditorAction.RemoveLabel -> removeLabel(action.id)
            EditorAction.Recalculate -> recalculate()
            EditorAction.DismissError -> _state.update { it.copy(errorMessage = null) }
        }
    }

    private fun handleGuideLineConfirm() {
        val current = _state.value
        val point = current.pendingPoint ?: return
        if (current.guideLineStart == null) {
            _state.update { it.copy(guideLineStart = point) }
        } else {
            val start = current.guideLineStart
            val d = sqrt(((point.x - start.x) * (point.x - start.x) + (point.y - start.y) * (point.y - start.y)).toDouble())
            if (d > 10.0) {
                pushUndo()
                _state.update { it.copy(guideLines = it.guideLines + GuideLine(start, point), guideLineStart = null) }
            } else {
                _state.update { it.copy(guideLineStart = null, errorMessage = "Points are too close, try again") }
            }
        }
    }

    private fun handleFinishPlacement() {
        recalculate()
        _state.update { it.copy(showOverlays = autoShowOverlays, showResultsPanel = true) }
    }

    private fun handleConfirmPlacement() {
        val current = _state.value
        val point = current.pendingPoint ?: return
        pushUndo()
        when (current.mode) {
            EditorMode.REFERENCE -> confirmReferencePlacement(point, current)
            EditorMode.POA -> {
                _state.update { it.copy(pointOfAim = point, mode = EditorMode.IMPACT) }
                recalculate()
            }
            EditorMode.IMPACT -> {
                val newImpact = ImpactPoint(id = current.nextImpactId, position = point, enabled = true)
                _state.update {
                    it.copy(
                        impacts = it.impacts + newImpact,
                        nextImpactId = it.nextImpactId + 1,
                        showOverlays = false,
                        showResultsPanel = false
                    )
                }
                recalculate()
            }
        }
    }

    private fun confirmReferencePlacement(point: Offset, current: EditorState) {
        when {
            current.referencePoint1 == null -> _state.update { it.copy(referencePoint1 = point) }
            current.referencePoint2 == null -> {
                val p1 = current.referencePoint1
                val dx = point.x - p1.x; val dy = point.y - p1.y
                val pixelDistance = sqrt((dx * dx + dy * dy).toDouble())
                if (pixelDistance < 20.0) {
                    _state.update { it.copy(errorMessage = "Reference points are too close. Place them further apart.") }
                    return
                }
                val sf = pixelDistance / current.referenceSize
                _state.update { it.copy(referencePoint2 = point, scaleFactor = sf, isCalibrated = true, mode = EditorMode.POA) }
            }
            else -> _state.update { it.copy(referencePoint1 = point, referencePoint2 = null, scaleFactor = 0.0, isCalibrated = false) }
        }
    }

    private fun cleanForStack(s: EditorState) = s.copy(pendingPoint = null, guideLineStart = null)

    private fun pushUndo() {
        undoStack.addLast(cleanForStack(_state.value))
        if (undoStack.size > maxUndoSize) undoStack.removeFirst()
        redoStack.clear()
    }

    private fun undo() {
        if (undoStack.isEmpty()) return
        redoStack.addLast(cleanForStack(_state.value))
        _state.value = undoStack.removeLast()
    }

    private fun redo() {
        if (redoStack.isEmpty()) return
        undoStack.addLast(cleanForStack(_state.value))
        _state.value = redoStack.removeLast()
    }

    private fun moveImpact(id: Int, pos: Offset) {
        _state.update { s -> s.copy(impacts = s.impacts.map { if (it.id == id) it.copy(position = pos) else it }) }
        recalculate()
    }
    private fun toggleImpact(id: Int) { pushUndo(); _state.update { s -> s.copy(impacts = s.impacts.map { if (it.id == id) it.copy(enabled = !it.enabled) else it }) }; recalculate() }
    private fun removeImpact(id: Int) { pushUndo(); _state.update { s -> s.copy(impacts = s.impacts.filter { it.id != id }) }; recalculate() }

    private fun toggleOverlay(o: OverlayType) {
        _state.update { s ->
            val v = s.overlayVisibility
            s.copy(overlayVisibility = when (o) {
                OverlayType.EXTREME_SPREAD -> v.copy(extremeSpread = !v.extremeSpread)
                OverlayType.CEP -> v.copy(cep = !v.cep)
                OverlayType.MEAN_RADIUS -> v.copy(meanRadius = !v.meanRadius)
                OverlayType.MPI -> v.copy(mpi = !v.mpi)
                OverlayType.OFFSET_LINE -> v.copy(offsetLine = !v.offsetLine)
            })
        }
    }

    private fun addLabel(text: String, pos: Offset) { pushUndo(); val id = (_state.value.labels.maxOfOrNull { it.id } ?: 0) + 1; _state.update { it.copy(labels = it.labels + TextLabel(id, text, pos)) } }
    private fun removeLabel(id: Int) { pushUndo(); _state.update { it.copy(labels = it.labels.filter { l -> l.id != id }) } }

    private fun recalculate() {
        val c = _state.value
        if (!c.canCalculate) { _state.update { it.copy(ballisticsResult = null, atzResult = null) }; return }
        try {
            val r = ballisticsCalculator.calculate(c.enabledImpacts.map { it.position }, c.pointOfAim, c.scaleFactor, c.distanceToTarget, c.distanceUnit, c.lengthUnit, c.angleUnit)
            val atz = if (c.canCalculateATZ) atzCalculator.calculate(r.meanWindage, r.meanElevation, c.angleUnit, c.turretClickValue, c.turretAngleUnit) else null
            _state.update { it.copy(ballisticsResult = r, atzResult = atz) }
        } catch (e: Exception) { _state.update { it.copy(errorMessage = "Calculation error: ${e.message}") } }
    }

    fun saveSession() {
        val c = _state.value; if (c.imageUri == null) return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            try {
                val session = ShootingSession(
                    id = c.sessionId ?: 0, imageUri = c.imageUri, scaleFactor = c.scaleFactor,
                    distanceToTarget = c.distanceToTarget, distanceUnit = c.distanceUnit, caliber = c.caliber,
                    lengthUnit = c.lengthUnit, angleUnit = c.angleUnit, referenceSize = c.referenceSize,
                    referencePoints = if (c.referencePoint1 != null && c.referencePoint2 != null) Pair(c.referencePoint1, c.referencePoint2) else null,
                    pointOfAim = c.pointOfAim, impacts = c.impacts, turretClickValue = c.turretClickValue,
                    turretAngleUnit = c.turretAngleUnit, notes = c.caliber
                )
                val savedId = sessionRepo.saveSession(session)
                _state.update { it.copy(sessionId = savedId, isSaving = false) }
            } catch (e: Exception) { _state.update { it.copy(isSaving = false, errorMessage = "Failed to save: ${e.message}") } }
        }
    }
}

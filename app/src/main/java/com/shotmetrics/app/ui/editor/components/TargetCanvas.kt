package com.shotmetrics.app.ui.editor.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import com.shotmetrics.app.domain.model.Calibers
import com.shotmetrics.app.domain.model.ImpactPoint
import com.shotmetrics.app.ui.editor.EditorMode
import com.shotmetrics.app.ui.editor.EditorState
import com.shotmetrics.app.ui.editor.GuideLine
import com.shotmetrics.app.ui.editor.TextLabel
import kotlin.math.max
import kotlin.math.min

private val REF_COLOR = Color(0xFF69F0AE)
private val REF_FILL_COLOR = Color(0x3369F0AE)
private val POA_COLOR = Color(0xFF42A5F5)
private val POA_FILL_COLOR = Color(0x3342A5F5)
private val IMPACT_COLOR = Color(0xFF00E676)
private val IMPACT_DISABLED_COLOR = Color(0x60888888)
private val MPI_COLOR = Color(0xFFFF9800)
private val CEP_COLOR = Color(0xFF9C27B0)
private val MEAN_RADIUS_COLOR = Color(0xFF00BCD4)
private val ES_COLOR = Color(0xFFFF5722)
private val OFFSET_LINE_COLOR = Color(0xFFFFEB3B)
private val GUIDE_LINE_COLOR = Color(0xFF69F0AE)

private const val MIN_SCREEN_RADIUS = 10f

@Composable
fun TargetCanvas(
    state: EditorState,
    onCenterPointChanged: (Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var imageSize by remember { mutableStateOf(Size.Zero) }

    var scale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    val textMeasurer = rememberTextMeasurer()

    LaunchedEffect(state.imageUri) {
        state.imageUri?.let { uriStr ->
            try {
                val uri = Uri.parse(uriStr)
                val inputStream = if (uriStr.startsWith("file:")) {
                    java.io.File(uri.path!!).inputStream()
                } else {
                    context.contentResolver.openInputStream(uri)
                }
                inputStream?.use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    if (bitmap != null) {
                        imageBitmap = bitmap.asImageBitmap()
                        imageSize = Size(bitmap.width.toFloat(), bitmap.height.toFloat())
                    }
                }
            } catch (_: Exception) { }
        }
    }

    // Continuously emit the image coordinate at screen center
    LaunchedEffect(Unit) {
        snapshotFlow {
            if (imageSize == Size.Zero || canvasSize == Size.Zero) null
            else {
                val imgScaleX = canvasSize.width / imageSize.width
                val imgScaleY = canvasSize.height / imageSize.height
                val imgScale = min(imgScaleX, imgScaleY) * scale
                val imgOffsetX = (canvasSize.width - imageSize.width * imgScale) / 2f + panOffset.x
                val imgOffsetY = (canvasSize.height - imageSize.height * imgScale) / 2f + panOffset.y
                val cx = canvasSize.width / 2f
                val cy = canvasSize.height / 2f
                val imageX = ((cx - imgOffsetX) / imgScale).coerceIn(0f, imageSize.width)
                val imageY = ((cy - imgOffsetY) / imgScale).coerceIn(0f, imageSize.height)
                Offset(imageX, imageY)
            }
        }.collect { offset -> offset?.let { onCenterPointChanged(it) } }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { canvasSize = Size(it.width.toFloat(), it.height.toFloat()) }
            .pointerInput(Unit) {
                // All gestures: single-finger = pan, two-finger = zoom + pan
                awaitEachGesture {
                    val firstDown = awaitFirstDown(requireUnconsumed = false)
                    var pointerId = firstDown.id

                    while (true) {
                        val event = awaitPointerEvent()
                        val pointerCount = event.changes.count { it.pressed }

                        if (pointerCount == 0) break

                        if (pointerCount >= 2) {
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            scale = (scale * zoom).coerceIn(0.5f, 10f)
                            panOffset += pan
                            event.changes.forEach { it.consume() }
                        } else {
                            val change = event.changes.firstOrNull { it.id == pointerId }
                                ?: event.changes.firstOrNull { it.pressed }
                            if (change != null) {
                                pointerId = change.id
                                panOffset += change.positionChange()
                                change.consume()
                            }
                        }
                    }
                }
            }
    ) {
        val bmp = imageBitmap ?: return@Canvas

        val imgScaleX = size.width / imageSize.width
        val imgScaleY = size.height / imageSize.height
        val baseImgScale = min(imgScaleX, imgScaleY)
        val imgScale = baseImgScale * scale
        val imgOffsetX = (size.width - imageSize.width * imgScale) / 2f + panOffset.x
        val imgOffsetY = (size.height - imageSize.height * imgScale) / 2f + panOffset.y

        drawImage(
            image = bmp,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(bmp.width, bmp.height),
            dstOffset = IntOffset(imgOffsetX.toInt(), imgOffsetY.toInt()),
            dstSize = IntSize((imageSize.width * imgScale).toInt(), (imageSize.height * imgScale).toInt())
        )

        fun toScreen(imageCoord: Offset): Offset =
            Offset(imageCoord.x * imgScale + imgOffsetX, imageCoord.y * imgScale + imgOffsetY)
        fun pixelsToScreen(px: Double): Float = (px * imgScale).toFloat()

        val calDiam = Calibers.getDiameterInch(state.caliber)
        val calFactor = calDiam / 0.308f

        fun markerRadius(baseScreenPx: Float): Float =
            max(baseScreenPx * calFactor * scale, MIN_SCREEN_RADIUS)
        fun strokeW(base: Float): Float = max(base * scale, 1f).coerceAtMost(base * 3f)

        // Guide lines (always visible)
        drawGuideLines(state.guideLines, ::toScreen)

        // Pending guide line: dashed from guideLineStart to screen center
        if (state.isDrawingGuideLines && state.guideLineStart != null) {
            val dash = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
            val startScreen = toScreen(state.guideLineStart)
            val centerScreen = Offset(size.width / 2f, size.height / 2f)
            drawLine(GUIDE_LINE_COLOR.copy(alpha = 0.6f), startScreen, centerScreen, strokeWidth = 1.5f, pathEffect = dash)
        }

        // Reference points: only visible in REFERENCE mode
        if (state.mode == EditorMode.REFERENCE) {
            drawReferencePoints(state, ::toScreen, ::markerRadius, ::strokeW)
        }

        // POA marker
        drawPointOfAim(state.pointOfAim, ::toScreen, ::markerRadius, ::strokeW)

        // Placed impact markers (with numbers)
        drawImpacts(state.impacts, ::toScreen, textMeasurer, ::markerRadius, ::strokeW)

        // Overlays (only when explicitly enabled via Finish)
        if (state.showOverlays) {
            drawOverlays(state, ::toScreen, ::pixelsToScreen)
        }

        // Text labels
        drawLabels(state.labels, ::toScreen, textMeasurer)

        // Center placement crosshair (drawn last, on top, scales with zoom)
        if (state.needsPlacement) {
            drawCenterCrosshair(
                cx = size.width / 2f,
                cy = size.height / 2f,
                state = state,
                markerRadius = ::markerRadius,
                strokeW = ::strokeW,
                toScreen = ::toScreen
            )
        }
    }
}

// ─── Center crosshair at screen center (scales with zoom) ─────────────────────

private fun DrawScope.drawCenterCrosshair(
    cx: Float,
    cy: Float,
    state: EditorState,
    markerRadius: (Float) -> Float,
    strokeW: (Float) -> Float,
    toScreen: (Offset) -> Offset
) {
    val center = Offset(cx, cy)

    if (state.isDrawingGuideLines) {
        // Guide line mode: show ref-style marker
        val r = markerRadius(14f)
        val sw = strokeW(2f)
        drawRefMarker(center, r, sw)
        return
    }

    when (state.mode) {
        EditorMode.REFERENCE -> {
            val r = markerRadius(14f)
            val sw = strokeW(2f)
            drawRefMarker(center, r, sw)
            if (state.referencePoint1 != null && state.referencePoint2 == null) {
                val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                drawLine(REF_COLOR.copy(alpha = 0.6f), toScreen(state.referencePoint1), center, strokeWidth = 1.5f, pathEffect = dash)
            }
        }
        EditorMode.POA -> {
            val r = markerRadius(16f)
            val sw = strokeW(2f)
            drawPoaMarker(center, r, sw, POA_COLOR, POA_FILL_COLOR)
        }
        EditorMode.IMPACT -> {
            val r = markerRadius(16f)
            val sw = strokeW(2f)
            drawCircle(Color.Black.copy(alpha = 0.35f), r, center, style = Fill)
            drawCircle(IMPACT_COLOR, r, center, style = Stroke(width = sw))
            drawReticleLines(center, r, r * 0.55f, IMPACT_COLOR, sw * 0.8f)
        }
    }
}

// ─── Reference marker (bright green circle + plus + corner brackets) ──────────

private fun DrawScope.drawRefMarker(center: Offset, r: Float, sw: Float) {
    val color = REF_COLOR
    drawCircle(REF_FILL_COLOR, r, center, style = Fill)
    drawCircle(color, r, center, style = Stroke(width = sw))
    val pl = r * 0.3f
    drawLine(color, Offset(center.x - pl, center.y), Offset(center.x + pl, center.y), sw * 0.8f)
    drawLine(color, Offset(center.x, center.y - pl), Offset(center.x, center.y + pl), sw * 0.8f)
    drawCornerBrackets(center, r * 1.25f, color, sw, r * 0.35f)
}

private fun DrawScope.drawCornerBrackets(center: Offset, s: Float, color: Color, sw: Float, bl: Float) {
    drawLine(color, Offset(center.x - s, center.y - s), Offset(center.x - s + bl, center.y - s), sw)
    drawLine(color, Offset(center.x - s, center.y - s), Offset(center.x - s, center.y - s + bl), sw)
    drawLine(color, Offset(center.x + s, center.y - s), Offset(center.x + s - bl, center.y - s), sw)
    drawLine(color, Offset(center.x + s, center.y - s), Offset(center.x + s, center.y - s + bl), sw)
    drawLine(color, Offset(center.x - s, center.y + s), Offset(center.x - s + bl, center.y + s), sw)
    drawLine(color, Offset(center.x - s, center.y + s), Offset(center.x - s, center.y + s - bl), sw)
    drawLine(color, Offset(center.x + s, center.y + s), Offset(center.x + s - bl, center.y + s), sw)
    drawLine(color, Offset(center.x + s, center.y + s), Offset(center.x + s, center.y + s - bl), sw)
}

// ─── Reticle lines helper (from circle edge inward) ───────────────────────────

private fun DrawScope.drawReticleLines(center: Offset, r: Float, gap: Float, color: Color, sw: Float) {
    drawLine(color, Offset(center.x - r, center.y), Offset(center.x - gap, center.y), sw)
    drawLine(color, Offset(center.x + r, center.y), Offset(center.x + gap, center.y), sw)
    drawLine(color, Offset(center.x, center.y - r), Offset(center.x, center.y - gap), sw)
    drawLine(color, Offset(center.x, center.y + r), Offset(center.x, center.y + gap), sw)
}

// ─── POA marker helper ────────────────────────────────────────────────────────

private fun DrawScope.drawPoaMarker(center: Offset, r: Float, sw: Float, color: Color, fill: Color) {
    drawCircle(fill, r, center, style = Fill)
    drawCircle(color, r, center, style = Stroke(width = sw))
    drawReticleLines(center, r, r * 0.55f, color, sw * 0.8f)
    drawCircle(color, sw * 1.2f, center, style = Fill)
}

// ─── Guide lines (bright green dashed) ────────────────────────────────────────

private fun DrawScope.drawGuideLines(
    lines: List<GuideLine>,
    toScreen: (Offset) -> Offset
) {
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
    lines.forEach { gl ->
        drawLine(GUIDE_LINE_COLOR, toScreen(gl.start), toScreen(gl.end), strokeWidth = 1.5f, pathEffect = dashEffect)
    }
}

// ─── Placed reference points ──────────────────────────────────────────────────

private fun DrawScope.drawReferencePoints(
    state: EditorState,
    toScreen: (Offset) -> Offset,
    markerRadius: (Float) -> Float,
    strokeW: (Float) -> Float
) {
    state.referencePoint1?.let { p1 ->
        val sp1 = toScreen(p1)
        val r = markerRadius(14f)
        val sw = strokeW(2f)
        drawRefMarker(sp1, r, sw)

        state.referencePoint2?.let { p2 ->
            val sp2 = toScreen(p2)
            val r2 = markerRadius(14f)
            val sw2 = strokeW(2f)
            drawRefMarker(sp2, r2, sw2)
            drawLine(REF_COLOR, sp1, sp2, strokeWidth = sw)
        }
    }
}

// ─── Placed POA marker ────────────────────────────────────────────────────────

private fun DrawScope.drawPointOfAim(
    poa: Offset?,
    toScreen: (Offset) -> Offset,
    markerRadius: (Float) -> Float,
    strokeW: (Float) -> Float
) {
    poa ?: return
    val sp = toScreen(poa)
    val r = markerRadius(16f)
    val sw = strokeW(2f)
    drawPoaMarker(sp, r, sw, POA_COLOR, POA_FILL_COLOR)
}

// ─── Placed impact markers (number shown, normal brightness) ──────────────────

private fun DrawScope.drawImpacts(
    impacts: List<ImpactPoint>,
    toScreen: (Offset) -> Offset,
    textMeasurer: TextMeasurer,
    markerRadius: (Float) -> Float,
    strokeW: (Float) -> Float
) {
    impacts.forEach { impact ->
        val sp = toScreen(impact.position)
        val color = if (impact.enabled) IMPACT_COLOR else IMPACT_DISABLED_COLOR
        val r = markerRadius(16f)
        val sw = strokeW(2f)

        drawCircle(color, r, sp, style = Stroke(width = sw))
        drawReticleLines(sp, r, r * 0.55f, color, sw * 0.7f)

        val fontSize = max(6f * r / 16f, 5f)
        val label = impact.id.toString()
        val lr = textMeasurer.measure(
            text = label,
            style = TextStyle(fontSize = fontSize.sp, fontWeight = FontWeight.Light, color = color)
        )
        drawText(lr, topLeft = Offset(sp.x - lr.size.width / 2f, sp.y - lr.size.height / 2f))
    }
}

// ─── Overlays ─────────────────────────────────────────────────────────────────

private fun DrawScope.drawOverlays(
    state: EditorState,
    toScreen: (Offset) -> Offset,
    pixelsToScreen: (Double) -> Float
) {
    val result = state.ballisticsResult ?: return
    val vis = state.overlayVisibility
    val mpiScreen = toScreen(result.meanPointOfImpact)

    if (vis.mpi) {
        drawCircle(MPI_COLOR, 6f, mpiScreen)
        val s = 14f
        drawLine(MPI_COLOR, Offset(mpiScreen.x - s, mpiScreen.y), Offset(mpiScreen.x + s, mpiScreen.y), 2.5f)
        drawLine(MPI_COLOR, Offset(mpiScreen.x, mpiScreen.y - s), Offset(mpiScreen.x, mpiScreen.y + s), 2.5f)
    }

    if (vis.offsetLine && state.pointOfAim != null) {
        val poaScreen = toScreen(state.pointOfAim)
        drawLine(OFFSET_LINE_COLOR, poaScreen, mpiScreen, strokeWidth = 2f)
    }

    if (vis.cep && result.cep > 0) {
        drawCircle(CEP_COLOR, pixelsToScreen(result.cep * state.scaleFactor), mpiScreen, style = Stroke(width = 2f))
    }
    if (vis.meanRadius && result.meanRadius > 0) {
        drawCircle(MEAN_RADIUS_COLOR, pixelsToScreen(result.meanRadius * state.scaleFactor), mpiScreen, style = Stroke(width = 2f))
    }
    if (vis.extremeSpread && result.extremeSpread > 0) {
        drawCircle(ES_COLOR, pixelsToScreen(result.extremeSpread * state.scaleFactor / 2.0), mpiScreen, style = Stroke(width = 2f))
    }
}

// ─── Text labels ──────────────────────────────────────────────────────────────

private fun DrawScope.drawLabels(
    labels: List<TextLabel>,
    toScreen: (Offset) -> Offset,
    textMeasurer: TextMeasurer
) {
    labels.forEach { label ->
        val sp = toScreen(label.position)
        val lr = textMeasurer.measure(label.text, TextStyle(fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Medium))
        val p = 4f
        drawRoundRect(
            Color.Black.copy(alpha = 0.6f),
            topLeft = Offset(sp.x - p, sp.y - p),
            size = Size(lr.size.width + p * 2, lr.size.height + p * 2),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f)
        )
        drawText(lr, topLeft = sp)
    }
}

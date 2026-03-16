package com.shotmetrics.app.ui.export

import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shotmetrics.app.domain.model.AngleUnit
import com.shotmetrics.app.domain.model.BallisticsResult
import com.shotmetrics.app.domain.model.LengthUnit
import com.shotmetrics.app.ui.editor.EditorState
import com.shotmetrics.app.ui.editor.OverlayType
import com.shotmetrics.app.ui.editor.OverlayVisibility
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

// ─── Main Screen ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExportPreviewScreen(
    editorState: EditorState,
    onBack: () -> Unit,
    vm: ExportPreviewViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.initialize(editorState) }

    if (state.showSettingsSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { vm.setShowSettingsSheet(false) }, sheetState = sheetState) {
            SettingsSheetContent(state, vm, editorState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.phase == ExportPhase.LABEL) "Edit Label" else "Crop Image", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = { if (state.phase == ExportPhase.CROP) vm.setPhase(ExportPhase.LABEL) else onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (state.isExporting) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(end = 12.dp))
                    } else {
                        if (state.phase == ExportPhase.LABEL) {
                            IconButton(onClick = { vm.setShowSettingsSheet(true) }) { Icon(Icons.Default.Settings, "Settings") }
                        }
                        IconButton(onClick = {
                            vm.exportImage(context) { uri ->
                                if (uri != null) { Toast.makeText(context, "Image saved to gallery", Toast.LENGTH_SHORT).show(); ExportUtils.shareFile(context, uri, "image/png") }
                                else Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
                            }
                        }) { Icon(Icons.Default.Save, "Export") }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth().clipToBounds().background(Color(0xFF1A1A1A))) {
                if (state.phase == ExportPhase.LABEL) LabelPreviewCanvas(editorState, state, vm)
                else CropPreviewCanvas(editorState, state, vm)
            }

            if (state.phase == ExportPhase.LABEL) {
                Surface(tonalElevation = 1.dp) {
                    Button(
                        onClick = { vm.proceedToCrop() },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).navigationBarsPadding()
                    ) {
                        Icon(Icons.Default.ContentCut, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Continue to Crop")
                    }
                }
            } else {
                CropBottomBar(state, vm, context)
            }
        }
    }
}

// ─── Label Preview Canvas ─────────────────────────────────────────────────────

@Composable
private fun LabelPreviewCanvas(editorState: EditorState, exportState: ExportPreviewState, vm: ExportPreviewViewModel) {
    val context = LocalContext.current
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var imageSize by remember { mutableStateOf(Size.Zero) }
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current.density
    val currentState = rememberUpdatedState(exportState)

    LaunchedEffect(editorState.imageUri) {
        editorState.imageUri?.let { uriStr ->
            try {
                val uri = Uri.parse(uriStr)
                val stream = if (uriStr.startsWith("file:")) java.io.File(uri.path!!).inputStream() else context.contentResolver.openInputStream(uri)
                stream?.use { s -> BitmapFactory.decodeStream(s)?.let { bmp -> imageBitmap = bmp.asImageBitmap(); imageSize = Size(bmp.width.toFloat(), bmp.height.toFloat()) } }
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(imageSize) {
        if (imageSize != Size.Zero) vm.initializeLegendPosition(imageSize.width, imageSize.height)
    }

    Canvas(
        modifier = Modifier.fillMaxSize().pointerInput(Unit) {
            awaitEachGesture {
                val first = awaitFirstDown(); first.consume()
                var prevPos = first.position; var prevDist = 0f; var wasMultiTouch = false
                val s = currentState.value
                val labelRect = computeLabelScreenRect(s, editorState, imageSize, textMeasurer, density, size.width.toFloat(), size.height.toFloat())
                val legendRect = computeLegendScreenRect(s, editorState, imageSize, textMeasurer, density, size.width.toFloat(), size.height.toFloat())
                var dragTarget = when {
                    s.showLabel && labelRect != null && labelRect.contains(first.position) -> DragTarget.LABEL
                    s.showLegend && legendRect != null && legendRect.contains(first.position) -> DragTarget.LEGEND
                    else -> DragTarget.IMAGE
                }
                while (true) {
                    val event = awaitPointerEvent(); val changes = event.changes
                    if (changes.isEmpty() || changes.all { !it.pressed }) break
                    val st = currentState.value
                    if (changes.size >= 2) {
                        wasMultiTouch = true
                        val p1 = changes[0].position; val p2 = changes[1].position; val dist = (p1 - p2).getDistance()
                        val c = (p1 + p2) / 2f; val pc = (changes[0].previousPosition + changes[1].previousPosition) / 2f
                        if (dragTarget == DragTarget.LABEL) { if (prevDist > 0f && dist > 0f) vm.setLabelScale(st.labelScale * (dist / prevDist)) }
                        else {
                            if (prevDist > 0f && dist > 0f) {
                                val zr = dist / prevDist; val nz = (st.imageZoom * zr).coerceIn(0.5f, 5f); val ratio = nz / st.imageZoom
                                val cW = size.width.toFloat(); val cH = size.height.toFloat()
                                val s0 = computeImageFit(cW, cH, imageSize).third; val oSc = s0 * st.imageZoom
                                val oOX = (cW - imageSize.width * oSc) / 2f + st.imageOffset.x; val oOY = (cH - imageSize.height * oSc) / 2f + st.imageOffset.y
                                val zOX = pc.x - (pc.x - oOX) * ratio; val zOY = pc.y - (pc.y - oOY) * ratio
                                val nSc = s0 * nz
                                val nOffX = zOX + (c.x - pc.x) - (cW - imageSize.width * nSc) / 2f
                                val nOffY = zOY + (c.y - pc.y) - (cH - imageSize.height * nSc) / 2f
                                vm.setImageZoom(nz); vm.setImageOffset(Offset(nOffX, nOffY))
                            } else { vm.setImageOffset(st.imageOffset + (c - pc)) }
                        }
                        prevDist = dist; changes.forEach { it.consume() }
                    } else {
                        val change = changes.first()
                        if (wasMultiTouch) {
                            prevPos = change.position; wasMultiTouch = false; prevDist = 0f; change.consume()
                        } else {
                            val delta = change.position - prevPos; prevPos = change.position
                            when (dragTarget) {
                                DragTarget.LABEL -> { val s0 = computeImageFit(size.width.toFloat(), size.height.toFloat(), imageSize).third * st.imageZoom; if (s0 > 0f) vm.setLabelOffsetPx(st.labelOffsetPx + Offset(delta.x / s0, delta.y / s0)) }
                                DragTarget.LEGEND -> { val s0 = computeImageFit(size.width.toFloat(), size.height.toFloat(), imageSize).third * st.imageZoom; if (s0 > 0f) vm.setLegendOffsetPx(st.legendOffsetPx + Offset(delta.x / s0, delta.y / s0)) }
                                DragTarget.IMAGE -> vm.setImageOffset(st.imageOffset + delta)
                            }
                            change.consume()
                        }
                    }
                }
            }
        }
    ) {
        val bmp = imageBitmap ?: return@Canvas
        val s0 = min(size.width / imageSize.width, size.height / imageSize.height); val sc = s0 * exportState.imageZoom
        val dW = imageSize.width * sc; val dH = imageSize.height * sc
        val oX = (size.width - dW) / 2f + exportState.imageOffset.x; val oY = (size.height - dH) / 2f + exportState.imageOffset.y
        drawImage(bmp, srcOffset = IntOffset.Zero, srcSize = IntSize(bmp.width, bmp.height), dstOffset = IntOffset(oX.toInt(), oY.toInt()), dstSize = IntSize(dW.toInt(), dH.toInt()))
        fun i2s(pt: Offset) = Offset(pt.x * sc + oX, pt.y * sc + oY)
        fun p2s(px: Double) = (px * sc).toFloat()
        drawMarkersAndOverlays(editorState, exportState, ::i2s, ::p2s, exportState.imageZoom)
        if (exportState.showLegend) editorState.ballisticsResult?.let { drawLegendPreview(it, exportState.overlayVisibility, editorState, oX + dW / 2f, oY + dH / 2f, sc, textMeasurer, density, exportState.legendOffsetPx) }
        if (exportState.showLabel) drawDataLabelPreview(exportState, editorState, oX + dW / 2f, oY + dH / 2f, sc, textMeasurer, density)
    }
}

// ─── Crop Preview Canvas ──────────────────────────────────────────────────────

@Composable
private fun CropPreviewCanvas(editorState: EditorState, exportState: ExportPreviewState, vm: ExportPreviewViewModel) {
    val context = LocalContext.current
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var imageSize by remember { mutableStateOf(Size.Zero) }
    val textMeasurer = rememberTextMeasurer(); val density = LocalDensity.current.density; val currentState = rememberUpdatedState(exportState)
    LaunchedEffect(editorState.imageUri) {
        editorState.imageUri?.let { uriStr -> try { val uri = Uri.parse(uriStr); val stream = if (uriStr.startsWith("file:")) java.io.File(uri.path!!).inputStream() else context.contentResolver.openInputStream(uri); stream?.use { s -> BitmapFactory.decodeStream(s)?.let { bmp -> imageBitmap = bmp.asImageBitmap(); imageSize = Size(bmp.width.toFloat(), bmp.height.toFloat()) } } } catch (_: Exception) {} }
    }
    var imgDrawRect by remember { mutableStateOf(Rect.Zero) }
    Canvas(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
        awaitEachGesture {
            val first = awaitFirstDown(); first.consume()
            var prevPos = first.position; var prevDist = 0f; var wasMultiTouch = false
            val st = currentState.value; val rect = imgDrawRect
            var cropHandle = -1
            if (rect.width > 0f && rect.height > 0f) {
                val nc = st.cropRect ?: Rect(0f,0f,1f,1f)
                val crop = Rect(rect.left + nc.left * rect.width, rect.top + nc.top * rect.height, rect.left + nc.right * rect.width, rect.top + nc.bottom * rect.height)
                val th = 48f; val corners = listOf(crop.topLeft, crop.topRight, crop.bottomLeft, crop.bottomRight)
                val edges = listOf(Offset(crop.center.x, crop.top), Offset(crop.right, crop.center.y), Offset(crop.center.x, crop.bottom), Offset(crop.left, crop.center.y))
                cropHandle = corners.indexOfFirst { (first.position - it).getDistance() < th }
                if (cropHandle == -1) { val ei = edges.indexOfFirst { (first.position - it).getDistance() < th }; cropHandle = if (ei >= 0) 10 + ei else if (crop.contains(first.position)) 4 else -1 }
            }
            while (true) {
                val event = awaitPointerEvent(); val changes = event.changes
                if (changes.isEmpty() || changes.all { !it.pressed }) break
                val cst = currentState.value
                if (cropHandle >= 0 && changes.size < 2 && !wasMultiTouch) {
                    val change = changes.first(); val da = change.position - prevPos; prevPos = change.position
                    val r = imgDrawRect
                    if (r.width > 0f && r.height > 0f) {
                        val dx = da.x / r.width; val dy = da.y / r.height; val c = cst.cropRect ?: Rect(0f,0f,1f,1f); val mn = 0.08f
                        val nr = when (cropHandle) {
                            0 -> Rect((c.left+dx).coerceIn(0f,c.right-mn),(c.top+dy).coerceIn(0f,c.bottom-mn),c.right,c.bottom)
                            1 -> Rect(c.left,(c.top+dy).coerceIn(0f,c.bottom-mn),(c.right+dx).coerceIn(c.left+mn,1f),c.bottom)
                            2 -> Rect((c.left+dx).coerceIn(0f,c.right-mn),c.top,c.right,(c.bottom+dy).coerceIn(c.top+mn,1f))
                            3 -> Rect(c.left,c.top,(c.right+dx).coerceIn(c.left+mn,1f),(c.bottom+dy).coerceIn(c.top+mn,1f))
                            4 -> { val w=c.width;val h=c.height;val nl=(c.left+dx).coerceIn(0f,1f-w);val nt=(c.top+dy).coerceIn(0f,1f-h);Rect(nl,nt,nl+w,nt+h) }
                            10 -> Rect(c.left,(c.top+dy).coerceIn(0f,c.bottom-mn),c.right,c.bottom)
                            11 -> Rect(c.left,c.top,(c.right+dx).coerceIn(c.left+mn,1f),c.bottom)
                            12 -> Rect(c.left,c.top,c.right,(c.bottom+dy).coerceIn(c.top+mn,1f))
                            13 -> Rect((c.left+dx).coerceIn(0f,c.right-mn),c.top,c.right,c.bottom)
                            else -> c
                        }; vm.setCropRect(nr)
                    }; change.consume()
                } else if (changes.size >= 2) {
                    wasMultiTouch = true; cropHandle = -1
                    val p1 = changes[0].position; val p2 = changes[1].position; val dist = (p1 - p2).getDistance()
                    val c = (p1 + p2) / 2f; val pc = (changes[0].previousPosition + changes[1].previousPosition) / 2f
                    if (prevDist > 0f && dist > 0f) {
                        val zr = dist / prevDist; val nz = (cst.imageZoom * zr).coerceIn(0.5f, 5f); val ratio = nz / cst.imageZoom
                        val cW = size.width.toFloat(); val cH = size.height.toFloat()
                        val s0 = computeImageFit(cW, cH, imageSize).third; val oSc = s0 * cst.imageZoom
                        val oOX = (cW - imageSize.width * oSc) / 2f + cst.imageOffset.x; val oOY = (cH - imageSize.height * oSc) / 2f + cst.imageOffset.y
                        val zOX = pc.x - (pc.x - oOX) * ratio; val zOY = pc.y - (pc.y - oOY) * ratio; val nSc = s0 * nz
                        val nOffX = zOX + (c.x - pc.x) - (cW - imageSize.width * nSc) / 2f; val nOffY = zOY + (c.y - pc.y) - (cH - imageSize.height * nSc) / 2f
                        vm.setImageZoom(nz); vm.setImageOffset(Offset(nOffX, nOffY))
                    } else { vm.setImageOffset(cst.imageOffset + (c - pc)) }
                    prevDist = dist; changes.forEach { it.consume() }
                } else {
                    val change = changes.first()
                    if (wasMultiTouch) {
                        prevPos = change.position; wasMultiTouch = false; prevDist = 0f; cropHandle = -1; change.consume()
                    } else {
                        val delta = change.position - prevPos; prevPos = change.position
                        vm.setImageOffset(cst.imageOffset + delta); change.consume()
                    }
                }
            }
        }
    }) {
        val bmp = imageBitmap ?: return@Canvas
        val s0 = min(size.width / imageSize.width, size.height / imageSize.height); val sc = s0 * exportState.imageZoom
        val dW = imageSize.width * sc; val dH = imageSize.height * sc
        val oX = (size.width - dW) / 2f + exportState.imageOffset.x; val oY = (size.height - dH) / 2f + exportState.imageOffset.y
        imgDrawRect = Rect(oX, oY, oX + dW, oY + dH)
        drawImage(bmp, srcOffset = IntOffset.Zero, srcSize = IntSize(bmp.width, bmp.height), dstOffset = IntOffset(oX.toInt(), oY.toInt()), dstSize = IntSize(dW.toInt(), dH.toInt()))
        fun i2s(pt: Offset) = Offset(pt.x * sc + oX, pt.y * sc + oY)
        fun p2s(px: Double) = (px * sc).toFloat()
        drawMarkersAndOverlays(editorState, exportState, ::i2s, ::p2s, exportState.imageZoom)
        val labelZoomFactor = if (exportState.labelEditZoom > 0f) exportState.imageZoom / exportState.labelEditZoom else 1f
        if (exportState.showLegend) editorState.ballisticsResult?.let { drawLegendPreview(it, exportState.overlayVisibility, editorState, oX + dW / 2f, oY + dH / 2f, sc, textMeasurer, density, exportState.legendOffsetPx, labelZoomFactor) }
        if (exportState.showLabel) drawDataLabelPreview(exportState, editorState, oX + dW / 2f, oY + dH / 2f, sc, textMeasurer, density, labelZoomFactor)
        val nc = exportState.cropRect ?: Rect(0f,0f,1f,1f); val crop = Rect(oX + nc.left * dW, oY + nc.top * dH, oX + nc.right * dW, oY + nc.bottom * dH)
        val dim = Color.Black.copy(alpha = 0.55f)
        drawRect(dim, Offset.Zero, Size(size.width, crop.top)); drawRect(dim, Offset(0f, crop.bottom), Size(size.width, size.height - crop.bottom))
        drawRect(dim, Offset(0f, crop.top), Size(crop.left, crop.height)); drawRect(dim, Offset(crop.right, crop.top), Size(size.width - crop.right, crop.height))
        drawRect(Color.White, Offset(crop.left, crop.top), Size(crop.width, crop.height), style = Stroke(1.5f))
        val tw = crop.width / 3f; val th = crop.height / 3f
        for (i in 1..2) { drawLine(Color.White.copy(0.3f), Offset(crop.left + tw * i, crop.top), Offset(crop.left + tw * i, crop.bottom), 0.5f); drawLine(Color.White.copy(0.3f), Offset(crop.left, crop.top + th * i), Offset(crop.right, crop.top + th * i), 0.5f) }
        val hl = 28f; val hw = 5f
        listOf(crop.topLeft to arrayOf(Offset(1f,0f),Offset(0f,1f)), crop.topRight to arrayOf(Offset(-1f,0f),Offset(0f,1f)), crop.bottomLeft to arrayOf(Offset(1f,0f),Offset(0f,-1f)), crop.bottomRight to arrayOf(Offset(-1f,0f),Offset(0f,-1f))).forEach { (corner, dirs) -> dirs.forEach { dir -> drawLine(Color.White, corner, Offset(corner.x + dir.x * hl, corner.y + dir.y * hl), hw) } }
        val bL = 22f; val bW = 4f
        listOf(Offset(crop.center.x,crop.top), Offset(crop.right,crop.center.y), Offset(crop.center.x,crop.bottom), Offset(crop.left,crop.center.y)).forEachIndexed { i, pos -> if (i % 2 == 0) drawLine(Color.White, Offset(pos.x - bL, pos.y), Offset(pos.x + bL, pos.y), bW) else drawLine(Color.White, Offset(pos.x, pos.y - bL), Offset(pos.x, pos.y + bL), bW) }
    }
}

// ─── Shared Drawing ───────────────────────────────────────────────────────────

private enum class DragTarget { IMAGE, LABEL, LEGEND }

private fun DrawScope.drawMarkersAndOverlays(es: EditorState, xs: ExportPreviewState, i2s: (Offset) -> Offset, p2s: (Double) -> Float, zoomFactor: Float = 1f) {
    val z = zoomFactor
    es.impacts.filter { it.enabled }.forEach { imp -> val sp = i2s(imp.position); val r = 10f * z; val g = r * 0.55f; val sw = 1.5f * z; drawCircle(Color(0xFF00E676), r, sp, style = Stroke(sw)); drawReticle(sp, r, g, Color(0xFF00E676), sw) }
    es.pointOfAim?.let { poa -> val sp = i2s(poa); val r = 12f * z; val g = r * 0.55f; val sw = 1.5f * z; drawCircle(Color(0x3342A5F5), r, sp, style = Fill); drawCircle(Color(0xFF42A5F5), r, sp, style = Stroke(sw)); drawReticle(sp, r, g, Color(0xFF42A5F5), sw); drawCircle(Color(0xFF42A5F5), sw * 1.2f, sp, style = Fill) }
    val res = es.ballisticsResult; val vis = xs.overlayVisibility
    if (res != null) { val m = i2s(res.meanPointOfImpact); val sf = es.scaleFactor; if (vis.mpi) { drawCircle(Color(0xFFFF9800), 5f * z, m, style = Fill); drawLine(Color(0xFFFF9800), Offset(m.x - 10f * z, m.y), Offset(m.x + 10f * z, m.y), 2f * z); drawLine(Color(0xFFFF9800), Offset(m.x, m.y - 10f * z), Offset(m.x, m.y + 10f * z), 2f * z) }; if (vis.offsetLine && es.pointOfAim != null) drawLine(Color(0xFFFFEB3B), i2s(es.pointOfAim), m, strokeWidth = 1.5f * z); if (vis.cep && res.cep > 0) drawCircle(Color(0xFF9C27B0), p2s(res.cep * sf), m, style = Stroke(1.5f * z)); if (vis.meanRadius && res.meanRadius > 0) drawCircle(Color(0xFF00BCD4), p2s(res.meanRadius * sf), m, style = Stroke(1.5f * z)); if (vis.extremeSpread && res.extremeSpread > 0) drawCircle(Color(0xFFFF5722), p2s(res.extremeSpread * sf / 2.0), m, style = Stroke(1.5f * z)) }
}

private fun DrawScope.drawReticle(c: Offset, r: Float, g: Float, col: Color, sw: Float) { drawLine(col, Offset(c.x-r,c.y), Offset(c.x-g,c.y), sw); drawLine(col, Offset(c.x+r,c.y), Offset(c.x+g,c.y), sw); drawLine(col, Offset(c.x,c.y-r), Offset(c.x,c.y-g), sw); drawLine(col, Offset(c.x,c.y+r), Offset(c.x,c.y+g), sw) }

// ─── Data Label ───────────────────────────────────────────────────────────────

/**
 * Derives a length-to-angle conversion factor from the BallisticsResult.
 * Returns the ratio such that (lengthValue * ratio) = angleValue.
 */
private fun anglePerLength(result: BallisticsResult, angleUnit: AngleUnit): Double {
    if (result.extremeSpread <= 0) return 0.0
    return when (angleUnit) {
        AngleUnit.MOA -> result.groupSizeMoa / result.extremeSpread
        AngleUnit.MIL -> result.groupSizeMil / result.extremeSpread
    }
}

private fun angleToLinearInch(angle: Double, angleUnit: AngleUnit, distYards: Double): Double {
    return when (angleUnit) {
        AngleUnit.MOA -> angle * 1.047 * distYards / 100.0
        AngleUnit.MIL -> angle * 3.6 * distYards / 100.0
    }
}

private fun formatAtzValue(atz: com.shotmetrics.app.domain.model.ATZResult, es: EditorState, unit: ATZDisplayUnit): String {
    val wDir = atz.windageDirection.arrow; val eDir = atz.elevationDirection.arrow
    return when (unit) {
        ATZDisplayUnit.CLICKS -> "ATZ: %s%d / %s%d clicks".format(wDir, atz.windageClicksRounded, eDir, atz.elevationClicksRounded)
        ATZDisplayUnit.MOA -> {
            val wMoa = if (atz.angleUnit == AngleUnit.MOA) atz.windageAngle else atz.windageAngle * 3.43775
            val eMoa = if (atz.angleUnit == AngleUnit.MOA) atz.elevationAngle else atz.elevationAngle * 3.43775
            "ATZ: %s%.2f / %s%.2f MOA".format(wDir, wMoa, eDir, eMoa)
        }
        ATZDisplayUnit.MIL -> {
            val wMil = if (atz.angleUnit == AngleUnit.MIL) atz.windageAngle else atz.windageAngle / 3.43775
            val eMil = if (atz.angleUnit == AngleUnit.MIL) atz.elevationAngle else atz.elevationAngle / 3.43775
            "ATZ: %s%.2f / %s%.2f MIL".format(wDir, wMil, eDir, eMil)
        }
        ATZDisplayUnit.INCH -> {
            val distYards = when (es.distanceUnit) { com.shotmetrics.app.domain.model.DistanceUnit.YARDS -> es.distanceToTarget; com.shotmetrics.app.domain.model.DistanceUnit.METERS -> es.distanceToTarget * 1.09361 }
            val wIn = angleToLinearInch(atz.windageAngle, atz.angleUnit, distYards)
            val eIn = angleToLinearInch(atz.elevationAngle, atz.angleUnit, distYards)
            "ATZ: %s%.2f / %s%.2f in".format(wDir, wIn, eDir, eIn)
        }
        ATZDisplayUnit.CM -> {
            val distYards = when (es.distanceUnit) { com.shotmetrics.app.domain.model.DistanceUnit.YARDS -> es.distanceToTarget; com.shotmetrics.app.domain.model.DistanceUnit.METERS -> es.distanceToTarget * 1.09361 }
            val wCm = angleToLinearInch(atz.windageAngle, atz.angleUnit, distYards) * 2.54
            val eCm = angleToLinearInch(atz.elevationAngle, atz.angleUnit, distYards) * 2.54
            "ATZ: %s%.2f / %s%.2f cm".format(wDir, wCm, eDir, eCm)
        }
    }
}

private fun buildLabelLines(state: ExportPreviewState, es: EditorState): List<Triple<String, Boolean, Boolean>> {
    val result = es.ballisticsResult; val atz = es.atzResult
    val aUnit = state.labelAngleUnit; val lUnit = state.labelLengthUnit
    val lines = mutableListOf<Triple<String, Boolean, Boolean>>()
    val hasTitle = state.customTitle.isNotBlank()
    if (hasTitle) lines.add(Triple(state.customTitle, true, true))
    val dist = es.distanceToTarget.let { if (it == it.toLong().toDouble()) it.toLong().toString() else "%.1f".format(it) }
    lines.add(Triple("$dist ${es.distanceUnit.abbreviation} / ${es.enabledImpacts.size} Shot Group", !hasTitle, true))
    if (state.locationText.isNotBlank()) lines.add(Triple("\uD83D\uDCCD ${state.locationText}", false, false))
    lines.add(Triple(state.customDate.ifBlank { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }, false, false))
    lines.add(Triple("", false, false))

    val apl = result?.let { anglePerLength(it, aUnit) } ?: 0.0

    state.labelDataFlags.forEach { flag ->
        when (flag) {
            LabelDataType.GROUP_SIZE -> result?.let {
                val angle = when (aUnit) { AngleUnit.MOA -> it.groupSizeMoa; AngleUnit.MIL -> it.groupSizeMil }
                lines.add(Triple("Group: %.3f %s / %.2f %s".format(it.extremeSpread, lUnit.abbreviation, angle, aUnit.abbreviation), true, false))
            }
            LabelDataType.EXTREME_SPREAD -> result?.let { lines.add(Triple("ES: %.3f %s".format(it.extremeSpread, lUnit.abbreviation), false, false)) }
            LabelDataType.MEAN_RADIUS -> result?.let {
                val mrAngle = it.meanRadius * apl
                lines.add(Triple("MR: %.3f %s / %.2f %s".format(it.meanRadius, lUnit.abbreviation, mrAngle, aUnit.abbreviation), true, false))
            }
            LabelDataType.CEP -> result?.let { lines.add(Triple("CEP: %.3f %s".format(it.cep, lUnit.abbreviation), false, false)) }
            LabelDataType.MPI_OFFSET -> result?.let { lines.add(Triple("W: %.2f / E: %.2f %s".format(it.meanWindage, it.meanElevation, aUnit.abbreviation), false, false)) }
            LabelDataType.RADIAL_SD -> result?.let { lines.add(Triple("R SD: %.3f %s".format(it.radialSD, lUnit.abbreviation), false, false)) }
            LabelDataType.VERTICAL_SD -> result?.let { lines.add(Triple("V SD: %.3f %s".format(it.verticalSD, lUnit.abbreviation), false, false)) }
            LabelDataType.HORIZONTAL_SD -> result?.let { lines.add(Triple("H SD: %.3f %s".format(it.horizontalSD, lUnit.abbreviation), false, false)) }
            LabelDataType.ATZ -> atz?.let {
                lines.add(Triple(formatAtzValue(it, es, state.atzDisplayUnit), false, false))
            }
        }
    }
    while (lines.isNotEmpty() && lines.last().first.isEmpty()) lines.removeAt(lines.lastIndex)
    return lines
}

private fun DrawScope.drawDataLabelPreview(state: ExportPreviewState, es: EditorState, imgCX: Float, imgCY: Float, imgScale: Float, tm: TextMeasurer, density: Float, imageZoom: Float = 1f) {
    val lines = buildLabelLines(state, es); if (lines.isEmpty()) return
    val baseSp = 13f * state.labelScale * imageZoom; val pad = baseSp * density * 0.6f; val sepH = baseSp * density * 0.4f
    val measured = lines.map { (text, bold, _) -> if (text.isEmpty()) null else tm.measure(text, TextStyle(fontSize = baseSp.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, color = Color.Black.copy(alpha = state.labelAlpha))) }
    val maxW = measured.maxOf { it?.size?.width?.toFloat() ?: 0f }; val labelW = maxW + pad * 2
    var totalH = pad * 2; lines.forEachIndexed { i, (text, _, _) -> totalH += if (text.isEmpty()) sepH else (measured[i]?.size?.height?.toFloat() ?: 0f) * 1.15f }
    val lx = imgCX + state.labelOffsetPx.x * imgScale - labelW / 2f; val ly = imgCY + state.labelOffsetPx.y * imgScale - totalH / 2f
    drawRoundRect(Color.White.copy(alpha = state.labelAlpha), Offset(lx, ly), Size(labelW, totalH), CornerRadius(pad * 0.5f))
    drawRoundRect(Color.Black.copy(alpha = state.labelAlpha * 0.5f), Offset(lx, ly), Size(labelW, totalH), CornerRadius(pad * 0.5f), style = Stroke(1.5f * imageZoom))
    var cy = ly + pad
    lines.forEachIndexed { i, (text, _, _) -> if (text.isEmpty()) cy += sepH else { val lr = measured[i] ?: return@forEachIndexed; drawText(lr, topLeft = Offset(lx + (labelW - lr.size.width) / 2f, cy)); cy += lr.size.height * 1.15f } }
}

private fun DrawScope.drawLegendPreview(result: BallisticsResult, vis: OverlayVisibility, es: EditorState, imgCX: Float, imgCY: Float, imgScale: Float, tm: TextMeasurer, density: Float, legendOff: Offset, imageZoom: Float = 1f) {
    val entries = mutableListOf<Pair<Color, String>>(); val u = es.lengthUnit.abbreviation
    if (vis.mpi) entries.add(Color(0xFFFF9800) to "MPI"); if (vis.cep && result.cep > 0) entries.add(Color(0xFF9C27B0) to "CEP: %.3f $u".format(result.cep))
    if (vis.meanRadius && result.meanRadius > 0) entries.add(Color(0xFF00BCD4) to "MR: %.3f $u".format(result.meanRadius)); if (vis.extremeSpread && result.extremeSpread > 0) entries.add(Color(0xFFFF5722) to "ES: %.3f $u".format(result.extremeSpread))
    if (vis.offsetLine) entries.add(Color(0xFFFFEB3B) to "POA→MPI"); if (entries.isEmpty()) return
    val fSp = 10f * imageZoom; val p = 6f * imageZoom; val dR = fSp * density * 0.3f; val me = entries.map { (c, t) -> c to tm.measure(t, TextStyle(fontSize = fSp.sp, color = Color.White)) }
    val mTW = me.maxOf { it.second.size.width.toFloat() }; val lH = me.maxOf { it.second.size.height.toFloat() } * 1.3f; val lW = dR * 2 + p * 3 + mTW + p; val lgH = entries.size * lH + p * 2
    val lx = imgCX + legendOff.x * imgScale - lW / 2f; val ly = imgCY + legendOff.y * imgScale - lgH / 2f
    drawRoundRect(Color.Black.copy(alpha = 0.6f), Offset(lx, ly), Size(lW, lgH), CornerRadius(4f * imageZoom))
    me.forEachIndexed { i, (color, lr) -> val cy = ly + p + i * lH + lH / 2f; drawCircle(color, dR, Offset(lx + p + dR, cy), style = Fill); drawText(lr, topLeft = Offset(lx + p + dR * 2 + p, cy - lr.size.height / 2f)) }
}

// ─── Hit-testing ──────────────────────────────────────────────────────────────

private fun computeImageFit(cW: Float, cH: Float, imageSize: Size): Triple<Float, Float, Float> { if (imageSize.width <= 0f || imageSize.height <= 0f) return Triple(0f, 0f, 1f); val s = min(cW / imageSize.width, cH / imageSize.height); return Triple((cW - imageSize.width * s) / 2f, (cH - imageSize.height * s) / 2f, s) }

private fun computeLabelScreenRect(state: ExportPreviewState, es: EditorState, imageSize: Size, tm: TextMeasurer, density: Float, cW: Float, cH: Float): Rect? {
    if (!state.showLabel) return null; val lines = buildLabelLines(state, es); if (lines.isEmpty()) return null
    val fit = computeImageFit(cW, cH, imageSize); val sc = fit.third * state.imageZoom; val dW = imageSize.width * sc; val dH = imageSize.height * sc
    val oX = (cW - dW) / 2f + state.imageOffset.x; val oY = (cH - dH) / 2f + state.imageOffset.y; val cx = oX + dW / 2f; val cy = oY + dH / 2f
    val baseSp = 13f * state.labelScale; val pad = baseSp * density * 0.6f; val sepH = baseSp * density * 0.4f
    val measured = lines.map { (text, bold, _) -> if (text.isEmpty()) null else tm.measure(text, TextStyle(fontSize = baseSp.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)) }
    val maxW = measured.maxOf { it?.size?.width?.toFloat() ?: 0f }; val lW = maxW + pad * 2; var tH = pad * 2; lines.forEachIndexed { i, (text, _, _) -> tH += if (text.isEmpty()) sepH else (measured[i]?.size?.height?.toFloat() ?: 0f) * 1.15f }
    val lx = cx + state.labelOffsetPx.x * sc - lW / 2f; val ly = cy + state.labelOffsetPx.y * sc - tH / 2f; return Rect(lx, ly, lx + lW, ly + tH)
}

private fun computeLegendScreenRect(state: ExportPreviewState, es: EditorState, imageSize: Size, tm: TextMeasurer, density: Float, cW: Float, cH: Float): Rect? {
    if (!state.showLegend) return null; val result = es.ballisticsResult ?: return null; val vis = state.overlayVisibility
    val entries = mutableListOf<String>(); val u = es.lengthUnit.abbreviation; if (vis.mpi) entries.add("MPI"); if (vis.cep && result.cep > 0) entries.add("CEP"); if (vis.meanRadius && result.meanRadius > 0) entries.add("MR"); if (vis.extremeSpread && result.extremeSpread > 0) entries.add("ES"); if (vis.offsetLine) entries.add("POA→MPI"); if (entries.isEmpty()) return null
    val fit = computeImageFit(cW, cH, imageSize); val sc = fit.third * state.imageZoom; val dW = imageSize.width * sc; val dH = imageSize.height * sc
    val oX = (cW - dW) / 2f + state.imageOffset.x; val oY = (cH - dH) / 2f + state.imageOffset.y
    val cx = oX + dW / 2f; val cy = oY + dH / 2f
    val fSp = 10f; val p = 6f; val dR = fSp * density * 0.3f; val mW = entries.maxOf { tm.measure(it, TextStyle(fontSize = fSp.sp)).size.width.toFloat() }; val lH = tm.measure("X", TextStyle(fontSize = fSp.sp)).size.height * 1.3f
    val lW = dR * 2 + p * 3 + mW + p; val lgH = entries.size * lH + p * 2; val lx = cx + state.legendOffsetPx.x * sc - lW / 2f; val ly = cy + state.legendOffsetPx.y * sc - lgH / 2f; return Rect(lx, ly, lx + lW, ly + lgH)
}

// ─── Bottom Bar ───────────────────────────────────────────────────────────────

@Composable
private fun CropBottomBar(state: ExportPreviewState, vm: ExportPreviewViewModel, context: android.content.Context) {
    Surface(tonalElevation = 3.dp) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).navigationBarsPadding(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { vm.resetCrop() }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Reset") }
            Button(onClick = { vm.exportImage(context) { uri -> if (uri != null) { Toast.makeText(context, "Image saved to gallery", Toast.LENGTH_SHORT).show(); ExportUtils.shareFile(context, uri, "image/png") } else Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show() } }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Save") }
        }
    }
}

// ─── Settings Sheet ───────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsSheetContent(state: ExportPreviewState, vm: ExportPreviewViewModel, es: EditorState) {
    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 32.dp).navigationBarsPadding()) {
        // Label content
        Text("Label", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) { Text("Show Label", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f)); Switch(checked = state.showLabel, onCheckedChange = { vm.setShowLabel(it) }) }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(state.customTitle, { vm.setCustomTitle(it) }, label = { Text("Custom Title (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(state.locationText, { vm.setLocationText(it) }, label = { Text("Location") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(state.customDate, { vm.setCustomDate(it) }, label = { Text("Date (leave blank for today)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())) })
        Spacer(Modifier.height(8.dp))
        Text("Label Opacity", style = MaterialTheme.typography.labelMedium)
        Slider(value = state.labelAlpha, onValueChange = { vm.setLabelAlpha(it) }, valueRange = 0.3f..1f)

        Spacer(Modifier.height(8.dp)); HorizontalDivider(); Spacer(Modifier.height(12.dp))

        Text("Data to display", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            LabelDataType.entries.forEach { type -> FilterChip(selected = type in state.labelDataFlags, onClick = { vm.toggleLabelData(type) }, label = { Text(type.displayName, style = MaterialTheme.typography.labelSmall) }) }
        }

        Spacer(Modifier.height(16.dp)); HorizontalDivider(); Spacer(Modifier.height(12.dp))

        Text("Display Units", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        Text("Angle unit (Group, MR, MPI)", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { AngleUnit.entries.forEach { unit -> FilterChip(selected = state.labelAngleUnit == unit, onClick = { vm.setLabelAngleUnit(unit) }, label = { Text(unit.abbreviation) }) } }
        Spacer(Modifier.height(8.dp))
        Text("Length unit (Group, MR, ES, CEP, SD)", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { LengthUnit.entries.forEach { unit -> FilterChip(selected = state.labelLengthUnit == unit, onClick = { vm.setLabelLengthUnit(unit) }, label = { Text(unit.abbreviation) }) } }
        Spacer(Modifier.height(8.dp))
        Text("ATZ unit", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(4.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { ATZDisplayUnit.entries.forEach { unit -> FilterChip(selected = state.atzDisplayUnit == unit, onClick = { vm.setAtzDisplayUnit(unit) }, label = { Text(unit.abbreviation) }) } }

        Spacer(Modifier.height(16.dp)); HorizontalDivider(); Spacer(Modifier.height(12.dp))

        Text("Overlays", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        listOf(Triple(OverlayType.MPI, "MPI", Color(0xFFFF9800)), Triple(OverlayType.CEP, "CEP", Color(0xFF9C27B0)), Triple(OverlayType.MEAN_RADIUS, "Mean Radius", Color(0xFF00BCD4)), Triple(OverlayType.EXTREME_SPREAD, "Extreme Spread", Color(0xFFFF5722)), Triple(OverlayType.OFFSET_LINE, "POA → MPI", Color(0xFFFFEB3B))).forEach { (type, name, color) ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { vm.toggleOverlay(type) }.padding(vertical = 2.dp)) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(color)); Spacer(Modifier.width(8.dp))
                Text(name, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                Checkbox(checked = when (type) { OverlayType.MPI -> state.overlayVisibility.mpi; OverlayType.CEP -> state.overlayVisibility.cep; OverlayType.MEAN_RADIUS -> state.overlayVisibility.meanRadius; OverlayType.EXTREME_SPREAD -> state.overlayVisibility.extremeSpread; OverlayType.OFFSET_LINE -> state.overlayVisibility.offsetLine }, onCheckedChange = { vm.toggleOverlay(type) }, modifier = Modifier.size(32.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) { Text("Show Legend", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f)); Switch(checked = state.showLegend, onCheckedChange = { vm.setShowLegend(it) }) }
    }
}

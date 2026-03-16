package com.shotmetrics.app.ui.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.shotmetrics.app.domain.model.BallisticsResult
import com.shotmetrics.app.ui.editor.EditorState
import com.shotmetrics.app.ui.editor.OverlayVisibility
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object ExportUtils {

    fun exportImage(
        context: Context,
        state: EditorState,
        onComplete: (Uri?) -> Unit
    ) {
        try {
            val sourceUri = state.imageUri ?: run { onComplete(null); return }
            val uri = Uri.parse(sourceUri)
            val inputStream = if (sourceUri.startsWith("file:")) File(uri.path!!).inputStream()
            else context.contentResolver.openInputStream(uri)
            val sourceBitmap = inputStream?.use { BitmapFactory.decodeStream(it) } ?: run { onComplete(null); return }
            val bitmap = sourceBitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(bitmap)
            drawOverlaysOnBitmap(canvas, bitmap.width, bitmap.height, state)
            val savedUri = saveBitmapToGallery(context, bitmap, "ShotMetrics_${timestamp()}")
            bitmap.recycle(); sourceBitmap.recycle()
            onComplete(savedUri)
        } catch (e: Exception) {
            Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
            onComplete(null)
        }
    }

    fun exportCSV(context: Context, state: EditorState, result: BallisticsResult): Uri? {
        try {
            val csv = buildString {
                appendLine("ShotMetrics - Shot Data Export")
                appendLine("Date,${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
                appendLine("Caliber,${state.caliber}")
                appendLine("Distance,${state.distanceToTarget} ${state.distanceUnit.abbreviation}")
                appendLine()
                appendLine("Statistics")
                appendLine("Group Size (MOA),${f(result.groupSizeMoa)}")
                appendLine("Group Size (MIL),${f(result.groupSizeMil)}")
                appendLine("Extreme Spread (${state.lengthUnit.abbreviation}),${f(result.extremeSpread)}")
                appendLine("Mean Radius (${state.lengthUnit.abbreviation}),${f(result.meanRadius)}")
                appendLine("CEP (${state.lengthUnit.abbreviation}),${f(result.cep)}")
                appendLine("Radial SD (${state.lengthUnit.abbreviation}),${f(result.radialSD)}")
                appendLine("Vertical SD (${state.lengthUnit.abbreviation}),${f(result.verticalSD)}")
                appendLine("Horizontal SD (${state.lengthUnit.abbreviation}),${f(result.horizontalSD)}")
                appendLine("Mean Windage (${state.angleUnit.abbreviation}),${f(result.meanWindage)}")
                appendLine("Mean Elevation (${state.angleUnit.abbreviation}),${f(result.meanElevation)}")
                appendLine()
                appendLine("Impact Points")
                appendLine("ID,X (px),Y (px),Enabled")
                state.impacts.forEach { impact ->
                    appendLine("${impact.id},${f(impact.position.x.toDouble())},${f(impact.position.y.toDouble())},${impact.enabled}")
                }
                state.pointOfAim?.let { poa ->
                    appendLine(); appendLine("Point of Aim"); appendLine("X (px),Y (px)")
                    appendLine("${f(poa.x.toDouble())},${f(poa.y.toDouble())}")
                }
            }
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val file = File(exportDir, "ShotMetrics_${timestamp()}.csv")
            file.writeText(csv)
            return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            Toast.makeText(context, "CSV export failed: ${e.message}", Toast.LENGTH_SHORT).show()
            return null
        }
    }

    fun shareFile(context: Context, uri: Uri, mimeType: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share via"))
    }

    // ── Advanced export with label / crop / overlay config ───────────────────

    fun exportImageWithOptions(context: Context, state: EditorState, config: ExportConfig): Uri? {
        val sourceUri = state.imageUri ?: return null
        val uri = Uri.parse(sourceUri)
        val inputStream = if (sourceUri.startsWith("file:")) File(uri.path!!).inputStream()
        else context.contentResolver.openInputStream(uri)
        val sourceBitmap = inputStream?.use { BitmapFactory.decodeStream(it) } ?: return null
        val fullBmp = sourceBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(fullBmp)
        val w = fullBmp.width; val h = fullBmp.height

        drawOverlaysOnBitmapWithConfig(canvas, w, h, state, config.overlayVisibility)
        if (config.showLegend) drawLegendOnBitmap(canvas, w, h, state, config.overlayVisibility, config.legendOffsetPx)
        if (config.showLabel) drawLabelOnBitmap(canvas, w, h, state, config)

        val finalBmp = if (config.cropRect != null) {
            val cr = config.cropRect
            val cx = (cr.left * w).coerceAtLeast(0f).roundToInt()
            val cy = (cr.top * h).coerceAtLeast(0f).roundToInt()
            val cw = min((cr.width * w).roundToInt(), w - cx)
            val ch = min((cr.height * h).roundToInt(), h - cy)
            if (cw > 0 && ch > 0) Bitmap.createBitmap(fullBmp, cx, cy, cw, ch) else fullBmp
        } else fullBmp

        val savedUri = saveBitmapToGallery(context, finalBmp, "ShotMetrics_${timestamp()}")
        if (finalBmp !== fullBmp) finalBmp.recycle()
        fullBmp.recycle(); sourceBitmap.recycle()
        return savedUri
    }

    // ── Internal drawing helpers ─────────────────────────────────────────────

    private fun drawOverlaysOnBitmap(canvas: Canvas, width: Int, height: Int, state: EditorState) {
        drawOverlaysOnBitmapWithConfig(canvas, width, height, state, state.overlayVisibility)
    }

    private fun drawOverlaysOnBitmapWithConfig(
        canvas: Canvas, width: Int, height: Int, state: EditorState, vis: OverlayVisibility
    ) {
        val result = state.ballisticsResult ?: return
        val paint = Paint().apply { isAntiAlias = true; style = Paint.Style.STROKE; strokeWidth = 3f }
        val mpi = result.meanPointOfImpact; val sf = state.scaleFactor

        if (vis.mpi) {
            paint.color = 0xFFFF9800.toInt(); paint.style = Paint.Style.FILL
            canvas.drawCircle(mpi.x, mpi.y, 8f, paint)
            paint.style = Paint.Style.STROKE
            canvas.drawLine(mpi.x - 25f, mpi.y, mpi.x + 25f, mpi.y, paint)
            canvas.drawLine(mpi.x, mpi.y - 25f, mpi.x, mpi.y + 25f, paint)
        }
        if (vis.offsetLine && state.pointOfAim != null) {
            paint.color = 0xFFFFEB3B.toInt(); paint.strokeWidth = 2f
            canvas.drawLine(state.pointOfAim.x, state.pointOfAim.y, mpi.x, mpi.y, paint)
        }
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 3f
        if (vis.cep && result.cep > 0) { paint.color = 0xFF9C27B0.toInt(); canvas.drawCircle(mpi.x, mpi.y, (result.cep * sf).toFloat(), paint) }
        if (vis.meanRadius && result.meanRadius > 0) { paint.color = 0xFF00BCD4.toInt(); canvas.drawCircle(mpi.x, mpi.y, (result.meanRadius * sf).toFloat(), paint) }
        if (vis.extremeSpread && result.extremeSpread > 0) { paint.color = 0xFFFF5722.toInt(); canvas.drawCircle(mpi.x, mpi.y, (result.extremeSpread * sf / 2.0).toFloat(), paint) }

        state.impacts.filter { it.enabled }.forEach { im ->
            val r = 16f; val gap = r * 0.35f
            val ip = Paint().apply { isAntiAlias = true; color = 0xFF00E676.toInt(); style = Paint.Style.STROKE; strokeWidth = 3f }
            canvas.drawCircle(im.position.x, im.position.y, r, ip)
            canvas.drawLine(im.position.x - r, im.position.y, im.position.x - gap, im.position.y, ip)
            canvas.drawLine(im.position.x + gap, im.position.y, im.position.x + r, im.position.y, ip)
            canvas.drawLine(im.position.x, im.position.y - r, im.position.x, im.position.y - gap, ip)
            canvas.drawLine(im.position.x, im.position.y + gap, im.position.x, im.position.y + r, ip)
            val tp = Paint().apply { isAntiAlias = true; color = 0xFF00E676.toInt(); textSize = 14f; textAlign = Paint.Align.CENTER }
            canvas.drawText(im.id.toString(), im.position.x, im.position.y + 5f, tp)
        }

        state.pointOfAim?.let { poa ->
            val r = 20f; val gap = r * 0.35f
            canvas.drawCircle(poa.x, poa.y, r, Paint().apply { isAntiAlias = true; color = 0x3342A5F5; style = Paint.Style.FILL })
            val pp = Paint().apply { isAntiAlias = true; color = 0xFF42A5F5.toInt(); style = Paint.Style.STROKE; strokeWidth = 3f }
            canvas.drawCircle(poa.x, poa.y, r, pp)
            canvas.drawLine(poa.x - r, poa.y, poa.x - gap, poa.y, pp)
            canvas.drawLine(poa.x + gap, poa.y, poa.x + r, poa.y, pp)
            canvas.drawLine(poa.x, poa.y - r, poa.x, poa.y - gap, pp)
            canvas.drawLine(poa.x, poa.y + gap, poa.x, poa.y + r, pp)
            pp.style = Paint.Style.FILL; canvas.drawCircle(poa.x, poa.y, pp.strokeWidth * 1.2f, pp)
        }
    }

    private fun drawLegendOnBitmap(canvas: Canvas, width: Int, height: Int, state: EditorState, vis: OverlayVisibility, legendOffset: androidx.compose.ui.geometry.Offset = androidx.compose.ui.geometry.Offset.Zero) {
        val result = state.ballisticsResult ?: return
        val unit = state.lengthUnit.abbreviation
        val entries = mutableListOf<Pair<Int, String>>()
        if (vis.mpi) entries.add(0xFFFF9800.toInt() to "MPI")
        if (vis.cep && result.cep > 0) entries.add(0xFF9C27B0.toInt() to "CEP: %.3f $unit".format(result.cep))
        if (vis.meanRadius && result.meanRadius > 0) entries.add(0xFF00BCD4.toInt() to "MR: %.3f $unit".format(result.meanRadius))
        if (vis.extremeSpread && result.extremeSpread > 0) entries.add(0xFFFF5722.toInt() to "ES: %.3f $unit".format(result.extremeSpread))
        if (vis.offsetLine) entries.add(0xFFFFEB3B.toInt() to "POA→MPI")
        if (entries.isEmpty()) return

        val fontSize = max(width * 0.018f, 14f)
        val pad = fontSize * 0.6f; val lineH = fontSize * 1.5f; val dotR = fontSize * 0.35f
        val tp = Paint().apply { isAntiAlias = true; color = 0xFFFFFFFF.toInt(); textSize = fontSize }
        val maxTw = entries.maxOf { (_, t) -> tp.measureText(t) }
        val legendW = dotR * 2 + pad * 3 + maxTw + pad; val legendH = entries.size * lineH + pad * 2
        val lx = width / 2f + legendOffset.x - legendW / 2f; val ly = height / 2f + legendOffset.y - legendH / 2f

        canvas.drawRoundRect(lx, ly, lx + legendW, ly + legendH, pad * 0.5f, pad * 0.5f,
            Paint().apply { isAntiAlias = true; color = 0x99000000.toInt(); style = Paint.Style.FILL })
        entries.forEachIndexed { i, (color, text) ->
            val cy = ly + pad + i * lineH + lineH / 2f
            canvas.drawCircle(lx + pad + dotR, cy, dotR, Paint().apply { isAntiAlias = true; this.color = color; style = Paint.Style.FILL })
            canvas.drawText(text, lx + pad + dotR * 2 + pad, cy + fontSize * 0.35f, tp)
        }
    }

    private fun drawLabelOnBitmap(canvas: Canvas, width: Int, height: Int, state: EditorState, config: ExportConfig) {
        val lines = buildExportLabelLines(state, config)
        if (lines.isEmpty()) return

        val baseFontSize = max(width * 0.022f, 16f)
        val fontSize = baseFontSize * config.labelScale
        val pad = fontSize * 0.7f
        val lineH = fontSize * 1.5f
        val separatorH = fontSize * 0.5f

        val tp = Paint().apply { isAntiAlias = true; textSize = fontSize; color = 0xFF000000.toInt(); textAlign = Paint.Align.CENTER }
        val tpBold = Paint(tp).apply { isFakeBoldText = true }

        val maxTw = lines.maxOf { (text, _, _) -> if (text.isEmpty()) 0f else tp.measureText(text) }
        val labelW = maxTw + pad * 2
        var totalH = pad * 2f
        lines.forEach { (text, _, _) -> totalH += if (text.isEmpty()) separatorH else lineH }

        val cx = width / 2f + config.labelOffsetPx.x
        val cy = height / 2f + config.labelOffsetPx.y
        val lx = cx - labelW / 2f
        val ly = cy - totalH / 2f

        val alpha = (config.labelAlpha * 255).roundToInt()

        // White translucent background
        val bg = Paint().apply { isAntiAlias = true; color = (alpha shl 24) or 0xFFFFFF; style = Paint.Style.FILL }
        canvas.drawRoundRect(RectF(lx, ly, lx + labelW, ly + totalH), pad * 0.6f, pad * 0.6f, bg)

        // Black border
        val border = Paint().apply { isAntiAlias = true; color = ((alpha * 0.6f).roundToInt() shl 24); style = Paint.Style.STROKE; strokeWidth = 2f }
        canvas.drawRoundRect(RectF(lx, ly, lx + labelW, ly + totalH), pad * 0.6f, pad * 0.6f, border)

        // Draw text centered
        tp.alpha = alpha; tpBold.alpha = alpha
        var textY = ly + pad
        val centerX = lx + labelW / 2f
        lines.forEach { (text, bold, _) ->
            if (text.isEmpty()) {
                textY += separatorH
            } else {
                val p = if (bold) tpBold else tp
                canvas.drawText(text, centerX, textY + fontSize * 0.85f, p)
                textY += lineH
            }
        }
    }

    private fun buildExportLabelLines(state: EditorState, config: ExportConfig): List<Triple<String, Boolean, Boolean>> {
        val result = state.ballisticsResult
        val atz = state.atzResult
        val aUnit = config.labelAngleUnit
        val lUnit = config.labelLengthUnit
        val lines = mutableListOf<Triple<String, Boolean, Boolean>>()

        val hasCustomTitle = config.customTitle.isNotBlank()
        if (hasCustomTitle) lines.add(Triple(config.customTitle, true, true))

        val dist = state.distanceToTarget.let { if (it == it.toLong().toDouble()) it.toLong().toString() else "%.1f".format(it) }
        val shotCount = state.enabledImpacts.size
        lines.add(Triple("$dist ${state.distanceUnit.abbreviation} / $shotCount Shot Group", !hasCustomTitle, true))

        if (config.locationText.isNotBlank()) lines.add(Triple("\uD83D\uDCCD ${config.locationText}", false, false))

        val dateStr = config.customDate.ifBlank { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
        lines.add(Triple(dateStr, false, false))
        lines.add(Triple("", false, false))

        val apl = if (result != null && result.extremeSpread > 0) {
            when (aUnit) {
                com.shotmetrics.app.domain.model.AngleUnit.MOA -> result.groupSizeMoa / result.extremeSpread
                com.shotmetrics.app.domain.model.AngleUnit.MIL -> result.groupSizeMil / result.extremeSpread
            }
        } else 0.0

        config.labelDataFlags.forEach { flag ->
            when (flag) {
                LabelDataType.GROUP_SIZE -> result?.let {
                    val angle = when (aUnit) { com.shotmetrics.app.domain.model.AngleUnit.MOA -> it.groupSizeMoa; com.shotmetrics.app.domain.model.AngleUnit.MIL -> it.groupSizeMil }
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
                LabelDataType.ATZ -> atz?.let { lines.add(Triple(formatAtzExport(it, state, config.atzDisplayUnit), false, false)) }
            }
        }

        while (lines.isNotEmpty() && lines.last().first.isEmpty()) lines.removeAt(lines.lastIndex)
        return lines
    }

    private fun formatAtzExport(atz: com.shotmetrics.app.domain.model.ATZResult, es: com.shotmetrics.app.ui.editor.EditorState, unit: ATZDisplayUnit): String {
        val wDir = atz.windageDirection.arrow; val eDir = atz.elevationDirection.arrow
        return when (unit) {
            ATZDisplayUnit.CLICKS -> "ATZ: %s%d / %s%d clicks".format(wDir, atz.windageClicksRounded, eDir, atz.elevationClicksRounded)
            ATZDisplayUnit.MOA -> {
                val wMoa = if (atz.angleUnit == com.shotmetrics.app.domain.model.AngleUnit.MOA) atz.windageAngle else atz.windageAngle * 3.43775
                val eMoa = if (atz.angleUnit == com.shotmetrics.app.domain.model.AngleUnit.MOA) atz.elevationAngle else atz.elevationAngle * 3.43775
                "ATZ: %s%.2f / %s%.2f MOA".format(wDir, wMoa, eDir, eMoa)
            }
            ATZDisplayUnit.MIL -> {
                val wMil = if (atz.angleUnit == com.shotmetrics.app.domain.model.AngleUnit.MIL) atz.windageAngle else atz.windageAngle / 3.43775
                val eMil = if (atz.angleUnit == com.shotmetrics.app.domain.model.AngleUnit.MIL) atz.elevationAngle else atz.elevationAngle / 3.43775
                "ATZ: %s%.2f / %s%.2f MIL".format(wDir, wMil, eDir, eMil)
            }
            ATZDisplayUnit.INCH -> {
                val distYards = when (es.distanceUnit) { com.shotmetrics.app.domain.model.DistanceUnit.YARDS -> es.distanceToTarget; com.shotmetrics.app.domain.model.DistanceUnit.METERS -> es.distanceToTarget * 1.09361 }
                fun a2i(a: Double) = when (atz.angleUnit) { com.shotmetrics.app.domain.model.AngleUnit.MOA -> a * 1.047 * distYards / 100.0; com.shotmetrics.app.domain.model.AngleUnit.MIL -> a * 3.6 * distYards / 100.0 }
                "ATZ: %s%.2f / %s%.2f in".format(wDir, a2i(atz.windageAngle), eDir, a2i(atz.elevationAngle))
            }
            ATZDisplayUnit.CM -> {
                val distYards = when (es.distanceUnit) { com.shotmetrics.app.domain.model.DistanceUnit.YARDS -> es.distanceToTarget; com.shotmetrics.app.domain.model.DistanceUnit.METERS -> es.distanceToTarget * 1.09361 }
                fun a2i(a: Double) = when (atz.angleUnit) { com.shotmetrics.app.domain.model.AngleUnit.MOA -> a * 1.047 * distYards / 100.0; com.shotmetrics.app.domain.model.AngleUnit.MIL -> a * 3.6 * distYards / 100.0 }
                "ATZ: %s%.2f / %s%.2f cm".format(wDir, a2i(atz.windageAngle) * 2.54, eDir, a2i(atz.elevationAngle) * 2.54)
            }
        }
    }

    private fun drawRefPointOnBitmap(canvas: Canvas, cx: Float, cy: Float) {
        val r = 14f; val color = 0xFF69F0AE.toInt()
        canvas.drawCircle(cx, cy, r, Paint().apply { isAntiAlias = true; this.color = 0x3369F0AE; style = Paint.Style.FILL })
        val stroke = Paint().apply { isAntiAlias = true; this.color = color; style = Paint.Style.STROKE; strokeWidth = 2f }
        canvas.drawCircle(cx, cy, r, stroke)
        val pl = r * 0.3f
        canvas.drawLine(cx - pl, cy, cx + pl, cy, stroke); canvas.drawLine(cx, cy - pl, cx, cy + pl, stroke)
        val s = r * 1.25f; val bl = r * 0.35f
        canvas.drawLine(cx - s, cy - s, cx - s + bl, cy - s, stroke); canvas.drawLine(cx - s, cy - s, cx - s, cy - s + bl, stroke)
        canvas.drawLine(cx + s, cy - s, cx + s - bl, cy - s, stroke); canvas.drawLine(cx + s, cy - s, cx + s, cy - s + bl, stroke)
        canvas.drawLine(cx - s, cy + s, cx - s + bl, cy + s, stroke); canvas.drawLine(cx - s, cy + s, cx - s, cy + s - bl, stroke)
        canvas.drawLine(cx + s, cy + s, cx + s - bl, cy + s, stroke); canvas.drawLine(cx + s, cy + s, cx + s, cy + s - bl, stroke)
    }

    private fun saveBitmapToGallery(context: Context, bitmap: Bitmap, name: String): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$name.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ShotMetrics")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        resolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear(); values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        return uri
    }

    private fun timestamp() = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    private fun f(v: Double) = String.format(Locale.US, "%.4f", v)
}

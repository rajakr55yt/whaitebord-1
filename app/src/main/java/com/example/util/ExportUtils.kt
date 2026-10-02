package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.example.model.*
import java.io.File
import java.io.FileOutputStream
import kotlin.math.cos
import kotlin.math.sin

object ExportUtils {

    fun renderPageToBitmap(
        page: BoardPage,
        width: Int,
        height: Int,
        backgroundColor: Long,
        texture: BoardTexture,
        visibleLayerIds: Set<String> = emptySet()
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw background
        val bgPaint = Paint().apply {
            color = backgroundColor.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Draw texture
        drawTexture(canvas, width, height, texture, backgroundColor)

        // Draw elements
        val paint = Paint().apply {
            isAntiAlias = true
        }

        for (elem in page.elements) {
            if (visibleLayerIds.isNotEmpty() && elem.layerId !in visibleLayerIds) {
                continue
            }
            val hasRotation = elem.rotation != 0f
            if (hasRotation) {
                val center = elem.getCenter()
                canvas.save()
                canvas.rotate(elem.rotation, center.x, center.y)
            }

            when (elem) {
                is CanvasElement.Stroke -> {
                    if (elem.points.size < 2) {
                        if (hasRotation) canvas.restore()
                        continue
                    }
                    paint.color = elem.color.toInt()
                    paint.strokeWidth = elem.strokeWidth
                    paint.style = Paint.Style.STROKE
                    paint.strokeCap = when (elem.toolType) {
                        ToolType.MARKER -> Paint.Cap.SQUARE
                        else -> Paint.Cap.ROUND
                    }
                    paint.strokeJoin = Paint.Join.ROUND
                    if (elem.toolType == ToolType.HIGHLIGHT) {
                        paint.alpha = 100
                    } else {
                        paint.alpha = ((elem.color shr 24) and 0xFF).toInt().let { if (it == 0) 255 else it }
                    }

                    val path = Path()
                    path.moveTo(elem.points[0].x, elem.points[0].y)
                    for (i in 1 until elem.points.size) {
                        val prev = elem.points[i - 1]
                        val curr = elem.points[i]
                        val midX = (prev.x + curr.x) / 2f
                        val midY = (prev.y + curr.y) / 2f
                        path.quadTo(prev.x, prev.y, midX, midY)
                    }
                    val last = elem.points.last()
                    path.lineTo(last.x, last.y)
                    canvas.drawPath(path, paint)
                }
                is CanvasElement.Shape -> {
                    paint.color = elem.color.toInt()
                    paint.strokeWidth = elem.strokeWidth
                    paint.style = if (elem.isFilled) Paint.Style.FILL else Paint.Style.STROKE
                    paint.alpha = 255
                    drawShapeOnCanvas(canvas, elem, paint)
                }
                is CanvasElement.Text -> {
                    val textPaint = Paint().apply {
                        color = elem.color.toInt()
                        textSize = elem.fontSize
                        isAntiAlias = true
                        typeface = Typeface.DEFAULT_BOLD
                    }
                    canvas.drawText(elem.text, elem.x, elem.y, textPaint)
                }
                is CanvasElement.Formula -> {
                    val formulaPaint = Paint().apply {
                        color = elem.color.toInt()
                        textSize = elem.fontSize
                        isAntiAlias = true
                        typeface = Typeface.SERIF
                    }
                    // Simple formula rendering
                    canvas.drawText(elem.formula, elem.x, elem.y, formulaPaint)
                }
                is CanvasElement.Sticker -> {
                    drawSticker(canvas, elem)
                }
                is CanvasElement.Science -> {
                    drawScience(canvas, elem)
                }
                is CanvasElement.Table -> {
                    drawTable(canvas, elem)
                }
            }

            if (hasRotation) {
                canvas.restore()
            }
        }

        return bitmap
    }

    private fun drawTexture(canvas: Canvas, width: Int, height: Int, texture: BoardTexture, bgColor: Long) {
        val isDark = ((bgColor shr 16 and 0xFF) * 0.299 + (bgColor shr 8 and 0xFF) * 0.587 + (bgColor and 0xFF) * 0.114) < 128
        val gridColor = if (isDark) Color.argb(40, 255, 255, 255) else Color.argb(40, 0, 0, 0)
        val gridPaint = Paint().apply {
            color = gridColor
            strokeWidth = 1.5f
            style = Paint.Style.STROKE
        }

        val step = 48f
        when (texture) {
            BoardTexture.PLAIN -> {}
            BoardTexture.GRID, BoardTexture.GRAPH -> {
                val s = if (texture == BoardTexture.GRAPH) 24f else 48f
                var x = 0f
                while (x <= width) {
                    canvas.drawLine(x, 0f, x, height.toFloat(), gridPaint)
                    x += s
                }
                var y = 0f
                while (y <= height) {
                    canvas.drawLine(0f, y, width.toFloat(), y, gridPaint)
                    y += s
                }
            }
            BoardTexture.LINES, BoardTexture.NOTEBOOK -> {
                var y = 60f
                while (y <= height) {
                    canvas.drawLine(0f, y, width.toFloat(), y, gridPaint)
                    y += step
                }
                if (texture == BoardTexture.NOTEBOOK) {
                    val marginPaint = Paint().apply {
                        color = Color.argb(120, 244, 67, 54)
                        strokeWidth = 2.5f
                    }
                    canvas.drawLine(72f, 0f, 72f, height.toFloat(), marginPaint)
                }
            }
            BoardTexture.DOTS -> {
                val dotPaint = Paint().apply {
                    color = gridColor
                    style = Paint.Style.FILL
                }
                var x = 24f
                while (x <= width) {
                    var y = 24f
                    while (y <= height) {
                        canvas.drawCircle(x, y, 2.5f, dotPaint)
                        y += step
                    }
                    x += step
                }
            }
            BoardTexture.CHALK -> {
                // Subtle chalk dust
                val chalkPaint = Paint().apply {
                    color = if (isDark) Color.argb(15, 255, 255, 255) else Color.argb(15, 0, 0, 0)
                    style = Paint.Style.FILL
                }
                for (i in 0 until 120) {
                    val rx = (Math.random() * width).toFloat()
                    val ry = (Math.random() * height).toFloat()
                    val radius = (Math.random() * 8 + 2).toFloat()
                    canvas.drawCircle(rx, ry, radius, chalkPaint)
                }
            }
            BoardTexture.ISOMETRIC -> {
                // Draw 60 degree diagonal lines
                var x = -height.toFloat()
                while (x <= width + height) {
                    canvas.drawLine(x, 0f, x + height, height.toFloat(), gridPaint)
                    canvas.drawLine(x + height, 0f, x, height.toFloat(), gridPaint)
                    x += 48f
                }
            }
            BoardTexture.DIAMOND -> {
                var x = -height.toFloat()
                while (x <= width + height) {
                    canvas.drawLine(x, 0f, x + height, height.toFloat(), gridPaint)
                    canvas.drawLine(x + height, 0f, x, height.toFloat(), gridPaint)
                    x += 40f
                }
            }
            BoardTexture.MUSIC -> {
                // Groups of 5 stave lines
                var startY = 80f
                while (startY <= height - 80f) {
                    for (line in 0 until 5) {
                        canvas.drawLine(20f, startY + line * 14f, width - 20f, startY + line * 14f, gridPaint)
                    }
                    startY += 120f
                }
            }
        }
    }

    private fun drawShapeOnCanvas(canvas: Canvas, shape: CanvasElement.Shape, paint: Paint) {
        val left = minOf(shape.startX, shape.endX)
        val top = minOf(shape.startY, shape.endY)
        val right = maxOf(shape.startX, shape.endX)
        val bottom = maxOf(shape.startY, shape.endY)
        val w = right - left
        val h = bottom - top

        when (shape.shapeType) {
            ShapeType.LINE -> {
                canvas.drawLine(shape.startX, shape.startY, shape.endX, shape.endY, paint)
            }
            ShapeType.ARROW -> {
                canvas.drawLine(shape.startX, shape.startY, shape.endX, shape.endY, paint)
                val angle = Math.atan2((shape.endY - shape.startY).toDouble(), (shape.endX - shape.startX).toDouble())
                val arrowLen = 24.0
                val a1 = angle - Math.PI / 6
                val a2 = angle + Math.PI / 6
                val x1 = (shape.endX - arrowLen * cos(a1)).toFloat()
                val y1 = (shape.endY - arrowLen * sin(a1)).toFloat()
                val x2 = (shape.endX - arrowLen * cos(a2)).toFloat()
                val y2 = (shape.endY - arrowLen * sin(a2)).toFloat()
                canvas.drawLine(shape.endX, shape.endY, x1, y1, paint)
                canvas.drawLine(shape.endX, shape.endY, x2, y2, paint)
            }
            ShapeType.RECT -> {
                canvas.drawRect(left, top, right, bottom, paint)
            }
            ShapeType.SQUARE -> {
                val side = maxOf(w, h)
                canvas.drawRect(left, top, left + side, top + side, paint)
            }
            ShapeType.CIRCLE -> {
                val radius = maxOf(w, h) / 2f
                val cx = (left + right) / 2f
                val cy = (top + bottom) / 2f
                canvas.drawCircle(cx, cy, radius, paint)
            }
            ShapeType.OVAL -> {
                canvas.drawOval(RectF(left, top, right, bottom), paint)
            }
            ShapeType.TRIANGLE -> {
                val path = Path()
                path.moveTo((left + right) / 2f, top)
                path.lineTo(right, bottom)
                path.lineTo(left, bottom)
                path.close()
                canvas.drawPath(path, paint)
            }
            ShapeType.RIGHT_TRIANGLE -> {
                val path = Path()
                path.moveTo(left, top)
                path.lineTo(left, bottom)
                path.lineTo(right, bottom)
                path.close()
                canvas.drawPath(path, paint)
            }
            ShapeType.STAR -> {
                val cx = (left + right) / 2f
                val cy = (top + bottom) / 2f
                val rOuter = minOf(w, h) / 2f
                val rInner = rOuter * 0.4f
                val path = Path()
                for (i in 0 until 10) {
                    val r = if (i % 2 == 0) rOuter else rInner
                    val a = (i * Math.PI / 5) - Math.PI / 2
                    val px = (cx + r * cos(a)).toFloat()
                    val py = (cy + r * sin(a)).toFloat()
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                canvas.drawPath(path, paint)
            }
            ShapeType.DIAMOND -> {
                val path = Path()
                val cx = (left + right) / 2f
                val cy = (top + bottom) / 2f
                path.moveTo(cx, top)
                path.lineTo(right, cy)
                path.lineTo(cx, bottom)
                path.lineTo(left, cy)
                path.close()
                canvas.drawPath(path, paint)
            }
            ShapeType.PENTAGON -> {
                val cx = (left + right) / 2f
                val cy = (top + bottom) / 2f
                val r = minOf(w, h) / 2f
                val path = Path()
                for (i in 0 until 5) {
                    val a = (i * 2 * Math.PI / 5) - Math.PI / 2
                    val px = (cx + r * cos(a)).toFloat()
                    val py = (cy + r * sin(a)).toFloat()
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                canvas.drawPath(path, paint)
            }
            ShapeType.HEXAGON -> {
                val cx = (left + right) / 2f
                val cy = (top + bottom) / 2f
                val r = minOf(w, h) / 2f
                val path = Path()
                for (i in 0 until 6) {
                    val a = (i * 2 * Math.PI / 6)
                    val px = (cx + r * cos(a)).toFloat()
                    val py = (cy + r * sin(a)).toFloat()
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                canvas.drawPath(path, paint)
            }
            ShapeType.PARALLEL -> {
                val offset = w * 0.25f
                val path = Path()
                path.moveTo(left + offset, top)
                path.lineTo(right, top)
                path.lineTo(right - offset, bottom)
                path.lineTo(left, bottom)
                path.close()
                canvas.drawPath(path, paint)
            }
            ShapeType.TRAPEZOID -> {
                val offset = w * 0.2f
                val path = Path()
                path.moveTo(left + offset, top)
                path.lineTo(right - offset, top)
                path.lineTo(right, bottom)
                path.lineTo(left, bottom)
                path.close()
                canvas.drawPath(path, paint)
            }
            ShapeType.HEART -> {
                val path = Path()
                val cx = (left + right) / 2f
                path.moveTo(cx, top + h * 0.3f)
                path.cubicTo(left, top, left, top + h * 0.6f, cx, bottom)
                path.cubicTo(right, top + h * 0.6f, right, top, cx, top + h * 0.3f)
                canvas.drawPath(path, paint)
            }
            ShapeType.CROSS -> {
                val armW = w * 0.3f
                val path = Path()
                val cx = (left + right) / 2f
                val cy = (top + bottom) / 2f
                path.moveTo(cx - armW / 2, top)
                path.lineTo(cx + armW / 2, top)
                path.lineTo(cx + armW / 2, cy - armW / 2)
                path.lineTo(right, cy - armW / 2)
                path.lineTo(right, cy + armW / 2)
                path.lineTo(cx + armW / 2, cy + armW / 2)
                path.lineTo(cx + armW / 2, bottom)
                path.lineTo(cx - armW / 2, bottom)
                path.lineTo(cx - armW / 2, cy + armW / 2)
                path.lineTo(left, cy + armW / 2)
                path.lineTo(left, cy - armW / 2)
                path.lineTo(cx - armW / 2, cy - armW / 2)
                path.close()
                canvas.drawPath(path, paint)
            }
            ShapeType.SEMICIRCLE -> {
                val path = Path()
                path.arcTo(RectF(left, top, right, top + h * 2), 180f, 180f)
                path.close()
                canvas.drawPath(path, paint)
            }
            ShapeType.SECTOR -> {
                val path = Path()
                val cx = (left + right) / 2f
                path.moveTo(cx, bottom)
                path.arcTo(RectF(left, top, right, bottom + h), 210f, 120f)
                path.close()
                canvas.drawPath(path, paint)
            }
            ShapeType.RIGHT_ANGLE -> {
                canvas.drawLine(left, top, left, bottom, paint)
                canvas.drawLine(left, bottom, right, bottom, paint)
                val squareSize = minOf(w, h) * 0.2f
                canvas.drawRect(left, bottom - squareSize, left + squareSize, bottom, Paint().apply {
                    color = shape.color.toInt()
                    style = Paint.Style.STROKE
                    strokeWidth = shape.strokeWidth * 0.8f
                })
            }
            ShapeType.CUBE -> {
                // 3D Isometric cube
                val d = minOf(w, h) * 0.3f
                val fL = left
                val fT = top + d
                val fR = right - d
                val fB = bottom
                canvas.drawRect(fL, fT, fR, fB, paint)
                val topPath = Path().apply {
                    moveTo(fL, fT)
                    lineTo(fL + d, top)
                    lineTo(right, top)
                    lineTo(fR, fT)
                    close()
                }
                canvas.drawPath(topPath, paint)
                val rightPath = Path().apply {
                    moveTo(fR, fT)
                    lineTo(right, top)
                    lineTo(right, fB - d)
                    lineTo(fR, fB)
                    close()
                }
                canvas.drawPath(rightPath, paint)
            }
            ShapeType.CYLINDER -> {
                val ovalH = h * 0.2f
                canvas.drawOval(RectF(left, top, right, top + ovalH), paint)
                canvas.drawLine(left, top + ovalH / 2, left, bottom - ovalH / 2, paint)
                canvas.drawLine(right, top + ovalH / 2, right, bottom - ovalH / 2, paint)
                canvas.drawArc(RectF(left, bottom - ovalH, right, bottom), 0f, 180f, false, paint)
            }
            ShapeType.CONE -> {
                val ovalH = h * 0.2f
                val apexX = (left + right) / 2f
                canvas.drawLine(apexX, top, left, bottom - ovalH / 2, paint)
                canvas.drawLine(apexX, top, right, bottom - ovalH / 2, paint)
                canvas.drawOval(RectF(left, bottom - ovalH, right, bottom), paint)
            }
            ShapeType.PYRAMID -> {
                val apexX = (left + right) / 2f
                canvas.drawLine(apexX, top, left, bottom, paint)
                canvas.drawLine(apexX, top, right, bottom, paint)
                canvas.drawLine(apexX, top, apexX + w * 0.15f, bottom - h * 0.08f, paint)
                canvas.drawLine(left, bottom, apexX + w * 0.15f, bottom - h * 0.08f, paint)
                canvas.drawLine(apexX + w * 0.15f, bottom - h * 0.08f, right, bottom, paint)
            }
            ShapeType.SPHERE -> {
                val radius = minOf(w, h) / 2f
                val cx = (left + right) / 2f
                val cy = (top + bottom) / 2f
                canvas.drawCircle(cx, cy, radius, paint)
                // draw equator and meridian
                val ellipsePaint = Paint().apply {
                    color = shape.color.toInt()
                    style = Paint.Style.STROKE
                    strokeWidth = shape.strokeWidth * 0.6f
                }
                canvas.drawOval(RectF(left, cy - radius * 0.3f, right, cy + radius * 0.3f), ellipsePaint)
                canvas.drawOval(RectF(cx - radius * 0.3f, top, cx + radius * 0.3f, bottom), ellipsePaint)
            }
        }
    }

    private fun drawSticker(canvas: Canvas, sticker: CanvasElement.Sticker) {
        val s = sticker.size
        val x = sticker.x
        val y = sticker.y
        val paint = Paint().apply { isAntiAlias = true }

        when (sticker.stickerKey) {
            "star", "smiley_star" -> {
                paint.color = Color.parseColor("#FBBF24")
                val path = Path()
                val cx = x + s / 2
                val cy = y + s / 2
                val rOut = s / 2
                val rIn = s * 0.22f
                for (i in 0 until 10) {
                    val r = if (i % 2 == 0) rOut else rIn
                    val a = (i * Math.PI / 5) - Math.PI / 2
                    val px = (cx + r * cos(a)).toFloat()
                    val py = (cy + r * sin(a)).toFloat()
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                canvas.drawPath(path, paint)
            }
            "trophy" -> {
                paint.color = Color.parseColor("#F59E0B")
                canvas.drawRect(x + s * 0.3f, y + s * 0.15f, x + s * 0.7f, y + s * 0.55f, paint)
                canvas.drawRect(x + s * 0.42f, y + s * 0.55f, x + s * 0.58f, y + s * 0.78f, paint)
                paint.color = Color.parseColor("#78350F")
                canvas.drawRect(x + s * 0.28f, y + s * 0.78f, x + s * 0.72f, y + s * 0.9f, paint)
            }
            "crown" -> {
                paint.color = Color.parseColor("#F59E0B")
                val path = Path().apply {
                    moveTo(x + s * 0.1f, y + s * 0.8f)
                    lineTo(x + s * 0.9f, y + s * 0.8f)
                    lineTo(x + s * 0.9f, y + s * 0.35f)
                    lineTo(x + s * 0.7f, y + s * 0.55f)
                    lineTo(x + s * 0.5f, y + s * 0.2f)
                    lineTo(x + s * 0.3f, y + s * 0.55f)
                    lineTo(x + s * 0.1f, y + s * 0.35f)
                    close()
                }
                canvas.drawPath(path, paint)
            }
            "a_plus" -> {
                paint.color = Color.parseColor("#16A34A")
                canvas.drawCircle(x + s / 2, y + s / 2, s / 2, paint)
                paint.color = Color.WHITE
                paint.textSize = s * 0.45f
                paint.typeface = Typeface.DEFAULT_BOLD
                canvas.drawText("A+", x + s * 0.22f, y + s * 0.65f, paint)
            }
            "hundred" -> {
                paint.color = Color.parseColor("#EA580C")
                canvas.drawCircle(x + s / 2, y + s / 2, s / 2, paint)
                paint.color = Color.WHITE
                paint.textSize = s * 0.32f
                paint.typeface = Typeface.DEFAULT_BOLD
                canvas.drawText("100%", x + s * 0.14f, y + s * 0.62f, paint)
            }
            "excellent" -> {
                paint.color = Color.parseColor("#16A34A")
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 4f
                canvas.drawRoundRect(RectF(x, y + s * 0.2f, x + s * 1.4f, y + s * 0.8f), 16f, 16f, paint)
                paint.style = Paint.Style.FILL
                paint.textSize = s * 0.28f
                paint.typeface = Typeface.DEFAULT_BOLD
                canvas.drawText("Excellent!", x + s * 0.1f, y + s * 0.6f, paint)
            }
            "good_job" -> {
                paint.color = Color.parseColor("#2563EB")
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 4f
                canvas.drawRoundRect(RectF(x, y + s * 0.2f, x + s * 1.4f, y + s * 0.8f), 16f, 16f, paint)
                paint.style = Paint.Style.FILL
                paint.textSize = s * 0.28f
                paint.typeface = Typeface.DEFAULT_BOLD
                canvas.drawText("Good Job!", x + s * 0.12f, y + s * 0.6f, paint)
            }
            else -> {
                // generic badge
                paint.color = Color.parseColor("#3B82F6")
                canvas.drawCircle(x + s / 2, y + s / 2, s / 2, paint)
            }
        }
    }

    private fun drawScience(canvas: Canvas, science: CanvasElement.Science) {
        val s = science.size
        val x = science.x
        val y = science.y
        val cx = x + s / 2
        val cy = y + s / 2
        val paint = Paint().apply { isAntiAlias = true }

        if (science.category == "atomic") {
            // Draw Bohr model
            // Nucleus
            paint.color = Color.parseColor("#EF4444")
            paint.style = Paint.Style.FILL
            val nRadius = s * 0.16f
            canvas.drawCircle(cx, cy, nRadius, paint)
            paint.color = Color.WHITE
            paint.textSize = s * 0.14f
            paint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText(science.symbol, cx - s * 0.05f, cy + s * 0.05f, paint)

            // Shell orbits
            val orbitPaint = Paint().apply {
                color = Color.parseColor("#3B82F6")
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }
            val numShells = when {
                science.atomicNumber <= 2 -> 1
                science.atomicNumber <= 10 -> 2
                else -> 3
            }
            for (shell in 1..numShells) {
                val r = nRadius + shell * (s * 0.12f)
                canvas.drawCircle(cx, cy, r, orbitPaint)
            }
        } else {
            // Lab equipment / glassware
            paint.color = Color.parseColor("#0284C7")
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 3f
            // Draw beaker / flask
            val path = Path().apply {
                moveTo(cx - s * 0.15f, cy - s * 0.35f)
                lineTo(cx + s * 0.15f, cy - s * 0.35f)
                lineTo(cx + s * 0.15f, cy - s * 0.1f)
                lineTo(cx + s * 0.35f, cy + s * 0.35f)
                lineTo(cx - s * 0.35f, cy + s * 0.35f)
                lineTo(cx - s * 0.15f, cy - s * 0.1f)
                close()
            }
            canvas.drawPath(path, paint)
        }
    }

    private fun drawTable(canvas: Canvas, table: CanvasElement.Table) {
        val paint = Paint().apply {
            color = table.color.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            isAntiAlias = true
        }
        val textPaint = Paint().apply {
            color = table.color.toInt()
            textSize = 24f
            isAntiAlias = true
        }

        canvas.drawRect(table.x, table.y, table.x + table.width, table.y + table.height, paint)
        val cellW = table.width / table.cols
        val cellH = table.height / table.rows

        for (c in 1 until table.cols) {
            val lx = table.x + c * cellW
            canvas.drawLine(lx, table.y, lx, table.y + table.height, paint)
        }
        for (r in 1 until table.rows) {
            val ly = table.y + r * cellH
            canvas.drawLine(table.x, ly, table.x + table.width, ly, paint)
        }

        // Draw cell contents
        for (r in 0 until minOf(table.rows, table.cells.size)) {
            val row = table.cells[r]
            for (c in 0 until minOf(table.cols, row.size)) {
                val txt = row[c]
                if (txt.isNotBlank()) {
                    canvas.drawText(
                        txt,
                        table.x + c * cellW + 8f,
                        table.y + (r + 1) * cellH - 12f,
                        textPaint
                    )
                }
            }
        }
    }

    fun exportToPdf(
        context: Context,
        pages: List<BoardPage>,
        backgroundColor: Long,
        texture: BoardTexture,
        visibleLayerIds: Set<String>
    ): File {
        val pdfDocument = PdfDocument()
        val pageWidth = 1080
        val pageHeight = 1920

        for (i in pages.indices) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, i + 1).create()
            val pdfPage = pdfDocument.startPage(pageInfo)
            val bitmap = renderPageToBitmap(
                page = pages[i],
                width = pageWidth,
                height = pageHeight,
                backgroundColor = backgroundColor,
                texture = texture,
                visibleLayerIds = visibleLayerIds
            )
            pdfPage.canvas.drawBitmap(bitmap, 0f, 0f, null)
            bitmap.recycle()
            pdfDocument.finishPage(pdfPage)
        }

        val pdfDir = File(context.cacheDir, "pdfs")
        if (!pdfDir.exists()) pdfDir.mkdirs()
        val file = File(pdfDir, "WhiteBoard_${System.currentTimeMillis()}.pdf")
        val out = FileOutputStream(file)
        pdfDocument.writeTo(out)
        out.close()
        pdfDocument.close()
        return file
    }

    fun saveBitmapToCache(context: Context, bitmap: Bitmap): File {
        val imgDir = File(context.cacheDir, "images")
        if (!imgDir.exists()) imgDir.mkdirs()
        val file = File(imgDir, "WhiteBoard_${System.currentTimeMillis()}.png")
        val out = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        out.close()
        return file
    }

    fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}

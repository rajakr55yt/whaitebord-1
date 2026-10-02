package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import kotlin.math.*

private enum class DragMode {
    NONE,
    MOVE_ELEMENT,
    ROTATE_ELEMENT,
    RESIZE_ELEMENT,
    DRAW_STROKE,
    DRAW_SHAPE,
    ERASE,
    LASER
}

@Composable
fun WhiteboardCanvas(
    backgroundColor: Long,
    texture: BoardTexture,
    activeTool: ToolType,
    brushColor: Long,
    brushSize: Float,
    selectedShapeType: ShapeType,
    isShapeFilled: Boolean,
    elements: List<CanvasElement>,
    visibleLayerIds: Set<String>,
    selectedElementId: String?,
    laserPoints: List<LaserPoint>,
    isRulerVisible: Boolean,
    onStrokeFinished: (List<PointData>) -> Unit,
    onShapeFinished: (startX: Float, startY: Float, endX: Float, endY: Float) -> Unit,
    onErase: (x: Float, y: Float) -> Unit,
    onLaserPoint: (x: Float, y: Float) -> Unit,
    onLaserClear: () -> Unit,
    onSelectElement: (String?) -> Unit,
    onMoveElement: (dx: Float, dy: Float) -> Unit,
    onRotateElement: (deltaDegrees: Float) -> Unit,
    onScaleElement: (scaleFactor: Float) -> Unit,
    onDeleteSelected: () -> Unit,
    onDuplicateSelected: () -> Unit,
    onResetRotationSelected: () -> Unit,
    onCommitTransform: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentPoints by remember { mutableStateOf<List<PointData>>(emptyList()) }
    var shapeStart by remember { mutableStateOf<Offset?>(null) }
    var shapeEnd by remember { mutableStateOf<Offset?>(null) }

    val density = LocalDensity.current
    val handleRadiusPx = with(density) { 14.dp.toPx() }
    val rotationStemLengthPx = with(density) { 36.dp.toPx() }

    val selectedElement = remember(elements, selectedElementId) {
        elements.find { it.id == selectedElementId }
    }

    var dragMode by remember { mutableStateOf(DragMode.NONE) }
    var prevDragOffset by remember { mutableStateOf(Offset.Zero) }
    var initialRotationAngle by remember { mutableFloatStateOf(0f) }
    var initialDistanceToCenter by remember { mutableFloatStateOf(1f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(backgroundColor))
            .pointerInput(activeTool, selectedElementId, elements) {
                // Two-finger pinch to scale and rotate selected element
                detectTransformGestures { centroid, pan, zoom, rotationDelta ->
                    if (selectedElement != null) {
                        if (pan.getDistance() > 1f) {
                            onMoveElement(pan.x, pan.y)
                        }
                        if (zoom != 1f && zoom > 0.01f) {
                            onScaleElement(zoom)
                        }
                        if (abs(rotationDelta) > 0.1f) {
                            onRotateElement(rotationDelta)
                        }
                    }
                }
            }
            .pointerInput(activeTool, selectedElementId, elements, brushColor, brushSize, selectedShapeType, isShapeFilled) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val sel = selectedElement
                        if (sel != null && (activeTool == ToolType.SELECT || activeTool == ToolType.LASSO || activeTool == ToolType.HAND_PAN)) {
                            val bounds = sel.getBounds()
                            val center = sel.getCenter()
                            val rotHandlePos = Offset(center.x, bounds.top - rotationStemLengthPx)

                            // 1. Check Rotation Handle
                            if ((offset - rotHandlePos).getDistance() <= handleRadiusPx * 2.2f) {
                                dragMode = DragMode.ROTATE_ELEMENT
                                initialRotationAngle = Math.toDegrees(atan2((offset.y - center.y).toDouble(), (offset.x - center.x).toDouble())).toFloat()
                                prevDragOffset = offset
                                return@detectDragGestures
                            }

                            // 2. Check 4 Corner Resize Handles
                            val corners = listOf(
                                Offset(bounds.left, bounds.top),
                                Offset(bounds.right, bounds.top),
                                Offset(bounds.left, bounds.bottom),
                                Offset(bounds.right, bounds.bottom)
                            )
                            val isCorner = corners.any { (offset - it).getDistance() <= handleRadiusPx * 2.2f }
                            if (isCorner) {
                                dragMode = DragMode.RESIZE_ELEMENT
                                initialDistanceToCenter = max((offset - center).getDistance(), 10f)
                                prevDragOffset = offset
                                return@detectDragGestures
                            }

                            // 3. Check Inside Bounding Box (Move)
                            if (bounds.inflate(handleRadiusPx).contains(offset)) {
                                dragMode = DragMode.MOVE_ELEMENT
                                prevDragOffset = offset
                                return@detectDragGestures
                            }
                        }

                        // If in Select/Lasso tool and tapped on another element
                        if (activeTool == ToolType.SELECT || activeTool == ToolType.LASSO) {
                            val hitElement = elements.asReversed().find { elem ->
                                elem.getBounds().inflate(handleRadiusPx).contains(offset)
                            }
                            if (hitElement != null) {
                                onSelectElement(hitElement.id)
                                dragMode = DragMode.MOVE_ELEMENT
                                prevDragOffset = offset
                                return@detectDragGestures
                            } else {
                                onSelectElement(null)
                                dragMode = DragMode.NONE
                                return@detectDragGestures
                            }
                        }

                        // Drawing / Editing tools
                        when (activeTool) {
                            ToolType.PEN, ToolType.BRUSH, ToolType.MARKER,
                            ToolType.HIGHLIGHT, ToolType.CALLIGRAPHY, ToolType.SMART -> {
                                dragMode = DragMode.DRAW_STROKE
                                currentPoints = listOf(PointData.fromOffset(offset))
                            }
                            ToolType.SHAPES -> {
                                dragMode = DragMode.DRAW_SHAPE
                                shapeStart = offset
                                shapeEnd = offset
                            }
                            ToolType.ERASER -> {
                                dragMode = DragMode.ERASE
                                onErase(offset.x, offset.y)
                            }
                            ToolType.LASER -> {
                                dragMode = DragMode.LASER
                                onLaserPoint(offset.x, offset.y)
                            }
                            else -> {
                                dragMode = DragMode.NONE
                            }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val pos = change.position

                        when (dragMode) {
                            DragMode.MOVE_ELEMENT -> {
                                onMoveElement(dragAmount.x, dragAmount.y)
                            }
                            DragMode.ROTATE_ELEMENT -> {
                                val sel = selectedElement ?: return@detectDragGestures
                                val center = sel.getCenter()
                                val currentAngle = Math.toDegrees(atan2((pos.y - center.y).toDouble(), (pos.x - center.x).toDouble())).toFloat()
                                val delta = currentAngle - initialRotationAngle
                                onRotateElement(delta)
                                initialRotationAngle = currentAngle
                            }
                            DragMode.RESIZE_ELEMENT -> {
                                val sel = selectedElement ?: return@detectDragGestures
                                val center = sel.getCenter()
                                val currentDist = max((pos - center).getDistance(), 10f)
                                val factor = currentDist / initialDistanceToCenter
                                if (factor in 0.8f..1.25f) {
                                    onScaleElement(factor)
                                    initialDistanceToCenter = currentDist
                                }
                            }
                            DragMode.DRAW_STROKE -> {
                                currentPoints = currentPoints + PointData.fromOffset(pos)
                            }
                            DragMode.DRAW_SHAPE -> {
                                shapeEnd = pos
                            }
                            DragMode.ERASE -> {
                                onErase(pos.x, pos.y)
                            }
                            DragMode.LASER -> {
                                onLaserPoint(pos.x, pos.y)
                            }
                            else -> {}
                        }
                    },
                    onDragEnd = {
                        when (dragMode) {
                            DragMode.MOVE_ELEMENT, DragMode.ROTATE_ELEMENT, DragMode.RESIZE_ELEMENT -> {
                                onCommitTransform()
                            }
                            DragMode.DRAW_STROKE -> {
                                if (currentPoints.isNotEmpty()) {
                                    onStrokeFinished(currentPoints)
                                    currentPoints = emptyList()
                                }
                            }
                            DragMode.DRAW_SHAPE -> {
                                val s = shapeStart
                                val e = shapeEnd
                                if (s != null && e != null && (s - e).getDistance() > 10f) {
                                    onShapeFinished(s.x, s.y, e.x, e.y)
                                }
                                shapeStart = null
                                shapeEnd = null
                            }
                            DragMode.LASER -> {
                                onLaserClear()
                            }
                            else -> {}
                        }
                        dragMode = DragMode.NONE
                    },
                    onDragCancel = {
                        currentPoints = emptyList()
                        shapeStart = null
                        shapeEnd = null
                        dragMode = DragMode.NONE
                        onLaserClear()
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val hitElement = elements.asReversed().find { elem ->
                        elem.getBounds().inflate(handleRadiusPx).contains(offset)
                    }
                    onSelectElement(hitElement?.id)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // 1. Draw Texture
            drawBoardTexture(texture, backgroundColor, width, height)

            // 2. Draw Committed Elements with their rotation
            for (elem in elements) {
                if (visibleLayerIds.isNotEmpty() && elem.layerId !in visibleLayerIds) {
                    continue
                }
                if (elem.rotation != 0f) {
                    withTransform({
                        rotate(elem.rotation, pivot = elem.getCenter())
                    }) {
                        drawSingleElement(elem)
                    }
                } else {
                    drawSingleElement(elem)
                }
            }

            // 3. Draw Transform Overlay for Selected Element
            if (selectedElement != null) {
                drawSelectionOverlay(
                    element = selectedElement,
                    handleRadius = handleRadiusPx,
                    stemLength = rotationStemLengthPx
                )
            }

            // 4. Draw In-Progress Stroke
            if (currentPoints.size > 1) {
                val liveStrokeColor = Color(brushColor).let {
                    if (activeTool == ToolType.HIGHLIGHT) it.copy(alpha = 0.45f) else it
                }
                val path = Path()
                path.moveTo(currentPoints[0].x, currentPoints[0].y)
                for (i in 1 until currentPoints.size) {
                    val prev = currentPoints[i - 1]
                    val curr = currentPoints[i]
                    val midX = (prev.x + curr.x) / 2f
                    val midY = (prev.y + curr.y) / 2f
                    path.quadraticTo(prev.x, prev.y, midX, midY)
                }
                drawPath(
                    path = path,
                    color = liveStrokeColor,
                    style = Stroke(
                        width = brushSize,
                        cap = if (activeTool == ToolType.MARKER) StrokeCap.Square else StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            // 5. Draw In-Progress Shape Preview
            val start = shapeStart
            val end = shapeEnd
            if (start != null && end != null) {
                drawLiveShapePreview(
                    shapeType = selectedShapeType,
                    start = start,
                    end = end,
                    color = Color(brushColor),
                    strokeWidth = brushSize,
                    isFilled = isShapeFilled
                )
            }

            // 6. Draw Laser Pointer Trail
            if (laserPoints.isNotEmpty()) {
                val now = System.currentTimeMillis()
                for (i in laserPoints.indices) {
                    val pt = laserPoints[i]
                    val age = now - pt.timestamp
                    if (age < 1200L) {
                        val alpha = (1f - (age / 1200f)).coerceIn(0f, 1f)
                        drawCircle(
                            color = Color(0xFFFF1744).copy(alpha = alpha),
                            radius = 16f * alpha,
                            center = Offset(pt.x, pt.y)
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = alpha * 0.8f),
                            radius = 6f * alpha,
                            center = Offset(pt.x, pt.y)
                        )
                    }
                }
            }

            // 7. Draw On-Screen Ruler if visible
            if (isRulerVisible) {
                drawRuler(Offset(100f, 600f))
            }
        }

        // Floating Action Bar above selected element
        if (selectedElement != null) {
            val bounds = selectedElement.getBounds()
            val center = selectedElement.getCenter()
            val topPos = (bounds.top - rotationStemLengthPx - 56f).coerceAtLeast(60f)
            val leftPos = (center.x - 120f).coerceIn(16f, 800f)

            Box(
                modifier = Modifier
                    .offset { IntOffset(leftPos.roundToInt(), topPos.roundToInt()) }
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF1E293B),
                    shadowElevation = 8.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3B82F6))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Rotation angle label
                        if (selectedElement.rotation != 0f) {
                            Text(
                                text = "${selectedElement.rotation.roundToInt()}°",
                                color = Color(0xFF38BDF8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }

                        // Reset rotation
                        IconButton(
                            onClick = onResetRotationSelected,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.RotateLeft,
                                contentDescription = "Reset Angle",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Duplicate
                        IconButton(
                            onClick = onDuplicateSelected,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Duplicate",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Delete
                        IconButton(
                            onClick = onDeleteSelected,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete",
                                tint = Color(0xFFF87171),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Close selection
                        IconButton(
                            onClick = { onSelectElement(null) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Done",
                                tint = Color(0xFF4ADE80),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawSingleElement(elem: CanvasElement) {
    when (elem) {
        is CanvasElement.Stroke -> drawStrokeElement(elem)
        is CanvasElement.Shape -> drawShapeElement(elem)
        is CanvasElement.Text -> drawTextElement(elem)
        is CanvasElement.Formula -> drawFormulaElement(elem)
        is CanvasElement.Sticker -> drawStickerElement(elem)
        is CanvasElement.Science -> drawScienceElement(elem)
        is CanvasElement.Table -> drawTableElement(elem)
    }
}

private fun DrawScope.drawSelectionOverlay(
    element: CanvasElement,
    handleRadius: Float,
    stemLength: Float
) {
    val bounds = element.getBounds()
    val center = element.getCenter()

    val primaryColor = Color(0xFF2563EB)
    val handleFill = Color.White
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)

    // Bounding Rectangle
    drawRect(
        color = primaryColor,
        topLeft = Offset(bounds.left, bounds.top),
        size = Size(bounds.width, bounds.height),
        style = Stroke(width = 2.5f, pathEffect = dashEffect)
    )

    // Rotation Stem Line
    val rotKnobCenter = Offset(center.x, bounds.top - stemLength)
    drawLine(
        color = primaryColor,
        start = Offset(center.x, bounds.top),
        end = rotKnobCenter,
        strokeWidth = 2f
    )

    // Rotation Knob
    drawCircle(
        color = primaryColor,
        radius = handleRadius * 1.1f,
        center = rotKnobCenter
    )
    drawCircle(
        color = Color.White,
        radius = handleRadius * 0.5f,
        center = rotKnobCenter
    )

    // 4 Corner Resize Handles
    val corners = listOf(
        Offset(bounds.left, bounds.top),
        Offset(bounds.right, bounds.top),
        Offset(bounds.left, bounds.bottom),
        Offset(bounds.right, bounds.bottom)
    )
    for (corner in corners) {
        drawCircle(color = primaryColor, radius = handleRadius, center = corner)
        drawCircle(color = handleFill, radius = handleRadius * 0.65f, center = corner)
    }
}

private fun DrawScope.drawBoardTexture(texture: BoardTexture, bgColor: Long, width: Float, height: Float) {
    val isDark = ((bgColor shr 16 and 0xFF) * 0.299 + (bgColor shr 8 and 0xFF) * 0.587 + (bgColor and 0xFF) * 0.114) < 128
    val gridColor = if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.12f)
    val step = 48f

    when (texture) {
        BoardTexture.PLAIN -> {}
        BoardTexture.GRID, BoardTexture.GRAPH -> {
            val s = if (texture == BoardTexture.GRAPH) 24f else 48f
            var x = 0f
            while (x <= width) {
                drawLine(gridColor, Offset(x, 0f), Offset(x, height), strokeWidth = 1f)
                x += s
            }
            var y = 0f
            while (y <= height) {
                drawLine(gridColor, Offset(0f, y), Offset(width, y), strokeWidth = 1f)
                y += s
            }
        }
        BoardTexture.LINES, BoardTexture.NOTEBOOK -> {
            var y = 60f
            while (y <= height) {
                drawLine(gridColor, Offset(0f, y), Offset(width, y), strokeWidth = 1.2f)
                y += step
            }
            if (texture == BoardTexture.NOTEBOOK) {
                drawLine(Color(0xFFEF4444).copy(alpha = 0.5f), Offset(72f, 0f), Offset(72f, height), strokeWidth = 2f)
            }
        }
        BoardTexture.DOTS -> {
            var x = 24f
            while (x <= width) {
                var y = 24f
                while (y <= height) {
                    drawCircle(gridColor, radius = 2f, center = Offset(x, y))
                    y += step
                }
                x += step
            }
        }
        BoardTexture.CHALK -> {
            val chalkColor = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.04f)
            drawCircle(chalkColor, radius = 60f, center = Offset(width * 0.2f, height * 0.3f))
            drawCircle(chalkColor, radius = 90f, center = Offset(width * 0.7f, height * 0.5f))
            drawCircle(chalkColor, radius = 120f, center = Offset(width * 0.4f, height * 0.8f))
        }
        BoardTexture.ISOMETRIC -> {
            var x = -height
            while (x <= width + height) {
                drawLine(gridColor, Offset(x, 0f), Offset(x + height, height), strokeWidth = 1f)
                drawLine(gridColor, Offset(x + height, 0f), Offset(x, height), strokeWidth = 1f)
                x += 48f
            }
        }
        BoardTexture.DIAMOND -> {
            var x = -height
            while (x <= width + height) {
                drawLine(gridColor, Offset(x, 0f), Offset(x + height, height), strokeWidth = 1f)
                drawLine(gridColor, Offset(x + height, 0f), Offset(x, height), strokeWidth = 1f)
                x += 36f
            }
        }
        BoardTexture.MUSIC -> {
            var startY = 80f
            while (startY <= height - 80f) {
                for (line in 0 until 5) {
                    drawLine(gridColor, Offset(24f, startY + line * 14f), Offset(width - 24f, startY + line * 14f), strokeWidth = 1.2f)
                }
                startY += 120f
            }
        }
    }
}

private fun DrawScope.drawStrokeElement(elem: CanvasElement.Stroke) {
    if (elem.points.size < 2) return
    val alpha = if (elem.toolType == ToolType.HIGHLIGHT) 0.45f else 1f
    val strokeColor = Color(elem.color).copy(alpha = alpha)

    val path = Path()
    path.moveTo(elem.points[0].x, elem.points[0].y)
    for (i in 1 until elem.points.size) {
        val prev = elem.points[i - 1]
        val curr = elem.points[i]
        val midX = (prev.x + curr.x) / 2f
        val midY = (prev.y + curr.y) / 2f
        path.quadraticTo(prev.x, prev.y, midX, midY)
    }
    val last = elem.points.last()
    path.lineTo(last.x, last.y)

    drawPath(
        path = path,
        color = strokeColor,
        style = Stroke(
            width = elem.strokeWidth,
            cap = if (elem.toolType == ToolType.MARKER) StrokeCap.Square else StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}

private fun DrawScope.drawShapeElement(elem: CanvasElement.Shape) {
    val shapeColor = Color(elem.color)
    val left = minOf(elem.startX, elem.endX)
    val top = minOf(elem.startY, elem.endY)
    val right = maxOf(elem.startX, elem.endX)
    val bottom = maxOf(elem.startY, elem.endY)
    val w = max(right - left, 1f)
    val h = max(bottom - top, 1f)

    val stroke = Stroke(width = elem.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)

    when (elem.shapeType) {
        ShapeType.LINE -> {
            drawLine(shapeColor, Offset(elem.startX, elem.startY), Offset(elem.endX, elem.endY), strokeWidth = elem.strokeWidth, cap = StrokeCap.Round)
        }
        ShapeType.ARROW -> {
            drawLine(shapeColor, Offset(elem.startX, elem.startY), Offset(elem.endX, elem.endY), strokeWidth = elem.strokeWidth, cap = StrokeCap.Round)
            val angle = atan2((elem.endY - elem.startY).toDouble(), (elem.endX - elem.startX).toDouble())
            val arrowLen = 28.0
            val a1 = angle - Math.PI / 6
            val a2 = angle + Math.PI / 6
            val x1 = (elem.endX - arrowLen * cos(a1)).toFloat()
            val y1 = (elem.endY - arrowLen * sin(a1)).toFloat()
            val x2 = (elem.endX - arrowLen * cos(a2)).toFloat()
            val y2 = (elem.endY - arrowLen * sin(a2)).toFloat()
            drawLine(shapeColor, Offset(elem.endX, elem.endY), Offset(x1, y1), strokeWidth = elem.strokeWidth, cap = StrokeCap.Round)
            drawLine(shapeColor, Offset(elem.endX, elem.endY), Offset(x2, y2), strokeWidth = elem.strokeWidth, cap = StrokeCap.Round)
        }
        ShapeType.RECT -> {
            if (elem.isFilled) {
                drawRect(shapeColor, Offset(left, top), Size(w, h))
            } else {
                drawRect(shapeColor, Offset(left, top), Size(w, h), style = stroke)
            }
        }
        ShapeType.SQUARE -> {
            val side = maxOf(w, h)
            if (elem.isFilled) {
                drawRect(shapeColor, Offset(left, top), Size(side, side))
            } else {
                drawRect(shapeColor, Offset(left, top), Size(side, side), style = stroke)
            }
        }
        ShapeType.CIRCLE -> {
            val radius = maxOf(w, h) / 2f
            val center = Offset((left + right) / 2f, (top + bottom) / 2f)
            if (elem.isFilled) {
                drawCircle(shapeColor, radius, center)
            } else {
                drawCircle(shapeColor, radius, center, style = stroke)
            }
        }
        ShapeType.OVAL -> {
            val rect = Rect(left, top, right, bottom)
            val path = Path().apply { addOval(rect) }
            if (elem.isFilled) drawPath(path, shapeColor) else drawPath(path, shapeColor, style = stroke)
        }
        ShapeType.TRIANGLE -> {
            val path = Path().apply {
                moveTo((left + right) / 2f, top)
                lineTo(right, bottom)
                lineTo(left, bottom)
                close()
            }
            if (elem.isFilled) drawPath(path, shapeColor) else drawPath(path, shapeColor, style = stroke)
        }
        ShapeType.RIGHT_TRIANGLE -> {
            val path = Path().apply {
                moveTo(left, top)
                lineTo(left, bottom)
                lineTo(right, bottom)
                close()
            }
            if (elem.isFilled) drawPath(path, shapeColor) else drawPath(path, shapeColor, style = stroke)
        }
        ShapeType.STAR -> {
            val cx = (left + right) / 2f
            val cy = (top + bottom) / 2f
            val rOut = minOf(w, h) / 2f
            val rIn = rOut * 0.42f
            val path = Path()
            for (i in 0 until 10) {
                val r = if (i % 2 == 0) rOut else rIn
                val a = (i * Math.PI / 5) - Math.PI / 2
                val px = (cx + r * cos(a)).toFloat()
                val py = (cy + r * sin(a)).toFloat()
                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            path.close()
            if (elem.isFilled) drawPath(path, shapeColor) else drawPath(path, shapeColor, style = stroke)
        }
        ShapeType.DIAMOND -> {
            val cx = (left + right) / 2f
            val cy = (top + bottom) / 2f
            val path = Path().apply {
                moveTo(cx, top)
                lineTo(right, cy)
                lineTo(cx, bottom)
                lineTo(left, cy)
                close()
            }
            if (elem.isFilled) drawPath(path, shapeColor) else drawPath(path, shapeColor, style = stroke)
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
            if (elem.isFilled) drawPath(path, shapeColor) else drawPath(path, shapeColor, style = stroke)
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
            if (elem.isFilled) drawPath(path, shapeColor) else drawPath(path, shapeColor, style = stroke)
        }
        ShapeType.HEART -> {
            val cx = (left + right) / 2f
            val path = Path().apply {
                moveTo(cx, top + h * 0.3f)
                cubicTo(left, top, left, top + h * 0.6f, cx, bottom)
                cubicTo(right, top + h * 0.6f, right, top, cx, top + h * 0.3f)
            }
            if (elem.isFilled) drawPath(path, shapeColor) else drawPath(path, shapeColor, style = stroke)
        }
        ShapeType.PARALLEL -> {
            val offset = w * 0.25f
            val path = Path().apply {
                moveTo(left + offset, top)
                lineTo(right, top)
                lineTo(right - offset, bottom)
                lineTo(left, bottom)
                close()
            }
            if (elem.isFilled) drawPath(path, shapeColor) else drawPath(path, shapeColor, style = stroke)
        }
        ShapeType.TRAPEZOID -> {
            val offset = w * 0.2f
            val path = Path().apply {
                moveTo(left + offset, top)
                lineTo(right - offset, top)
                lineTo(right, bottom)
                lineTo(left, bottom)
                close()
            }
            if (elem.isFilled) drawPath(path, shapeColor) else drawPath(path, shapeColor, style = stroke)
        }
        ShapeType.CROSS -> {
            val armW = w * 0.3f
            val cx = (left + right) / 2f
            val cy = (top + bottom) / 2f
            val path = Path().apply {
                moveTo(cx - armW / 2, top)
                lineTo(cx + armW / 2, top)
                lineTo(cx + armW / 2, cy - armW / 2)
                lineTo(right, cy - armW / 2)
                lineTo(right, cy + armW / 2)
                lineTo(cx + armW / 2, cy + armW / 2)
                lineTo(cx + armW / 2, bottom)
                lineTo(cx - armW / 2, bottom)
                lineTo(cx - armW / 2, cy + armW / 2)
                lineTo(left, cy + armW / 2)
                lineTo(left, cy - armW / 2)
                lineTo(cx - armW / 2, cy - armW / 2)
                close()
            }
            if (elem.isFilled) drawPath(path, shapeColor) else drawPath(path, shapeColor, style = stroke)
        }
        ShapeType.CUBE -> {
            val d = minOf(w, h) * 0.3f
            val fL = left
            val fT = top + d
            val fR = right - d
            val fB = bottom
            drawRect(shapeColor, Offset(fL, fT), Size(fR - fL, fB - fT), style = stroke)
            val topP = Path().apply {
                moveTo(fL, fT); lineTo(fL + d, top); lineTo(right, top); lineTo(fR, fT); close()
            }
            drawPath(topP, shapeColor, style = stroke)
            val rightP = Path().apply {
                moveTo(fR, fT); lineTo(right, top); lineTo(right, fB - d); lineTo(fR, fB); close()
            }
            drawPath(rightP, shapeColor, style = stroke)
        }
        ShapeType.CYLINDER -> {
            val ovalH = h * 0.22f
            val topOval = Path().apply { addOval(Rect(left, top, right, top + ovalH)) }
            drawPath(topOval, shapeColor, style = stroke)
            drawLine(shapeColor, Offset(left, top + ovalH / 2), Offset(left, bottom - ovalH / 2), strokeWidth = elem.strokeWidth)
            drawLine(shapeColor, Offset(right, top + ovalH / 2), Offset(right, bottom - ovalH / 2), strokeWidth = elem.strokeWidth)
            drawArc(shapeColor, 0f, 180f, false, Offset(left, bottom - ovalH), Size(w, ovalH), style = stroke)
        }
        ShapeType.CONE -> {
            val ovalH = h * 0.22f
            val apexX = (left + right) / 2f
            drawLine(shapeColor, Offset(apexX, top), Offset(left, bottom - ovalH / 2), strokeWidth = elem.strokeWidth)
            drawLine(shapeColor, Offset(apexX, top), Offset(right, bottom - ovalH / 2), strokeWidth = elem.strokeWidth)
            val bottomOval = Path().apply { addOval(Rect(left, bottom - ovalH, right, bottom)) }
            drawPath(bottomOval, shapeColor, style = stroke)
        }
        ShapeType.PYRAMID -> {
            val apexX = (left + right) / 2f
            drawLine(shapeColor, Offset(apexX, top), Offset(left, bottom), strokeWidth = elem.strokeWidth)
            drawLine(shapeColor, Offset(apexX, top), Offset(right, bottom), strokeWidth = elem.strokeWidth)
            drawLine(shapeColor, Offset(apexX, top), Offset(apexX + w * 0.15f, bottom - h * 0.08f), strokeWidth = elem.strokeWidth)
            drawLine(shapeColor, Offset(left, bottom), Offset(apexX + w * 0.15f, bottom - h * 0.08f), strokeWidth = elem.strokeWidth)
            drawLine(shapeColor, Offset(apexX + w * 0.15f, bottom - h * 0.08f), Offset(right, bottom), strokeWidth = elem.strokeWidth)
        }
        ShapeType.SPHERE -> {
            val radius = minOf(w, h) / 2f
            val center = Offset((left + right) / 2f, (top + bottom) / 2f)
            drawCircle(shapeColor, radius, center, style = stroke)
            val eq = Path().apply { addOval(Rect(left, center.y - radius * 0.3f, right, center.y + radius * 0.3f)) }
            drawPath(eq, shapeColor, style = stroke)
            val me = Path().apply { addOval(Rect(center.x - radius * 0.3f, top, center.x + radius * 0.3f, bottom)) }
            drawPath(me, shapeColor, style = stroke)
        }
        else -> {
            drawRect(shapeColor, Offset(left, top), Size(w, h), style = stroke)
        }
    }
}

private fun DrawScope.drawLiveShapePreview(
    shapeType: ShapeType,
    start: Offset,
    end: Offset,
    color: Color,
    strokeWidth: Float,
    isFilled: Boolean
) {
    drawShapeElement(
        CanvasElement.Shape(
            id = "preview",
            layerId = "active",
            shapeType = shapeType,
            startX = start.x,
            startY = start.y,
            endX = end.x,
            endY = end.y,
            color = color.toArgb().toLong(),
            strokeWidth = strokeWidth,
            isFilled = isFilled
        )
    )
}

private fun DrawScope.drawTextElement(elem: CanvasElement.Text) {
    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            color = elem.color.toInt()
            textSize = elem.fontSize
            isAntiAlias = true
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        drawText(elem.text, elem.x, elem.y, paint)
    }
}

private fun DrawScope.drawFormulaElement(elem: CanvasElement.Formula) {
    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            color = elem.color.toInt()
            textSize = elem.fontSize
            isAntiAlias = true
            typeface = android.graphics.Typeface.SERIF
        }
        drawText(elem.formula, elem.x, elem.y, paint)
    }
}

private fun DrawScope.drawStickerElement(elem: CanvasElement.Sticker) {
    val s = elem.size
    val x = elem.x
    val y = elem.y

    when (elem.stickerKey) {
        "star", "smiley_star" -> {
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
            drawPath(path, Color(0xFFFBBF24))
        }
        "trophy" -> {
            drawRect(Color(0xFFF59E0B), Offset(x + s * 0.3f, y + s * 0.15f), Size(s * 0.4f, s * 0.4f))
            drawRect(Color(0xFFF59E0B), Offset(x + s * 0.42f, y + s * 0.55f), Size(s * 0.16f, s * 0.23f))
            drawRect(Color(0xFF78350F), Offset(x + s * 0.28f, y + s * 0.78f), Size(s * 0.44f, s * 0.12f))
        }
        "crown" -> {
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
            drawPath(path, Color(0xFFF59E0B))
        }
        "a_plus" -> {
            drawCircle(Color(0xFF16A34A), radius = s / 2, center = Offset(x + s / 2, y + s / 2))
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = s * 0.45f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    isAntiAlias = true
                }
                drawText("A+", x + s * 0.22f, y + s * 0.65f, paint)
            }
        }
        "hundred" -> {
            drawCircle(Color(0xFFEA580C), radius = s / 2, center = Offset(x + s / 2, y + s / 2))
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = s * 0.32f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    isAntiAlias = true
                }
                drawText("100%", x + s * 0.14f, y + s * 0.62f, paint)
            }
        }
        "excellent" -> {
            drawRoundRect(Color(0xFF16A34A), Offset(x, y + s * 0.2f), Size(s * 1.5f, s * 0.6f), CornerRadius(16f, 16f), style = Stroke(width = 4f))
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#16A34A")
                    textSize = s * 0.32f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    isAntiAlias = true
                }
                drawText("Excellent!", x + s * 0.12f, y + s * 0.62f, paint)
            }
        }
        "good_job" -> {
            drawRoundRect(Color(0xFF2563EB), Offset(x, y + s * 0.2f), Size(s * 1.5f, s * 0.6f), CornerRadius(16f, 16f), style = Stroke(width = 4f))
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#2563EB")
                    textSize = s * 0.32f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    isAntiAlias = true
                }
                drawText("Good Job!", x + s * 0.14f, y + s * 0.62f, paint)
            }
        }
        else -> {
            drawCircle(Color(0xFF3B82F6), radius = s / 2, center = Offset(x + s / 2, y + s / 2))
        }
    }
}

private fun DrawScope.drawScienceElement(elem: CanvasElement.Science) {
    val s = elem.size
    val cx = elem.x + s / 2
    val cy = elem.y + s / 2

    if (elem.category == "atomic") {
        val nRadius = s * 0.16f
        drawCircle(Color(0xFFEF4444), radius = nRadius, center = Offset(cx, cy))
        drawContext.canvas.nativeCanvas.apply {
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = s * 0.15f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                isAntiAlias = true
            }
            drawText(elem.symbol, cx - s * 0.05f, cy + s * 0.05f, paint)
        }

        val numShells = when {
            elem.atomicNumber <= 2 -> 1
            elem.atomicNumber <= 10 -> 2
            else -> 3
        }
        for (shell in 1..numShells) {
            val r = nRadius + shell * (s * 0.12f)
            drawCircle(Color(0xFF3B82F6), radius = r, center = Offset(cx, cy), style = Stroke(width = 2f))
            val numElectrons = if (shell == 1) minOf(2, elem.atomicNumber) else minOf(8, elem.atomicNumber - 2)
            for (e in 0 until numElectrons) {
                val a = e * (2 * Math.PI / numElectrons)
                val ex = (cx + r * cos(a)).toFloat()
                val ey = (cy + r * sin(a)).toFloat()
                drawCircle(Color(0xFF1D4ED8), radius = 5f, center = Offset(ex, ey))
            }
        }
    } else {
        val path = Path().apply {
            moveTo(cx - s * 0.15f, cy - s * 0.35f)
            lineTo(cx + s * 0.15f, cy - s * 0.35f)
            lineTo(cx + s * 0.15f, cy - s * 0.1f)
            lineTo(cx + s * 0.35f, cy + s * 0.35f)
            lineTo(cx - s * 0.35f, cy + s * 0.35f)
            lineTo(cx - s * 0.15f, cy - s * 0.1f)
            close()
        }
        drawPath(path, Color(0xFF0284C7), style = Stroke(width = 3.5f))
    }
}

private fun DrawScope.drawTableElement(elem: CanvasElement.Table) {
    val tableColor = Color(elem.color)
    val stroke = Stroke(width = 2.5f)
    drawRect(tableColor, Offset(elem.x, elem.y), Size(elem.width, elem.height), style = stroke)

    val cellW = elem.width / elem.cols
    val cellH = elem.height / elem.rows

    for (c in 1 until elem.cols) {
        val lx = elem.x + c * cellW
        drawLine(tableColor, Offset(lx, elem.y), Offset(lx, elem.y + elem.height), strokeWidth = 2f)
    }
    for (r in 1 until elem.rows) {
        val ly = elem.y + r * cellH
        drawLine(tableColor, Offset(elem.x, ly), Offset(elem.x + elem.width, ly), strokeWidth = 2f)
    }
}

private fun DrawScope.drawRuler(pos: Offset) {
    val w = 500f
    val h = 100f
    drawRoundRect(
        color = Color(0xDDFFFBEB),
        topLeft = pos,
        size = Size(w, h),
        cornerRadius = CornerRadius(12f, 12f)
    )
    drawRoundRect(
        color = Color(0xFFB45309),
        topLeft = pos,
        size = Size(w, h),
        cornerRadius = CornerRadius(12f, 12f),
        style = Stroke(width = 2f)
    )

    var x = 20f
    var cm = 0
    while (x <= w - 20f) {
        val tickH = if (cm % 5 == 0) 30f else 16f
        drawLine(
            color = Color(0xFF78350F),
            start = Offset(pos.x + x, pos.y),
            end = Offset(pos.x + x, pos.y + tickH),
            strokeWidth = 2f
        )
        if (cm % 5 == 0) {
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#78350F")
                    textSize = 22f
                    isAntiAlias = true
                }
                drawText("${cm / 5}", pos.x + x - 6f, pos.y + tickH + 22f, paint)
            }
        }
        x += 16f
        cm++
    }
}

package com.example.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.cos
import kotlin.math.sin

enum class ToolType(val displayName: String) {
    SELECT("Select"),
    PEN("Pen"),
    BRUSH("Brush"),
    MARKER("Marker"),
    HIGHLIGHT("Highlight"),
    CALLIGRAPHY("Calligraphy"),
    SMART("Smart Draw"),
    SHAPES("Shapes"),
    ERASER("Eraser"),
    BOARD("Board"),
    TEXT("Text"),
    FORMULA("Formula"),
    TABLE("Table"),
    LAYERS("Layers"),
    STICKERS("Stickers"),
    SCIENCE("Science"),
    LASER("Laser"),
    RULER("Ruler"),
    LASSO("Lasso"),
    HAND_PAN("Hand/Pan"),
    CUSTOMIZE("Customize"),
    SAVE("Save"),
    SHARE("Share"),
    PDF_EXPORT("PDF Export"),
    UNDO("Undo"),
    REDO("Redo"),
    CLEAR("Clear Canvas")
}

enum class ShapeCategory {
    TWO_D,
    THREE_D
}

enum class ShapeType(val displayName: String, val category: ShapeCategory) {
    LINE("Line", ShapeCategory.TWO_D),
    ARROW("Arrow", ShapeCategory.TWO_D),
    RECT("Rect", ShapeCategory.TWO_D),
    SQUARE("Square", ShapeCategory.TWO_D),
    CIRCLE("Circle", ShapeCategory.TWO_D),
    OVAL("Oval", ShapeCategory.TWO_D),
    TRIANGLE("Triangle", ShapeCategory.TWO_D),
    RIGHT_TRIANGLE("Right △", ShapeCategory.TWO_D),
    STAR("Star", ShapeCategory.TWO_D),
    PENTAGON("Pentagon", ShapeCategory.TWO_D),
    HEXAGON("Hexagon", ShapeCategory.TWO_D),
    DIAMOND("Diamond", ShapeCategory.TWO_D),
    PARALLEL("Parallel", ShapeCategory.TWO_D),
    TRAPEZOID("Trapezoid", ShapeCategory.TWO_D),
    HEART("Heart", ShapeCategory.TWO_D),
    CROSS("Cross", ShapeCategory.TWO_D),
    SEMICIRCLE("Semicircle", ShapeCategory.TWO_D),
    SECTOR("Sector", ShapeCategory.TWO_D),
    RIGHT_ANGLE("Right ∠", ShapeCategory.TWO_D),
    
    // 3D
    CUBE("Cube", ShapeCategory.THREE_D),
    CYLINDER("Cylinder", ShapeCategory.THREE_D),
    CONE("Cone", ShapeCategory.THREE_D),
    PYRAMID("Pyramid", ShapeCategory.THREE_D),
    SPHERE("Sphere", ShapeCategory.THREE_D)
}

enum class BoardTexture(val displayName: String) {
    PLAIN("Plain"),
    GRID("Grid"),
    LINES("Lines"),
    DOTS("Dots"),
    CHALK("Chalk"),
    ISOMETRIC("Isometric"),
    GRAPH("Graph"),
    MUSIC("Music"),
    DIAMOND("Diamond"),
    NOTEBOOK("Notebook")
}

data class PointData(
    val x: Float,
    val y: Float
) {
    fun toOffset(): Offset = Offset(x, y)
    companion object {
        fun fromOffset(offset: Offset): PointData = PointData(offset.x, offset.y)
    }
}

data class LayerInfo(
    val id: String,
    val name: String,
    val isVisible: Boolean = true,
    val colorDot: Long = 0xFF7C4DFF
)

sealed class CanvasElement {
    abstract val id: String
    abstract val layerId: String
    abstract val rotation: Float

    abstract fun getBounds(): Rect
    abstract fun getCenter(): Offset
    abstract fun translated(dx: Float, dy: Float): CanvasElement
    abstract fun rotated(degrees: Float): CanvasElement
    abstract fun scaled(factor: Float): CanvasElement
    abstract fun withColor(color: Long): CanvasElement

    data class Stroke(
        override val id: String,
        override val layerId: String,
        val points: List<PointData>,
        val color: Long,
        val strokeWidth: Float,
        val toolType: ToolType,
        override val rotation: Float = 0f
    ) : CanvasElement() {
        override fun getBounds(): Rect {
            if (points.isEmpty()) return Rect.Zero
            var minX = Float.MAX_VALUE
            var minY = Float.MAX_VALUE
            var maxX = Float.MIN_VALUE
            var maxY = Float.MIN_VALUE
            for (p in points) {
                if (p.x < minX) minX = p.x
                if (p.y < minY) minY = p.y
                if (p.x > maxX) maxX = p.x
                if (p.y > maxY) maxY = p.y
            }
            val pad = strokeWidth / 2f + 8f
            return Rect(minX - pad, minY - pad, maxX + pad, maxY + pad)
        }

        override fun getCenter(): Offset {
            val b = getBounds()
            return Offset(b.left + b.width / 2f, b.top + b.height / 2f)
        }

        override fun translated(dx: Float, dy: Float): CanvasElement {
            return copy(points = points.map { PointData(it.x + dx, it.y + dy) })
        }

        override fun rotated(degrees: Float): CanvasElement {
            val center = getCenter()
            val rad = Math.toRadians(degrees.toDouble())
            val cosA = cos(rad).toFloat()
            val sinA = sin(rad).toFloat()
            val newPoints = points.map { p ->
                val rx = p.x - center.x
                val ry = p.y - center.y
                PointData(
                    center.x + (rx * cosA - ry * sinA),
                    center.y + (rx * sinA + ry * cosA)
                )
            }
            return copy(points = newPoints, rotation = (rotation + degrees) % 360f)
        }

        override fun scaled(factor: Float): CanvasElement {
            val center = getCenter()
            val newPoints = points.map { p ->
                PointData(
                    center.x + (p.x - center.x) * factor,
                    center.y + (p.y - center.y) * factor
                )
            }
            return copy(
                points = newPoints,
                strokeWidth = (strokeWidth * factor).coerceIn(2f, 80f)
            )
        }

        override fun withColor(color: Long): CanvasElement {
            return copy(color = color)
        }
    }

    data class Shape(
        override val id: String,
        override val layerId: String,
        val shapeType: ShapeType,
        val startX: Float,
        val startY: Float,
        val endX: Float,
        val endY: Float,
        val color: Long,
        val strokeWidth: Float,
        val isFilled: Boolean,
        override val rotation: Float = 0f
    ) : CanvasElement() {
        override fun getBounds(): Rect {
            val minX = minOf(startX, endX)
            val minY = minOf(startY, endY)
            val maxX = maxOf(startX, endX)
            val maxY = maxOf(startY, endY)
            val pad = strokeWidth / 2f + 4f
            return Rect(minX - pad, minY - pad, maxX + pad, maxY + pad)
        }

        override fun getCenter(): Offset {
            return Offset((startX + endX) / 2f, (startY + endY) / 2f)
        }

        override fun translated(dx: Float, dy: Float): CanvasElement {
            return copy(startX = startX + dx, startY = startY + dy, endX = endX + dx, endY = endY + dy)
        }

        override fun rotated(degrees: Float): CanvasElement {
            return copy(rotation = (rotation + degrees) % 360f)
        }

        override fun scaled(factor: Float): CanvasElement {
            val cx = (startX + endX) / 2f
            val cy = (startY + endY) / 2f
            val halfW = ((endX - startX).let { if (it == 0f) 20f else it } / 2f) * factor
            val halfH = ((endY - startY).let { if (it == 0f) 20f else it } / 2f) * factor
            return copy(
                startX = cx - halfW,
                startY = cy - halfH,
                endX = cx + halfW,
                endY = cy + halfH
            )
        }

        override fun withColor(color: Long): CanvasElement {
            return copy(color = color)
        }
    }

    data class Text(
        override val id: String,
        override val layerId: String,
        val text: String,
        val x: Float,
        val y: Float,
        val color: Long,
        val fontSize: Float,
        override val rotation: Float = 0f
    ) : CanvasElement() {
        override fun getBounds(): Rect {
            val approxWidth = (text.length * fontSize * 0.62f).coerceAtLeast(60f)
            val approxHeight = (fontSize * 1.3f).coerceAtLeast(30f)
            return Rect(x - 8f, y - approxHeight, x + approxWidth + 8f, y + 8f)
        }

        override fun getCenter(): Offset {
            val b = getBounds()
            return Offset(b.left + b.width / 2f, b.top + b.height / 2f)
        }

        override fun translated(dx: Float, dy: Float): CanvasElement {
            return copy(x = x + dx, y = y + dy)
        }

        override fun rotated(degrees: Float): CanvasElement {
            return copy(rotation = (rotation + degrees) % 360f)
        }

        override fun scaled(factor: Float): CanvasElement {
            return copy(fontSize = (fontSize * factor).coerceIn(16f, 180f))
        }

        override fun withColor(color: Long): CanvasElement {
            return copy(color = color)
        }
    }

    data class Formula(
        override val id: String,
        override val layerId: String,
        val formula: String,
        val x: Float,
        val y: Float,
        val color: Long,
        val fontSize: Float,
        override val rotation: Float = 0f
    ) : CanvasElement() {
        override fun getBounds(): Rect {
            val approxWidth = (formula.length * fontSize * 0.65f).coerceAtLeast(80f)
            val approxHeight = (fontSize * 1.4f).coerceAtLeast(36f)
            return Rect(x - 8f, y - approxHeight, x + approxWidth + 8f, y + 8f)
        }

        override fun getCenter(): Offset {
            val b = getBounds()
            return Offset(b.left + b.width / 2f, b.top + b.height / 2f)
        }

        override fun translated(dx: Float, dy: Float): CanvasElement {
            return copy(x = x + dx, y = y + dy)
        }

        override fun rotated(degrees: Float): CanvasElement {
            return copy(rotation = (rotation + degrees) % 360f)
        }

        override fun scaled(factor: Float): CanvasElement {
            return copy(fontSize = (fontSize * factor).coerceIn(16f, 160f))
        }

        override fun withColor(color: Long): CanvasElement {
            return copy(color = color)
        }
    }

    data class Sticker(
        override val id: String,
        override val layerId: String,
        val stickerKey: String,
        val x: Float,
        val y: Float,
        val size: Float,
        override val rotation: Float = 0f
    ) : CanvasElement() {
        override fun getBounds(): Rect {
            return Rect(x - 4f, y - 4f, x + size + 4f, y + size + 4f)
        }

        override fun getCenter(): Offset {
            return Offset(x + size / 2f, y + size / 2f)
        }

        override fun translated(dx: Float, dy: Float): CanvasElement {
            return copy(x = x + dx, y = y + dy)
        }

        override fun rotated(degrees: Float): CanvasElement {
            return copy(rotation = (rotation + degrees) % 360f)
        }

        override fun scaled(factor: Float): CanvasElement {
            val newSize = (size * factor).coerceIn(40f, 600f)
            val cx = x + size / 2f
            val cy = y + size / 2f
            return copy(
                size = newSize,
                x = cx - newSize / 2f,
                y = cy - newSize / 2f
            )
        }

        override fun withColor(color: Long): CanvasElement {
            return this
        }
    }

    data class Science(
        override val id: String,
        override val layerId: String,
        val category: String, // "atomic", "lab", "measuring"
        val symbol: String,   // "H", "He", "beaker", etc.
        val label: String,
        val atomicNumber: Int = 0,
        val x: Float,
        val y: Float,
        val size: Float,
        override val rotation: Float = 0f
    ) : CanvasElement() {
        override fun getBounds(): Rect {
            return Rect(x - 4f, y - 4f, x + size + 4f, y + size + 4f)
        }

        override fun getCenter(): Offset {
            return Offset(x + size / 2f, y + size / 2f)
        }

        override fun translated(dx: Float, dy: Float): CanvasElement {
            return copy(x = x + dx, y = y + dy)
        }

        override fun rotated(degrees: Float): CanvasElement {
            return copy(rotation = (rotation + degrees) % 360f)
        }

        override fun scaled(factor: Float): CanvasElement {
            val newSize = (size * factor).coerceIn(50f, 600f)
            val cx = x + size / 2f
            val cy = y + size / 2f
            return copy(
                size = newSize,
                x = cx - newSize / 2f,
                y = cy - newSize / 2f
            )
        }

        override fun withColor(color: Long): CanvasElement {
            return this
        }
    }

    data class Table(
        override val id: String,
        override val layerId: String,
        val rows: Int,
        val cols: Int,
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
        val cells: List<List<String>>,
        val color: Long,
        override val rotation: Float = 0f
    ) : CanvasElement() {
        override fun getBounds(): Rect {
            return Rect(x - 4f, y - 4f, x + width + 4f, y + height + 4f)
        }

        override fun getCenter(): Offset {
            return Offset(x + width / 2f, y + height / 2f)
        }

        override fun translated(dx: Float, dy: Float): CanvasElement {
            return copy(x = x + dx, y = y + dy)
        }

        override fun rotated(degrees: Float): CanvasElement {
            return copy(rotation = (rotation + degrees) % 360f)
        }

        override fun scaled(factor: Float): CanvasElement {
            val newW = (width * factor).coerceIn(80f, 1200f)
            val newH = (height * factor).coerceIn(60f, 900f)
            val cx = x + width / 2f
            val cy = y + height / 2f
            return copy(
                width = newW,
                height = newH,
                x = cx - newW / 2f,
                y = cy - newH / 2f
            )
        }

        override fun withColor(color: Long): CanvasElement {
            return copy(color = color)
        }
    }
}

data class BoardPage(
    val pageId: String,
    val elements: List<CanvasElement> = emptyList()
)

data class LaserPoint(
    val x: Float,
    val y: Float,
    val timestamp: Long
)

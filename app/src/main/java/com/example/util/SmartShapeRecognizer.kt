package com.example.util

import com.example.model.CanvasElement
import com.example.model.PointData
import com.example.model.ShapeType
import java.util.UUID
import kotlin.math.*

object SmartShapeRecognizer {

    fun recognize(
        points: List<PointData>,
        color: Long,
        strokeWidth: Float,
        layerId: String
    ): CanvasElement? {
        if (points.size < 8) return null

        val first = points.first()
        val last = points.last()
        val totalDx = last.x - first.x
        val totalDy = last.y - first.y
        val startEndDist = hypot(totalDx, totalDy)

        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE
        var pathLength = 0.0

        for (i in points.indices) {
            val p = points[i]
            if (p.x < minX) minX = p.x
            if (p.y < minY) minY = p.y
            if (p.x > maxX) maxX = p.x
            if (p.y > maxY) maxY = p.y
            if (i > 0) {
                val prev = points[i - 1]
                pathLength += hypot(p.x - prev.x, p.y - prev.y)
            }
        }

        val boxWidth = maxX - minX
        val boxHeight = maxY - minY
        val diagonal = hypot(boxWidth, boxHeight)
        if (diagonal < 30f) return null

        val isClosed = startEndDist < (diagonal * 0.35f) || (startEndDist < 80f)

        if (!isClosed) {
            // Straight line or arrow
            val straightRatio = pathLength / (startEndDist + 1e-4)
            if (straightRatio < 1.35) {
                return CanvasElement.Shape(
                    id = UUID.randomUUID().toString(),
                    layerId = layerId,
                    shapeType = ShapeType.LINE,
                    startX = first.x,
                    startY = first.y,
                    endX = last.x,
                    endY = last.y,
                    color = color,
                    strokeWidth = strokeWidth,
                    isFilled = false
                )
            }
            return null
        }

        // Closed shape
        val cx = (minX + maxX) / 2f
        val cy = (minY + maxY) / 2f
        val avgRadius = (boxWidth + boxHeight) / 4f

        var radiusVarianceSum = 0.0
        for (p in points) {
            val dist = hypot(p.x - cx, p.y - cy)
            val diff = dist - avgRadius
            radiusVarianceSum += diff * diff
        }
        val variance = sqrt(radiusVarianceSum / points.size) / avgRadius

        // Circle / Oval check
        if (variance < 0.22) {
            val aspectRatio = boxWidth / (boxHeight + 1e-4)
            val shapeType = if (aspectRatio in 0.85f..1.18f) ShapeType.CIRCLE else ShapeType.OVAL
            return CanvasElement.Shape(
                id = UUID.randomUUID().toString(),
                layerId = layerId,
                shapeType = shapeType,
                startX = minX,
                startY = minY,
                endX = maxX,
                endY = maxY,
                color = color,
                strokeWidth = strokeWidth,
                isFilled = false
            )
        }

        // Check aspect ratio for rectangle or triangle
        val aspectRatio = boxWidth / (boxHeight + 1e-4)
        val shapeType = if (aspectRatio in 0.82f..1.22f) ShapeType.SQUARE else ShapeType.RECT
        return CanvasElement.Shape(
            id = UUID.randomUUID().toString(),
            layerId = layerId,
            shapeType = shapeType,
            startX = minX,
            startY = minY,
            endX = maxX,
            endY = maxY,
            color = color,
            strokeWidth = strokeWidth,
            isFilled = false
        )
    }
}

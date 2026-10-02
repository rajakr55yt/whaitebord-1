package com.example.data

import com.example.model.*
import org.json.JSONArray
import org.json.JSONObject

object CanvasSerializer {

    fun serializePages(pages: List<BoardPage>): String {
        val rootArray = JSONArray()
        for (page in pages) {
            val pageObj = JSONObject()
            pageObj.put("pageId", page.pageId)
            val elementsArray = JSONArray()
            for (elem in page.elements) {
                val elemObj = JSONObject()
                elemObj.put("id", elem.id)
                elemObj.put("layerId", elem.layerId)
                elemObj.put("rotation", elem.rotation.toDouble())
                when (elem) {
                    is CanvasElement.Stroke -> {
                        elemObj.put("type", "stroke")
                        elemObj.put("color", elem.color)
                        elemObj.put("strokeWidth", elem.strokeWidth.toDouble())
                        elemObj.put("toolType", elem.toolType.name)
                        val ptsArray = JSONArray()
                        for (pt in elem.points) {
                            val pObj = JSONObject()
                            pObj.put("x", pt.x.toDouble())
                            pObj.put("y", pt.y.toDouble())
                            ptsArray.put(pObj)
                        }
                        elemObj.put("points", ptsArray)
                    }
                    is CanvasElement.Shape -> {
                        elemObj.put("type", "shape")
                        elemObj.put("shapeType", elem.shapeType.name)
                        elemObj.put("startX", elem.startX.toDouble())
                        elemObj.put("startY", elem.startY.toDouble())
                        elemObj.put("endX", elem.endX.toDouble())
                        elemObj.put("endY", elem.endY.toDouble())
                        elemObj.put("color", elem.color)
                        elemObj.put("strokeWidth", elem.strokeWidth.toDouble())
                        elemObj.put("isFilled", elem.isFilled)
                    }
                    is CanvasElement.Text -> {
                        elemObj.put("type", "text")
                        elemObj.put("text", elem.text)
                        elemObj.put("x", elem.x.toDouble())
                        elemObj.put("y", elem.y.toDouble())
                        elemObj.put("color", elem.color)
                        elemObj.put("fontSize", elem.fontSize.toDouble())
                    }
                    is CanvasElement.Formula -> {
                        elemObj.put("type", "formula")
                        elemObj.put("formula", elem.formula)
                        elemObj.put("x", elem.x.toDouble())
                        elemObj.put("y", elem.y.toDouble())
                        elemObj.put("color", elem.color)
                        elemObj.put("fontSize", elem.fontSize.toDouble())
                    }
                    is CanvasElement.Sticker -> {
                        elemObj.put("type", "sticker")
                        elemObj.put("stickerKey", elem.stickerKey)
                        elemObj.put("x", elem.x.toDouble())
                        elemObj.put("y", elem.y.toDouble())
                        elemObj.put("size", elem.size.toDouble())
                    }
                    is CanvasElement.Science -> {
                        elemObj.put("type", "science")
                        elemObj.put("category", elem.category)
                        elemObj.put("symbol", elem.symbol)
                        elemObj.put("label", elem.label)
                        elemObj.put("atomicNumber", elem.atomicNumber)
                        elemObj.put("x", elem.x.toDouble())
                        elemObj.put("y", elem.y.toDouble())
                        elemObj.put("size", elem.size.toDouble())
                    }
                    is CanvasElement.Table -> {
                        elemObj.put("type", "table")
                        elemObj.put("rows", elem.rows)
                        elemObj.put("cols", elem.cols)
                        elemObj.put("x", elem.x.toDouble())
                        elemObj.put("y", elem.y.toDouble())
                        elemObj.put("width", elem.width.toDouble())
                        elemObj.put("height", elem.height.toDouble())
                        elemObj.put("color", elem.color)
                        val cellsArray = JSONArray()
                        for (row in elem.cells) {
                            val rowArray = JSONArray()
                            for (cell in row) {
                                rowArray.put(cell)
                            }
                            cellsArray.put(rowArray)
                        }
                        elemObj.put("cells", cellsArray)
                    }
                }
                elementsArray.put(elemObj)
            }
            pageObj.put("elements", elementsArray)
            rootArray.put(pageObj)
        }
        return rootArray.toString()
    }

    fun deserializePages(json: String): List<BoardPage> {
        val result = mutableListOf<BoardPage>()
        if (json.isBlank()) return result
        try {
            val rootArray = JSONArray(json)
            for (i in 0 until rootArray.length()) {
                val pageObj = rootArray.getJSONObject(i)
                val pageId = pageObj.optString("pageId", "page_$i")
                val elementsArray = pageObj.optJSONArray("elements") ?: JSONArray()
                val elements = mutableListOf<CanvasElement>()

                for (j in 0 until elementsArray.length()) {
                    val elemObj = elementsArray.getJSONObject(j)
                    val id = elemObj.getString("id")
                    val layerId = elemObj.optString("layerId", "layer_1")
                    val rotation = elemObj.optDouble("rotation", 0.0).toFloat()
                    val type = elemObj.optString("type")

                    when (type) {
                        "stroke" -> {
                            val color = elemObj.optLong("color", 0xFFFFFFFF)
                            val strokeWidth = elemObj.optDouble("strokeWidth", 8.0).toFloat()
                            val toolTypeName = elemObj.optString("toolType", ToolType.PEN.name)
                            val toolType = runCatching { ToolType.valueOf(toolTypeName) }.getOrDefault(ToolType.PEN)
                            val ptsArray = elemObj.optJSONArray("points") ?: JSONArray()
                            val points = mutableListOf<PointData>()
                            for (k in 0 until ptsArray.length()) {
                                val pObj = ptsArray.getJSONObject(k)
                                points.add(
                                    PointData(
                                        pObj.optDouble("x", 0.0).toFloat(),
                                        pObj.optDouble("y", 0.0).toFloat()
                                    )
                                )
                            }
                            elements.add(
                                CanvasElement.Stroke(
                                    id = id,
                                    layerId = layerId,
                                    points = points,
                                    color = color,
                                    strokeWidth = strokeWidth,
                                    toolType = toolType,
                                    rotation = rotation
                                )
                            )
                        }
                        "shape" -> {
                            val shapeTypeName = elemObj.optString("shapeType", ShapeType.RECT.name)
                            val shapeType = runCatching { ShapeType.valueOf(shapeTypeName) }.getOrDefault(ShapeType.RECT)
                            elements.add(
                                CanvasElement.Shape(
                                    id = id,
                                    layerId = layerId,
                                    shapeType = shapeType,
                                    startX = elemObj.optDouble("startX", 0.0).toFloat(),
                                    startY = elemObj.optDouble("startY", 0.0).toFloat(),
                                    endX = elemObj.optDouble("endX", 0.0).toFloat(),
                                    endY = elemObj.optDouble("endY", 0.0).toFloat(),
                                    color = elemObj.optLong("color", 0xFFFFFFFF),
                                    strokeWidth = elemObj.optDouble("strokeWidth", 4.0).toFloat(),
                                    isFilled = elemObj.optBoolean("isFilled", false),
                                    rotation = rotation
                                )
                            )
                        }
                        "text" -> {
                            elements.add(
                                CanvasElement.Text(
                                    id = id,
                                    layerId = layerId,
                                    text = elemObj.optString("text", ""),
                                    x = elemObj.optDouble("x", 0.0).toFloat(),
                                    y = elemObj.optDouble("y", 0.0).toFloat(),
                                    color = elemObj.optLong("color", 0xFFFFFFFF),
                                    fontSize = elemObj.optDouble("fontSize", 48.0).toFloat(),
                                    rotation = rotation
                                )
                            )
                        }
                        "formula" -> {
                            elements.add(
                                CanvasElement.Formula(
                                    id = id,
                                    layerId = layerId,
                                    formula = elemObj.optString("formula", ""),
                                    x = elemObj.optDouble("x", 0.0).toFloat(),
                                    y = elemObj.optDouble("y", 0.0).toFloat(),
                                    color = elemObj.optLong("color", 0xFFFFFFFF),
                                    fontSize = elemObj.optDouble("fontSize", 40.0).toFloat(),
                                    rotation = rotation
                                )
                            )
                        }
                        "sticker" -> {
                            elements.add(
                                CanvasElement.Sticker(
                                    id = id,
                                    layerId = layerId,
                                    stickerKey = elemObj.optString("stickerKey", "star"),
                                    x = elemObj.optDouble("x", 0.0).toFloat(),
                                    y = elemObj.optDouble("y", 0.0).toFloat(),
                                    size = elemObj.optDouble("size", 120.0).toFloat(),
                                    rotation = rotation
                                )
                            )
                        }
                        "science" -> {
                            elements.add(
                                CanvasElement.Science(
                                    id = id,
                                    layerId = layerId,
                                    category = elemObj.optString("category", "atomic"),
                                    symbol = elemObj.optString("symbol", "H"),
                                    label = elemObj.optString("label", "Hydrogen"),
                                    atomicNumber = elemObj.optInt("atomicNumber", 1),
                                    x = elemObj.optDouble("x", 0.0).toFloat(),
                                    y = elemObj.optDouble("y", 0.0).toFloat(),
                                    size = elemObj.optDouble("size", 140.0).toFloat(),
                                    rotation = rotation
                                )
                            )
                        }
                        "table" -> {
                            val rows = elemObj.optInt("rows", 3)
                            val cols = elemObj.optInt("cols", 3)
                            val cellsArray = elemObj.optJSONArray("cells")
                            val cells = mutableListOf<List<String>>()
                            if (cellsArray != null) {
                                for (r in 0 until cellsArray.length()) {
                                    val rowArr = cellsArray.getJSONArray(r)
                                    val row = mutableListOf<String>()
                                    for (c in 0 until rowArr.length()) {
                                        row.add(rowArr.optString(c, ""))
                                    }
                                    cells.add(row)
                                }
                            }
                            elements.add(
                                CanvasElement.Table(
                                    id = id,
                                    layerId = layerId,
                                    rows = rows,
                                    cols = cols,
                                    x = elemObj.optDouble("x", 0.0).toFloat(),
                                    y = elemObj.optDouble("y", 0.0).toFloat(),
                                    width = elemObj.optDouble("width", 300.0).toFloat(),
                                    height = elemObj.optDouble("height", 200.0).toFloat(),
                                    cells = cells,
                                    color = elemObj.optLong("color", 0xFFFFFFFF),
                                    rotation = rotation
                                )
                            )
                        }
                    }
                }
                result.add(BoardPage(pageId = pageId, elements = elements))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (result.isEmpty()) {
            result.add(BoardPage(pageId = "page_1"))
        }
        return result
    }
}

package com.example.viewmodel

import android.app.Application
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BoardEntity
import com.example.data.CanvasSerializer
import com.example.data.WhiteboardDatabase
import com.example.model.*
import com.example.util.ExportUtils
import com.example.util.SmartShapeRecognizer
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

data class WhiteboardUiState(
    val boardId: String = UUID.randomUUID().toString(),
    val boardTitle: String = "Untitled Board",
    val backgroundColor: Long = 0xFFFFFFFF, // default white or dark
    val texture: BoardTexture = BoardTexture.PLAIN,
    val activeTool: ToolType = ToolType.PEN,
    val brushColor: Long = 0xFF000000,
    val brushSize: Float = 12f,
    val selectedShapeType: ShapeType = ShapeType.RECT,
    val isShapeFilled: Boolean = false,
    val currentPageIndex: Int = 0,
    val pages: List<BoardPage> = listOf(BoardPage(pageId = "page_1")),
    val layers: List<LayerInfo> = listOf(LayerInfo(id = "layer_1", name = "Layer 1", isVisible = true, colorDot = 0xFF7C4DFF)),
    val activeLayerId: String = "layer_1",
    val selectedElementId: String? = null,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val isToolbarCollapsed: Boolean = false,
    val enabledTools: Set<ToolType> = setOf(
        ToolType.SELECT, ToolType.UNDO, ToolType.REDO, ToolType.CLEAR, ToolType.PEN, ToolType.SMART,
        ToolType.SHAPES, ToolType.ERASER, ToolType.BOARD, ToolType.TEXT, ToolType.FORMULA,
        ToolType.TABLE, ToolType.LAYERS, ToolType.STICKERS, ToolType.SCIENCE, ToolType.LASER,
        ToolType.RULER, ToolType.LASSO, ToolType.HAND_PAN, ToolType.CUSTOMIZE
    ),
    val isRulerVisible: Boolean = false,
    val rulerOffset: PointData = PointData(100f, 400f),
    val isLaserActive: Boolean = false,
    val messageSnackbar: String? = null
)

class WhiteboardViewModel(application: Application) : AndroidViewModel(application) {

    private val database = WhiteboardDatabase.getDatabase(application)
    private val boardDao = database.boardDao()

    private val _uiState = MutableStateFlow(WhiteboardUiState())
    val uiState: StateFlow<WhiteboardUiState> = _uiState.asStateFlow()

    // Undo / Redo history for current page
    private val undoStack = mutableListOf<List<CanvasElement>>()
    private val redoStack = mutableListOf<List<CanvasElement>>()

    val savedBoards: StateFlow<List<BoardEntity>> = boardDao.getAllBoards()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _laserPoints = MutableStateFlow<List<LaserPoint>>(emptyList())
    val laserPoints: StateFlow<List<LaserPoint>> = _laserPoints.asStateFlow()

    init {
        saveStateToUndo()
    }

    fun newBoard(isDark: Boolean = false) {
        val newId = UUID.randomUUID().toString()
        val bg = if (isDark) 0xFF121212 else 0xFFFFFFFF
        val strokeCol = if (isDark) 0xFFFFFFFF else 0xFF000000
        undoStack.clear()
        redoStack.clear()

        _uiState.update {
            it.copy(
                boardId = newId,
                boardTitle = "Board ${System.currentTimeMillis() % 1000}",
                backgroundColor = bg,
                brushColor = strokeCol,
                texture = BoardTexture.PLAIN,
                currentPageIndex = 0,
                pages = listOf(BoardPage(pageId = "page_1")),
                layers = listOf(LayerInfo(id = "layer_1", name = "Layer 1", isVisible = true, colorDot = 0xFF7C4DFF)),
                activeLayerId = "layer_1",
                selectedElementId = null,
                canUndo = false,
                canRedo = false,
                isRulerVisible = false
            )
        }
        saveStateToUndo()
        autoSave()
    }

    fun loadBoard(board: BoardEntity) {
        val pages = CanvasSerializer.deserializePages(board.contentJson)
        undoStack.clear()
        redoStack.clear()

        _uiState.update {
            it.copy(
                boardId = board.id,
                boardTitle = board.title,
                backgroundColor = board.backgroundColor,
                texture = runCatching { BoardTexture.valueOf(board.textureName) }.getOrDefault(BoardTexture.PLAIN),
                currentPageIndex = 0,
                pages = if (pages.isEmpty()) listOf(BoardPage(pageId = "page_1")) else pages,
                selectedElementId = null,
                canUndo = false,
                canRedo = false
            )
        }
        saveStateToUndo()
    }

    fun deleteBoard(boardId: String) {
        viewModelScope.launch {
            boardDao.deleteBoardById(boardId)
        }
    }

    fun setActiveTool(tool: ToolType) {
        if (tool != ToolType.SELECT && tool != ToolType.LASSO) {
            _uiState.update { it.copy(activeTool = tool, selectedElementId = null) }
        } else {
            _uiState.update { it.copy(activeTool = tool) }
        }
    }

    fun setBrushColor(color: Long) {
        _uiState.update { it.copy(brushColor = color) }
        // Also update selected element if any
        if (_uiState.value.selectedElementId != null) {
            changeSelectedElementColor(color)
        }
    }

    fun setBrushSize(size: Float) {
        _uiState.update { it.copy(brushSize = size) }
    }

    fun setBoardColor(color: Long) {
        _uiState.update { it.copy(backgroundColor = color) }
        autoSave()
    }

    fun setBoardTexture(texture: BoardTexture) {
        _uiState.update { it.copy(texture = texture) }
        autoSave()
    }

    fun setSelectedShape(shapeType: ShapeType) {
        _uiState.update { it.copy(selectedShapeType = shapeType, activeTool = ToolType.SHAPES, selectedElementId = null) }
    }

    fun setShapeFilled(filled: Boolean) {
        _uiState.update { it.copy(isShapeFilled = filled) }
    }

    fun toggleRuler() {
        _uiState.update { it.copy(isRulerVisible = !it.isRulerVisible) }
    }

    fun toggleToolbarCollapsed() {
        _uiState.update { it.copy(isToolbarCollapsed = !it.isToolbarCollapsed) }
    }

    fun setToolEnabled(tool: ToolType, enabled: Boolean) {
        _uiState.update { state ->
            val set = state.enabledTools.toMutableSet()
            if (enabled) set.add(tool) else set.remove(tool)
            state.copy(enabledTools = set)
        }
    }

    fun resetToolbar() {
        _uiState.update {
            it.copy(
                enabledTools = setOf(
                    ToolType.SELECT, ToolType.UNDO, ToolType.REDO, ToolType.CLEAR, ToolType.PEN, ToolType.SMART,
                    ToolType.SHAPES, ToolType.ERASER, ToolType.BOARD, ToolType.TEXT, ToolType.FORMULA,
                    ToolType.TABLE, ToolType.LAYERS, ToolType.STICKERS, ToolType.SCIENCE, ToolType.LASER,
                    ToolType.RULER, ToolType.LASSO, ToolType.HAND_PAN, ToolType.CUSTOMIZE
                )
            )
        }
    }

    // Selection & Transform API
    fun selectElement(id: String?) {
        _uiState.update {
            it.copy(
                selectedElementId = id,
                activeTool = if (id != null && it.activeTool != ToolType.SELECT) ToolType.SELECT else it.activeTool
            )
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedElementId = null) }
    }

    fun findElementAt(x: Float, y: Float): CanvasElement? {
        val curPage = getCurrentPage() ?: return null
        val activeLayer = _uiState.value.activeLayerId
        // check elements in reverse (top-most first)
        for (elem in curPage.elements.asReversed()) {
            if (elem.layerId != activeLayer) continue
            val bounds = elem.getBounds()
            if (bounds.contains(Offset(x, y))) {
                return elem
            }
        }
        return null
    }

    fun moveSelectedElement(dx: Float, dy: Float) {
        val selId = _uiState.value.selectedElementId ?: return
        val curPage = getCurrentPage() ?: return
        val updated = curPage.elements.map { elem ->
            if (elem.id == selId) elem.translated(dx, dy) else elem
        }
        updateCurrentPageElements(updated, recordUndo = false)
    }

    fun rotateSelectedElement(deltaDegrees: Float) {
        val selId = _uiState.value.selectedElementId ?: return
        val curPage = getCurrentPage() ?: return
        val updated = curPage.elements.map { elem ->
            if (elem.id == selId) elem.rotated(deltaDegrees) else elem
        }
        updateCurrentPageElements(updated, recordUndo = false)
    }

    fun scaleSelectedElement(scaleFactor: Float) {
        val selId = _uiState.value.selectedElementId ?: return
        val curPage = getCurrentPage() ?: return
        val updated = curPage.elements.map { elem ->
            if (elem.id == selId) elem.scaled(scaleFactor) else elem
        }
        updateCurrentPageElements(updated, recordUndo = false)
    }

    fun resetRotationSelectedElement() {
        val selId = _uiState.value.selectedElementId ?: return
        val curPage = getCurrentPage() ?: return
        saveStateToUndo()
        val updated = curPage.elements.map { elem ->
            if (elem.id == selId) elem.rotated(-elem.rotation) else elem
        }
        updateCurrentPageElements(updated, recordUndo = true)
    }

    fun deleteSelectedElement() {
        val selId = _uiState.value.selectedElementId ?: return
        val curPage = getCurrentPage() ?: return
        saveStateToUndo()
        val remaining = curPage.elements.filter { it.id != selId }
        _uiState.update { it.copy(selectedElementId = null) }
        updateCurrentPageElements(remaining, recordUndo = true)
    }

    fun duplicateSelectedElement() {
        val selId = _uiState.value.selectedElementId ?: return
        val curPage = getCurrentPage() ?: return
        val target = curPage.elements.find { it.id == selId } ?: return
        saveStateToUndo()
        val cloned = when (target) {
            is CanvasElement.Stroke -> target.copy(id = UUID.randomUUID().toString()).translated(40f, 40f)
            is CanvasElement.Shape -> target.copy(id = UUID.randomUUID().toString()).translated(40f, 40f)
            is CanvasElement.Text -> target.copy(id = UUID.randomUUID().toString()).translated(40f, 40f)
            is CanvasElement.Formula -> target.copy(id = UUID.randomUUID().toString()).translated(40f, 40f)
            is CanvasElement.Sticker -> target.copy(id = UUID.randomUUID().toString()).translated(40f, 40f)
            is CanvasElement.Science -> target.copy(id = UUID.randomUUID().toString()).translated(40f, 40f)
            is CanvasElement.Table -> target.copy(id = UUID.randomUUID().toString()).translated(40f, 40f)
        }
        val newElements = curPage.elements + cloned
        _uiState.update { it.copy(selectedElementId = cloned.id) }
        updateCurrentPageElements(newElements, recordUndo = true)
    }

    fun changeSelectedElementColor(color: Long) {
        val selId = _uiState.value.selectedElementId ?: return
        val curPage = getCurrentPage() ?: return
        val updated = curPage.elements.map { elem ->
            if (elem.id == selId) elem.withColor(color) else elem
        }
        updateCurrentPageElements(updated, recordUndo = true)
    }

    fun commitTransform() {
        saveStateToUndo()
        autoSave()
    }

    // Canvas Stroke & Shape addition
    fun addStroke(points: List<PointData>) {
        if (points.isEmpty()) return
        val state = _uiState.value
        saveStateToUndo()

        val element = if (state.activeTool == ToolType.SMART) {
            SmartShapeRecognizer.recognize(
                points = points,
                color = state.brushColor,
                strokeWidth = state.brushSize,
                layerId = state.activeLayerId
            ) ?: CanvasElement.Stroke(
                id = UUID.randomUUID().toString(),
                layerId = state.activeLayerId,
                points = points,
                color = state.brushColor,
                strokeWidth = state.brushSize,
                toolType = ToolType.PEN
            )
        } else {
            CanvasElement.Stroke(
                id = UUID.randomUUID().toString(),
                layerId = state.activeLayerId,
                points = points,
                color = state.brushColor,
                strokeWidth = state.brushSize,
                toolType = state.activeTool
            )
        }

        addElementToCurrentPage(element)
    }

    fun addShape(startX: Float, startY: Float, endX: Float, endY: Float) {
        val state = _uiState.value
        saveStateToUndo()

        val shape = CanvasElement.Shape(
            id = UUID.randomUUID().toString(),
            layerId = state.activeLayerId,
            shapeType = state.selectedShapeType,
            startX = startX,
            startY = startY,
            endX = endX,
            endY = endY,
            color = state.brushColor,
            strokeWidth = state.brushSize,
            isFilled = state.isShapeFilled
        )
        addElementToCurrentPage(shape)
        selectElement(shape.id)
    }

    fun addText(text: String, x: Float = 200f, y: Float = 300f, color: Long = _uiState.value.brushColor, fontSize: Float = 48f) {
        if (text.isBlank()) return
        saveStateToUndo()
        val textElem = CanvasElement.Text(
            id = UUID.randomUUID().toString(),
            layerId = _uiState.value.activeLayerId,
            text = text,
            x = x,
            y = y,
            color = color,
            fontSize = fontSize
        )
        addElementToCurrentPage(textElem)
        selectElement(textElem.id)
    }

    fun addFormula(formula: String, x: Float = 200f, y: Float = 350f, color: Long = _uiState.value.brushColor, fontSize: Float = 40f) {
        if (formula.isBlank()) return
        saveStateToUndo()
        val formulaElem = CanvasElement.Formula(
            id = UUID.randomUUID().toString(),
            layerId = _uiState.value.activeLayerId,
            formula = formula,
            x = x,
            y = y,
            color = color,
            fontSize = fontSize
        )
        addElementToCurrentPage(formulaElem)
        selectElement(formulaElem.id)
    }

    fun addSticker(stickerKey: String, x: Float = 300f, y: Float = 400f, size: Float = 140f) {
        saveStateToUndo()
        val sticker = CanvasElement.Sticker(
            id = UUID.randomUUID().toString(),
            layerId = _uiState.value.activeLayerId,
            stickerKey = stickerKey,
            x = x,
            y = y,
            size = size
        )
        addElementToCurrentPage(sticker)
        selectElement(sticker.id)
    }

    fun addScienceElement(
        category: String,
        symbol: String,
        label: String,
        atomicNumber: Int,
        x: Float = 260f,
        y: Float = 400f,
        size: Float = 160f
    ) {
        saveStateToUndo()
        val science = CanvasElement.Science(
            id = UUID.randomUUID().toString(),
            layerId = _uiState.value.activeLayerId,
            category = category,
            symbol = symbol,
            label = label,
            atomicNumber = atomicNumber,
            x = x,
            y = y,
            size = size
        )
        addElementToCurrentPage(science)
        selectElement(science.id)
    }

    fun addTable(rows: Int, cols: Int, x: Float = 150f, y: Float = 300f) {
        saveStateToUndo()
        val initialCells = List(rows) { List(cols) { "" } }
        val table = CanvasElement.Table(
            id = UUID.randomUUID().toString(),
            layerId = _uiState.value.activeLayerId,
            rows = rows,
            cols = cols,
            x = x,
            y = y,
            width = cols * 100f,
            height = rows * 60f,
            cells = initialCells,
            color = _uiState.value.brushColor
        )
        addElementToCurrentPage(table)
        selectElement(table.id)
    }

    // Eraser by point radius
    fun eraseAt(x: Float, y: Float, radius: Float = 36f) {
        val curPage = getCurrentPage() ?: return
        val activeLayer = _uiState.value.activeLayerId
        var modified = false

        val newElements = curPage.elements.filterNot { elem ->
            if (elem.layerId != activeLayer) return@filterNot false
            val bounds = elem.getBounds()
            val hit = bounds.inflate(radius).contains(Offset(x, y))
            if (hit) modified = true
            hit
        }

        if (modified) {
            saveStateToUndo()
            updateCurrentPageElements(newElements)
        }
    }

    // Laser pointer
    fun addLaserPoint(x: Float, y: Float) {
        val now = System.currentTimeMillis()
        val current = _laserPoints.value.filter { now - it.timestamp < 1200L }
        _laserPoints.value = current + LaserPoint(x, y, now)
    }

    fun clearLaser() {
        _laserPoints.value = emptyList()
    }

    // Undo / Redo
    fun undo() {
        if (undoStack.isEmpty()) return
        val currentElements = getCurrentPage()?.elements ?: emptyList()
        redoStack.add(currentElements)

        val previousElements = undoStack.removeAt(undoStack.lastIndex)
        updateCurrentPageElements(previousElements, recordUndo = false)

        _uiState.update {
            it.copy(
                canUndo = undoStack.isNotEmpty(),
                canRedo = true,
                selectedElementId = null
            )
        }
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val currentElements = getCurrentPage()?.elements ?: emptyList()
        undoStack.add(currentElements)

        val nextElements = redoStack.removeAt(redoStack.lastIndex)
        updateCurrentPageElements(nextElements, recordUndo = false)

        _uiState.update {
            it.copy(
                canUndo = true,
                canRedo = redoStack.isNotEmpty(),
                selectedElementId = null
            )
        }
    }

    fun clearCanvas(onlyActiveLayer: Boolean = false) {
        saveStateToUndo()
        val curPage = getCurrentPage() ?: return
        val remaining = if (onlyActiveLayer) {
            curPage.elements.filter { it.layerId != _uiState.value.activeLayerId }
        } else {
            emptyList()
        }
        _uiState.update { it.copy(selectedElementId = null) }
        updateCurrentPageElements(remaining)
    }

    // Multi-page management
    fun addPage() {
        val state = _uiState.value
        val newPageId = "page_${state.pages.size + 1}"
        val newPages = state.pages + BoardPage(pageId = newPageId)
        val newIndex = newPages.lastIndex
        undoStack.clear()
        redoStack.clear()

        _uiState.update {
            it.copy(
                pages = newPages,
                currentPageIndex = newIndex,
                selectedElementId = null,
                canUndo = false,
                canRedo = false
            )
        }
        autoSave()
    }

    fun selectPage(index: Int) {
        if (index !in _uiState.value.pages.indices) return
        undoStack.clear()
        redoStack.clear()
        _uiState.update {
            it.copy(
                currentPageIndex = index,
                selectedElementId = null,
                canUndo = false,
                canRedo = false
            )
        }
    }

    fun deleteCurrentPage() {
        val state = _uiState.value
        if (state.pages.size <= 1) {
            clearCanvas()
            return
        }
        val newPages = state.pages.toMutableList()
        newPages.removeAt(state.currentPageIndex)
        val newIndex = minOf(state.currentPageIndex, newPages.lastIndex)
        undoStack.clear()
        redoStack.clear()

        _uiState.update {
            it.copy(
                pages = newPages,
                currentPageIndex = newIndex,
                selectedElementId = null,
                canUndo = false,
                canRedo = false
            )
        }
        autoSave()
    }

    // Layer management
    fun addLayer() {
        val state = _uiState.value
        val newLayerId = "layer_${state.layers.size + 1}"
        val colors = listOf(0xFF7C4DFF, 0xFF00E676, 0xFFFF5252, 0xFFFFD600, 0xFF00B0FF)
        val dotColor = colors[state.layers.size % colors.size]
        val newLayer = LayerInfo(id = newLayerId, name = "Layer ${state.layers.size + 1}", colorDot = dotColor)

        _uiState.update {
            it.copy(
                layers = it.layers + newLayer,
                activeLayerId = newLayerId,
                selectedElementId = null
            )
        }
    }

    fun selectActiveLayer(layerId: String) {
        _uiState.update { it.copy(activeLayerId = layerId, selectedElementId = null) }
    }

    fun toggleLayerVisibility(layerId: String) {
        _uiState.update { state ->
            val updated = state.layers.map {
                if (it.id == layerId) it.copy(isVisible = !it.isVisible) else it
            }
            state.copy(layers = updated)
        }
    }

    fun deleteLayer(layerId: String) {
        val state = _uiState.value
        if (state.layers.size <= 1) return
        val newLayers = state.layers.filter { it.id != layerId }
        val newActive = if (state.activeLayerId == layerId) newLayers.first().id else state.activeLayerId

        val updatedPages = state.pages.map { page ->
            page.copy(elements = page.elements.filter { it.layerId != layerId })
        }

        _uiState.update {
            it.copy(
                layers = newLayers,
                activeLayerId = newActive,
                pages = updatedPages,
                selectedElementId = null
            )
        }
        autoSave()
    }

    fun renameLayer(layerId: String, newName: String) {
        _uiState.update { state ->
            val updated = state.layers.map {
                if (it.id == layerId) it.copy(name = newName) else it
            }
            state.copy(layers = updated)
        }
    }

    // Helpers
    fun getCurrentPage(): BoardPage? {
        val state = _uiState.value
        return state.pages.getOrNull(state.currentPageIndex)
    }

    private fun addElementToCurrentPage(element: CanvasElement) {
        val state = _uiState.value
        val curPage = getCurrentPage() ?: return
        val newElements = curPage.elements + element
        updateCurrentPageElements(newElements)
    }

    private fun updateCurrentPageElements(elements: List<CanvasElement>, recordUndo: Boolean = true) {
        _uiState.update { state ->
            val updatedPages = state.pages.mapIndexed { idx, page ->
                if (idx == state.currentPageIndex) page.copy(elements = elements) else page
            }
            state.copy(
                pages = updatedPages,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }
        autoSave()
    }

    private fun saveStateToUndo() {
        val curPage = getCurrentPage() ?: return
        undoStack.add(curPage.elements)
        redoStack.clear()
        if (undoStack.size > 40) {
            undoStack.removeAt(0)
        }
        _uiState.update { it.copy(canUndo = true, canRedo = false) }
    }

    fun autoSave() {
        viewModelScope.launch {
            val state = _uiState.value
            val json = CanvasSerializer.serializePages(state.pages)
            val entity = BoardEntity(
                id = state.boardId,
                title = state.boardTitle,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                backgroundColor = state.backgroundColor,
                textureName = state.texture.name,
                pageCount = state.pages.size,
                contentJson = json
            )
            boardDao.insertBoard(entity)
        }
    }

    fun renameBoard(title: String) {
        _uiState.update { it.copy(boardTitle = title) }
        autoSave()
    }

    fun setMessage(msg: String?) {
        _uiState.update { it.copy(messageSnackbar = msg) }
    }
}

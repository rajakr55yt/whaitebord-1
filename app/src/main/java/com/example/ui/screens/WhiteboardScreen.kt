package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.model.BoardTexture
import com.example.model.ToolType
import com.example.ui.components.BottomToolbar
import com.example.ui.components.TopControlBar
import com.example.ui.components.WhiteboardCanvas
import com.example.ui.components.dialogs.*
import com.example.util.ExportUtils
import com.example.viewmodel.WhiteboardViewModel

@Composable
fun WhiteboardScreen(
    viewModel: WhiteboardViewModel,
    onBackToHome: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val laserPoints by viewModel.laserPoints.collectAsState()

    // Dialog visibilities
    var showBrushesDialog by remember { mutableStateOf(false) }
    var showBoardColorDialog by remember { mutableStateOf(false) }
    var showShapesDialog by remember { mutableStateOf(false) }
    var showTextDialog by remember { mutableStateOf(false) }
    var showFormulaDialog by remember { mutableStateOf(false) }
    var showStickersDialog by remember { mutableStateOf(false) }
    var showScienceDialog by remember { mutableStateOf(false) }
    var showTableDialog by remember { mutableStateOf(false) }
    var showLayersDialog by remember { mutableStateOf(false) }
    var showCustomizeDialog by remember { mutableStateOf(false) }
    var showPagesDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    // Intercept hardware/system back button
    BackHandler {
        viewModel.autoSave()
        onBackToHome()
    }

    val currentPage = uiState.pages.getOrNull(uiState.currentPageIndex)
    val currentElements = currentPage?.elements ?: emptyList()
    val visibleLayerIds = remember(uiState.layers) {
        uiState.layers.filter { it.isVisible }.map { it.id }.toSet()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(uiState.backgroundColor))
    ) {
        // 1. Drawing Canvas
        WhiteboardCanvas(
            backgroundColor = uiState.backgroundColor,
            texture = uiState.texture,
            activeTool = uiState.activeTool,
            brushColor = uiState.brushColor,
            brushSize = uiState.brushSize,
            selectedShapeType = uiState.selectedShapeType,
            isShapeFilled = uiState.isShapeFilled,
            elements = currentElements,
            visibleLayerIds = visibleLayerIds,
            selectedElementId = uiState.selectedElementId,
            laserPoints = laserPoints,
            isRulerVisible = uiState.isRulerVisible,
            onStrokeFinished = { points ->
                viewModel.addStroke(points)
            },
            onShapeFinished = { sx, sy, ex, ey ->
                viewModel.addShape(sx, sy, ex, ey)
            },
            onErase = { x, y ->
                viewModel.eraseAt(x, y)
            },
            onLaserPoint = { x, y ->
                viewModel.addLaserPoint(x, y)
            },
            onLaserClear = {
                viewModel.clearLaser()
            },
            onSelectElement = { id ->
                viewModel.selectElement(id)
            },
            onMoveElement = { dx, dy ->
                viewModel.moveSelectedElement(dx, dy)
            },
            onRotateElement = { delta ->
                viewModel.rotateSelectedElement(delta)
            },
            onScaleElement = { factor ->
                viewModel.scaleSelectedElement(factor)
            },
            onDeleteSelected = {
                viewModel.deleteSelectedElement()
            },
            onDuplicateSelected = {
                viewModel.duplicateSelectedElement()
            },
            onResetRotationSelected = {
                viewModel.resetRotationSelectedElement()
            },
            onCommitTransform = {
                viewModel.commitTransform()
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Top Bar
        TopControlBar(
            currentPage = uiState.currentPageIndex + 1,
            totalPages = uiState.pages.size,
            canUndo = uiState.canUndo,
            canRedo = uiState.canRedo,
            currentTexture = uiState.texture,
            onBackClick = {
                viewModel.autoSave()
                onBackToHome()
            },
            onPagesClick = { showPagesDialog = true },
            onUndoClick = { viewModel.undo() },
            onRedoClick = { viewModel.redo() },
            onGridToggleClick = { showBoardColorDialog = true },
            onSaveClick = {
                viewModel.autoSave()
                Toast.makeText(context, "Board saved successfully!", Toast.LENGTH_SHORT).show()
            },
            onShareClick = {
                val curPage = currentPage ?: return@TopControlBar
                val bitmap = ExportUtils.renderPageToBitmap(
                    page = curPage,
                    width = 1080,
                    height = 1920,
                    backgroundColor = uiState.backgroundColor,
                    texture = uiState.texture,
                    visibleLayerIds = visibleLayerIds
                )
                val file = ExportUtils.saveBitmapToCache(context, bitmap)
                ExportUtils.shareFile(context, file, "image/png", "Share Whiteboard")
            },
            onPdfExportClick = {
                val file = ExportUtils.exportToPdf(
                    context = context,
                    pages = uiState.pages,
                    backgroundColor = uiState.backgroundColor,
                    texture = uiState.texture,
                    visibleLayerIds = visibleLayerIds
                )
                ExportUtils.shareFile(context, file, "application/pdf", "Export Whiteboard PDF")
            },
            onRewardAdClick = {
                val activity = context as? android.app.Activity
                if (activity != null) {
                    com.example.util.RewardedAdManager.showAd(
                        activity = activity,
                        onRewardEarned = { amount, type ->
                            android.widget.Toast.makeText(context, "🎉 Test Reward Earned: +$amount $type!", android.widget.Toast.LENGTH_LONG).show()
                        }
                    )
                }
            },
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // 3. Bottom Toolbar
        BottomToolbar(
            activeTool = uiState.activeTool,
            enabledTools = uiState.enabledTools,
            isCollapsed = uiState.isToolbarCollapsed,
            onToolClick = { tool ->
                when (tool) {
                    ToolType.SELECT -> {
                        viewModel.setActiveTool(ToolType.SELECT)
                        Toast.makeText(context, "Select Mode: Drag to move, corners to resize, top knob to rotate", Toast.LENGTH_SHORT).show()
                    }
                    ToolType.PEN, ToolType.BRUSH, ToolType.MARKER, ToolType.HIGHLIGHT, ToolType.CALLIGRAPHY -> {
                        viewModel.setActiveTool(tool)
                        showBrushesDialog = true
                    }
                    ToolType.SMART -> {
                        viewModel.setActiveTool(ToolType.SMART)
                        Toast.makeText(context, "Smart Draw Active: draw any shape!", Toast.LENGTH_SHORT).show()
                    }
                    ToolType.SHAPES -> {
                        viewModel.setActiveTool(ToolType.SHAPES)
                        showShapesDialog = true
                    }
                    ToolType.ERASER -> {
                        viewModel.setActiveTool(ToolType.ERASER)
                    }
                    ToolType.BOARD -> {
                        showBoardColorDialog = true
                    }
                    ToolType.TEXT -> {
                        viewModel.setActiveTool(ToolType.TEXT)
                        showTextDialog = true
                    }
                    ToolType.FORMULA -> {
                        viewModel.setActiveTool(ToolType.FORMULA)
                        showFormulaDialog = true
                    }
                    ToolType.TABLE -> {
                        showTableDialog = true
                    }
                    ToolType.LAYERS -> {
                        showLayersDialog = true
                    }
                    ToolType.STICKERS -> {
                        showStickersDialog = true
                    }
                    ToolType.SCIENCE -> {
                        showScienceDialog = true
                    }
                    ToolType.LASER -> {
                        viewModel.setActiveTool(ToolType.LASER)
                        Toast.makeText(context, "Laser Pointer Active", Toast.LENGTH_SHORT).show()
                    }
                    ToolType.RULER -> {
                        viewModel.toggleRuler()
                    }
                    ToolType.CUSTOMIZE -> {
                        showCustomizeDialog = true
                    }
                    ToolType.UNDO -> {
                        viewModel.undo()
                    }
                    ToolType.REDO -> {
                        viewModel.redo()
                    }
                    ToolType.CLEAR -> {
                        showClearConfirmDialog = true
                    }
                    ToolType.SAVE -> {
                        viewModel.autoSave()
                        Toast.makeText(context, "Saved!", Toast.LENGTH_SHORT).show()
                    }
                    ToolType.SHARE -> {
                        val curPage = currentPage ?: return@BottomToolbar
                        val bitmap = ExportUtils.renderPageToBitmap(
                            page = curPage,
                            width = 1080,
                            height = 1920,
                            backgroundColor = uiState.backgroundColor,
                            texture = uiState.texture,
                            visibleLayerIds = visibleLayerIds
                        )
                        val file = ExportUtils.saveBitmapToCache(context, bitmap)
                        ExportUtils.shareFile(context, file, "image/png", "Share Whiteboard")
                    }
                    ToolType.PDF_EXPORT -> {
                        val file = ExportUtils.exportToPdf(
                            context = context,
                            pages = uiState.pages,
                            backgroundColor = uiState.backgroundColor,
                            texture = uiState.texture,
                            visibleLayerIds = visibleLayerIds
                        )
                        ExportUtils.shareFile(context, file, "application/pdf", "Export Whiteboard PDF")
                    }
                    else -> {
                        viewModel.setActiveTool(tool)
                    }
                }
            },
            onToggleCollapse = {
                viewModel.toggleToolbarCollapsed()
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // Modal Sheets & Dialogs
    if (showBrushesDialog) {
        BrushesDialog(
            currentTool = uiState.activeTool,
            currentColor = uiState.brushColor,
            currentSize = uiState.brushSize,
            onToolSelected = { tool ->
                viewModel.setActiveTool(tool)
            },
            onColorSelected = { color ->
                viewModel.setBrushColor(color)
            },
            onSizeChanged = { size ->
                viewModel.setBrushSize(size)
            },
            onDismiss = { showBrushesDialog = false }
        )
    }

    if (showBoardColorDialog) {
        BoardColorDialog(
            currentColor = uiState.backgroundColor,
            currentTexture = uiState.texture,
            onColorSelected = { color ->
                viewModel.setBoardColor(color)
            },
            onTextureSelected = { texture ->
                viewModel.setBoardTexture(texture)
            },
            onDismiss = { showBoardColorDialog = false }
        )
    }

    if (showShapesDialog) {
        ShapesDialog(
            selectedShape = uiState.selectedShapeType,
            isShapeFilled = uiState.isShapeFilled,
            onShapeSelected = { shape ->
                viewModel.setSelectedShape(shape)
            },
            onFillToggled = { filled ->
                viewModel.setShapeFilled(filled)
            },
            onDismiss = { showShapesDialog = false }
        )
    }

    if (showTextDialog) {
        TextDialog(
            initialColor = uiState.brushColor,
            onAddText = { text, color, size ->
                viewModel.addText(text = text, color = color, fontSize = size)
            },
            onDismiss = { showTextDialog = false }
        )
    }

    if (showFormulaDialog) {
        FormulaDialog(
            initialColor = uiState.brushColor,
            onAddFormula = { formula, color, size ->
                viewModel.addFormula(formula = formula, color = color, fontSize = size)
            },
            onDismiss = { showFormulaDialog = false }
        )
    }

    if (showStickersDialog) {
        StickersDialog(
            onStickerSelected = { stickerKey ->
                viewModel.addSticker(stickerKey)
            },
            onDismiss = { showStickersDialog = false }
        )
    }

    if (showScienceDialog) {
        ScienceDialog(
            onElementSelected = { category, symbol, label, atomicNumber ->
                viewModel.addScienceElement(category, symbol, label, atomicNumber)
            },
            onDismiss = { showScienceDialog = false }
        )
    }

    if (showTableDialog) {
        TableDialog(
            onInsertTable = { rows, cols ->
                viewModel.addTable(rows, cols)
            },
            onDismiss = { showTableDialog = false }
        )
    }

    if (showLayersDialog) {
        LayersDialog(
            layers = uiState.layers,
            activeLayerId = uiState.activeLayerId,
            onAddLayer = { viewModel.addLayer() },
            onSelectLayer = { viewModel.selectActiveLayer(it) },
            onToggleVisibility = { viewModel.toggleLayerVisibility(it) },
            onDeleteLayer = { viewModel.deleteLayer(it) },
            onRenameLayer = { id, name -> viewModel.renameLayer(id, name) },
            onDismiss = { showLayersDialog = false }
        )
    }

    if (showCustomizeDialog) {
        CustomizeToolbarDialog(
            enabledTools = uiState.enabledTools,
            onToggleTool = { tool, enabled ->
                viewModel.setToolEnabled(tool, enabled)
            },
            onReset = { viewModel.resetToolbar() },
            onDismiss = { showCustomizeDialog = false }
        )
    }

    if (showPagesDialog) {
        PagesDialog(
            pages = uiState.pages,
            currentPageIndex = uiState.currentPageIndex,
            onSelectPage = { viewModel.selectPage(it) },
            onAddPage = { viewModel.addPage() },
            onDeletePage = { viewModel.deleteCurrentPage() },
            onDismiss = { showPagesDialog = false }
        )
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear Canvas") },
            text = { Text("Are you sure you want to clear the canvas on this page?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearCanvas()
                        showClearConfirmDialog = false
                    }
                ) {
                    Text("Clear All", color = Color(0xFFEF4444))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

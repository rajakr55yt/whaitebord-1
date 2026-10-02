package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ToolType

@Composable
fun BottomToolbar(
    activeTool: ToolType,
    enabledTools: Set<ToolType>,
    isCollapsed: Boolean,
    onToolClick: (ToolType) -> Unit,
    onToggleCollapse: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF1F3F5),
        shadowElevation = 6.dp
    ) {
        if (isCollapsed) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quick Active Tool indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = getToolIcon(activeTool),
                        contentDescription = activeTool.displayName,
                        tint = Color(0xFF1E293B),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = activeTool.displayName,
                        color = Color(0xFF1E293B),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                IconButton(
                    onClick = onToggleCollapse,
                    modifier = Modifier.testTag("expand_toolbar_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Expand Toolbar",
                        tint = Color(0xFF1E293B)
                    )
                }
            }
        } else {
            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // All enabled tools in consistent order
                val orderedTools = listOf(
                    ToolType.SELECT,
                    ToolType.UNDO,
                    ToolType.REDO,
                    ToolType.CLEAR,
                    ToolType.PEN,
                    ToolType.SMART,
                    ToolType.SHAPES,
                    ToolType.ERASER,
                    ToolType.BOARD,
                    ToolType.TEXT,
                    ToolType.FORMULA,
                    ToolType.TABLE,
                    ToolType.LAYERS,
                    ToolType.STICKERS,
                    ToolType.SCIENCE,
                    ToolType.LASER,
                    ToolType.RULER,
                    ToolType.LASSO,
                    ToolType.HAND_PAN,
                    ToolType.CUSTOMIZE
                )

                for (tool in orderedTools) {
                    if (tool in enabledTools) {
                        ToolbarItem(
                            tool = tool,
                            isSelected = (activeTool == tool),
                            onClick = { onToolClick(tool) }
                        )
                    }
                }

                // Collapse Button
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onToggleCollapse() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "Collapse",
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Collapse",
                        fontSize = 10.sp,
                        color = Color(0xFF475569),
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolbarItem(
    tool: ToolType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) Color(0xFFDCE2E6) else Color.Transparent
    val tintColor = if (isSelected) Color(0xFF0F172A) else Color(0xFF475569)

    Column(
        modifier = Modifier
            .widthIn(min = 52.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .testTag("tool_${tool.name.lowercase()}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = getToolIcon(tool),
            contentDescription = tool.displayName,
            tint = tintColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = tool.displayName,
            fontSize = 10.sp,
            color = tintColor,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

fun getToolIcon(tool: ToolType): ImageVector {
    return when (tool) {
        ToolType.SELECT -> Icons.Default.NearMe
        ToolType.UNDO -> Icons.AutoMirrored.Filled.Undo
        ToolType.REDO -> Icons.AutoMirrored.Filled.Redo
        ToolType.CLEAR -> Icons.Default.DeleteOutline
        ToolType.PEN, ToolType.BRUSH, ToolType.MARKER, ToolType.HIGHLIGHT, ToolType.CALLIGRAPHY -> Icons.Default.Edit
        ToolType.SMART -> Icons.Default.AutoFixHigh
        ToolType.SHAPES -> Icons.Default.Category
        ToolType.ERASER -> Icons.Default.CleaningServices
        ToolType.BOARD -> Icons.Default.FormatPaint
        ToolType.TEXT -> Icons.Default.TextFields
        ToolType.FORMULA -> Icons.Default.Functions
        ToolType.TABLE -> Icons.Default.TableChart
        ToolType.LAYERS -> Icons.Default.Layers
        ToolType.STICKERS -> Icons.Default.SentimentSatisfiedAlt
        ToolType.SCIENCE -> Icons.Default.Science
        ToolType.LASER -> Icons.Default.Highlight
        ToolType.RULER -> Icons.Default.Straighten
        ToolType.LASSO -> Icons.Default.Polyline
        ToolType.HAND_PAN -> Icons.Default.PanTool
        ToolType.CUSTOMIZE -> Icons.Default.Tune
        ToolType.SAVE -> Icons.Default.Save
        ToolType.SHARE -> Icons.Default.Share
        ToolType.PDF_EXPORT -> Icons.Default.PictureAsPdf
    }
}

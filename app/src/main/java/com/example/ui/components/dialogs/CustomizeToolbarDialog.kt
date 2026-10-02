package com.example.ui.components.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.ToolType
import com.example.ui.components.getToolIcon

@Composable
fun CustomizeToolbarDialog(
    enabledTools: Set<ToolType>,
    onToggleTool: (ToolType, Boolean) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    val allCustomizableTools = listOf(
        ToolType.SELECT,
        ToolType.SAVE,
        ToolType.SHARE,
        ToolType.PDF_EXPORT,
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
        ToolType.HAND_PAN
    )

    val essentialTools = setOf(
        ToolType.SELECT, ToolType.SAVE, ToolType.SHARE, ToolType.PDF_EXPORT,
        ToolType.UNDO, ToolType.REDO, ToolType.CLEAR, ToolType.PEN
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Customize Toolbar",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            onClick = onReset,
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF332014)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reset",
                                    tint = Color(0xFFFF8A65),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Reset",
                                    color = Color(0xFFFF8A65),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Drag to reorder tools\nEnable/disable tools in toolbar",
                    fontSize = 12.sp,
                    color = Color(0xFF9E9E9E),
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(allCustomizableTools) { tool ->
                        val isEnabled = tool in enabledTools
                        val isEssential = tool in essentialTools

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF2A2A2A),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DragIndicator,
                                        contentDescription = "Drag",
                                        tint = Color(0xFF616161),
                                        modifier = Modifier.size(20.dp)
                                    )

                                    Icon(
                                        imageVector = getToolIcon(tool),
                                        contentDescription = tool.displayName,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )

                                    Text(
                                        text = tool.displayName,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (isEssential) {
                                        Text(
                                            text = "Essential",
                                            fontSize = 11.sp,
                                            color = Color(0xFF4CAF50),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    Switch(
                                        checked = isEnabled,
                                        onCheckedChange = { onToggleTool(tool, it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = Color(0xFF4CAF50),
                                            uncheckedThumbColor = Color(0xFF9E9E9E),
                                            uncheckedTrackColor = Color(0xFF424242)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

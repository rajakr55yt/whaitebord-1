package com.example.ui.components.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.ToolType

@Composable
fun BrushesDialog(
    currentTool: ToolType,
    currentColor: Long,
    currentSize: Float,
    onToolSelected: (ToolType) -> Unit,
    onColorSelected: (Long) -> Unit,
    onSizeChanged: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val brushPalette = listOf(
        0xFF000000, // Black
        0xFFE53935, // Red
        0xFFFB8C00, // Orange
        0xFFFDD835, // Yellow
        0xFF43A047, // Green
        0xFF1E88E5, // Blue
        0xFFE040FB, // Magenta
        0xFFFFFFFF  // White
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Brush Types Top Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    BrushTypeButton("Pen", Icons.Default.Edit, currentTool == ToolType.PEN) {
                        onToolSelected(ToolType.PEN)
                    }
                    BrushTypeButton("Brush", Icons.Default.Brush, currentTool == ToolType.BRUSH) {
                        onToolSelected(ToolType.BRUSH)
                    }
                    BrushTypeButton("Marker", Icons.Default.BorderColor, currentTool == ToolType.MARKER) {
                        onToolSelected(ToolType.MARKER)
                    }
                    BrushTypeButton("Highlight", Icons.Default.Highlight, currentTool == ToolType.HIGHLIGHT) {
                        onToolSelected(ToolType.HIGHLIGHT)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Calligraphy Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    BrushTypeButton("Calligraphy", Icons.Default.FormatItalic, currentTool == ToolType.CALLIGRAPHY) {
                        onToolSelected(ToolType.CALLIGRAPHY)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color(0xFFE2E8F0))

                // Color header
                Text(
                    text = "Color",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Color Palette row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (colorLong in brushPalette) {
                        val isSelected = currentColor == colorLong
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(colorLong))
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) Color(0xFF2563EB) else Color(0x33000000),
                                    shape = CircleShape
                                )
                                .clickable { onColorSelected(colorLong) }
                        )
                    }

                    // Rainbow / Custom color picker
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.sweepGradient(
                                    listOf(
                                        Color.Red, Color.Yellow, Color.Green,
                                        Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                                    )
                                )
                            )
                            .clickable {
                                // Rotate to another bright color
                                val altColors = listOf(0xFF00E5FF, 0xFFFF0055, 0xFF76FF03, 0xFFFFD600)
                                onColorSelected(altColors.random())
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Custom Color",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Size Section
                Text(
                    text = "Size",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${currentSize.toInt()}",
                    fontSize = 15.sp,
                    color = Color(0xFF2563EB),
                    fontWeight = FontWeight.Bold
                )

                Slider(
                    value = currentSize,
                    onValueChange = onSizeChanged,
                    valueRange = 2f..72f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF2563EB),
                        activeTrackColor = Color(0xFF2563EB),
                        inactiveTrackColor = Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun BrushTypeButton(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderMod = if (isSelected) {
        Modifier.border(1.5.dp, Color(0xFF2563EB), RoundedCornerShape(12.dp))
    } else {
        Modifier
    }

    Column(
        modifier = Modifier
            .width(66.dp)
            .height(64.dp)
            .then(borderMod)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Color(0xFFEFF6FF) else Color.Transparent)
            .clickable { onClick() }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) Color(0xFF2563EB) else Color(0xFF475569),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = if (isSelected) Color(0xFF2563EB) else Color(0xFF475569),
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

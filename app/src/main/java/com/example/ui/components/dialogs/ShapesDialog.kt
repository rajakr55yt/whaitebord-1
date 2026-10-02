package com.example.ui.components.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatPaint
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
import com.example.model.ShapeCategory
import com.example.model.ShapeType

@Composable
fun ShapesDialog(
    selectedShape: ShapeType,
    isShapeFilled: Boolean,
    onShapeSelected: (ShapeType) -> Unit,
    onFillToggled: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Shapes",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF64748B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Fill Shape Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FormatPaint,
                            contentDescription = "Fill",
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Fill Shape",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF334155)
                        )
                    }
                    Switch(
                        checked = isShapeFilled,
                        onCheckedChange = onFillToggled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF2563EB)
                        )
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF1F5F9))

                // 2D Shapes Header
                Text(
                    text = "2D Shapes",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF64748B),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(12.dp))

                val twoDShapes = ShapeType.entries.filter { it.category == ShapeCategory.TWO_D }
                ShapeGrid(
                    shapes = twoDShapes,
                    selectedShape = selectedShape,
                    onSelect = {
                        onShapeSelected(it)
                        onDismiss()
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = Color(0xFFF1F5F9))

                // 3D Shapes Header
                Text(
                    text = "3D Shapes",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF64748B),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(12.dp))

                val threeDShapes = ShapeType.entries.filter { it.category == ShapeCategory.THREE_D }
                ShapeGrid(
                    shapes = threeDShapes,
                    selectedShape = selectedShape,
                    onSelect = {
                        onShapeSelected(it)
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun ShapeGrid(
    shapes: List<ShapeType>,
    selectedShape: ShapeType,
    onSelect: (ShapeType) -> Unit
) {
    val chunked = shapes.chunked(4)
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        for (row in chunked) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (shape in row) {
                    val isSelected = shape == selectedShape
                    ShapeGridItem(
                        shape = shape,
                        isSelected = isSelected,
                        onClick = { onSelect(shape) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ShapeGridItem(
    shape: ShapeType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderMod = if (isSelected) {
        Modifier.border(2.dp, Color(0xFF2563EB), RoundedCornerShape(12.dp))
    } else {
        Modifier.border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
    }

    Column(
        modifier = Modifier
            .width(66.dp)
            .height(64.dp)
            .then(borderMod)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC))
            .clickable { onClick() }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon or mini symbol
        Text(
            text = getShapeSymbol(shape),
            fontSize = 18.sp,
            color = if (isSelected) Color(0xFF2563EB) else Color(0xFF334155),
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = shape.displayName,
            fontSize = 10.sp,
            color = if (isSelected) Color(0xFF2563EB) else Color(0xFF64748B),
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

private fun getShapeSymbol(shape: ShapeType): String {
    return when (shape) {
        ShapeType.LINE -> "—"
        ShapeType.ARROW -> "➔"
        ShapeType.RECT -> "▭"
        ShapeType.SQUARE -> "□"
        ShapeType.CIRCLE -> "○"
        ShapeType.OVAL -> "⬭"
        ShapeType.TRIANGLE -> "△"
        ShapeType.RIGHT_TRIANGLE -> "◺"
        ShapeType.STAR -> "★"
        ShapeType.PENTAGON -> "⬠"
        ShapeType.HEXAGON -> "⬡"
        ShapeType.DIAMOND -> "◇"
        ShapeType.PARALLEL -> "▱"
        ShapeType.TRAPEZOID -> "⏢"
        ShapeType.HEART -> "♥"
        ShapeType.CROSS -> "✚"
        ShapeType.SEMICIRCLE -> "◠"
        ShapeType.SECTOR -> "⌔"
        ShapeType.RIGHT_ANGLE -> "∟"
        ShapeType.CUBE -> "🧊"
        ShapeType.CYLINDER -> "🛢"
        ShapeType.CONE -> "▲"
        ShapeType.PYRAMID -> "△"
        ShapeType.SPHERE -> "●"
    }
}

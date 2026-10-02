package com.example.ui.components.dialogs

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.BoardTexture

@Composable
fun BoardColorDialog(
    currentColor: Long,
    currentTexture: BoardTexture,
    onColorSelected: (Long) -> Unit,
    onTextureSelected: (BoardTexture) -> Unit,
    onDismiss: () -> Unit
) {
    val darkColors = listOf(
        0xFFFFFFFF, 0xFF000000, 0xFF2A2E35, 0xFF374151, 0xFF1E293B,
        0xFF14532D, 0xFF3E2723, 0xFF042F2E, 0xFF0F172A, 0xFF121212
    )

    val pastelColors = listOf(
        0xFFFFFBEB, 0xFFFFF1F2, 0xFFF0FDF4, 0xFFF0F9FF, 0xFFFAF5FF, 0xFFFEF3C7,
        0xFFFFE4E6, 0xFFE0E7FF, 0xFFE0F2FE, 0xFFDCFCE7, 0xFFF3E8FF, 0xFFF1F5F9
    )

    val mutedColors = listOf(
        0xFF9E8E8E, 0xFF8E9E8E, 0xFF8392A0, 0xFFA09383, 0xFF968AA5, 0xFFA58B79,
        0xFF7C8B7C, 0xFF7D8B96, 0xFF9E8A78, 0xFF8C7E8C, 0xFF6E7E85, 0xFF877C6E
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
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
                    Spacer(modifier = Modifier.size(24.dp))
                    Text(
                        text = "Board Color",
                        fontSize = 17.sp,
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

                Spacer(modifier = Modifier.height(14.dp))

                // Dark Section
                Text(
                    text = "Dark",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(8.dp))
                ColorGridRow(colors = darkColors, selectedColor = currentColor, onColorSelected = onColorSelected)

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF1F5F9))

                // Pastel Section
                Text(
                    text = "Pastel",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(8.dp))
                ColorGridRow(colors = pastelColors, selectedColor = currentColor, onColorSelected = onColorSelected)

                Spacer(modifier = Modifier.height(12.dp))

                // Muted Section
                Text(
                    text = "Muted",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(8.dp))
                ColorGridRow(colors = mutedColors, selectedColor = currentColor, onColorSelected = onColorSelected)

                Spacer(modifier = Modifier.height(12.dp))

                // Rainbow '+' Button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.sweepGradient(
                                listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
                            )
                        )
                        .align(Alignment.CenterHorizontally)
                        .clickable {
                            val brightColors = listOf(0xFF0284C7, 0xFF059669, 0xFFD97706, 0xFFDC2626, 0xFF7C3AED)
                            onColorSelected(brightColors.random())
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(20.dp))
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = Color(0xFFE2E8F0))

                // Texture Section
                Text(
                    text = "Texture",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(12.dp))

                val textures = listOf(
                    BoardTexture.PLAIN,
                    BoardTexture.GRID,
                    BoardTexture.LINES,
                    BoardTexture.DOTS,
                    BoardTexture.CHALK,
                    BoardTexture.ISOMETRIC,
                    BoardTexture.GRAPH,
                    BoardTexture.MUSIC,
                    BoardTexture.DIAMOND,
                    BoardTexture.NOTEBOOK
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    for (tex in textures) {
                        TextureItem(
                            texture = tex,
                            isSelected = (tex == currentTexture),
                            onClick = { onTextureSelected(tex) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorGridRow(
    colors: List<Long>,
    selectedColor: Long,
    onColorSelected: (Long) -> Unit
) {
    val chunked = colors.chunked(5)
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        for (row in chunked) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (col in row) {
                    val isSelected = selectedColor == col
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(col))
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) Color(0xFF2563EB) else Color(0x3394A3B8),
                                shape = CircleShape
                            )
                            .clickable { onColorSelected(col) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TextureItem(
    texture: BoardTexture,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(62.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFFAFAFA))
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) Color(0xFF2563EB) else Color(0xFFCBD5E1),
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            // Draw miniature texture pattern representation
            MiniTexturePreview(texture)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = texture.displayName,
            fontSize = 11.sp,
            color = if (isSelected) Color(0xFF2563EB) else Color(0xFF475569),
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

@Composable
private fun MiniTexturePreview(texture: BoardTexture) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val c = Color(0xFF94A3B8)
        when (texture) {
            BoardTexture.PLAIN -> {}
            BoardTexture.GRID, BoardTexture.GRAPH -> {
                val step = 10f
                var x = 0f
                while (x <= w) {
                    drawLine(c, androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Offset(x, h), strokeWidth = 1f)
                    x += step
                }
                var y = 0f
                while (y <= h) {
                    drawLine(c, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(w, y), strokeWidth = 1f)
                    y += step
                }
            }
            BoardTexture.LINES, BoardTexture.NOTEBOOK -> {
                var y = 8f
                while (y <= h) {
                    drawLine(c, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(w, y), strokeWidth = 1f)
                    y += 10f
                }
                if (texture == BoardTexture.NOTEBOOK) {
                    drawLine(Color(0xFFEF4444), androidx.compose.ui.geometry.Offset(10f, 0f), androidx.compose.ui.geometry.Offset(10f, h), strokeWidth = 1.2f)
                }
            }
            BoardTexture.DOTS -> {
                var x = 6f
                while (x <= w) {
                    var y = 6f
                    while (y <= h) {
                        drawCircle(c, radius = 1.2f, center = androidx.compose.ui.geometry.Offset(x, y))
                        y += 10f
                    }
                    x += 10f
                }
            }
            BoardTexture.CHALK -> {
                drawCircle(c.copy(alpha = 0.3f), radius = 6f, center = androidx.compose.ui.geometry.Offset(w * 0.3f, h * 0.4f))
                drawCircle(c.copy(alpha = 0.3f), radius = 9f, center = androidx.compose.ui.geometry.Offset(w * 0.7f, h * 0.6f))
            }
            BoardTexture.ISOMETRIC, BoardTexture.DIAMOND -> {
                var x = -h
                while (x <= w + h) {
                    drawLine(c, androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Offset(x + h, h), strokeWidth = 0.8f)
                    drawLine(c, androidx.compose.ui.geometry.Offset(x + h, 0f), androidx.compose.ui.geometry.Offset(x, h), strokeWidth = 0.8f)
                    x += 10f
                }
            }
            BoardTexture.MUSIC -> {
                for (i in 0 until 5) {
                    val y = 14f + i * 4f
                    drawLine(c, androidx.compose.ui.geometry.Offset(4f, y), androidx.compose.ui.geometry.Offset(w - 4f, y), strokeWidth = 0.8f)
                }
            }
        }
    }
}

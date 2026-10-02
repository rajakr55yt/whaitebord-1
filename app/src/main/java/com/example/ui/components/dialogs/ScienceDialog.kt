package com.example.ui.components.dialogs

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlin.math.cos
import kotlin.math.sin

data class AtomData(
    val symbol: String,
    val name: String,
    val atomicNumber: Int
)

data class LabToolData(
    val id: String,
    val name: String,
    val iconEmoji: String
)

@Composable
fun ScienceDialog(
    onElementSelected: (category: String, symbol: String, label: String, atomicNumber: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Atomic Structure", "Flasks & Beakers", "Measuring")

    val atoms = listOf(
        AtomData("H", "Hydrogen", 1),
        AtomData("He", "Helium", 2),
        AtomData("Li", "Lithium", 3),
        AtomData("Be", "Beryllium", 4),
        AtomData("B", "Boron", 5),
        AtomData("C", "Carbon", 6),
        AtomData("N", "Nitrogen", 7),
        AtomData("O", "Oxygen", 8),
        AtomData("F", "Fluorine", 9),
        AtomData("Ne", "Neon", 10),
        AtomData("Na", "Sodium", 11),
        AtomData("Mg", "Magnesium", 12)
    )

    val labGlassware = listOf(
        LabToolData("erlenmeyer", "Erlenmeyer Flask", "🧪"),
        LabToolData("beaker", "Beaker", "🥛"),
        LabToolData("test_tube", "Test Tube", "🧪"),
        LabToolData("burner", "Bunsen Burner", "🔥"),
        LabToolData("cylinder", "Graduated Cylinder", "📏"),
        LabToolData("funnel", "Funnel", "⏳")
    )

    val measuringTools = listOf(
        LabToolData("ruler", "Ruler", "📏"),
        LabToolData("scale", "Digital Scale", "⚖️"),
        LabToolData("thermometer", "Thermometer", "🌡️"),
        LabToolData("compass", "Compass", "🧭"),
        LabToolData("magnet", "Magnet", "🧲"),
        LabToolData("microscope", "Microscope", "🔬")
    )

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
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.size(24.dp))
                    Text(
                        text = "Science",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    tabs.forEachIndexed { index, title ->
                        val isSelected = selectedTab == index
                        Surface(
                            onClick = { selectedTab = index },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) Color(0xFFEFF6FF) else Color.Transparent,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3B82F6)) else null,
                            modifier = Modifier.height(34.dp)
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = title,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) Color(0xFF2563EB) else Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "1/8 ▶", fontSize = 11.sp, color = Color(0xFF94A3B8))
                Spacer(modifier = Modifier.height(10.dp))

                if (selectedTab == 0) {
                    // Atomic Structure Grid
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val chunked = atoms.chunked(3)
                        for (row in chunked) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                for (atom in row) {
                                    AtomCard(atom = atom, onClick = {
                                        onElementSelected("atomic", atom.symbol, atom.name, atom.atomicNumber)
                                        onDismiss()
                                    })
                                }
                            }
                        }
                    }
                } else {
                    val list = if (selectedTab == 1) labGlassware else measuringTools
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val chunked = list.chunked(3)
                        for (row in chunked) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                for (tool in row) {
                                    LabToolCard(tool = tool, onClick = {
                                        onElementSelected("lab", tool.id, tool.name, 0)
                                        onDismiss()
                                    })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AtomCard(
    atom: AtomData,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .size(88.dp)
            .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                // Miniature Bohr Orbit
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val r1 = 12f
                    val r2 = 20f
                    val r3 = 26f

                    // Nucleus
                    drawCircle(Color(0xFFEF4444), radius = 8f, center = Offset(cx, cy))

                    // Shell 1
                    drawCircle(Color(0xFF3B82F6), radius = r1, center = Offset(cx, cy), style = Stroke(width = 1f))
                    if (atom.atomicNumber >= 1) {
                        drawCircle(Color(0xFF1D4ED8), radius = 2f, center = Offset(cx, cy - r1))
                    }
                    if (atom.atomicNumber >= 2) {
                        drawCircle(Color(0xFF1D4ED8), radius = 2f, center = Offset(cx, cy + r1))
                    }

                    // Shell 2
                    if (atom.atomicNumber >= 3) {
                        drawCircle(Color(0xFF3B82F6), radius = r2, center = Offset(cx, cy), style = Stroke(width = 1f))
                        val eCount = minOf(8, atom.atomicNumber - 2)
                        for (e in 0 until eCount) {
                            val a = e * (2 * Math.PI / eCount)
                            drawCircle(Color(0xFF1D4ED8), radius = 2f, center = Offset((cx + r2 * cos(a)).toFloat(), (cy + r2 * sin(a)).toFloat()))
                        }
                    }

                    // Shell 3
                    if (atom.atomicNumber >= 11) {
                        drawCircle(Color(0xFF3B82F6), radius = r3, center = Offset(cx, cy), style = Stroke(width = 1f))
                    }
                }

                // Symbol in center
                Text(
                    text = atom.symbol,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "${atom.atomicNumber}",
                fontSize = 11.sp,
                color = Color(0xFF64748B),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun LabToolCard(
    tool: LabToolData,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .size(88.dp)
            .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = tool.iconEmoji, fontSize = 28.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = tool.name,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF334155),
                maxLines = 1
            )
        }
    }
}

package com.example.ui.components.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.LayerInfo

@Composable
fun LayersDialog(
    layers: List<LayerInfo>,
    activeLayerId: String,
    onAddLayer: () -> Unit,
    onSelectLayer: (String) -> Unit,
    onToggleVisibility: (String) -> Unit,
    onDeleteLayer: (String) -> Unit,
    onRenameLayer: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var editingLayerId by remember { mutableStateOf<String?>(null) }
    var renameText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .padding(6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header: Layers, +, X
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Layers",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onAddLayer) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Layer", tint = Color(0xFF2563EB))
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFE2E8F0))

                // Layer list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(layers) { layer ->
                        val isActive = layer.id == activeLayerId
                        val rowBg = if (isActive) Color(0xFFF3E8FF) else Color(0xFFF8FAFC)

                        Surface(
                            onClick = { onSelectLayer(layer.id) },
                            shape = RoundedCornerShape(12.dp),
                            color = rowBg,
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
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Eye icon
                                    IconButton(
                                        onClick = { onToggleVisibility(layer.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (layer.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = "Visibility",
                                            tint = if (layer.isVisible) Color(0xFF7C3AED) else Color(0xFF94A3B8),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    // Color Dot
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color(layer.colorDot))
                                    )

                                    if (editingLayerId == layer.id) {
                                        OutlinedTextField(
                                            value = renameText,
                                            onValueChange = { renameText = it },
                                            modifier = Modifier.width(140.dp),
                                            singleLine = true
                                        )
                                    } else {
                                        Text(
                                            text = layer.name,
                                            fontSize = 14.sp,
                                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isActive) Color(0xFF7C3AED) else Color(0xFF334155)
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (editingLayerId == layer.id) {
                                        IconButton(onClick = {
                                            if (renameText.isNotBlank()) {
                                                onRenameLayer(layer.id, renameText)
                                            }
                                            editingLayerId = null
                                        }) {
                                            Icon(imageVector = Icons.Default.Check, contentDescription = "Done", tint = Color(0xFF16A34A))
                                        }
                                    } else {
                                        IconButton(onClick = {
                                            renameText = layer.name
                                            editingLayerId = layer.id
                                        }) {
                                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Rename", tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                                        }
                                    }

                                    if (layers.size > 1) {
                                        IconButton(onClick = { onDeleteLayer(layer.id) }) {
                                            Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

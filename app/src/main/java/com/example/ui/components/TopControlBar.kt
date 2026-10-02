package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BoardTexture

@Composable
fun TopControlBar(
    currentPage: Int,
    totalPages: Int,
    canUndo: Boolean,
    canRedo: Boolean,
    currentTexture: BoardTexture,
    onBackClick: () -> Unit,
    onPagesClick: () -> Unit,
    onUndoClick: () -> Unit,
    onRedoClick: () -> Unit,
    onGridToggleClick: () -> Unit,
    onSaveClick: () -> Unit,
    onShareClick: () -> Unit,
    onPdfExportClick: () -> Unit,
    onRewardAdClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left section: Back button & Page indicator
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x33808080))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            // Page indicator pill (e.g. 1/1)
            Surface(
                onClick = onPagesClick,
                shape = RoundedCornerShape(12.dp),
                color = Color(0x55303030),
                modifier = Modifier.height(36.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "$currentPage/$totalPages",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                    )
                }
            }
        }

        // Center / Right actions: Undo, Redo, Grid, Save, Share, PDF
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = onUndoClick,
                enabled = canUndo,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (canUndo) Color(0x33808080) else Color(0x15808080))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Undo",
                    tint = if (canUndo) Color.White else Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onRedoClick,
                enabled = canRedo,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (canRedo) Color(0x33808080) else Color(0x15808080))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Redo,
                    contentDescription = "Redo",
                    tint = if (canRedo) Color.White else Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Grid button
            IconButton(
                onClick = onGridToggleClick,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (currentTexture != BoardTexture.PLAIN) Color(0xFF2563EB) else Color(0x33808080))
            ) {
                Icon(
                    imageVector = Icons.Default.GridOn,
                    contentDescription = "Grid",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Save button
            IconButton(
                onClick = onSaveClick,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0x33808080))
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = "Save",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Share / Export button
            IconButton(
                onClick = onShareClick,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0x33808080))
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            // PDF Export button
            IconButton(
                onClick = onPdfExportClick,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0x33808080))
            ) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = "PDF",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Rewarded Ad test button
            IconButton(
                onClick = onRewardAdClick,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2979FF))
            ) {
                Icon(
                    imageVector = Icons.Default.CardGiftcard,
                    contentDescription = "Rewarded Test Ad",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

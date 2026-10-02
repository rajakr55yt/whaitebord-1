package com.example.ui.components.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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

data class StickerData(
    val key: String,
    val name: String,
    val emojiOrText: String
)

@Composable
fun StickersDialog(
    onStickerSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Teaching Rewards", "Feedback Stamps", "Math Symbols")

    val teachingStickers = listOf(
        StickerData("star", "Star", "⭐"),
        StickerData("trophy", "Trophy", "🏆"),
        StickerData("medal", "Medal", "🥇"),
        StickerData("ribbon", "Ribbon", "🎖️"),
        StickerData("crown", "Crown", "👑"),
        StickerData("a_plus", "A+", "🅰️+"),
        StickerData("hundred", "100%", "💯"),
        StickerData("thumbs_up", "Thumbs Up", "👍"),
        StickerData("certificate", "Certificate", "📜"),
        StickerData("smiley_star", "Happy Star", "🌟"),
        StickerData("excellent", "Excellent!", "✨"),
        StickerData("good_job", "Good Job!", "👏")
    )

    val feedbackStickers = listOf(
        StickerData("excellent", "Excellent!", "EXCELLENT"),
        StickerData("good_job", "Good Job!", "GOOD JOB"),
        StickerData("great_work", "Great Work", "GREAT WORK"),
        StickerData("keep_it_up", "Keep it up!", "KEEP IT UP"),
        StickerData("well_done", "Well Done", "WELL DONE"),
        StickerData("try_again", "Try Again", "TRY AGAIN")
    )

    val mathStickers = listOf(
        StickerData("pi", "Pi", "π"),
        StickerData("infinity", "Infinity", "∞"),
        StickerData("sigma", "Sigma", "Σ"),
        StickerData("integral", "Integral", "∫"),
        StickerData("sqrt", "Root", "√"),
        StickerData("angle", "Angle", "∠"),
        StickerData("delta", "Delta", "Δ"),
        StickerData("approx", "Approx", "≈")
    )

    val currentList = when (selectedTab) {
        0 -> teachingStickers
        1 -> feedbackStickers
        else -> mathStickers
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
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
                        text = "Stickers",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

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
                            border = if (isSelected) BorderStroke(1.dp, Color(0xFF3B82F6)) else null,
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

                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "1/8 ▶", fontSize = 11.sp, color = Color(0xFF94A3B8))
                Spacer(modifier = Modifier.height(10.dp))

                // Stickers Grid
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val chunked = currentList.chunked(3)
                    for (row in chunked) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            for (sticker in row) {
                                StickerCard(sticker = sticker, onClick = {
                                    onStickerSelected(sticker.key)
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

@Composable
private fun StickerCard(
    sticker: StickerData,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .size(86.dp)
            .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when (sticker.key) {
                "excellent" -> {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.5.dp, Color(0xFF16A34A)),
                        color = Color(0xFFF0FDF4),
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Text(
                            text = "Excellent!",
                            color = Color(0xFF16A34A),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }
                }
                "good_job" -> {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.5.dp, Color(0xFF2563EB)),
                        color = Color(0xFFEFF6FF),
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Text(
                            text = "Good Job!",
                            color = Color(0xFF2563EB),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }
                }
                "a_plus" -> {
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = Color(0xFF16A34A),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = "A+", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        }
                    }
                }
                "hundred" -> {
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = Color(0xFFEA580C),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = "100%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
                else -> {
                    Text(
                        text = sticker.emojiOrText,
                        fontSize = 34.sp
                    )
                }
            }
        }
    }
}

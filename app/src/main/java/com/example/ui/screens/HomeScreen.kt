package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun HomeScreen(
    onNewBoardClick: () -> Unit,
    onHistoryClick: () -> Unit
) {
    val context = LocalContext.current
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showRateDialog by remember { mutableStateOf(false) }
    var showRemoveAdsDialog by remember { mutableStateOf(false) }
    var isProUnlocked by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF18181A))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Gear settings icon top right
        IconButton(
            onClick = { showSettingsDialog = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .testTag("settings_button")
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = Color(0xFFCCCCCC),
                modifier = Modifier.size(28.dp)
            )
        }

        // Center Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // WhiteBoard Title
            Text(
                text = "WhiteBoard",
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(64.dp))

            // "+ New Board" Blue Button
            Button(
                onClick = onNewBoardClick,
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2979FF)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .testTag("new_board_button"),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Board",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "New Board",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // "🕒 History" Grey Button
            Surface(
                onClick = onHistoryClick,
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFF3E3E42),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .testTag("history_button")
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "History",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "History",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Rate and Share row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                // Rate Pill
                PillButton(
                    icon = Icons.Default.Star,
                    label = "Rate",
                    onClick = { showRateDialog = true }
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Share Pill
                PillButton(
                    icon = Icons.Default.Share,
                    label = "Share",
                    onClick = {
                        shareApp(context)
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Remove Ads Pill
            PillButton(
                icon = Icons.Default.Block,
                label = if (isProUnlocked) "Pro Active (Ad-Free)" else "Remove Ads",
                onClick = { showRemoveAdsDialog = true }
            )
        }
    }

    // Settings Dialog
    if (showSettingsDialog) {
        Dialog(onDismissRequest = { showSettingsDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF262628)),
                modifier = Modifier.padding(8.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("WhiteBoard Settings", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("App Version: 1.0.0 (Pro Ready)", color = Color(0xFFCCCCCC), fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Features:", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("• Multi-layer canvas with live preview\n• Smart shape recognition\n• Science Bohr atomic models\n• Formula quick-insert\n• PDF & Image export\n• 10 custom board textures", color = Color(0xFF9E9E9E), fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showSettingsDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2979FF)),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Close", color = Color.White)
                    }
                }
            }
        }
    }

    // Rate Dialog
    if (showRateDialog) {
        Dialog(onDismissRequest = { showRateDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF262628)),
                modifier = Modifier.padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Rate WhiteBoard", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Enjoying your whiteboard experience? Give us a 5-star rating!", color = Color(0xFFB0B0B0), fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(5) {
                            Icon(imageVector = Icons.Default.Star, contentDescription = "Star", tint = Color(0xFFFFD700), modifier = Modifier.size(32.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { showRateDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2979FF))
                    ) {
                        Text("Submit Rating", color = Color.White)
                    }
                }
            }
        }
    }

    // Remove Ads / Pro Dialog
    if (showRemoveAdsDialog) {
        Dialog(onDismissRequest = { showRemoveAdsDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF262628)),
                modifier = Modifier.padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(imageVector = Icons.Default.PlayCircleOutline, contentDescription = "Rewarded Ad", tint = Color(0xFF2979FF), modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Rewarded Test Ad", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Ad Unit: ca-app-pub-3940256099942544/5224354917\nWatch a short test ad to earn 24 hours of Pro Ad-Free Whiteboard access!",
                        color = Color(0xFFB0B0B0),
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            val activity = context as? android.app.Activity
                            if (activity != null) {
                                com.example.util.RewardedAdManager.showAd(
                                    activity = activity,
                                    onRewardEarned = { amount, type ->
                                        isProUnlocked = true
                                        android.widget.Toast.makeText(context, "🎉 Reward Earned! Pro Unlocked: +$amount $type", android.widget.Toast.LENGTH_LONG).show()
                                    },
                                    onAdClosed = {
                                        showRemoveAdsDialog = false
                                    }
                                )
                            } else {
                                isProUnlocked = true
                                showRemoveAdsDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2979FF)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Play")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Watch Rewarded Ad", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = {
                            isProUnlocked = true
                            showRemoveAdsDialog = false
                            android.widget.Toast.makeText(context, "Pro features enabled!", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Skip (Direct Unlock)", color = Color(0xFF9E9E9E), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PillButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF242426),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38383A))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color(0xFFE0E0E0),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFE0E0E0)
            )
        }
    }
}

private fun shareApp(context: Context) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, "Check out WhiteBoard app for clean drawing, math formulas, science diagrams, and PDF exports!")
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Share WhiteBoard")
    context.startActivity(shareIntent)
}

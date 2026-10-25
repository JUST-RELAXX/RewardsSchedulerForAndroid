package com.example.rewardsscheduler.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rewardsscheduler.theme.*

/**
 * Replicates the PC app's #globalPointsCanvas lavish container.
 * Displays total points earned today across all profiles with glowing yellow typography.
 * Holding (long-press) opens the PC-style "Reset global points & start fresh" menu.
 */
@Composable
fun GlobalPointsCanvas(
    totalPoints: Int,
    onResetGlobalPoints: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showResetMenu by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x40000000))
            .border(1.dp, Color(0x0FFFFFFF), RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        showResetMenu = true
                    },
                    onTap = {
                        if (showResetMenu) showResetMenu = false
                    }
                )
            }
            .padding(vertical = 18.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "GLOBAL POINTS EARNED TODAY",
                color = Color(0xFFB0BEC5),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )

            Spacer(Modifier.height(4.dp))

            Box(contentAlignment = Alignment.Center) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = totalPoints.toString(),
                        color = Color(0xFFFFEB3B),
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        lineHeight = 52.sp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "PTS",
                        color = Color(0xFFB0BEC5),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                // Popup menu matching PC #globalPointsContextMenu exactly (Image 5)
                this@Column.AnimatedVisibility(
                    visible = showResetMenu,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF1A2235),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A354A)),
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .clickable {
                                showResetMenu = false
                                showConfirmDialog = true
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = AppIcons.RotateCcw,
                                contentDescription = null,
                                tint = Color(0xFFFFEB3B),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Reset global points & start fresh",
                                color = Color(0xFFFFEB3B),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Across all your configured profiles",
                color = Color(0xFF78909C),
                fontSize = 11.sp
            )
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            containerColor = BgCard,
            title = {
                Text(
                    text = "Reset Global Points",
                    color = Accent,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to completely reset the Global Points tracker and start fresh for today?",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        onResetGlobalPoints()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEB3B)),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Reset",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showConfirmDialog = false },
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Border)
                ) {
                    Text(
                        text = "Cancel",
                        color = TextPrimary,
                        fontSize = 12.sp
                    )
                }
            }
        )
    }
}

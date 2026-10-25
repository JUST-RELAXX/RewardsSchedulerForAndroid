package com.example.rewardsscheduler.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.rewardsscheduler.data.AccountProfile
import com.example.rewardsscheduler.theme.*

@Composable
fun PacingChartDialog(
    profile: AccountProfile,
    onDismiss: () -> Unit
) {
    val delays = profile.recentPacingDelays

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            shape = RoundedCornerShape(16.dp),
            color = BgPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Pacing Analytics",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = profile.label,
                            color = Accent,
                            fontSize = 11.sp
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = AppIcons.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                if (delays.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(BgSecondary, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No search latency recorded yet.\nStart a search session to generate pacing data.",
                            color = TextMuted,
                            fontSize = 11.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(BgSecondary, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val maxVal = (delays.maxOrNull() ?: 30f).coerceAtLeast(10f) * 1.2f

                            // Draw horizontal grid lines
                            for (i in 0..4) {
                                val y = h - (i * (h / 4f))
                                drawLine(
                                    color = Color(0x2200B0FF),
                                    start = Offset(0f, y),
                                    end = Offset(w, y),
                                    strokeWidth = 1f
                                )
                            }

                            // Plot data points and connecting curve
                            if (delays.size > 1) {
                                val xStep = w / (delays.size - 1)
                                val path = Path()

                                delays.forEachIndexed { i, delayVal ->
                                    val x = i * xStep
                                    val y = h - (delayVal / maxVal * h)

                                    if (i == 0) path.moveTo(x, y)
                                    else path.lineTo(x, y)

                                    // Draw point
                                    drawCircle(
                                        color = Accent,
                                        radius = 3.5f,
                                        center = Offset(x, y)
                                    )
                                    drawCircle(
                                        color = Color.White,
                                        radius = 1.5f,
                                        center = Offset(x, y)
                                    )
                                }

                                drawPath(
                                    path = path,
                                    color = Accent,
                                    style = Stroke(width = 2.5f)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Delays recorded: ${delays.size} | Avg: ${if (delays.isNotEmpty()) String.format("%.1f", delays.average()) else "0"}s",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

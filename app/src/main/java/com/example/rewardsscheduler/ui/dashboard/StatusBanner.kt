package com.example.rewardsscheduler.ui.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rewardsscheduler.theme.*

/**
 * Replicates the PC app's .status-banner:
 * - Glowing status circle (Green = Ready/Done, Cyan = Searching, Yellow = Paused, Red = Error)
 * - Status text (e.g. "Ready — Select accounts and start your session", "Searching — 4 accounts running")
 * - Status timer / total points indicator
 * - 100% clean, ZERO emojis.
 */
@Composable
fun StatusBanner(
    statusText: String,
    totalPoints: Int = 0,
    elapsedText: String = "",
    modifier: Modifier = Modifier
) {
    val isSearching = statusText.contains("SEARCHING", ignoreCase = true) || statusText.contains("RUNNING", ignoreCase = true)
    val isPaused = statusText.contains("PAUSED", ignoreCase = true)
    val isDone = statusText.contains("COMPLETED", ignoreCase = true) || statusText.contains("DONE", ignoreCase = true)

    val dotColor by animateColorAsState(
        targetValue = when {
            isSearching -> Accent
            isPaused -> Warning
            isDone -> Success
            else -> Color(0xFF10B981) // PC app's exact ready green (#10b981)
        },
        label = "statusDotColor"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Glowing Dot + Status Text
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Status icon circle with glow
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(dotColor.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                }

                Spacer(Modifier.width(10.dp))

                Text(
                    text = statusText,
                    color = TextSecondary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }

            // Right: Optional Elapsed Time or Points pill
            if (elapsedText.isNotBlank()) {
                Text(
                    text = elapsedText,
                    color = Accent,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            } else if (totalPoints > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Accent.copy(alpha = 0.12f))
                        .border(1.dp, Accent.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "$totalPoints pts",
                        color = Accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

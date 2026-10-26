package com.example.rewardsscheduler.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rewardsscheduler.data.AccountProfile
import com.example.rewardsscheduler.theme.*

/**
 * Replicates the PC Electron app's Dashboard module (.dashboard-module) 1:1:
 * - Module Header: Cyan Grid icon + "Dashboard" text + Collapse toggle
 * - Holistic Points Container: Huge glowing yellow #ffeb3b points counter + "PTS" label
 *   (Shows ONLY points earned by profiles in the CURRENT session, in this session!)
 * - Subheader: "NUTSHELL (Live) (i)" with color legend dialog toggle + "X pts today"
 * - Nutshell list:
 *   - Status dots (Green: Active, Blue: Idle/Waiting, Red: Error/Stopped, Grey: Completed)
 *   - Profile display name / email with glowing text effect (matching PC text-shadow & pulse-glow)
 *   - Edit (pencil) action to change max points directly
 *   - Points/max display (e.g. 18/60)
 * - Zero emojis!
 */
@Composable
fun NutshellCard(
    profiles: List<AccountProfile>,
    sessionSearchCounts: Map<String, Int> = emptyMap(),
    sessionTotalPoints: Int,
    runningProfileIds: Set<String> = emptySet(),
    waitingProfileIds: Set<String> = emptySet(),
    onLocateProfile: (profileId: String) -> Unit,
    onEditMaxPoints: (AccountProfile) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showLegend by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(true) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, Border, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ═══════════════════════════════════════════════════
            // 1. Module Header (.module-header) with Collapse
            // ═══════════════════════════════════════════════════
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(AccentGlow),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = AppIcons.Grid,
                            contentDescription = null,
                            tint = Accent,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Text(
                        text = "Dashboard",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        letterSpacing = 0.3.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { isExpanded = !isExpanded }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(if (isExpanded) "Collapse" else "Expand", color = Accent, fontSize = 11.sp)
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // ═══════════════════════════════════════════════════
                    // 2. Holistic Points Container (.holistic-points-container)
                    // ═══════════════════════════════════════════════════
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = sessionTotalPoints.toString(),
                            color = Color(0xFFFFEB3B),
                            style = TextStyle(
                                shadow = Shadow(
                                    color = Color(0xFFFFEB3B).copy(alpha = 0.5f),
                                    offset = Offset(0f, 0f),
                                    blurRadius = 14f
                                ),
                                fontSize = 38.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.5).sp
                            )
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "PTS",
                            color = Color(0xFF90A4AE),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    // Divider (.module-subheader border-bottom)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.05f))
                    )

                    // ═══════════════════════════════════════════════════
                    // 3. Module Subheader: NUTSHELL (Live) (i)
                    // ═══════════════════════════════════════════════════
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "NUTSHELL (Live)",
                                color = Color(0xFFB0BEC5),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                letterSpacing = 0.3.sp
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "(i)",
                                color = Accent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { showLegend = !showLegend }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Nutshell Info Modal (Color Legend)
                    AnimatedVisibility(visible = showLegend) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = BgElevated,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Border)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Color Legend", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "✕",
                                        color = TextMuted,
                                        fontSize = 11.sp,
                                        modifier = Modifier.clickable { showLegend = false }
                                    )
                                }
                                LegendRow(color = Color(0xFF69F0AE), label = "GREEN", desc = "ACTIVELY SEARCHING!")
                                LegendRow(color = Color(0xFF00B0FF), label = "BLUE", desc = "IDLE-WAITING FOR DELAY TO PASS...")
                                LegendRow(color = Color(0xFFFF5252), label = "RED", desc = "ERROR/STOPPED!!")
                                LegendRow(color = Color(0xFFB0BEC5), label = "GREY", desc = "COMPLETED!")
                            }
                        }
                    }

                    // ═══════════════════════════════════════════════════
                    // 4. Nutshell List (.nutshell-list)
                    // ═══════════════════════════════════════════════════
                    if (profiles.isEmpty()) {
                        Text(
                            text = "No accounts in active session.",
                            color = TextMuted,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            profiles.forEach { profile ->
                                val sessionCount = sessionSearchCounts[profile.id] ?: profile.searchCount
                                val isRunning = runningProfileIds.contains(profile.id) || profile.inProgress
                                val isWaiting = waitingProfileIds.contains(profile.id)
                                NutshellProfileRow(
                                    profile = profile,
                                    sessionSearchCount = sessionCount,
                                    isRunning = isRunning,
                                    isWaiting = isWaiting,
                                    onClick = { onLocateProfile(profile.id) },
                                    onEditMaxPoints = { onEditMaxPoints(profile) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NutshellProfileRow(
    profile: AccountProfile,
    sessionSearchCount: Int,
    isRunning: Boolean,
    isWaiting: Boolean,
    onClick: () -> Unit,
    onEditMaxPoints: () -> Unit
) {
    val isDone = profile.searchesDone
    val isActivelySearching = isRunning && !isWaiting && !isDone
    val isWaitingInDelay = isRunning && isWaiting && !isDone
    val isStoppedOrHalted = sessionSearchCount > 0 && !isRunning && !isDone

    val statusColor = when {
        isDone -> Color(0xFFB0BEC5)           // Grey: Completed
        isActivelySearching -> Color(0xFF69F0AE) // Green: Actively searching
        isStoppedOrHalted -> Color(0xFFFF5252)  // Red: Stopped/Halted/Error
        else -> Color(0xFF00B0FF)             // Blue: Idle/Waiting
    }

    // Pulse animation for active searching (matching PC pulse-glow keyframes)
    val infiniteTransition = rememberInfiniteTransition(label = "pulseGlow")
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseProgress"
    )

    // Dynamic glowing text color matching PC styles: glow green ONLY during active searching!
    val textColor = when {
        isActivelySearching -> lerp(Color(0xFF4CAF50), Color(0xFF69F0AE), pulseProgress)
        isStoppedOrHalted -> Color(0xFFFF5252)
        isDone -> Color(0xFFB0BEC5)
        else -> Color(0xFF00B0FF)
    }

    // Dynamic text glow shadow matching PC: only glows green when actively searching! In between delays it remains blur blue
    val textShadow = when {
        isActivelySearching -> Shadow(
            color = Color(0xFF69F0AE).copy(alpha = 0.85f * pulseProgress),
            offset = Offset(0f, 0f),
            blurRadius = 14f * pulseProgress + 4f
        )
        isStoppedOrHalted -> Shadow(
            color = Color(0xFFFF5252).copy(alpha = 0.85f),
            offset = Offset(0f, 0f),
            blurRadius = 14f
        )
        !isDone -> Shadow(
            color = Color(0xFF00B0FF).copy(alpha = 0.5f),
            offset = Offset(0f, 0f),
            blurRadius = 8f
        )
        else -> null
    }

    val bulletColor = when {
        isActivelySearching -> Color(0xFF69F0AE).copy(alpha = pulseProgress)
        else -> statusColor
    }

    // Per-profile session points capped at maxPoints: min(points, maxPoints)
    val currentPoints = minOf(sessionSearchCount * 3, profile.maxPoints)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(Color.Black.copy(alpha = 0.2f))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Dot + Profile name/email with glowing text effect
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(bulletColor)
                )
                Spacer(Modifier.width(8.dp))
                val displayName = profile.email.ifBlank { profile.label }
                Text(
                    text = displayName,
                    color = textColor,
                    style = TextStyle(
                        shadow = textShadow,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Right: Points/max display + Edit pencil button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Points/max display (e.g. 18/60)
                Text(
                    text = "$currentPoints/${profile.maxPoints}",
                    color = Accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                // Edit pencil button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onEditMaxPoints() }
                        .padding(3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AppIcons.Edit,
                        contentDescription = "Edit Max Points",
                        tint = TextSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendRow(color: Color, label: String, desc: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = "$label: ",
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = desc,
            color = TextSecondary,
            fontSize = 10.sp
        )
    }
}

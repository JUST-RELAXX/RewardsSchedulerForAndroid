package com.example.rewardsscheduler.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
 * Replicates the PC Electron app's Controls module (.controls-module) 1:1:
 * - Module Header: Cyan Settings/Gear icon + "Controls" text
 * - Inline top-right action pills:
 *   - Fetch: Cyan download pill (unlocks API budget)
 *   - Stop: Amber pause pill (halts searching, windows stay open)
 *   - Exit: Red square pill (ends session)
 *   - Collapse: Module collapse/expand toggle
 * - Global actions below:
 *   - Refresh All: Ghost outlined button with refresh icon
 *   - START GLOBAL / RESUME GLOBAL SEARCH: Solid cyan #00b0ff button with white text
 * - Zero emojis! Uses clean vector icons.
 */
@Composable
fun ControlsRow(
    isSessionRunning: Boolean,
    isSessionPaused: Boolean,
    isFetchUnlocked: Boolean,
    selectedCount: Int,
    onFetchClick: () -> Unit,
    onStopClick: () -> Unit,
    onExitClick: () -> Unit,
    onRefreshAllClick: () -> Unit,
    onStartGlobalClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ═══════════════════════════════════════════════════
            // 1. Module Header (.module-header) with inline pills
            // ═══════════════════════════════════════════════════
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Header Left: Cyan icon + "Controls"
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = AppIcons.Settings,
                        contentDescription = null,
                        tint = Accent,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Controls",
                        color = Accent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.3.sp
                    )
                }

                // Header Right: Inline Pills (Fetch, Stop, Exit, Collapse)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Fetch Pill
                    CompactPillButton(
                        label = if (isFetchUnlocked) "Fetch Again" else "Fetch",
                        icon = AppIcons.Download,
                        borderColor = Accent.copy(alpha = 0.4f),
                        backgroundColor = if (isFetchUnlocked) Accent.copy(alpha = 0.25f) else Accent.copy(alpha = 0.12f),
                        contentColor = Accent,
                        onClick = onFetchClick
                    )

                    // Stop Pill
                    CompactPillButton(
                        label = "Stop",
                        icon = AppIcons.Pause,
                        borderColor = Color(0xFFFFB74D).copy(alpha = 0.4f),
                        backgroundColor = Color(0xFFFFB74D).copy(alpha = 0.12f),
                        contentColor = Color(0xFFFFB74D),
                        enabled = isSessionRunning && !isSessionPaused,
                        onClick = onStopClick
                    )

                    // Exit Pill
                    CompactPillButton(
                        label = "Exit",
                        icon = AppIcons.Square,
                        borderColor = Color(0xFFFF5252).copy(alpha = 0.4f),
                        backgroundColor = Color(0xFFFF5252).copy(alpha = 0.12f),
                        contentColor = Color(0xFFFF5252),
                        enabled = true,
                        onClick = onExitClick
                    )

                    // Collapse / Expand toggle
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { isExpanded = !isExpanded }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(if (isExpanded) "Collapse" else "Expand", color = Accent, fontSize = 11.sp)
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Divider separating header from actions (.module-header border-bottom)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.05f))
                    )

                    // ═══════════════════════════════════════════════════
                    // 2. Global Actions Row (.global-actions)
                    // ═══════════════════════════════════════════════════
                    Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Refresh All (Ghost button)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                        .clickable { onRefreshAllClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = AppIcons.Refresh,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Refresh All",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // START GLOBAL / RESUME Button (Solid #00b0ff)
                val isResume = isSessionRunning && isSessionPaused
                Button(
                    onClick = onStartGlobalClick,
                    modifier = Modifier
                        .weight(1.3f)
                        .height(36.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Accent,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = AppIcons.Play,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (isResume) "RESUME GLOBAL" else "START GLOBAL",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
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
private fun CompactPillButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    borderColor: Color,
    backgroundColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val alpha = if (enabled) 1f else 0.4f
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor.copy(alpha = backgroundColor.alpha * alpha))
            .border(1.dp, borderColor.copy(alpha = borderColor.alpha * alpha), RoundedCornerShape(6.dp))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 7.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor.copy(alpha = alpha),
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = label,
                color = contentColor.copy(alpha = alpha),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

package com.example.rewardsscheduler.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rewardsscheduler.data.LogEntry
import com.example.rewardsscheduler.data.LogLevel
import com.example.rewardsscheduler.theme.*

/**
 * Replicates the PC Electron app's Console module (.console-module) 1:1:
 * - Module Header: Cyan Terminal prompt icon (>_) + "Console" text
 * - Action buttons: Clear & Collapse
 * - Dark console body (#0f111a) with color-coded log entries:
 *   - Info: #81d4fa (light cyan)
 *   - Success: #69f0ae (bright green)
 *   - Warning: #ffd740 (yellow/amber)
 *   - Error: #ff5252 (red)
 * - Zero emojis!
 */
@Composable
fun ConsoleCard(
    logs: List<LogEntry>,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()

    // Auto-scroll to bottom on new log
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty() && isExpanded) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ═══════════════════════════════════════════════════
            // Module Header (.module-header)
            // ═══════════════════════════════════════════════════
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = AppIcons.Terminal,
                        contentDescription = null,
                        tint = Accent,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Console",
                        color = Accent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.3.sp
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "(${logs.size})",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Clear pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onClear() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("Clear", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }

                    // Collapse / Expand toggle
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { isExpanded = !isExpanded }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(if (isExpanded) "Collapse" else "Expand", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF0F111A))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
                        .padding(8.dp)
                ) {
                    if (logs.isEmpty()) {
                        Text(
                            text = "System initialized. Waiting for commands...",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            items(logs) { entry ->
                                ConsoleLogLine(entry)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConsoleLogLine(entry: LogEntry) {
    val messageColor = when (entry.level) {
        LogLevel.INFO -> Color(0xFF81D4FA)    // #81d4fa light cyan
        LogLevel.SUCCESS -> Color(0xFF69F0AE) // #69f0ae green
        LogLevel.WARN -> Color(0xFFFFD740)    // #ffd740 yellow
        LogLevel.ERROR -> Color(0xFFFF5252)   // #ff5252 red
        LogLevel.FALLBACK -> Color(0xFF78909C)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "[${entry.timestamp}]",
            color = Color(0xFF546E7A),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = entry.message,
            color = messageColor,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = 15.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

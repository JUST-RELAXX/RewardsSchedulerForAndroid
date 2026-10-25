package com.example.rewardsscheduler.ui.dashboard

import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.rewardsscheduler.data.AccountProfile
import com.example.rewardsscheduler.engine.PromptQueue
import com.example.rewardsscheduler.engine.SearchSessionEngine
import com.example.rewardsscheduler.theme.*
import com.example.rewardsscheduler.webview.WebViewManager

/**
 * Replicates the PC Electron app's .webview-wrapper 1:1:
 * - Header Bar (.webview-header):
 *   - Profile title/email in Cyan #00b0ff
 *   - Action buttons (.webview-header-buttons):
 *     - [Graph] Inline toggle for Advanced Search Analytics panel (opens below WebView, taking 30-35%)
 *     - [Config] Inline toggle for Auto Typer Config panel (opens below WebView, taking 35-40%)
 *     - [X] Red close button
 * - Interactive Controllable WebView:
 *   - Direct touch control with disallowInterceptTouchEvent to enable full scrolling, pulling, pinching
 * - Collapsible Inline Graph Panel directly beneath the WebView (.graph-panel-view)
 * - Collapsible Inline Config Panel directly beneath the WebView (.extension-panel)
 * - Zero popups upon clicking Graph or Config in session view!
 * - Zero emojis!
 */
@Composable
fun SessionTile(
    profile: AccountProfile,
    isCollapsed: Boolean,
    isLocateGlow: Boolean,
    webViewManager: WebViewManager,
    searchEngine: SearchSessionEngine,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val webView = remember { webViewManager.getOrCreateWebView(profile.id) }
    val isDesktopMode by webViewManager.desktopModeFlow.collectAsState()

    var isGraphExpanded by remember { mutableStateOf(false) }
    var isConfigExpanded by remember { mutableStateOf(false) }
    var isFullExpanded by remember { mutableStateOf(false) }
    var customDesireText by remember { mutableStateOf("") }
    var promptRefreshNotice by remember { mutableStateOf<Pair<List<String>, Int>?>(null) }

    val runningProfileIds by searchEngine.runningProfileIds.collectAsState()
    val isProfileActive = runningProfileIds.contains(profile.id)

    val glowBorderColor by animateColorAsState(
        targetValue = if (isLocateGlow) Accent else Border,
        animationSpec = tween(durationMillis = 300),
        label = "glowBorderColor"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(if (isLocateGlow) 2.dp else 1.dp, glowBorderColor, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard)
    ) {
        Column {
            // ═══════════════════════════════════════════════════
            // 1. Header Bar (.webview-header)
            // ═══════════════════════════════════════════════════
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0C101A))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Profile Name/Email in Cyan #00b0ff
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = profile.label,
                        color = Accent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Right: Action buttons (Expand, Graph, Config, Close)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Expand/Fit Pill Button (expands WebView height for easy bonus task completion)
                    WebviewPillButton(
                        label = if (isFullExpanded) "Fit" else "Expand",
                        icon = if (isFullExpanded) AppIcons.Minimize else AppIcons.Maximize,
                        borderColor = if (isFullExpanded) Color(0xFF69F0AE) else Accent.copy(alpha = 0.4f),
                        backgroundColor = if (isFullExpanded) Color(0xFF69F0AE).copy(alpha = 0.2f) else Accent.copy(alpha = 0.08f),
                        contentColor = if (isFullExpanded) Color(0xFF69F0AE) else Accent,
                        onClick = { isFullExpanded = !isFullExpanded }
                    )

                    // Graph Pill Button (toggles inline Graph panel below webview)
                    WebviewPillButton(
                        label = "Graph",
                        icon = AppIcons.Activity,
                        borderColor = if (isGraphExpanded) Accent else Accent.copy(alpha = 0.4f),
                        backgroundColor = if (isGraphExpanded) Accent.copy(alpha = 0.25f) else Accent.copy(alpha = 0.08f),
                        contentColor = Accent,
                        onClick = {
                            isGraphExpanded = !isGraphExpanded
                            if (isGraphExpanded) isConfigExpanded = false
                        }
                    )

                    // Config Pill Button (toggles inline Config panel below webview)
                    WebviewPillButton(
                        label = "Config",
                        icon = AppIcons.Sliders,
                        borderColor = if (isConfigExpanded) Accent else Accent.copy(alpha = 0.4f),
                        backgroundColor = if (isConfigExpanded) Accent.copy(alpha = 0.25f) else Accent.copy(alpha = 0.08f),
                        contentColor = Accent,
                        onClick = {
                            isConfigExpanded = !isConfigExpanded
                            if (isConfigExpanded) isGraphExpanded = false
                        }
                    )

                    // Close Pill Button (Red)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFFF4D4D).copy(alpha = 0.1f))
                            .border(1.dp, Color(0xFFFF4D4D).copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .clickable { onCloseClick() }
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = AppIcons.Close,
                            contentDescription = "Close",
                            tint = Color(0xFFFF4D4D),
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }

            // Divider separating header from webview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.08f))
            )

            // ═══════════════════════════════════════════════════
            // 2. Controllable WebView Container (Top ~60-70%)
            // ═══════════════════════════════════════════════════
            val webViewHeight = if (isCollapsed) {
                1.dp
            } else if (isFullExpanded) {
                460.dp
            } else if (isDesktopMode) {
                310.dp
            } else {
                240.dp
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(webViewHeight)
                    .background(Color(0xFF050505))
            ) {
                AndroidView(
                    factory = {
                        webView.parent?.let { parent ->
                            (parent as? ViewGroup)?.removeView(webView)
                        }
                        webView.layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        // Disallow parent Compose scroll interception so web contents scroll smoothly!
                        webView.setOnTouchListener { view, event ->
                            when (event.actionMasked) {
                                MotionEvent.ACTION_DOWN -> {
                                    view.parent?.requestDisallowInterceptTouchEvent(true)
                                    view.requestFocus()
                                }
                                MotionEvent.ACTION_MOVE -> {
                                    view.parent?.requestDisallowInterceptTouchEvent(true)
                                }
                                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                    view.parent?.requestDisallowInterceptTouchEvent(false)
                                }
                            }
                            false
                        }
                        webView
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // ═══════════════════════════════════════════════════
            // 3. Dedicated Inline Graph Panel (.graph-panel-view)
            // Opens directly BELOW WebView, occupying 30-35% height
            // ═══════════════════════════════════════════════════
            AnimatedVisibility(visible = isGraphExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F141E))
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.1f)
                        )
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Advanced Search Analytics",
                            color = Accent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Searches: ${profile.searchCount} / ${profile.totalTargetSearches.coerceAtLeast(1)}",
                            color = Accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Status: ${if (profile.searchesDone) "Completed" else if (profile.inProgress) "Searching" else "Idle"}",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "Points: ${profile.searchCount * 3} pts",
                            color = Color(0xFFFFEB3B),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Pacing delay latency graph
                    val delays = profile.recentPacingDelays
                    if (delays.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .background(Color(0xFF080C14), RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Analytics active — Real-time delays will plot on next search.",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .background(Color(0xFF080C14), RoundedCornerShape(6.dp))
                                .padding(6.dp)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height
                                val maxVal = (delays.maxOrNull() ?: 30f).coerceAtLeast(10f) * 1.2f

                                // Grid lines
                                for (i in 0..3) {
                                    val y = h - (i * (h / 3f))
                                    drawLine(
                                        color = Color(0x2200B0FF),
                                        start = Offset(0f, y),
                                        end = Offset(w, y),
                                        strokeWidth = 1f
                                    )
                                }

                                if (delays.size > 1) {
                                    val xStep = w / (delays.size - 1)
                                    val path = Path()

                                    delays.forEachIndexed { i, delayVal ->
                                        val x = i * xStep
                                        val y = h - (delayVal / maxVal * h)

                                        if (i == 0) path.moveTo(x, y)
                                        else path.lineTo(x, y)

                                        drawCircle(color = Accent, radius = 3f, center = Offset(x, y))
                                        drawCircle(color = Color.White, radius = 1.2f, center = Offset(x, y))
                                    }

                                    drawPath(
                                        path = path,
                                        color = Accent,
                                        style = Stroke(width = 2f)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Min: ${String.format("%.1f", delays.minOrNull() ?: 0f)}s",
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                            Text(
                                text = "Avg: ${String.format("%.1f", delays.average())}s",
                                color = Accent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Max: ${String.format("%.1f", delays.maxOrNull() ?: 0f)}s",
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            // ═══════════════════════════════════════════════════
            // 4. Dedicated Inline Config Panel (.extension-panel)
            // Opens directly BELOW WebView, occupying 35-40% height
            // ═══════════════════════════════════════════════════
            AnimatedVisibility(visible = isConfigExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F141E))
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.1f)
                        )
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "AUTO TYPER (v2.3)",
                        color = Accent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    // Prompt Refresh Notification Box (Matching PC Image 3)
                    promptRefreshNotice?.let { (topics, queryCount) ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF0D233A).copy(alpha = 0.85f))
                                .border(1.dp, Color(0xFF00B0FF).copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = "↻ Auto-selected: ${topics.joinToString(", ")}",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 11.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Text(
                                    text = "Loaded $queryCount queries",
                                    color = Color(0xFF69F0AE),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    // Searching vs Offline / Start vs Stop (Matching PC Images 1 & 2)
                    if (isProfileActive) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "SEARCHING",
                                color = Color(0xFF00B0FF),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )

                            Button(
                                onClick = {
                                    searchEngine.pauseProfile(profile.id)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Accent,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Stop",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (profile.searchesDone) "Done" else "Offline",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            Button(
                                onClick = {
                                    searchEngine.unlockFetchGate()
                                    searchEngine.startProfile(profile.id)
                                },
                                modifier = Modifier.height(30.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Accent,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Start",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Progress Section (.progress-section)
                    val currentTarget = profile.totalTargetSearches.coerceAtLeast(1)
                    val progressFloat = (profile.searchCount.toFloat() / currentTarget).coerceIn(0f, 1f)
                    val progressPct = (progressFloat * 100).toInt()

                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Search Progress", color = Color(0xFFB0BEC5), fontSize = 11.sp)
                            Text("$progressPct%", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        LinearProgressIndicator(
                            progress = { progressFloat },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = Accent,
                            trackColor = Color(0x3300B0FF)
                        )
                    }

                    // Current Query Box (.current-query-box)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Current Query:", color = Color(0xFFB0BEC5), fontSize = 10.sp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF080C14), RoundedCornerShape(4.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = profile.currentQuery.ifEmpty { "Ready..." },
                                color = Color(0xFF00E5FF),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Stats Section (.stats-section)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Done: ${profile.searchCount}",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "Target: ${profile.totalTargetSearches}",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "Remaining: ${(profile.totalTargetSearches - profile.searchCount).coerceAtLeast(0)}",
                            color = Accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Custom Topic Input Row (+ Add your desire...) matching Image 3
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(30.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF080C14))
                                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (customDesireText.isEmpty()) {
                                Text(
                                    text = "+ Add your desire...",
                                    color = Color.White.copy(alpha = 0.35f),
                                    fontSize = 11.sp
                                )
                            }
                            BasicTextField(
                                value = customDesireText,
                                onValueChange = { customDesireText = it },
                                singleLine = true,
                                textStyle = TextStyle(color = Color.White, fontSize = 11.sp),
                                cursorBrush = SolidColor(Accent),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Box(
                            modifier = Modifier
                                .height(30.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Accent)
                                .clickable { customDesireText = "" }
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Add",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Refresh Prompts Button matching PC Image 3
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(30.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Transparent)
                            .border(1.dp, Accent, RoundedCornerShape(4.dp))
                            .clickable {
                                val selectedTopics = if (profile.selectedCategories.isNotEmpty()) {
                                    profile.selectedCategories.shuffled().take(3)
                                } else {
                                    PromptQueue.DEFAULT_TOPICS.shuffled().take(3)
                                }
                                promptRefreshNotice = Pair(selectedTopics, 25)
                                searchEngine.addLog(
                                    "[${profile.label}] Auto-selected: ${selectedTopics.joinToString(", ")} - Loaded 25 queries",
                                    "success"
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Refresh Prompts",
                            color = Accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WebviewPillButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    borderColor: Color,
    backgroundColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(11.dp)
            )
            Text(
                text = label,
                color = contentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

package com.example.rewardsscheduler.ui.dashboard

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rewardsscheduler.data.AccountProfile
import com.example.rewardsscheduler.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Replicates the PC app's .account-card 1:1.
 * Features:
 * - 3dp colored top bar
 * - Square checkbox with checkmark
 * - Status badge (PENDING / SEARCHING / SEARCHES DONE)
 * - 3-dots menu button & Long-Press (3-second hold / right click) Context Menu:
 *   - "Mark as done" when pending
 *   - "Mark as pending" when searches done
 * - Name + dir
 * - Progress bar (executed/total points)
 * - Online timer / Done at time
 * - Quick Action buttons for Settings and Delete
 * - Zero emojis!
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AccountCard(
    profile: AccountProfile,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onSignInClick: () -> Unit = {},
    onConfigClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onMarkDone: () -> Unit = {},
    onMarkPending: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDone = profile.searchesDone
    val isSearching = profile.inProgress

    val currentPoints = profile.searchCount * 3
    val totalPoints = profile.maxPoints
    val progressFraction = when {
        isDone -> 1f
        totalPoints > 0 -> (currentPoints.toFloat() / totalPoints).coerceIn(0f, 1f)
        else -> 0f
    }

    val lightGreen = Color(0xFF00E676)
    val darkGreen = Color(0xFF1B5E20)
    val darkGreenBorder = Color(0xFF2E7D32)

    // 3dp top accent bar color
    val topBarColor = when {
        isSearching -> Warning
        isDone && isSelected -> lightGreen
        isDone && !isSelected -> darkGreenBorder
        isSelected -> Accent
        else -> Color(0x33FFFFFF)
    }

    // Card border
    val cardBorderColor = when {
        isSearching -> Warning
        isDone && isSelected -> lightGreen
        isDone && !isSelected -> darkGreenBorder
        isSelected -> Accent
        else -> Border
    }

    val cardBorderWidth = when {
        isSearching -> 1.5.dp
        isDone && isSelected -> 2.dp
        isDone && !isSelected -> 1.dp
        isSelected -> 1.5.dp
        else -> 1.dp
    }

    var showContextMenu by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    val completedTimeStr = remember(profile.completedAt) {
        profile.completedAt?.let {
            SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(it))
        }
    }

    Box(modifier = modifier) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(
                    width = cardBorderWidth,
                    color = cardBorderColor,
                    shape = RoundedCornerShape(12.dp)
                )
                .combinedClickable(
                    onClick = { onToggleSelect() },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showContextMenu = true
                    }
                ),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    isDone && isSelected -> lightGreen.copy(alpha = 0.08f)
                    isSelected -> BgCardHover
                    isDone && !isSelected -> BgCard.copy(alpha = 0.75f)
                    else -> BgCard
                }
            )
        ) {
            Column {
                // 3dp Top Accent Bar (Replicating .account-card::before)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(topBarColor)
                )

                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Card Header: Checkbox + Status Badge + 3-dots Menu Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Square Checkbox (.card-checkbox)
                        val checkboxBg = when {
                            isDone && isSelected -> lightGreen
                            isDone && !isSelected -> darkGreen
                            isSelected -> Accent
                            else -> Color.Transparent
                        }

                        val checkboxBorder = when {
                            isDone && isSelected -> lightGreen
                            isDone && !isSelected -> darkGreenBorder
                            isSelected -> Accent
                            else -> TextMuted
                        }

                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(checkboxBg)
                                .border(
                                    1.5.dp,
                                    checkboxBorder,
                                    RoundedCornerShape(4.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected || isDone) {
                                Icon(
                                    imageVector = AppIcons.Check,
                                    contentDescription = null,
                                    tint = if (isDone && !isSelected) Color(0xFF81C784) else Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }

                        // Right side of header: Status Badge + 3-Dots Menu Trigger
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val (badgeBg, badgeColor, badgeText) = when {
                                isDone -> Triple(SuccessGlow, Success, "SEARCHES DONE")
                                isSearching -> Triple(WarningGlow, Warning, "SEARCHING")
                                else -> Triple(BgElevated, TextMuted, "PENDING")
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(badgeBg)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = badgeText,
                                    color = badgeColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            // 3-dots context menu button
                            IconButton(
                                onClick = { showContextMenu = true },
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(
                                    imageVector = AppIcons.MoreVertical,
                                    contentDescription = "Options",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    // Card Name & Directory
                    Column {
                        Text(
                            text = profile.label,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = profile.email.ifBlank { profile.id },
                            color = TextMuted,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Progress Track + Text (.card-progress)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = when {
                                isDone -> Success
                                isSearching -> Warning
                                else -> Accent
                            },
                            trackColor = BgPrimary
                        )

                        Text(
                            text = if (isDone) "$totalPoints/$totalPoints" else "$currentPoints/$totalPoints",
                            color = if (isDone) Success else TextMuted,
                            fontSize = 11.sp,
                            fontWeight = if (isDone) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }

                    // Card Meta: Online Timer / Done At + Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Online timer / Completed time
                        Text(
                            text = if (isDone && completedTimeStr != null) {
                                "Done at $completedTimeStr"
                            } else if (profile.onlineTimeDone) {
                                "30m Done"
                            } else {
                                "${profile.onlineMinutes}m / 30m"
                            },
                            color = if (isDone && completedTimeStr != null) Success else TextMuted,
                            fontSize = 11.sp,
                            fontWeight = if (isDone && completedTimeStr != null) FontWeight.Medium else FontWeight.Normal
                        )

                        // Quick Actions: Config / Delete
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onConfigClick,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = AppIcons.Settings,
                                    contentDescription = "Settings",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            IconButton(
                                onClick = onDeleteClick,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = AppIcons.Close,
                                    contentDescription = "Delete",
                                    tint = TextMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Context Menu matching PC right-click popup (.profileContextMenu)
        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false },
            modifier = Modifier
                .background(Color(0xFF1A2235))
                .border(1.dp, Color(0xFF2A354A), RoundedCornerShape(8.dp))
        ) {
            if (!isDone) {
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = AppIcons.Check,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Mark as done",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    },
                    onClick = {
                        showContextMenu = false
                        onMarkDone()
                    }
                )
            } else {
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = AppIcons.Close,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Mark as pending",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    },
                    onClick = {
                        showContextMenu = false
                        onMarkPending()
                    }
                )
            }
        }
    }
}

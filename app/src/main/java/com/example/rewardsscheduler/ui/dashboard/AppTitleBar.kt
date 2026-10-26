package com.example.rewardsscheduler.ui.dashboard

import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rewardsscheduler.R
import com.example.rewardsscheduler.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Replicates the PC Electron app's Custom Title Bar (.titlebar):
 * - MS Rewards Scheduler title with icon
 * - v2.0 badge
 * - Live clock in center/right
 * - Two-way seamless toggle:
 *   - When in Session view: [← Profiles] on top left
 *   - When in Profiles view: [Session →] / [Live Session →] on top right
 */
@Composable
fun AppTitleBar(
    isSessionView: Boolean,
    isSessionRunning: Boolean,
    canNavigateToSession: Boolean = true,
    onNavigateBack: () -> Unit,
    onNavigateToSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("hh:mm:ss a", Locale.US)
        while (true) {
            currentTime = timeFormat.format(Date())
            delay(1000)
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = BgSecondary,
        tonalElevation = 2.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Back button (when in Session) OR Title + Badge (when in Profiles)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isSessionView) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = BgElevated,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Accent.copy(alpha = 0.5f)),
                            modifier = Modifier.clickable { onNavigateBack() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = AppIcons.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Accent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Profiles",
                                    color = Accent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        // PC app's titlebar-icon
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = "App Logo",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        text = "MS Rewards Scheduler",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    )
                }

                // Right: Clock and Session toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Live clock (.live-clock)
                    Text(
                        text = currentTime.ifEmpty { "--:--:-- --" },
                        color = TextSecondary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )

                    if (!isSessionView && canNavigateToSession) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = BgElevated,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSessionRunning) Accent else Color.White.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier.clickable { onNavigateToSession() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (isSessionRunning) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Accent)
                                    )
                                }
                                Text(
                                    text = if (isSessionRunning) "Live Session" else "Session",
                                    color = if (isSessionRunning) Accent else TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = AppIcons.ArrowForward,
                                    contentDescription = "To Session",
                                    tint = if (isSessionRunning) Accent else TextPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Bottom border line matching PC titlebar border
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Border)
            )
        }
    }
}
